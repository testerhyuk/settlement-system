package com.hyuk.settlement.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import com.hyuk.settlement.infrastructure.dlq.DlqMessageService;
import com.hyuk.settlement.shared.BudgetResult;
import com.hyuk.settlement.shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetDBWriterConsumer {
    private final AdCampaignRepository adCampaignRepository;
    private final ObjectMapper objectMapper;
    private final ProcessedEventJpaRepository processedEventJpaRepository;
    private final DlqMessageService dlqMessageService;

    @Transactional
    @KafkaListener(
            topics = {
                    "${app.kafka.budget-results-topic}"
            },
            groupId = "budget-db-writer",
            concurrency = "6"
    )
    public void handle(ConsumerRecord<String, String> record) {
        try {
            BudgetResult budgetResult = objectMapper.readValue(record.value(), BudgetResult.class);

            if (processedEventJpaRepository.existsById(budgetResult.getEventId())) {
                log.info("이미 처리된 이벤트 스킵 - eventId : {}", budgetResult.getEventId());
                return;
            }

            AdCampaign adCampaign = adCampaignRepository.findById(budgetResult.getCampaignId()).orElse(null);

            if (adCampaign == null) {
                log.warn("존재하지 않는 이벤트 스킵 campaignId: {}, eventId: {}", budgetResult.getCampaignId(), budgetResult.getEventId());
                return;
            }

            switch (budgetResult.getResultType()) {
                case CHARGED, DEDUCTED -> {
                    Money money = new Money(budgetResult.getRemainingBudget(), budgetResult.getCurrency());

                    adCampaign.updateBudget(money);

                    adCampaignRepository.save(adCampaign);
                }
                case REJECTED -> {}
            }

            processedEventJpaRepository.save(new ProcessedEvent(budgetResult.getEventId()));

            log.debug("이벤트 처리 완료 - type: {}, campaignId: {}, eventId: {}", budgetResult.getResultType(), budgetResult.getCampaignId(), budgetResult.getEventId());

            String dlqMessageId = headerAsString(record.headers(), "x-dlq-message-id");

            if (dlqMessageId != null) {
                dlqMessageService.markAsReplayed(dlqMessageId);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("BudgetResult 역직렬화 실패", e);
        }
    }

    private String headerAsString(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
