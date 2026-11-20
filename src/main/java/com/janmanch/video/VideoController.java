package com.janmanch.video;

import com.janmanch.common.PageResponse;
import com.janmanch.video.VideoDtos.UploadResponse;
import com.janmanch.video.VideoDtos.VideoDetails;
import com.janmanch.video.VideoDtos.VideoSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@AuthenticationPrincipal Long userId,
                                 @RequestParam("file") MultipartFile file,
                               @RequestParam("title") String title,
                               @RequestParam(name = "description", required = false) String description,
                               @RequestParam(name = "tags", required = false) String tags) {
        return videoService.upload(userId, file, title, description, tags);
    }

    @GetMapping("/feed")
    public PageResponse<VideoSummary> feed(@RequestParam(name = "page", defaultValue = "0") int page,
                                           @RequestParam(name = "size", defaultValue = "20") int size) {
        return videoService.feed(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    @GetMapping("/trending")
    public PageResponse<VideoSummary> trending() {
        return videoService.trending();
    }

    @GetMapping("/search")
    public PageResponse<VideoSummary> search(@RequestParam("q") String query,
                                             @RequestParam(name = "page", defaultValue = "0") int page,
                                             @RequestParam(name = "size", defaultValue = "20") int size) {
        return videoService.search(query, page, size);
    }

    @GetMapping("/subscriptions")
    public PageResponse<VideoSummary> subscriptions(@AuthenticationPrincipal Long userId,
                                                    @RequestParam(name = "page", defaultValue = "0") int page,
                                                    @RequestParam(name = "size", defaultValue = "20") int size) {
        return videoService.subscriptionFeed(userId, page, size);
    }

    /** userId is null when the visitor is not logged in. */
    @GetMapping("/{id}")
    public VideoDetails details(@PathVariable("id") Long id, @AuthenticationPrincipal Long userId) {
        return videoService.details(id, userId);
    }

    /** Called by the player when playback starts. */
    @PostMapping("/{id}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void view(@PathVariable("id") Long id, @AuthenticationPrincipal Long userId) {
        videoService.recordView(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id, @AuthenticationPrincipal Long userId) {
        videoService.delete(id, userId);
    }
}
