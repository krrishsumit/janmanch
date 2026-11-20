package com.janmanch.engagement;

import com.janmanch.common.ApiException;
import com.janmanch.common.PageResponse;
import com.janmanch.engagement.EngagementDtos.CommentResponse;
import com.janmanch.engagement.EngagementDtos.ReactionResponse;
import com.janmanch.user.UserRepository;
import com.janmanch.video.VideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EngagementService {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public EngagementService(LikeRepository likeRepository, CommentRepository commentRepository,
                             VideoRepository videoRepository, UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    /** Sets the user's reaction to LIKE, DISLIKE or NONE (removes it). */
    @Transactional
    public ReactionResponse react(Long videoId, Long userId, String reaction) {
        requireVideo(videoId);
        LikeId id = new LikeId(userId, videoId);
        String myReaction = null;

        if ("NONE".equalsIgnoreCase(reaction)) {
            likeRepository.findById(id).ifPresent(likeRepository::delete);
        } else {
            LikeType type;
            try {
                type = LikeType.valueOf(reaction == null ? "" : reaction.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Reaction must be LIKE, DISLIKE or NONE");
            }
            VideoLike like = likeRepository.findById(id).orElseGet(() -> {
                VideoLike created = new VideoLike();
                created.setId(id);
                return created;
            });
            like.setType(type);
            likeRepository.save(like);
            myReaction = type.name();
        }
        likeRepository.flush();
        return new ReactionResponse(
                likeRepository.countByIdVideoIdAndType(videoId, LikeType.LIKE),
                likeRepository.countByIdVideoIdAndType(videoId, LikeType.DISLIKE),
                myReaction);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> comments(Long videoId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(commentRepository.findByVideoId(videoId, pageable).map(this::toResponse));
    }

    @Transactional
    public CommentResponse addComment(Long videoId, Long userId, String text) {
        requireVideo(videoId);
        Comment comment = new Comment();
        comment.setVideo(videoRepository.getReferenceById(videoId));
        comment.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found")));
        comment.setText(text.trim());
        return toResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!comment.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can delete only your own comments");
        }
        commentRepository.delete(comment);
    }

    private void requireVideo(Long videoId) {
        if (!videoRepository.existsById(videoId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Video not found");
        }
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getUser().getId(),
                comment.getUser().getName(), comment.getText(), comment.getCreatedAt());
    }
}
