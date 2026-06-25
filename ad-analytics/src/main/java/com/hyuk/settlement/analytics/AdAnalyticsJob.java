package com.hyuk.settlement.analytics;

import com.hyuk.settlement.analytics.budget.BudgetMetricPipeline;
import com.hyuk.settlement.analytics.budget.BudgetMetricPipelineResult;
import com.hyuk.settlement.analytics.budget.CampaignBudgetMetric;
import com.hyuk.settlement.analytics.click.AdClickMetric;
import com.hyuk.settlement.analytics.click.AdClickMetricPipeline;
import com.hyuk.settlement.analytics.click.AdClickMetricPipelineResult;
import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.analytics.conversion.AdConversionMetric;
import com.hyuk.settlement.analytics.conversion.AdConversionMetricPipeline;
import com.hyuk.settlement.analytics.conversion.AdConversionMetricPipelineResult;
import com.hyuk.settlement.analytics.impression.AdImpressionMetric;
import com.hyuk.settlement.analytics.impression.AdImpressionMetricPipeline;
import com.hyuk.settlement.analytics.impression.AdImpressionsMetricPipelineResult;
import com.hyuk.settlement.analytics.performance.CampaignPerformanceMetric;
import com.hyuk.settlement.analytics.performance.CampaignPerformancePipeline;
import com.hyuk.settlement.analytics.segment.SegmentPerformanceMetric;
import com.hyuk.settlement.analytics.segment.SegmentPerformancePipeline;
import com.hyuk.settlement.analytics.segment.SegmentPerformancePipelineResult;
import com.hyuk.settlement.analytics.segment.UserSegmentPipeline;
import com.hyuk.settlement.analytics.segment.UserSegmentPipelineResult;
import com.hyuk.settlement.analytics.sink.ClickHouseSinkFactory;
import com.hyuk.settlement.analytics.sink.KafkaSinkFactory;
import com.hyuk.settlement.analytics.source.KafkaSourceFactory;
import com.hyuk.settlement.analytics.source.KafkaStreamFactory;
import com.hyuk.settlement.shared.AdClickEvent;
import com.hyuk.settlement.shared.AdConversionEvent;
import com.hyuk.settlement.shared.AdImpressionEvent;
import com.hyuk.settlement.shared.UserSegmentEvent;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.RestartStrategyOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.core.execution.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;

