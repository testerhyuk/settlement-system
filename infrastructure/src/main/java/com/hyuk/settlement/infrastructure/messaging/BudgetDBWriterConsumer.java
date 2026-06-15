package com.hyuk.settlement.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import com.hyuk.settlement.shared.BudgetResult;
import com.hyuk.settlement.shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetDBWriterConsumer {
    private final AdCampaignRepository adCampaignRepository;
    private final ObjectMapper objectMapper;
    private final ProcessedEventJpaRepository processedEventJpaRepository;

    @Transactional
    @KafkaListener(
            topics = {"budget-results", "dr.budget-results"},
            groupId = "budget-db-writer",
            concurrency = "6"
    )
    public void handle(String message) {
        try {
            BudgetResult budgetResult = objectMapper.readValue(message, BudgetResult.class);

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
        } catch (JsonProcessingException e) {
            log.error("BudgetResult 역직렬화 실패 : {}", message, e);
        }
    }
}
