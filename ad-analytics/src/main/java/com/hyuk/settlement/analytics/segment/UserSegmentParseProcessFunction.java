package com.hyuk.settlement.analytics.segment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hyuk.settlement.shared.UserSegmentEvent;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.ArrayList;

public class UserSegmentParseProcessFunction extends ProcessFunction<String, UserSegmentEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final OutputTag<String> PARSE_ERROR_TAG = new OutputTag<String>("user-segments-parse-error"){};

    @Override
    public void processElement(String rawMessage, ProcessFunction<String, UserSegmentEvent>.Context context, Collector<UserSegmentEvent> collector) throws Exception {
        UserSegmentEvent userSegment;

        try {
            userSegment = objectMapper.readValue(rawMessage, UserSegmentEvent.class);
        } catch (Exception e) {
            String errorMessage = "rawMessage=" + rawMessage + ", errorMessage=" + e.getMessage();
            context.output(PARSE_ERROR_TAG, errorMessage);
            return;
        }

        collector.collect(userSegment);
    }
}
