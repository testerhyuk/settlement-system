package com.hyuk.settlement.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClusterRouter {

    private static final String ACTIVE_CLUSTER_KEY = "kafka:active-cluster";
    private static final String PRIMARY = "primary";
    private static final String DR = "dr";

    private final RedisTemplate<String, String> lockRedisTemplate;

    public ClusterRouter(RedisTemplate<String, String> lockRedisTemplate) {
        this.lockRedisTemplate = lockRedisTemplate;
    }

    public boolean isPrimaryActive() {
        String active = lockRedisTemplate.opsForValue().get(ACTIVE_CLUSTER_KEY);
        // 값이 없으면 기본 primary
        return active == null || PRIMARY.equals(active);
    }

    public void switchToDr() {
        lockRedisTemplate.opsForValue().set(ACTIVE_CLUSTER_KEY, DR);
        log.warn("클러스터 라우팅 전환: PRIMARY -> DR");
    }

    public void switchToPrimary() {
        lockRedisTemplate.opsForValue().set(ACTIVE_CLUSTER_KEY, PRIMARY);
        log.warn("클러스터 라우팅 전환: DR -> PRIMARY");
    }

    /**
     * Primary 발행 실패 감지 시 자동 failover.
     * 이미 DR이면 아무것도 하지 않음 (멱등 + 로그 폭주 방지).
     */
    public void failoverToDr() {
        if (isPrimaryActive()) {
            switchToDr();
            log.error("Primary 발행 실패 감지 - 자동 FAILOVER to DR 수행");
        }
    }
}