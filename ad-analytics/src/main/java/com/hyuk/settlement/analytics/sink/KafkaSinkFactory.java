package com.hyuk.settlement.analytics.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.analytics.segment.UnknownSegmentEvent;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;

public class KafkaSinkFactory {
    private final AnalyticsProperties properties;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public KafkaSinkFactory(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public KafkaSink<String> budgetResultsParseDlqSink() {
        return stringSink(properties.getBudgetResultsParseDlqTopic());
    }

    public KafkaSink<String> adImpressionsParseDlqSink() {
        return stringSink(properties.getAdImpressionsParseDlqTopic());
    }

    public KafkaSink<String> adClicksParseDlqSink() {
        return stringSink(properties.getAdClicksParseDlqTopic());
    }

    public KafkaSink<String> adConversionsParseDlqSink() {
        return stringSink(properties.getAdConversionsParseDlqTopic());
    }

    public KafkaSink<String> userSegmentsParseDlqSink() {
        return stringSink(properties.getUserSegmentsParseDlqTopic());
    }

    public KafkaSink<UnknownSegmentEvent> unknownSegmentsDlqSink() {
        return KafkaSink.<UnknownSegmentEvent>builder()
                .setBootstrapServers(properties.getBootstrapServers())
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.builder()
                                .setTopic(properties.getUnknownSegmentsDlqTopic())
                                .setValueSerializationSchema(unknownSegmentSerializationSchema())
                                .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
    }

    private SerializationSchema<UnknownSegmentEvent> unknownSegmentSerializationSchema() {
        return event -> {
            try {
                return OBJECT_MAPPER.writeValueAsBytes(event);
            } catch (Exception e) {
                throw new RuntimeException("UnknownSegmentEvent 직렬화 실패", e);
            }
        };
    }

    private KafkaSink<String> stringSink(String topic) {
        return KafkaSink.<String>builder()
                .setBootstrapServers(properties.getBootstrapServers())
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.builder()
                                .setTopic(topic)
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
    }
}