package com.hyuk.settlement.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.shared.BudgetEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdClickStreamConfig {

    private static final String TOPIC = "budget-events";
    private static final String STORE_NAME = "campaign-budget-store";
    private static final String DR_TOPIC = "dr.budget-events";

    private final ObjectMapper objectMapper;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfig(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.streams.application-id}") String applicationId,
            @Value("${spring.kafka.streams.default-key-serde}") String keySerde,
            @Value("${spring.kafka.streams.default-value-serde}") String valueSerde,
            @Value("${spring.kafka.streams.properties.num.stream.threads}") int numThreads) {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, numThreads);

        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public KStream<String, String> adClickStream(StreamsBuilder builder) {
        StoreBuilder<KeyValueStore<String, BigDecimal>> storeBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(STORE_NAME),
                        Serdes.String(),
                        bigDecimalSerde()
                );
        builder.addStateStore(storeBuilder);

        KStream<String, String> stream = builder.stream(List.of(TOPIC, DR_TOPIC));

        stream.process(() -> new Processor<String, String, Void, Void>() {

            private KeyValueStore<String, BigDecimal> store;

            @Override
            public void init(ProcessorContext<Void, Void> context) {
                store = context.getStateStore(STORE_NAME);
            }

            @Override
            public void process(Record<String, String> record) {
                try {
                    BudgetEvent event = objectMapper.readValue(record.value(), BudgetEvent.class);
                    String campaignId = event.getCampaignId();
                    BigDecimal amount = event.getAmount();
                    BigDecimal currentBudget = store.get(campaignId);

                    if (currentBudget == null) {
                        currentBudget = BigDecimal.ZERO;
                    }

                    if (event.getType() == BudgetEvent.BudgetEventType.CHARGE) {
                        BigDecimal newBudget = currentBudget.add(amount);
                        store.put(campaignId, newBudget);

                        log.info("예산 충전 - campaignId: {}, 충전액: {}, 잔여 예산: {}", campaignId, amount, newBudget);

                    } else if (event.getType() == BudgetEvent.BudgetEventType.DEDUCT) {
                        BigDecimal newBudget = currentBudget.subtract(amount);
                        store.put(campaignId, newBudget);
                        log.info("예산 차감 - campaignId: {}, 차감액: {}, 잔여 예산: {}", campaignId, amount, newBudget);

                        if (newBudget.compareTo(BigDecimal.ZERO) <= 0) {
                            log.info("예산 소진 - campaignId: {}, 상태: BUDGET_EXHAUSTED", campaignId);
                        }
                    }

                } catch (Exception e) {
                    log.error("이벤트 처리 실패 - {}", e.getMessage());
                }
            }
        }, STORE_NAME);

        return stream;
    }

    // BigDecimal을 String으로 직렬화/역직렬화하는 커스텀 Serde
    private Serde<BigDecimal> bigDecimalSerde() {
        return Serdes.serdeFrom(
                (topic, data) -> data.toPlainString().getBytes(),
                (topic, data) -> new BigDecimal(new String(data))
        );
    }
}