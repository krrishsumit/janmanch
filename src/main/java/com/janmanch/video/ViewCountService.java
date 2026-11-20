package com.janmanch.video;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Counts views in Redis and writes them to MySQL in batches.
 * A view is one fast Redis increment instead of one database update,
 * so the displayed count can lag by up to a minute.
 */
@Service
public class ViewCountService {

    private static final Logger log = LoggerFactory.getLogger(ViewCountService.class);
    private static final String PENDING_KEY = "janmanch:views:pending";
    private static final String FLUSHING_KEY = "janmanch:views:flushing";

    private final StringRedisTemplate redis;
    private final VideoRepository videoRepository;

    public ViewCountService(StringRedisTemplate redis, VideoRepository videoRepository) {
        this.redis = redis;
        this.videoRepository = videoRepository;
    }

    public void increment(Long videoId) {
        redis.opsForHash().increment(PENDING_KEY, String.valueOf(videoId), 1);
    }

    @Scheduled(fixedDelayString = "${janmanch.views.flush-interval-ms}")
    @Transactional
    public void flushToDatabase() {
        try {
            // Finish a batch left over from a failed run before taking a new one.
            writeBatch();
            if (Boolean.TRUE.equals(redis.hasKey(PENDING_KEY))) {
                // Renaming is atomic, so views arriving during the flush go into a fresh hash.
                redis.rename(PENDING_KEY, FLUSHING_KEY);
                writeBatch();
            }
        } catch (RuntimeException ex) {
            log.warn("View count flush failed; it will be retried", ex);
        }
    }

    private void writeBatch() {
        Map<Object, Object> batch = redis.opsForHash().entries(FLUSHING_KEY);
        if (batch.isEmpty()) {
            return;
        }
        batch.forEach((videoId, views) ->
                videoRepository.addViews(Long.valueOf((String) videoId), Long.parseLong((String) views)));
        redis.delete(FLUSHING_KEY);
        log.debug("Flushed view counts for {} videos", batch.size());
    }
}
