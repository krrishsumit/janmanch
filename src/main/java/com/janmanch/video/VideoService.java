package com.janmanch.video;

import com.janmanch.channel.Channel;
import com.janmanch.channel.ChannelRepository;
import com.janmanch.channel.SubscriptionId;
import com.janmanch.channel.SubscriptionRepository;
import com.janmanch.common.ApiException;
import com.janmanch.common.PageResponse;
import com.janmanch.engagement.CommentRepository;
import com.janmanch.engagement.LikeId;
import com.janmanch.engagement.LikeRepository;
import com.janmanch.engagement.LikeType;
import com.janmanch.history.WatchHistory;
import com.janmanch.history.WatchHistoryRepository;
import com.janmanch.processing.VideoTopics;
import com.janmanch.storage.StorageService;
import com.janmanch.video.VideoDtos.HistoryItem;
import com.janmanch.video.VideoDtos.StudioVideo;
import com.janmanch.video.VideoDtos.UploadResponse;
import com.janmanch.video.VideoDtos.VideoDetails;
import com.janmanch.video.VideoDtos.VideoSummary;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int TRENDING_SIZE = 20;

    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final WatchHistoryRepository historyRepository;
    private final StorageService storage;
    private final ViewCountService viewCountService;
    private final KafkaTemplate<String, String> kafka;

    public VideoService(VideoRepository videoRepository, ChannelRepository channelRepository,
                        SubscriptionRepository subscriptionRepository, LikeRepository likeRepository,
                        CommentRepository commentRepository, WatchHistoryRepository historyRepository,
                        StorageService storage, ViewCountService viewCountService,
                        KafkaTemplate<String, String> kafka) {
        this.videoRepository = videoRepository;
        this.channelRepository = channelRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.storage = storage;
        this.viewCountService = viewCountService;
        this.kafka = kafka;
    }

    // ---------- Upload ----------

    /**
     * Stores the file, saves the video as PROCESSING and hands the heavy work
     * (thumbnail, duration) to the Kafka consumer, so the request returns quickly.
     * Not transactional on purpose: the row must be committed before the event is sent.
     */
    public UploadResponse upload(Long userId, MultipartFile file, String title, String description,
                                 String tags) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A video file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only video files can be uploaded");
        }
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required (up to 200 characters)");
        }
        Channel channel = channelRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Channel not found"));

        String key = "videos/" + UUID.randomUUID() + "." + extensionOf(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            storage.store(key, in, file.getSize(), contentType);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the video file");
        }

        Video video = new Video();
        video.setChannel(channel);
        video.setTitle(title.trim());
        video.setDescription(description);
        video.setTags(parseTags(tags));
        video.setS3Key(key);
        video = videoRepository.save(video);

        kafka.send(VideoTopics.UPLOADED, String.valueOf(video.getId()), String.valueOf(video.getId()));
        return new UploadResponse(video.getId(), video.getTitle(), video.getStatus().name());
    }

    // ---------- Reading ----------

    /** Home feed: newest READY videos. Cached in Redis for 60 seconds. */
    @Cacheable(cacheNames = "feed", key = "#page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResponse<VideoSummary> feed(int page, int size) {
        return PageResponse.from(videoRepository
                .findByStatus(VideoStatus.READY, newestFirst(page, size))
                .map(this::toSummary));
    }

    /** Most viewed READY videos. Cached in Redis for 60 seconds. */
    @Cacheable(cacheNames = "trending")
    @Transactional(readOnly = true)
    public PageResponse<VideoSummary> trending() {
        Pageable pageable = PageRequest.of(0, TRENDING_SIZE, Sort.by(Sort.Direction.DESC, "viewCount"));
        return PageResponse.from(videoRepository.findByStatus(VideoStatus.READY, pageable)
                .map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoSummary> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Search text is required");
        }
        return PageResponse.from(videoRepository
                .search(query.trim(), VideoStatus.READY, newestFirst(page, size))
                .map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoSummary> channelVideos(Long channelId, int page, int size) {
        return PageResponse.from(videoRepository
                .findByChannelIdAndStatus(channelId, VideoStatus.READY, newestFirst(page, size))
                .map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoSummary> subscriptionFeed(Long userId, int page, int size) {
        return PageResponse.from(videoRepository
                .findSubscriptionFeed(userId, VideoStatus.READY, newestFirst(page, size))
                .map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoryItem> history(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clamp(size),
                Sort.by(Sort.Direction.DESC, "watchedAt"));
        return PageResponse.from(historyRepository.findByUserId(userId, pageable)
                .map(h -> new HistoryItem(toSummary(h.getVideo()), h.getWatchedAt())));
    }

    /** The logged-in user's own videos in every status, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<StudioVideo> myVideos(Long userId, int page, int size) {
        Channel channel = channelRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Channel not found"));
        return PageResponse.from(videoRepository.findByChannelId(channel.getId(), newestFirst(page, size))
                .map(video -> new StudioVideo(
                        video.getId(), video.getTitle(), video.getStatus().name(),
                        video.getThumbnailKey() != null ? storage.urlFor(video.getThumbnailKey()) : null,
                        video.getDurationSeconds(), video.getViewCount(),
                        likeRepository.countByIdVideoIdAndType(video.getId(), LikeType.LIKE),
                        commentRepository.countByVideoId(video.getId()),
                        video.getUploadedAt())));
    }

    /** Full details for the watch page. userId is null for visitors who are not logged in. */
    @Transactional(readOnly = true)
    public VideoDetails details(Long videoId, Long userId) {
        Video video = videoRepository.findWithChannelById(videoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Video not found"));
        Channel channel = video.getChannel();

        String myReaction = null;
        boolean subscribed = false;
        if (userId != null) {
            myReaction = likeRepository.findById(new LikeId(userId, videoId))
                    .map(like -> like.getType().name()).orElse(null);
            subscribed = subscriptionRepository.existsById(new SubscriptionId(userId, channel.getId()));
        }

        boolean ready = video.getStatus() == VideoStatus.READY;
        return new VideoDetails(
                video.getId(), video.getTitle(), video.getDescription(),
                video.getTags().stream().sorted().toList(),
                video.getStatus().name(),
                ready && video.getS3Key() != null ? storage.urlFor(video.getS3Key()) : null,
                video.getThumbnailKey() != null ? storage.urlFor(video.getThumbnailKey()) : null,
                channel.getId(), channel.getName(),
                subscriptionRepository.countByIdChannelId(channel.getId()),
                video.getViewCount(),
                likeRepository.countByIdVideoIdAndType(videoId, LikeType.LIKE),
                likeRepository.countByIdVideoIdAndType(videoId, LikeType.DISLIKE),
                video.getDurationSeconds(), video.getUploadedAt(), myReaction, subscribed);
    }

    // ---------- Views ----------

    @Transactional
    public void recordView(Long videoId, Long userId) {
        if (!videoRepository.existsById(videoId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Video not found");
        }
        viewCountService.increment(videoId);
        if (userId != null) {
            WatchHistory entry = new WatchHistory();
            entry.setUserId(userId);
            entry.setVideo(videoRepository.getReferenceById(videoId));
            historyRepository.save(entry);
        }
    }

    // ---------- Delete ----------

    @CacheEvict(cacheNames = {"feed", "trending"}, allEntries = true)
    @Transactional
    public void delete(Long videoId, Long userId) {
        Video video = videoRepository.findWithChannelById(videoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Video not found"));
        if (!video.getChannel().getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can delete only your own videos");
        }
        likeRepository.deleteByIdVideoId(videoId);
        commentRepository.deleteByVideoId(videoId);
        historyRepository.deleteByVideoId(videoId);
        videoRepository.delete(video);

        if (video.getS3Key() != null) {
            storage.delete(video.getS3Key());
        }
        if (video.getThumbnailKey() != null) {
            storage.delete(video.getThumbnailKey());
        }
    }

    // ---------- Helpers ----------

    private VideoSummary toSummary(Video video) {
        return new VideoSummary(
                video.getId(), video.getTitle(),
                video.getThumbnailKey() != null ? storage.urlFor(video.getThumbnailKey()) : null,
                video.getChannel().getId(), video.getChannel().getName(),
                video.getViewCount(), video.getDurationSeconds(), video.getUploadedAt());
    }

    private Pageable newestFirst(int page, int size) {
        return PageRequest.of(Math.max(page, 0), clamp(size), Sort.by(Sort.Direction.DESC, "uploadedAt"));
    }

    private int clamp(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    /** "Music, Live , music" becomes {"music", "live"}; at most 10 tags of 50 characters. */
    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(tags.split(","))
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isEmpty() && tag.length() <= 50)
                .limit(10)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Keeps only a short alphanumeric extension; anything else becomes "mp4". */
    private String extensionOf(String filename) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0) {
                String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.matches("[a-z0-9]{1,5}")) {
                    return ext;
                }
            }
        }
        return "mp4";
    }
}
