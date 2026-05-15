package com.hyuk.settlement.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class DistributedLockManager {
    private final RedisTemplate<String, Object> redisTemplate;

    public boolean tryLock(String key, long ttlSeconds) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "locked", Duration.ofSeconds(ttlSeconds));

        return Boolean.TRUE.equals(result);
    }

    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
