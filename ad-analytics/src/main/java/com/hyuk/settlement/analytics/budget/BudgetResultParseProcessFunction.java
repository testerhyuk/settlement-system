package com.hyuk.settlement.analytics.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hyuk.settlement.shared.BudgetResult;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class BudgetResultParseProcessFunction extends ProcessFunction<String, BudgetResult> {
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final OutputTag<String> PARSE_ERROR_TAG = new OutputTag<String>("budget-result-parse-error"){};

    @Override
    public void processElement(String s, ProcessFunction<String, BudgetResult>.Context context, Collector<BudgetResult> collector) throws Exception {
        BudgetResult budgetStream;

        try {
            budgetStream = objectMapper.readValue(s, BudgetResult.class);
        } catch (Exception e) {
            String errorMessage = "rawMessage=" + s + ", errorMessage=" + e.getMessage();
            context.output(PARSE_ERROR_TAG, errorMessage);
            return;
        }

        collector.collect(budgetStream);
    }
}
