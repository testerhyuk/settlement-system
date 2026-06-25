package com.hyuk.settlement.api.adserving;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.infrastructure.kafka.ClusterRouter;
import com.hyuk.settlement.shared.AdImpressionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AdImpressionEventProducer {
    @Value("${app.kafka.ad-impressions-topic}")
    private String topic;

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> primaryKafkaTemplate;
    private final KafkaTemplate<String, String> drKafkaTemplate;
    private final ClusterRouter clusterRouter;

    public AdImpressionEventProducer(
            @Qualifier("primaryKafkaTemplate") KafkaTemplate<String, String> primaryKafkaTemplate,
            @Qualifier("drKafkaTemplate") KafkaTemplate<String, String> drKafkaTemplate,
            ClusterRouter clusterRouter,
            ObjectMapper objectMapper
    ) {
        this.primaryKafkaTemplate = primaryKafkaTemplate;
        this.drKafkaTemplate = drKafkaTemplate;
        this.clusterRouter = clusterRouter;
        this.objectMapper = objectMapper;
    }

    public void sendAll(List<AdImpressionEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        events.forEach(this::send);
    }

    private void send(AdImpressionEvent event) {
        String value;

        try {
            value = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AdImpressionEvent 직렬화 실패", e);
        }

        final String key = event.getCampaignId();
        final boolean wasPrimary = clusterRouter.isPrimaryActive();

        KafkaTemplate<String, String> template = wasPrimary ? primaryKafkaTemplate : drKafkaTemplate;

        template.send(topic, key, value).whenComplete((result, exception) -> {
            if (exception == null) {
                return;
            }

            if (wasPrimary) {
                log.error("Primary 발행 실패 (impressionId={}), DR로 재발행 시도. 원인: {}",
                        event.getImpressionId(), exception.toString());

                clusterRouter.failoverToDr();
                sendToDrWithLogging(event, key, value);
            } else {
                log.error("DR 재발행 실패 (impressionId={}). 메시지 유실! 원인: {}",
                        event.getImpressionId(), exception.toString());
            }
        });
    }

    private void sendToDrWithLogging(AdImpressionEvent event, String key, String value) {
        drKafkaTemplate.send(topic, key, value).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("DR 재발행도 실패 (impressionId={}). 메시지 유실! 원인: {}",
                        event.getImpressionId(), ex.toString());
            } else {
                log.warn("DR 재발행 성공 (impressionId={})", event.getImpressionId());
            }
        });
    }
}
