package com.janmanch.processing;

import com.janmanch.storage.StorageService;
import com.janmanch.video.Video;
import com.janmanch.video.VideoRepository;
import com.janmanch.video.VideoStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Runs after an upload, outside the HTTP request: reads the video's duration,
 * creates a thumbnail and marks the video READY so it appears in feeds.
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

        Path source = null;
        Path thumbnail = null;
        try {
            source = storage.fetchToLocal(video.getS3Key());
            ffmpeg.probeDurationSeconds(source).ifPresent(video::setDurationSeconds);

            thumbnail = Files.createTempFile("janmanch-thumb-", ".jpg");
            if (ffmpeg.createThumbnail(source, thumbnail) && Files.size(thumbnail) > 0) {
                String key = "thumbnails/" + videoId + ".jpg";
                try (InputStream in = Files.newInputStream(thumbnail)) {
                    storage.store(key, in, Files.size(thumbnail), "image/jpeg");
                }
                video.setThumbnailKey(key);
            } else {
                // The video is still playable, so publish it without a thumbnail.
                log.warn("No thumbnail created for video {}", videoId);
            }
            video.setStatus(VideoStatus.READY);
        } catch (IOException ex) {
            log.error("Processing failed for video {}", videoId, ex);
            video.setStatus(VideoStatus.FAILED);
        } finally {
            if (source != null) {
                storage.releaseLocal(source);
            }
            deleteQuietly(thumbnail);
        }
        videoRepository.save(video);
        log.info("Video {} is now {}", videoId, video.getStatus());
    }

    private void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // A leftover temp file is harmless.
            }
        }
    }
}
