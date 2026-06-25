package com.hyuk.settlement.analytics.conversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hyuk.settlement.shared.AdConversionEvent;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class AdConversionParseProcessFunction extends ProcessFunction<String, AdConversionEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final OutputTag<String> PARSE_ERROR_TAG = new OutputTag<String>("ad-conversions-parse-error"){};

    @Override
    public void processElement(String rawMessage, ProcessFunction<String, AdConversionEvent>.Context context, Collector<AdConversionEvent> collector) throws Exception {
        AdConversionEvent event;

        try {
            event = objectMapper.readValue(rawMessage, AdConversionEvent.class);
        } catch (Exception e) {
            String errorMessage = "rawMessage=" + rawMessage + ", " + "errorMessage=" + e.getMessage();
            context.output(PARSE_ERROR_TAG, errorMessage);
            return;
        }

        collector.collect(event);
    }
}
