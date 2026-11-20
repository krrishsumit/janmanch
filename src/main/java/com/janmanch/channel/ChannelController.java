package com.janmanch.channel;

import com.janmanch.common.ApiException;
import com.janmanch.common.PageResponse;
import com.janmanch.video.VideoDtos.VideoSummary;
import com.janmanch.video.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/channels")
public class ChannelController {

    private final ChannelRepository channelRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VideoService videoService;

    public ChannelController(ChannelRepository channelRepository,
                             SubscriptionRepository subscriptionRepository,
                             VideoService videoService) {
        this.channelRepository = channelRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.videoService = videoService;
    }

    public record ChannelResponse(Long id, String name, String description, long subscriberCount,
                                  boolean subscribed) {
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ChannelResponse channel(@PathVariable("id") Long channelId,
                                   @AuthenticationPrincipal Long userId) {
        return toResponse(requireChannel(channelId), userId);
    }

    @GetMapping("/{id}/videos")
    public PageResponse<VideoSummary> videos(@PathVariable("id") Long channelId,
                                             @RequestParam(name = "page", defaultValue = "0") int page,
                                             @RequestParam(name = "size", defaultValue = "20") int size) {
        return videoService.channelVideos(channelId, page, size);
    }

    @PostMapping("/{id}/subscribe")
    @Transactional
    public ChannelResponse subscribe(@PathVariable("id") Long channelId,
                                     @AuthenticationPrincipal Long userId) {
        Channel channel = requireChannel(channelId);
        if (channel.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot subscribe to your own channel");
        }
        SubscriptionId id = new SubscriptionId(userId, channelId);
        if (!subscriptionRepository.existsById(id)) {
            subscriptionRepository.saveAndFlush(new Subscription(id));
        }
        return toResponse(channel, userId);
    }

    @DeleteMapping("/{id}/subscribe")
    @Transactional
    public ChannelResponse unsubscribe(@PathVariable("id") Long channelId,
                                       @AuthenticationPrincipal Long userId) {
        Channel channel = requireChannel(channelId);
        SubscriptionId id = new SubscriptionId(userId, channelId);
        if (subscriptionRepository.existsById(id)) {
            subscriptionRepository.deleteById(id);
            subscriptionRepository.flush();
        }
        return toResponse(channel, userId);
    }

    private Channel requireChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Channel not found"));
    }

    private ChannelResponse toResponse(Channel channel, Long userId) {
        boolean subscribed = userId != null
                && subscriptionRepository.existsById(new SubscriptionId(userId, channel.getId()));
        return new ChannelResponse(channel.getId(), channel.getName(), channel.getDescription(),
                subscriptionRepository.countByIdChannelId(channel.getId()), subscribed);
    }
}
