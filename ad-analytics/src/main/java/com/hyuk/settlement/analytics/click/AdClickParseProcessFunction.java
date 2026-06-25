package com.hyuk.settlement.analytics.click;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hyuk.settlement.shared.AdClickEvent;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class AdClickParseProcessFunction extends ProcessFunction<String, AdClickEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final OutputTag<String> PARSE_ERROR_TAG = new OutputTag<String>("ad-clicks-parse-error"){};

    @Override
    public void processElement(String s, ProcessFunction<String, AdClickEvent>.Context context, Collector<AdClickEvent> collector) throws Exception {
        AdClickEvent clickStream;

        try {
            clickStream = objectMapper.readValue(s, AdClickEvent.class);

        } catch (Exception e) {
            String errorMessage = "rawMessage=" + s + ", " + "errorMessage=" + e.getMessage();
            context.output(PARSE_ERROR_TAG, errorMessage);
            return;
        }

        collector.collect(clickStream);
    }
}
