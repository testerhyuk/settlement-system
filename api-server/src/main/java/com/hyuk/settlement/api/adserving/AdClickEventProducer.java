package com.hyuk.settlement.api.adserving;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.infrastructure.kafka.ClusterRouter;
import com.hyuk.settlement.shared.AdClickEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AdClickEventProducer {
    @Value("${app.kafka.ad-clicks-topic}")
    private String topic;

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> primaryKafkaTemplate;
    private final KafkaTemplate<String, String> drKafkaTemplate;
    private final ClusterRouter clusterRouter;

    public AdClickEventProducer(
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

    public void sendAll(List<AdClickEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        events.forEach(this::send);
    }

    private void send(AdClickEvent event) {
        String value;

        try {
            value = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalArgumentException("AdClickEvent 직렬화 실패", e);
        }

        final String key = event.getCampaignId();
        boolean wasPrimary = clusterRouter.isPrimaryActive();

        KafkaTemplate<String, String> kafkaTemplate = wasPrimary ? primaryKafkaTemplate : drKafkaTemplate;

        kafkaTemplate.send(topic, key, value).whenComplete((result, ex) -> {
            if (ex == null) return;

            if (wasPrimary) {
                log.error("Primary 발행 실패 (clickId={}), DR로 재발행 시도. 원인: {}",
                        event.getClickId(), ex.toString());

                clusterRouter.failoverToDr();
                sendToDrWithLogging(event, key, value);
            } else {
                log.error("DR 재발행 실패 (clickId={}). 메시지 유실! 원인: {}",
                        event.getClickId(), ex.toString());
            }
        });
    }

    private void sendToDrWithLogging(AdClickEvent event, String key, String value) {
        drKafkaTemplate.send(topic, key, value).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("DR 재발행도 실패 (clickId={}). 메시지 유실! 원인: {}",
                        event.getClickId(), ex.toString());
            } else {
                log.warn("DR 재발행 성공 (clickId={})", event.getClickId());
            }
        });
    }
}
