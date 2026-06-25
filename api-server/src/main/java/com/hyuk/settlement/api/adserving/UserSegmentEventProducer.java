package com.hyuk.settlement.api.adserving;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.infrastructure.kafka.ClusterRouter;
import com.hyuk.settlement.shared.UserSegmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class UserSegmentEventProducer {
    @Value("${app.kafka.user-segments-topic}")
    private String topic;

    private final ObjectMapper objectMapper;
    private final ClusterRouter clusterRouter;
    private final KafkaTemplate<String, String> primaryKafkaTemplate;
    private final KafkaTemplate<String, String> drKafkaTemplate;

    public UserSegmentEventProducer(
            @Qualifier("primaryKafkaTemplate") KafkaTemplate<String, String> primaryKafkaTemplate,
            @Qualifier("drKafkaTemplate") KafkaTemplate<String, String> drKafkaTemplate,
            ObjectMapper objectMapper,
            ClusterRouter clusterRouter
    ) {
        this.primaryKafkaTemplate = primaryKafkaTemplate;
        this.drKafkaTemplate = drKafkaTemplate;
        this.objectMapper = objectMapper;
        this.clusterRouter = clusterRouter;
    }

    public void sendAll(List<UserSegmentEvent> requestList) {
        if (requestList == null || requestList.isEmpty()) return;

        requestList.forEach(this::send);
    }

    public void send(UserSegmentEvent event) {
        String value;

        try {
            value = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("UserSegmentEvent 직렬화 실패 ", e);
        }

        String key = event.getUserId();
        boolean wasPrimary = clusterRouter.isPrimaryActive();

        KafkaTemplate<String, String> template = wasPrimary ? primaryKafkaTemplate : drKafkaTemplate;

        template.send(topic, key, value).whenComplete((result, exception) -> {
            if (exception == null) return;

            if (wasPrimary) {
                log.error("Primary 발행 실패 (userId={}), DR로 재발행 시도. 원인: {}",
                        event.getUserId(), exception.toString());

                clusterRouter.failoverToDr();
                sendToDrWithLogging(event, key, value);
            } else {
                log.error("DR 재발행 실패 (userId={}). 메시지 유실! 원인: {}",
                        event.getUserId(), exception.toString());
            }
        });
    }

    private void sendToDrWithLogging(UserSegmentEvent event, String key, String value) {
        drKafkaTemplate.send(topic, key, value).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("DR 재발행도 실패 (userId={}). 메시지 유실! 원인: {}",
                        event.getUserId(), exception.toString());
            } else {
                log.warn("DR 재발행 성공 (userId={})", event.getUserId());
            }
        });
    }
}
