package com.janmanch.channel;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {

    long countByIdChannelId(Long channelId);

    List<Subscription> findByIdUserIdOrderBySubscribedAtDesc(Long userId);
}
