package com.hyuk.settlement.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import com.hyuk.settlement.shared.AdClickEvent;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdClickStreamConfig {

    private static final String TOPIC = "ad-click-events";
    private static final String STORE_NAME = "campaign-budget-store";

    private final AdCampaignRepository adCampaignRepository;
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

        // ad-click-events 토픽 구독
        KStream<String, String> stream = builder.stream(TOPIC);

        // 각 클릭 이벤트를 처리하는 Processor 연결
        stream.process(() -> new Processor<String, String, Void, Void>() {

            private KeyValueStore<String, BigDecimal> store;

            @Override
            public void init(ProcessorContext<Void, Void> context) {
                // State Store 초기화
                store = context.getStateStore(STORE_NAME);
            }

            @Override
            public void process(Record<String, String> record) {
                try {
                    // JSON 문자열을 AdClickEvent 객체로 역직렬화
                    AdClickEvent event = objectMapper.readValue(record.value(), AdClickEvent.class);
                    String campaignId = event.getCampaignId();

                    // State Store에서 현재 예산 조회
                    BigDecimal currentBudget = store.get(campaignId);

                    // State Store에 예산이 없으면 DB에서 초기값 로드
                    if (currentBudget == null) {
                        AdCampaign campaign = adCampaignRepository.findActiveCampaign(campaignId, LocalDate.now())
                                .orElseThrow(() -> new IllegalArgumentException("활성화된 캠페인이 없습니다: " + campaignId));
                        currentBudget = campaign.getBudget().getAmount();
                        store.put(campaignId, currentBudget);
                        log.info("State Store 초기화 - campaignId: {}, budget: {}", campaignId, currentBudget);
                    }

                    // CPC 금액만큼 예산 차감
                    BigDecimal cpcAmount = event.getCpcAmount().getAmount();
                    BigDecimal newBudget = currentBudget.subtract(cpcAmount);

                    // 차감된 예산을 State Store에 저장
                    store.put(campaignId, newBudget);
                    log.info("예산 차감 - campaignId: {}, 차감액: {}, 남은 예산: {}", campaignId, cpcAmount, newBudget);

                    // 예산 소진 시 DB에 상태 변경 반영
                    if (newBudget.compareTo(BigDecimal.ZERO) <= 0) {
                        AdCampaign campaign = adCampaignRepository.findActiveCampaign(campaignId, LocalDate.now())
                                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId));
                        campaign.exhaustBudget();
                        adCampaignRepository.save(campaign);
                        log.info("예산 소진 - campaignId: {}, 상태: BUDGET_EXHAUSTED", campaignId);
                    }

                } catch (Exception e) {
                    log.error("클릭 이벤트 처리 실패 - {}", e.getMessage());
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