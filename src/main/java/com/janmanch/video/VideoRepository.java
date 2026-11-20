package com.janmanch.video;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long> {

    // The entity graph loads the channel in the same query (avoids one query per video).
    @EntityGraph(attributePaths = "channel")
    Page<Video> findByStatus(VideoStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "channel")
    Page<Video> findByChannelIdAndStatus(Long channelId, VideoStatus status, Pageable pageable);

    /** All of a channel's videos in any status, for the creator studio. */
    @EntityGraph(attributePaths = "channel")
    Page<Video> findByChannelId(Long channelId, Pageable pageable);

    @EntityGraph(attributePaths = "channel")
    Optional<Video> findWithChannelById(Long id);

    /** Matches the search text against the title, or exactly against a tag. */
    @EntityGraph(attributePaths = "channel")
    @Query(value = """
            select distinct v from Video v left join v.tags t
            where v.status = :status
              and (lower(v.title) like lower(concat('%', :q, '%')) or lower(t) = lower(:q))
            """,
            countQuery = """
            select count(distinct v) from Video v left join v.tags t
            where v.status = :status
              and (lower(v.title) like lower(concat('%', :q, '%')) or lower(t) = lower(:q))
            """)
    Page<Video> search(@Param("q") String q, @Param("status") VideoStatus status, Pageable pageable);

    /** Videos from the channels a user is subscribed to. */
    @EntityGraph(attributePaths = "channel")
    @Query("""
            select v from Video v
            where v.status = :status
              and v.channel.id in (select s.id.channelId from Subscription s where s.id.userId = :userId)
            """)
    Page<Video> findSubscriptionFeed(@Param("userId") Long userId, @Param("status") VideoStatus status,
                                     Pageable pageable);

    @Modifying
    @Query("update Video v set v.viewCount = v.viewCount + :views where v.id = :id")
    int addViews(@Param("id") Long id, @Param("views") long views);
}