public class AdAnalyticsJob {
    public static void main(String[] args) throws Exception {
        ParameterTool parameters = ParameterTool.fromPropertiesFile("src/main/resources/ad-analytics.properties");


        AnalyticsProperties properties = AnalyticsProperties.from(parameters);

        KafkaSinkFactory kafkaSinkFactory = new KafkaSinkFactory(properties);

        Configuration configuration = new Configuration();

        configuration.set(
                TaskManagerOptions.NETWORK_MEMORY_MIN,
                MemorySize.parse(properties.getFlinkNetworkMemoryMin())
        );

        configuration.set(
                TaskManagerOptions.NETWORK_MEMORY_MAX,
                MemorySize.parse(properties.getFlinkNetworkMemoryMax())
        );

        configuration.set(
                RestartStrategyOptions.RESTART_STRATEGY,
                "fixed-delay"
        );

        configuration.set(
                RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS,
                properties.getFlinkRestartAttempts()
        );

        configuration.set(
                RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_DELAY,
                Duration.ofMillis(properties.getFlinkRestartDelayMs())
        );

        configuration.setString(
                "metrics.reporter.prometheus.factory.class",
                "org.apache.flink.metrics.prometheus.PrometheusReporterFactory"
        );

        configuration.setString(
                "metrics.reporter.prometheus.port",
                properties.getFlinkPrometheusMetricsPort()
        );

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(configuration);

        env.setParallelism(properties.getFlinkParallelism());

        env.enableCheckpointing(
                properties.getFlinkCheckpointIntervalMs(),
                CheckpointingMode.EXACTLY_ONCE
        );
        env.getCheckpointConfig()
                .setCheckpointTimeout(properties.getFlinkCheckpointTimeoutMs());

        env.getCheckpointConfig()
                .setMinPauseBetweenCheckpoints(properties.getFlinkMinPauseBetweenCheckpointsMs());

        env.getCheckpointConfig()
                .setExternalizedCheckpointRetention(
                        ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION
                );

        env.getCheckpointConfig()
                .setTolerableCheckpointFailureNumber(
                        properties.getFlinkTolerableCheckpointFailureNumber()
                );

        KafkaSourceFactory sourceFactory = new KafkaSourceFactory(properties);
        KafkaStreamFactory streamFactory = new KafkaStreamFactory(env, sourceFactory);

        DataStream<String> budgetStream = streamFactory.budgetStream();
        DataStream<String> adImpressionsStream = streamFactory.adImpressionsStream();
        DataStream<String> adClicksStream = streamFactory.adClicksStream();
        DataStream<String> adConversionsStream = streamFactory.adConversionsStream();
        DataStream<String> userSegmentsStream = streamFactory.userSegmentsStream();

        // Budget
        BudgetMetricPipeline budgetMetricPipeline = new BudgetMetricPipeline(properties);
        BudgetMetricPipelineResult budgetMetricPipelineResult = budgetMetricPipeline.build(budgetStream);
        DataStream<CampaignBudgetMetric> budgetMetricStream = budgetMetricPipelineResult.metricStream();
        budgetMetricPipelineResult.parseErrorStream().sinkTo(kafkaSinkFactory.budgetResultsParseDlqSink());

        // Ad Impressions
        AdImpressionMetricPipeline adImpressionMetricPipeline = new AdImpressionMetricPipeline(properties);
        AdImpressionsMetricPipelineResult adImpressionsMetricPipelineResult = adImpressionMetricPipeline.build(adImpressionsStream);
        DataStream<AdImpressionMetric> impressionMetricStream = adImpressionsMetricPipelineResult.metricStream();
        adImpressionsMetricPipelineResult.parseErrorStream().sinkTo(kafkaSinkFactory.adImpressionsParseDlqSink());
        DataStream<AdImpressionEvent> impressionEventStream = adImpressionsMetricPipelineResult.eventStream();

        // Ad Click
        AdClickMetricPipeline adClickMetricPipeline = new AdClickMetricPipeline(properties);
        AdClickMetricPipelineResult adClickMetricPipelineResult = adClickMetricPipeline.build(adClicksStream);
        DataStream<AdClickMetric> clickMetricStream = adClickMetricPipelineResult.metricStream();
        adClickMetricPipelineResult.parseErrorStream().sinkTo(kafkaSinkFactory.adClicksParseDlqSink());
        DataStream<AdClickEvent> clickEventStream = adClickMetricPipelineResult.eventStream();

        // Ad Conversions
        AdConversionMetricPipeline adConversionMetricPipeline = new AdConversionMetricPipeline(properties);
        AdConversionMetricPipelineResult adConversionMetricPipelineResult = adConversionMetricPipeline.build(adConversionsStream);
        DataStream<AdConversionMetric> conversionMetricStream = adConversionMetricPipelineResult.metricStream();
        adConversionMetricPipelineResult.parseErrorStream().sinkTo(kafkaSinkFactory.adConversionsParseDlqSink());
        DataStream<AdConversionEvent> conversionEventStream = adConversionMetricPipelineResult.eventStream();

        // User Segment
        UserSegmentPipeline userSegmentPipeline = new UserSegmentPipeline(properties);
        UserSegmentPipelineResult userSegmentPipelineResult = userSegmentPipeline.build(userSegmentsStream);
        DataStream<UserSegmentEvent> userSegmentStream = userSegmentPipelineResult.eventStream();
        userSegmentPipelineResult.parseErrorStream().sinkTo(kafkaSinkFactory.userSegmentsParseDlqSink());

        // Performance
        SegmentPerformancePipeline segmentPerformancePipeline =
                new SegmentPerformancePipeline(properties);

        SegmentPerformancePipelineResult segmentPerformancePipelineResult =
                segmentPerformancePipeline.build(
                        impressionEventStream,
                        clickEventStream,
                        conversionEventStream,
                        userSegmentStream
                );

        DataStream<SegmentPerformanceMetric> segmentPerformanceStream =
                segmentPerformancePipelineResult.metricStream();

        segmentPerformancePipelineResult.unknownSegmentStream()
                .sinkTo(kafkaSinkFactory.unknownSegmentsDlqSink());

        CampaignPerformancePipeline campaignPerformancePipeline = new CampaignPerformancePipeline(properties);
        DataStream<CampaignPerformanceMetric> performanceStream = campaignPerformancePipeline.build(
                budgetMetricStream,
                impressionMetricStream,
                clickMetricStream,
                conversionMetricStream
        );

        ClickHouseSinkFactory clickHouseSinkFactory = new ClickHouseSinkFactory(properties);
        budgetMetricStream.sinkTo(clickHouseSinkFactory.campaignBudgetMetricSink());
        impressionMetricStream.sinkTo(clickHouseSinkFactory.adImpressionMetricSink());
        clickMetricStream.sinkTo(clickHouseSinkFactory.adClickMetricSink());
        conversionMetricStream.sinkTo(clickHouseSinkFactory.adConversionMetricSink());
        performanceStream.sinkTo(clickHouseSinkFactory.campaignPerformanceMetricSink());
        segmentPerformanceStream.sinkTo(clickHouseSinkFactory.segmentPerformanceMetricSink());

        env.execute("ad-analytics-job");
    }
}
