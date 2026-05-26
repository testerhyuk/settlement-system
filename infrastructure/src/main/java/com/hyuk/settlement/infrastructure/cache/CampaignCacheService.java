package com.hyuk.settlement.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.advertiser.AdCampaign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class CampaignCacheService {
    private final RedisTemplate<String, Object> cacheRedisTemplate;
    private final ObjectMapper objectMapper;

    private final Duration DURATION = Duration.ofHours(1);
    private static final String KEY_PREFIX = "campaign:";

    public CampaignCacheService(
            @Qualifier("cacheRedisTemplate") RedisTemplate<String, Object> cacheRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.cacheRedisTemplate = cacheRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(AdCampaign adCampaign) {
        try {
            String json = objectMapper.writeValueAsString(adCampaign);
            cacheRedisTemplate.opsForValue().set(key(adCampaign.getCampaignId()), json, DURATION);
        } catch (JsonProcessingException e) {
            log.error("캠페인 직렬화 실패 : ", e);
        }
    }

    public Optional<AdCampaign> findById(String campaignId) {
        Object value = cacheRedisTemplate.opsForValue().get(key(campaignId));

        if (value == null) {
            return Optional.empty();
        }

        try {
            AdCampaign adCampaign = objectMapper.readValue(value.toString(), AdCampaign.class);
            return Optional.of(adCampaign);
        } catch (JsonProcessingException e) {
            log.error("캠페인 역직렬화 실패 : ", e);
            return Optional.empty();
        }
    }

    public void evict(String campaignId) {
        cacheRedisTemplate.delete(key(campaignId));
    }

    private String key(String campaignId) {
        return KEY_PREFIX + campaignId;
    }
}
