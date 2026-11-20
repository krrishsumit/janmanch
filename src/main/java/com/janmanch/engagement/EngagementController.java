package com.janmanch.engagement;

import com.janmanch.common.PageResponse;
import com.janmanch.engagement.EngagementDtos.CommentRequest;
import com.janmanch.engagement.EngagementDtos.CommentResponse;
import com.janmanch.engagement.EngagementDtos.ReactionRequest;
import com.janmanch.engagement.EngagementDtos.ReactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @PutMapping("/videos/{id}/reaction")
    public ReactionResponse react(@PathVariable("id") Long videoId,
                                  @AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody ReactionRequest request) {
        return engagementService.react(videoId, userId, request.reaction());
    }

    @GetMapping("/videos/{id}/comments")
    public PageResponse<CommentResponse> comments(@PathVariable("id") Long videoId,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "20") int size) {
        return engagementService.comments(videoId, page, size);
    }

    @PostMapping("/videos/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable("id") Long videoId,
                                      @AuthenticationPrincipal Long userId,
                                      @Valid @RequestBody CommentRequest request) {
        return engagementService.addComment(videoId, userId, request.text());
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable("id") Long commentId, @AuthenticationPrincipal Long userId) {
        engagementService.deleteComment(commentId, userId);
    }
}
