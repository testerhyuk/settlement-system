package com.hyuk.settlement.analytics.impression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hyuk.settlement.shared.AdImpressionEvent;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class AdImpressionParseProcessFunction extends ProcessFunction<String, AdImpressionEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final OutputTag<String> PARSE_ERROR_TAG = new OutputTag<String>("ad-impressions-parse-error"){};

    @Override
    public void processElement(String s, ProcessFunction<String, AdImpressionEvent>.Context context, Collector<AdImpressionEvent> collector) throws Exception {
        AdImpressionEvent impressionStream;

        try {
            impressionStream = objectMapper.readValue(s, AdImpressionEvent.class);
        } catch (Exception e) {
            String errorMessage = "rawMessage=" + s + ", " + "errorMessage=" + e.getMessage();
            context.output(PARSE_ERROR_TAG, errorMessage);
            return;
        }

        collector.collect(impressionStream);
    }
}
