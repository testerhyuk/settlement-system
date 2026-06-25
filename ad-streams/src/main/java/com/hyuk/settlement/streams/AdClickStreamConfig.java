package com.hyuk.settlement.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hyuk.settlement.shared.BudgetEvent;
import com.hyuk.settlement.shared.BudgetResult;
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

    private static final String STORE_NAME = "campaign-budget-store";
    private static final String EVENT_STORE_NAME = "event-id-store";

    @Value("${app.kafka.budget-events-topic}")
    private String BUDGET_EVENTS_TOPIC;
    @Value("${app.kafka.budget-results-topic}")
    private String BUDGET_RESULTS_TOPIC;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

        StoreBuilder<KeyValueStore<String, String>> eventIdStoreBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(EVENT_STORE_NAME),
                        Serdes.String(),
                        Serdes.String()
                );

        builder.addStateStore(storeBuilder);
        builder.addStateStore(eventIdStoreBuilder);

        KStream<String, String> stream = builder.stream(BUDGET_EVENTS_TOPIC);

        KStream<String, String> resultStream = stream.process(() -> new Processor<String, String, String, String>() {

            private KeyValueStore<String, BigDecimal> store;
            private KeyValueStore<String, String> eventIdStore;
            private ProcessorContext<String, String> processorContext;

            @Override
            public void init(ProcessorContext<String, String> context) {
                store = context.getStateStore(STORE_NAME);
                processorContext = context;
                eventIdStore = context.getStateStore(EVENT_STORE_NAME);
            }

            @Override
            public void process(Record<String, String> record) {
                try {
                    BudgetEvent event = objectMapper.readValue(record.value(), BudgetEvent.class);
                    String campaignId = event.getCampaignId();
                    BigDecimal amount = event.getAmount();
                    BigDecimal currentBudget = store.get(campaignId);
                    String eventId = eventIdStore.get(event.getEventId());

                    if (eventId != null) {
                        log.debug("중복 예산 이벤트 스킵 - eventId: {}, campaignId: {}", event.getEventId(), campaignId);
                        return;
                    }

                    if (event.getType() == BudgetEvent.BudgetEventType.CHARGE) {
                        if (currentBudget == null) {
                            currentBudget = BigDecimal.ZERO;
                        }

                        BigDecimal newBudget = currentBudget.add(amount);
                        store.put(campaignId, newBudget);

                        log.info("예산 충전 - campaignId: {}, 충전액: {}, 잔여 예산: {}", campaignId, amount, newBudget);

                        BudgetResult result = BudgetResult.builder()
                                .campaignId(campaignId)
                                .eventId(event.getEventId())
                                .requestAmount(amount)
                                .remainingBudget(newBudget)
                                .currency(event.getCurrency())
                                .resultType(BudgetResult.BudgetResultType.CHARGED)
                                .reason(null)
                                .build();

                        String resultData = objectMapper.writeValueAsString(result);
                        Record<String, String> resultRecord = new Record<>(campaignId, resultData, record.timestamp());
                        processorContext.forward(resultRecord);
                    } else if (event.getType() == BudgetEvent.BudgetEventType.DEDUCT) {
                        BudgetResult.BudgetResultBuilder resultBuilder = BudgetResult.builder()
                                .campaignId(campaignId)
                                .eventId(event.getEventId())
                                .requestAmount(amount)
                                .currency(event.getCurrency());

                        BudgetResult result;

                        if (currentBudget == null) {
                            log.error("null은 잘못된 값입니다. campaignId: {}, 잘못된 값: {}", campaignId, currentBudget);
                            result = resultBuilder
                                        .remainingBudget(currentBudget)
                                        .resultType(BudgetResult.BudgetResultType.REJECTED)
                                        .reason(BudgetResult.FailureReason.BUDGET_STATE_NOT_FOUND)
                                        .build();
                        } else if (currentBudget.compareTo(BigDecimal.ZERO) < 0) {
                            log.error("현재 예산이 음수일 수 없습니다. campaignId: {}, 잘못된 값: {}", campaignId, currentBudget);
                            result = resultBuilder
                                    .remainingBudget(currentBudget)
                                    .resultType(BudgetResult.BudgetResultType.REJECTED)
                                    .reason(BudgetResult.FailureReason.INVALID_BUDGET_STATE)
                                    .build();
                        } else if (currentBudget.compareTo(amount) < 0) {
                            log.warn("잔액 부족 - campaignId: {}, 현재 예산: {}", campaignId, currentBudget);
                            result = resultBuilder
                                        .remainingBudget(currentBudget)
                                        .resultType(BudgetResult.BudgetResultType.REJECTED)
                                        .reason(BudgetResult.FailureReason.INSUFFICIENT_BUDGET)
                                        .build();
                        } else {
                            BigDecimal newBudget = currentBudget.subtract(amount);

                            store.put(campaignId, newBudget);
                            log.info("예산 차감 - campaignId: {}, 차감액: {}, 잔여 예산: {}", campaignId, amount, newBudget);

                            if (newBudget.compareTo(BigDecimal.ZERO) == 0) {
                                log.info("예산 소진 - campaignId: {}, 상태: BUDGET_EXHAUSTED", campaignId);
                            }

                            result = resultBuilder
                                        .remainingBudget(newBudget)
                                        .resultType(BudgetResult.BudgetResultType.DEDUCTED)
                                        .reason(null)
                                        .build();
                        }

                        String resultData = objectMapper.writeValueAsString(result);
                        Record<String, String> resultRecord = new Record<>(campaignId, resultData, record.timestamp());
                        processorContext.forward(resultRecord);
                    }

                    eventIdStore.put(event.getEventId(), "PROCESSED");
                } catch (Exception e) {
                    throw new RuntimeException("예산 이벤트 처리 실패 : ", e);
                }
            }
        }, STORE_NAME, EVENT_STORE_NAME);


        resultStream.to(BUDGET_RESULTS_TOPIC);

        return resultStream;
    }

    // BigDecimal을 String으로 직렬화/역직렬화하는 커스텀 Serde
    private Serde<BigDecimal> bigDecimalSerde() {
        return Serdes.serdeFrom(
                (topic, data) -> data.toPlainString().getBytes(),
                (topic, data) -> new BigDecimal(new String(data))
        );
    }
}