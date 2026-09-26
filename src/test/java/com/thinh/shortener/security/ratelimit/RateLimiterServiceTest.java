package com.thinh.shortener.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("Request within capacity should be allowed with correct remaining tokens")
    void tryConsume_WithinLimit_Allowed() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(List.of(3L, 55L));

        RateLimitResult result = rateLimiterService.tryConsume("test:key", 10, 60);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.remaining()).isEqualTo(7);
        assertThat(result.retryAfterSeconds()).isEqualTo(0);
    }

    @Test
    @DisplayName("Request exceeding capacity should be rejected with accurate retryAfter seconds")
    void tryConsume_ExceedingLimit_Rejected() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(List.of(11L, 42L));

        RateLimitResult result = rateLimiterService.tryConsume("test:key", 10, 60);

        assertThat(result.allowed()).isFalse();
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.remaining()).isEqualTo(0);
        assertThat(result.retryAfterSeconds()).isEqualTo(42);
    }

    @Test
    @DisplayName("Fail-open: Redis connection failure should gracefully allow requests instead of crashing")
    void tryConsume_RedisDown_FailsOpen() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        RateLimitResult result = rateLimiterService.tryConsume("test:key", 10, 60);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.remaining()).isEqualTo(10);
    }
}
