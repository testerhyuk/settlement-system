package com.hyuk.settlement.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import com.hyuk.settlement.shared.BudgetEvent;
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
            topics = {"budget-events", "dr.budget-events"},
            groupId = "budget-db-writer",
            concurrency = "6"
    )
    public void handle(String message) {
        try {
            BudgetEvent budgetEvent = objectMapper.readValue(message, BudgetEvent.class);

            if (processedEventJpaRepository.existsById(budgetEvent.getEventId())) {
                log.info("이미 처리된 이벤트 스킵 - eventId : {}", budgetEvent.getEventId());
                return;
            }

            AdCampaign adCampaign = adCampaignRepository.findById(budgetEvent.getCampaignId()).orElse(null);

            if (adCampaign == null) {
                log.warn("존재하지 않는 이벤트 스킵 campaignId: {}, eventId: {}", budgetEvent.getCampaignId(), budgetEvent.getEventId());
                return;
            }

            Money money = new Money(budgetEvent.getAmount(), budgetEvent.getCurrency());

            switch (budgetEvent.getType()) {
                case CHARGE -> adCampaign.chargeBudget(money);
                case DEDUCT -> adCampaign.deductBudget(money);
            }

            adCampaignRepository.save(adCampaign);

            processedEventJpaRepository.save(new ProcessedEvent(budgetEvent.getEventId()));

            log.debug("이벤트 처리 완료 - type: {}, campaignId: {}, eventId: {}", budgetEvent.getType(), budgetEvent.getCampaignId(), budgetEvent.getEventId());
        } catch (JsonProcessingException e) {
            log.error("BudgetEvent 역직렬화 실패 : {}", message, e);
        }
    }
}
