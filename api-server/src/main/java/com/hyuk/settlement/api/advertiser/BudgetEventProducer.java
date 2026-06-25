package com.hyuk.settlement.api.advertiser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.infrastructure.kafka.ClusterRouter;
import com.hyuk.settlement.shared.BudgetEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BudgetEventProducer {

    @Value("${app.kafka.budget-events-topic}")
    private String topic;

    private final KafkaTemplate<String, String> primaryKafkaTemplate;
    private final KafkaTemplate<String, String> drKafkaTemplate;
    private final ClusterRouter clusterRouter;
    private final ObjectMapper objectMapper;

    public BudgetEventProducer(
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

    public void send(BudgetEvent event) {
        final String value;
        try {
            value = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("BudgetEvent 직렬화 실패", e);
        }

        final String key = event.getCampaignId();
        final boolean wasPrimary = clusterRouter.isPrimaryActive();

        KafkaTemplate<String, String> template = wasPrimary ? primaryKafkaTemplate : drKafkaTemplate;

        template.send(topic, key, value).whenComplete((result, ex) -> {
            if (ex == null) {
                return; // 발행 성공
            }

            if (wasPrimary) {
                // Primary 발행 실패 -> 자동 failover + DR 재발행
                log.error("Primary 발행 실패 (eventId={}), DR로 재발행 시도. 원인: {}",
                        event.getEventId(), ex.toString());
                clusterRouter.failoverToDr();
                sendToDrWithLogging(event, key, value);
            } else {
                // 이미 DR로 발행했는데도 실패 -> 양쪽 다 죽은 상황
                log.error("DR 발행 실패 (eventId={}). 메시지 유실 위험. 원인: {}",
                        event.getEventId(), ex.toString());
            }
        });
    }

    private void sendToDrWithLogging(BudgetEvent event, String key, String value) {
        drKafkaTemplate.send(topic, key, value).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("DR 재발행도 실패 (eventId={}). 메시지 유실! 원인: {}",
                        event.getEventId(), ex.toString());
            } else {
                log.warn("DR 재발행 성공 (eventId={})", event.getEventId());
            }
        });
    }
}