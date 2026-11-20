package com.janmanch.video;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/** Response payloads for the video endpoints. */
public final class VideoDtos {

    private VideoDtos() {
    }

    /** One card in a feed or search result. Serializable for the Redis cache. */
    public record VideoSummary(Long id, String title, String thumbnailUrl, Long channelId,
                               String channelName, long viewCount, Integer durationSeconds,
                               Instant uploadedAt) implements Serializable {
    }

    /** Everything the watch page needs. */
    public record VideoDetails(Long id, String title, String description, List<String> tags,
                               String status, String streamUrl, String thumbnailUrl,
                               Long channelId, String channelName, long subscriberCount,
                               long viewCount, long likes, long dislikes, Integer durationSeconds,
                               Instant uploadedAt, String myReaction, boolean subscribed) {
    }

    /** Returned right after an upload; the video is still PROCESSING at this point. */
    public record UploadResponse(Long id, String title, String status) {
    }

    /** One row in the creator studio: includes videos that are not READY yet. */
    public record StudioVideo(Long id, String title, String status, String thumbnailUrl,
                              Integer durationSeconds, long viewCount, long likes, long comments,
                              Instant uploadedAt) {
    }

    public record HistoryItem(VideoSummary video, Instant watchedAt) {
    }
}
