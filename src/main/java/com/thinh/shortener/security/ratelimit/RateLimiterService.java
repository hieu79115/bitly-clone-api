package com.thinh.shortener.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String LUA_SCRIPT =
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 or redis.call('TTL', KEYS[1]) == -1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "local ttl = redis.call('TTL', KEYS[1])\n" +
            "return {current, ttl}";

    private final DefaultRedisScript<List> rateLimitScript =
            new DefaultRedisScript<>(LUA_SCRIPT, List.class);

    public RateLimitResult tryConsume(String key, long limit, long durationSeconds) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> result = stringRedisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(durationSeconds)
            );

            if (result != null && result.size() >= 2) {
                long current = ((Number) result.get(0)).longValue();
                long ttl = ((Number) result.get(1)).longValue();

                if (current > limit) {
                    long retryAfter = ttl > 0 ? ttl : durationSeconds;
                    log.warn("Rate limit exceeded for key '{}': current={}, limit={}, retryAfter={}s",
                            key, current, limit, retryAfter);
                    return RateLimitResult.rejected(limit, retryAfter);
                }

                long remaining = Math.max(0, limit - current);
                return RateLimitResult.allowed(limit, remaining);
            }
        } catch (Exception ex) {
            log.warn("Failed to check rate limit via Redis for key '{}', failing open: {}", key, ex.getMessage());
        }

        // Fail-open fallback: allow the request if Redis is temporarily unreachable
        return RateLimitResult.allowed(limit, limit);
    }
}
