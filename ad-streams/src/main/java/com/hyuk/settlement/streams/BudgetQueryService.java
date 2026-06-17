package com.hyuk.settlement.streams;

import com.hyuk.settlement.shared.BudgetGateRequest;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetQueryService {
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public BigDecimal getRemainingBudget(String campaignId) {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

        ReadOnlyKeyValueStore<String, BigDecimal> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                        "campaign-budget-store",
                        QueryableStoreTypes.keyValueStore()
                )
        );

        BigDecimal budget = store.get(campaignId);

        if (budget == null) return null;

        return budget.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : budget;
    }

    public Map<String, Boolean> canServeAds(List<BudgetGateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }

        Set<String> campaignIds = new HashSet<>();

        for (BudgetGateRequest request : requests) {
            if (request == null || request.getCampaignId() == null || request.getCampaignId().isBlank()) {
                throw new IllegalArgumentException("campaignId는 필수입니다");
            }

            if (!campaignIds.add(request.getCampaignId())) {
                throw new IllegalArgumentException("중복 campaignId가 포함되어 있습니다: " + request.getCampaignId());
            }
        }

        return requests.stream()
                .collect(Collectors.toMap(
                        BudgetGateRequest::getCampaignId,
                        request -> canServeAd(request.getCampaignId(), request.getCpcAmount())
                ));
    }

    private boolean canServeAd(String campaignId, BigDecimal cpcAmount) {
        if (campaignId == null || cpcAmount == null || cpcAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

        ReadOnlyKeyValueStore<String, BigDecimal> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                        "campaign-budget-store",
                        QueryableStoreTypes.keyValueStore()
                )
        );

        BigDecimal budget = store.get(campaignId);

        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return budget.compareTo(cpcAmount) >= 0;
    }
}
