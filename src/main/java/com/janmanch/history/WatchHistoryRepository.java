package com.janmanch.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    @EntityGraph(attributePaths = {"video", "video.channel"})
    Page<WatchHistory> findByUserId(Long userId, Pageable pageable);

    void deleteByVideoId(Long videoId);
}
