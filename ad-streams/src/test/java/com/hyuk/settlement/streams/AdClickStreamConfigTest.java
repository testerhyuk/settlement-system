package com.hyuk.settlement.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.settlement.shared.BudgetEvent;
import com.hyuk.settlement.shared.BudgetResult;
import com.hyuk.settlement.shared.Currency;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AdClickStreamConfigTest {

    private static final String INPUT_TOPIC = "budget-events";
    private static final String OUTPUT_TOPIC = "budget-results";

    private ObjectMapper objectMapper;
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> outputTopic;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        StreamsBuilder builder = new StreamsBuilder();

        AdClickStreamConfig config = new AdClickStreamConfig(objectMapper);

        ReflectionTestUtils.setField(config, "BUDGET_EVENTS_TOPIC", INPUT_TOPIC);
        ReflectionTestUtils.setField(config, "BUDGET_RESULTS_TOPIC", OUTPUT_TOPIC);

        config.adClickStream(builder);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ad-click-streams-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        testDriver = new TopologyTestDriver(builder.build(), props);

        inputTopic = testDriver.createInputTopic(
                INPUT_TOPIC,
                new StringSerializer(),
                new StringSerializer()
        );

        outputTopic = testDriver.createOutputTopic(
                OUTPUT_TOPIC,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    @AfterEach
    void tearDown() {
        if (testDriver != null) {
            testDriver.close();
        }
    }

    @Test
    void 예산을_충전하면_CHARGED_결과를_발행한다() throws Exception {
        BudgetEvent event = BudgetEvent.builder()
                .eventId("charge-1")
                .campaignId("campaign-1")
                .amount(BigDecimal.valueOf(1000))
                .currency(Currency.KRW)
                .type(BudgetEvent.BudgetEventType.CHARGE)
                .build();

        inputTopic.pipeInput("campaign-1", objectMapper.writeValueAsString(event));

        KeyValue<String, String> output = outputTopic.readKeyValue();
        BudgetResult result = objectMapper.readValue(output.value, BudgetResult.class);

        assertThat(output.key).isEqualTo("campaign-1");
        assertThat(result.getResultType()).isEqualTo(BudgetResult.BudgetResultType.CHARGED);
        assertThat(result.getRemainingBudget()).isEqualByComparingTo("1000");
    }

    @Test
    void 예산을_차감하면_DEDUCTED_결과를_발행한다() throws Exception {
        BudgetEvent chargeEvent = BudgetEvent.builder()
                .eventId("charge-2")
                .campaignId("campaign-2")
                .amount(BigDecimal.valueOf(1000))
                .currency(Currency.KRW)
                .type(BudgetEvent.BudgetEventType.CHARGE)
                .build();

        BudgetEvent deductEvent = BudgetEvent.builder()
                .eventId("deduct-2")
                .campaignId("campaign-2")
                .amount(BigDecimal.valueOf(300))
                .currency(Currency.KRW)
                .type(BudgetEvent.BudgetEventType.DEDUCT)
                .build();

        inputTopic.pipeInput("campaign-2", objectMapper.writeValueAsString(chargeEvent));
        outputTopic.readKeyValue();

        inputTopic.pipeInput("campaign-2", objectMapper.writeValueAsString(deductEvent));

        KeyValue<String, String> output = outputTopic.readKeyValue();
        BudgetResult result = objectMapper.readValue(output.value, BudgetResult.class);

        assertThat(output.key).isEqualTo("campaign-2");
        assertThat(result.getEventId()).isEqualTo("deduct-2");
        assertThat(result.getResultType()).isEqualTo(BudgetResult.BudgetResultType.DEDUCTED);
        assertThat(result.getRequestAmount()).isEqualByComparingTo("300");
        assertThat(result.getRemainingBudget()).isEqualByComparingTo("700");
        assertThat(result.getReason()).isNull();
    }

    @Test
    void 잔액이_부족하면_REJECTED_결과를_발행하고_예산을_차감하지_않는다() throws Exception {
        BudgetEvent chargeEvent = BudgetEvent.builder()
                .eventId("charge-3")
                .campaignId("campaign-3")
                .amount(BigDecimal.valueOf(100))
                .currency(Currency.KRW)
                .type(BudgetEvent.BudgetEventType.CHARGE)
                .build();

        BudgetEvent deductEvent = BudgetEvent.builder()
                .eventId("deduct-3")
                .campaignId("campaign-3")
                .amount(BigDecimal.valueOf(300))
                .currency(Currency.KRW)
                .type(BudgetEvent.BudgetEventType.DEDUCT)
                .build();

        inputTopic.pipeInput("campaign-3", objectMapper.writeValueAsString(chargeEvent));
        outputTopic.readKeyValue();

        inputTopic.pipeInput("campaign-3", objectMapper.writeValueAsString(deductEvent));

        KeyValue<String, String> output = outputTopic.readKeyValue();
        BudgetResult result = objectMapper.readValue(output.value, BudgetResult.class);

        assertThat(output.key).isEqualTo("campaign-3");
        assertThat(result.getEventId()).isEqualTo("deduct-3");
        assertThat(result.getResultType()).isEqualTo(BudgetResult.BudgetResultType.REJECTED);
        assertThat(result.getReason()).isEqualTo(BudgetResult.FailureReason.INSUFFICIENT_BUDGET);
        assertThat(result.getRequestAmount()).isEqualByComparingTo("300");
        assertThat(result.getRemainingBudget()).isEqualByComparingTo("100");
    }

    @Test
    void 같은_eventId가_중복으로_들어오면_한_번만_처리한다() throws Exception {
        BudgetEvent chargeEvent = BudgetEvent.builder()
                .eventId("charge-4")
                .campaignId("campaign-4")
                .amount(BigDecimal.valueOf(1000))
                .currency(Currency.KRW)
                .type(BudgetEvent.BudgetEventType.CHARGE)
                .build();

        BudgetEvent firstDeductEvent = BudgetEvent.builder()
                .eventId("deduct-4")
                .campaignId("campaign-4")
                .amount(BigDecimal.valueOf(300))
                .currency(Currency.KRW)
                .type(BudgetEvent.BudgetEventType.DEDUCT)
                .build();

        BudgetEvent duplicatedDeductEvent = BudgetEvent.builder()
                .eventId("deduct-4")
                .campaignId("campaign-4")
                .amount(BigDecimal.valueOf(300))
                .currency(Currency.KRW)
                .type(BudgetEvent.BudgetEventType.DEDUCT)
                .build();

        inputTopic.pipeInput("campaign-4", objectMapper.writeValueAsString(chargeEvent));
        outputTopic.readKeyValue();

        inputTopic.pipeInput("campaign-4", objectMapper.writeValueAsString(firstDeductEvent));

        KeyValue<String, String> output = outputTopic.readKeyValue();
        BudgetResult result = objectMapper.readValue(output.value, BudgetResult.class);

        inputTopic.pipeInput("campaign-4", objectMapper.writeValueAsString(duplicatedDeductEvent));

        assertThat(output.key).isEqualTo("campaign-4");
        assertThat(result.getEventId()).isEqualTo("deduct-4");
        assertThat(result.getResultType()).isEqualTo(BudgetResult.BudgetResultType.DEDUCTED);
        assertThat(result.getRemainingBudget()).isEqualByComparingTo("700");
        assertThat(outputTopic.isEmpty()).isTrue();
    }
}