package com.pgs.assistant.chat;

import com.pgs.assistant.config.AssistantProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Fixed-window rate limit per merchant. Model calls cost money (Bedrock) or CPU (Ollama), so one
 * merchant must not be able to run up the bill. Single-instance only: with several replicas this
 * would move to Redis.
 */
@Component
public class RateLimiter {

    private record Window(long minute, int count) {
    }

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final Clock clock;

    @Autowired
    public RateLimiter(AssistantProperties properties) {
        this(properties.rateLimit().requestsPerMinute(), Clock.systemUTC());
    }

    RateLimiter(int limit, Clock clock) {
        this.limit = limit;
        this.clock = clock;
    }

    /** @throws RateLimitExceededException if the merchant has used up this minute's requests */
    public void acquire(String merchantId) {
        long minute = clock.millis() / 60_000;
        Window w = windows.compute(merchantId, (k, old) ->
                old == null || old.minute() != minute ? new Window(minute, 1) : new Window(minute, old.count() + 1));
        if (w.count() > limit) {
            throw new RateLimitExceededException(limit);
        }
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(int limit) {
            super("Rate limit exceeded: at most " + limit + " assistant messages per minute");
        }
    }
}
