package com.hyuk.settlement.analytics.performance;

import com.hyuk.settlement.analytics.budget.CampaignBudgetMetric;
import com.hyuk.settlement.analytics.click.AdClickMetric;
import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.analytics.conversion.AdConversionMetric;
import com.hyuk.settlement.analytics.impression.AdImpressionMetric;
import org.apache.flink.streaming.api.datastream.DataStream;

public class CampaignPerformancePipeline {
    private final AnalyticsProperties properties;

    public CampaignPerformancePipeline(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public DataStream<CampaignPerformanceMetric> build(
            DataStream<CampaignBudgetMetric> budgetMetricStream,
            DataStream<AdImpressionMetric> impressionMetricStream,
            DataStream<AdClickMetric> clickMetricStream,
            DataStream<AdConversionMetric> conversionMetricStream
    ) {
        DataStream<CampaignPerformanceInput> budgetInputStream =
                budgetMetricStream.map(CampaignPerformanceInput::fromBudgetMetric);

        DataStream<CampaignPerformanceInput> impressionInputStream =
                impressionMetricStream.map(CampaignPerformanceInput::fromImpressionMetric);

        DataStream<CampaignPerformanceInput> clickInputStream =
                clickMetricStream.map(CampaignPerformanceInput::fromClickMetric);

        DataStream<CampaignPerformanceInput> conversionInputStream =
                conversionMetricStream.map(CampaignPerformanceInput::fromConversionMetric);

        DataStream<CampaignPerformanceInput> performanceInputStream =
                budgetInputStream
                        .union(impressionInputStream, clickInputStream, conversionInputStream);

        return performanceInputStream
                .keyBy(input -> input.getCampaignId() + "|" + input.getWindowStart())
                .process(new CampaignPerformanceProcessFunction(properties.getPerformanceEmitTimeoutMs()));
    }
}
