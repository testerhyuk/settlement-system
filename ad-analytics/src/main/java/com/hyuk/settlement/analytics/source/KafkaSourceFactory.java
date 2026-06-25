package com.hyuk.settlement.analytics.source;

import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

public class KafkaSourceFactory {
    private final AnalyticsProperties properties;

    public KafkaSourceFactory(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public KafkaSource<String> budgetResultsSource() {
        return createSource(
                properties.getBudgetResultsTopic(),
                properties.getBudgetResultsGroup()
        );
    }

    public KafkaSource<String> adImpressionsSource() {
        return createSource(
                properties.getAdImpressionsTopic(),
                properties.getAdImpressionsGroup()
        );
    }

    public KafkaSource<String> adClicksSource() {
        return createSource(
                properties.getAdClicksTopic(),
                properties.getAdClicksGroup()
        );
    }

    public KafkaSource<String> adConversionsSource() {
        return createSource(
                properties.getAdConversionsTopic(),
                properties.getAdConversionsGroup()
        );
    }

    public KafkaSource<String> userSegmentsSource() {
        return createSource(
                properties.getUserSegmentsTopic(),
                properties.getUserSegmentsGroup(),
                OffsetsInitializer.earliest()
        );
    }

    private KafkaSource<String> createSource(String topic, String groupId) {
        return createSource(
                topic,
                groupId,
                OffsetsInitializer.committedOffsets(OffsetResetStrategy.LATEST)
        );
    }

    private KafkaSource<String> createSource(
            String topic,
            String groupId,
            OffsetsInitializer offsetsInitializer
    ) {
        return KafkaSource.<String>builder()
                .setBootstrapServers(properties.getBootstrapServers())
                .setTopics(topic)
                .setGroupId(groupId)
                .setStartingOffsets(offsetsInitializer)
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
    }
}
