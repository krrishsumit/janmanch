package com.janmanch.engagement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Composite key: a user can react to a video only once. */
@Embeddable
public class LikeId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "video_id")
    private Long videoId;

    public LikeId() {
    }

    public LikeId(Long userId, Long videoId) {
        this.userId = userId;
        this.videoId = videoId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getVideoId() {
        return videoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LikeId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId) && Objects.equals(videoId, other.videoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, videoId);
    }
}
