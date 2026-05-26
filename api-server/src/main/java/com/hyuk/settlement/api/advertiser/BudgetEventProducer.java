package com.hyuk.settlement.api.advertiser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.shared.BudgetEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BudgetEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(BudgetEvent event) {
        try {
            String value = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("budget-events", event.getCampaignId(), value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("BudgetEvent 직렬화 실패", e);
        }
    }
}