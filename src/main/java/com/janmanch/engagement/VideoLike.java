package com.janmanch.engagement;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "likes", indexes = @Index(name = "idx_likes_video_type", columnList = "video_id, type"))
public class VideoLike {

    @EmbeddedId
    private LikeId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LikeType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public LikeId getId() {
        return id;
    }

    public void setId(LikeId id) {
        this.id = id;
    }

    public LikeType getType() {
        return type;
    }

    public void setType(LikeType type) {
        this.type = type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
