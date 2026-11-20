package com.janmanch.user;

import com.janmanch.channel.Channel;
import com.janmanch.channel.ChannelRepository;
import com.janmanch.channel.Subscription;
import com.janmanch.channel.SubscriptionRepository;
import com.janmanch.common.ApiException;
import com.janmanch.common.PageResponse;
import com.janmanch.video.VideoDtos.HistoryItem;
import com.janmanch.video.VideoDtos.StudioVideo;
import com.janmanch.video.VideoService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VideoService videoService;

    public UserController(UserRepository userRepository, ChannelRepository channelRepository,
                          SubscriptionRepository subscriptionRepository, VideoService videoService) {
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.videoService = videoService;
    }

    public record ProfileResponse(Long userId, String name, String email, Instant createdAt,
                                  Long channelId, String channelName) {
    }

    public record SubscribedChannel(Long id, String name) {
    }

    /** Returns the logged-in user's profile and channel. */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ProfileResponse me(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Channel channel = channelRepository.findByUserId(userId).orElse(null);
        return new ProfileResponse(
                user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(),
                channel == null ? null : channel.getId(),
                channel == null ? null : channel.getName());
    }

    /** Videos the logged-in user has watched, most recent first. */
    @GetMapping("/me/history")
    public PageResponse<HistoryItem> history(@AuthenticationPrincipal Long userId,
                                             @RequestParam(name = "page", defaultValue = "0") int page,
                                             @RequestParam(name = "size", defaultValue = "20") int size) {
        return videoService.history(userId, page, size);
    }

    /** The logged-in user's own videos in every status, for the creator studio. */
    @GetMapping("/me/videos")
    public PageResponse<StudioVideo> myVideos(@AuthenticationPrincipal Long userId,
                                              @RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "50") int size) {
        return videoService.myVideos(userId, page, size);
    }

    /** Channels the logged-in user is subscribed to, most recent first (for the side menu). */
    @GetMapping("/me/subscriptions")
    @Transactional(readOnly = true)
    public List<SubscribedChannel> subscriptions(@AuthenticationPrincipal Long userId) {
        List<Long> channelIds = subscriptionRepository.findByIdUserIdOrderBySubscribedAtDesc(userId).stream()
                .map(Subscription::getId)
                .map(id -> id.getChannelId())
                .toList();
        Map<Long, Channel> channels = channelRepository.findAllById(channelIds).stream()
                .collect(Collectors.toMap(Channel::getId, Function.identity()));
        return channelIds.stream()
                .map(channels::get)
                .filter(channel -> channel != null)
                .map(channel -> new SubscribedChannel(channel.getId(), channel.getName()))
                .toList();
    }
}
