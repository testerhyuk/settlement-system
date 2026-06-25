package com.hyuk.settlement.analytics.source;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class KafkaStreamFactory {
    private final KafkaSourceFactory kafkaSourceFactory;
    private final StreamExecutionEnvironment env;

    public KafkaStreamFactory(StreamExecutionEnvironment env, KafkaSourceFactory kafkaSourceFactory) {
        this.env = env;
        this.kafkaSourceFactory = kafkaSourceFactory;
    }

    public DataStream<String> budgetStream() {
        return env.fromSource(
                kafkaSourceFactory.budgetResultsSource(),
                WatermarkStrategy.noWatermarks(),
                "kafka-budget-results-source"
        );
    }

    public DataStream<String> adImpressionsStream() {
        return env.fromSource(
                kafkaSourceFactory.adImpressionsSource(),
                WatermarkStrategy.noWatermarks(),
                "kafka-ad-impressions-source"
        );
    }

    public DataStream<String> adClicksStream() {
        return env.fromSource(
                kafkaSourceFactory.adClicksSource(),
                WatermarkStrategy.noWatermarks(),
                "kafka-ad-clicks-source"
        );
    }

    public DataStream<String> adConversionsStream() {
        return env.fromSource(
                kafkaSourceFactory.adConversionsSource(),
                WatermarkStrategy.noWatermarks(),
                "kafka-ad-conversions-source"
        );
    }

    public DataStream<String> userSegmentsStream() {
        return env.fromSource(
                kafkaSourceFactory.userSegmentsSource(),
                WatermarkStrategy.noWatermarks(),
                "kafka-user-segments-source"
        );
    }
}
