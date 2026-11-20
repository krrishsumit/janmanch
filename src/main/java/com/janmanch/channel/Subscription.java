package com.janmanch.channel;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "subscriptions",
        indexes = @Index(name = "idx_subscriptions_channel", columnList = "channel_id"))
public class Subscription {

    @EmbeddedId
    private SubscriptionId id;

    @Column(name = "subscribed_at", nullable = false)
    private Instant subscribedAt = Instant.now();

    public Subscription() {
    }

    public Subscription(SubscriptionId id) {
        this.id = id;
    }

    public SubscriptionId getId() {
        return id;
    }

    public void setId(SubscriptionId id) {
        this.id = id;
    }

    public Instant getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(Instant subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
}
