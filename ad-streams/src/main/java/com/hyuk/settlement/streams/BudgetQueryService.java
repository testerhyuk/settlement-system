package com.hyuk.settlement.streams;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
}
