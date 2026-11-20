package com.janmanch.engagement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<VideoLike, LikeId> {

    long countByIdVideoIdAndType(Long videoId, LikeType type);

    void deleteByIdVideoId(Long videoId);
}
