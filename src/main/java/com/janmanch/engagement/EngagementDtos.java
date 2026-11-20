package com.janmanch.engagement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class EngagementDtos {

    private EngagementDtos() {
    }

    /** reaction is LIKE, DISLIKE or NONE. */
    public record ReactionRequest(@NotBlank String reaction) {
    }

    public record ReactionResponse(long likes, long dislikes, String myReaction) {
    }

    public record CommentRequest(@NotBlank @Size(max = 2000) String text) {
    }

    public record CommentResponse(Long id, Long userId, String userName, String text, Instant createdAt) {
    }
}
