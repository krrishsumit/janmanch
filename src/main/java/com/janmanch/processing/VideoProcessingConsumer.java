package com.janmanch.processing;

import com.janmanch.storage.StorageService;
import com.janmanch.video.Video;
import com.janmanch.video.VideoRepository;
import com.janmanch.video.VideoStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Runs after an upload, outside the HTTP request: reads the video's duration,
 * creates a thumbnail and marks the video READY so it appears in feeds.
 * On start-up it also fills in thumbnails for older videos that have none.
 */
@Component
public class VideoProcessingConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingConsumer.class);

    private final VideoRepository videoRepository;
    private final StorageService storage;
    private final FfmpegService ffmpeg;

    public VideoProcessingConsumer(VideoRepository videoRepository, StorageService storage,
                                   FfmpegService ffmpeg) {
        this.videoRepository = videoRepository;
        this.storage = storage;
        this.ffmpeg = ffmpeg;
    }

    @KafkaListener(topics = VideoTopics.UPLOADED)
    public void onVideoUploaded(String videoIdText) {
        Long videoId;
        try {
            videoId = Long.valueOf(videoIdText);
        } catch (NumberFormatException ex) {
            log.warn("Ignoring malformed video event: {}", videoIdText);
            return;
        }
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || video.getStatus() != VideoStatus.PROCESSING) {
            return; // Deleted meanwhile, or already handled (Kafka can deliver twice).
        }
        try {
            addMediaDetails(video);
            video.setStatus(VideoStatus.READY);
        } catch (IOException ex) {
            log.error("Processing failed for video {}", videoId, ex);
            video.setStatus(VideoStatus.FAILED);
        }
        videoRepository.save(video);
        log.info("Video {} is now {}", videoId, video.getStatus());
    }

    /** After start-up, creates thumbnails for ready videos that were processed without FFmpeg. */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillThumbnails() {
        Thread worker = new Thread(() -> {
            if (!ffmpeg.isAvailable()) {
                return;
            }
            List<Video> missing = videoRepository.findTop50ByStatusAndThumbnailKeyIsNull(VideoStatus.READY);
            int done = 0;
            for (Video video : missing) {
                try {
                    if (addMediaDetails(video)) {
                        videoRepository.save(video);
                        done++;
                    }
                } catch (IOException | RuntimeException ex) {
                    log.warn("Could not create a thumbnail for video {}", video.getId(), ex);
                }
            }
            if (!missing.isEmpty()) {
                log.info("Created thumbnails for {} of {} older videos", done, missing.size());
            }
        }, "thumbnail-backfill");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Sets the duration and thumbnail on the video (not saved).
     * Returns true if a thumbnail was created. Throws only if the file cannot be read.
     */
    private boolean addMediaDetails(Video video) throws IOException {
        if (video.getS3Key() == null || !ffmpeg.isAvailable()) {
            return false; // The video is still playable; it just has no thumbnail.
        }
        Path source = storage.fetchToLocal(video.getS3Key());
        Path thumbnail = null;
        try {
            ffmpeg.probeDurationSeconds(source).ifPresent(video::setDurationSeconds);
            thumbnail = Files.createTempFile("janmanch-thumb-", ".jpg");
            if (!ffmpeg.createThumbnail(source, thumbnail)) {
                log.warn("No thumbnail created for video {}", video.getId());
                return false;
            }
            String key = "thumbnails/" + video.getId() + ".jpg";
            try (InputStream in = Files.newInputStream(thumbnail)) {
                storage.store(key, in, Files.size(thumbnail), "image/jpeg");
            }
            video.setThumbnailKey(key);
            return true;
        } finally {
            storage.releaseLocal(source);
            if (thumbnail != null) {
                Files.deleteIfExists(thumbnail);
            }
        }
    }
}
