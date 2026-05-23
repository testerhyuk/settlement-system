package com.hyuk.settlement.api.advertiser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.shared.AdClickEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AdClickProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(AdClickEvent event) {
        try {
            String value = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("ad-click-events", event.getCampaignId(), value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AdClickEvent 직렬화 실패", e);
        }
    }
}
