package com.janmanch.channel;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Composite key: a user subscribes to a channel at most once. */
@Embeddable
public class SubscriptionId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "channel_id")
    private Long channelId;

    public SubscriptionId() {
    }

    public SubscriptionId(Long userId, Long channelId) {
        this.userId = userId;
        this.channelId = channelId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getChannelId() {
        return channelId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId) && Objects.equals(channelId, other.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, channelId);
    }
}
