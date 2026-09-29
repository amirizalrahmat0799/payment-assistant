package com.mizal.pgs.assistant.chat;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    @Test
    void allowsUpToTheLimitPerMinutePerMerchant() {
        RateLimiter limiter = new RateLimiter(2, Clock.fixed(Instant.parse("2026-09-30T10:00:10Z"), ZoneOffset.UTC));

        limiter.acquire("m-1");
        limiter.acquire("m-1");
        assertThatThrownBy(() -> limiter.acquire("m-1")).isInstanceOf(RateLimiter.RateLimitExceededException.class);
        assertThatCode(() -> limiter.acquire("m-2")).doesNotThrowAnyException();
    }

    @Test
    void resetsInTheNextMinute() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-30T10:00:59Z"));
        RateLimiter limiter = new RateLimiter(1, clock);

        limiter.acquire("m-1");
        assertThatThrownBy(() -> limiter.acquire("m-1")).isInstanceOf(RateLimiter.RateLimitExceededException.class);

        clock.now = Instant.parse("2026-09-30T10:01:00Z");
        assertThatCode(() -> limiter.acquire("m-1")).doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {
        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
