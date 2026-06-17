package com.hyuk.settlement.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hyuk.settlement.infrastructure.dlq.*;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class BudgetResultDltConsumer {
    private final DlqMessageService dlqMessageService;

    @KafkaListener(
            topics = "${app.kafka.budget_results_dlt}",
            groupId = "${app.kafka.budget_results_dlt_group}"
    )
    public void dltConsumer(ConsumerRecord<String, String> record) {
        Headers headers = record.headers();

        String exceptionClass = headerAsString(headers, KafkaHeaders.DLT_EXCEPTION_FQCN);
        String exceptionMessage = headerAsString(headers, KafkaHeaders.DLT_EXCEPTION_MESSAGE);

        String dlqMessageId = headerAsString(headers, "x-dlq-message-id");

        DlqRequestDto dto = DlqRequestDto.builder()
                .dlqMessageId(dlqMessageId)
                .sourceTopic(headerAsString(headers, KafkaHeaders.DLT_ORIGINAL_TOPIC))
                .sourcePartition(headerAsInt(headers, KafkaHeaders.DLT_ORIGINAL_PARTITION))
                .sourceOffset(headerAsLong(headers, KafkaHeaders.DLT_ORIGINAL_OFFSET))
                .exceptionClass(exceptionClass)
                .exceptionMessage(headerAsString(headers, KafkaHeaders.DLT_EXCEPTION_MESSAGE))
                .dlqErrorType(resolveErrorType(exceptionClass, exceptionMessage))
                .rawMessage(record.value())
                .dlqStatus(DlqStatus.PENDING)
                .build();

        dlqMessageService.save(dto);
    }

    private DlqErrorType resolveErrorType(String exceptionClass, String exceptionMessage) {
        String text = ((exceptionClass == null ? "" : exceptionClass) + " " +
                (exceptionMessage == null ? "" : exceptionMessage));

        if (text.contains("JsonProcessingException")
                || text.contains("JsonParseException")
                || text.contains("JsonMappingException")
                || text.contains("역직렬화")
                || text.contains("deserialize")) {
            return DlqErrorType.DESERIALIZATION_ERROR;
        }

        if (text.contains("DataAccessException")
                || text.contains("SQLException")
                || text.contains("JDBCConnectionException")) {
            return DlqErrorType.DB_ERROR;
        }

        if (text.contains("IllegalArgumentException")
                || text.contains("IllegalStateException")) {
            return DlqErrorType.VALIDATION_ERROR;
        }

        return DlqErrorType.UNKNOWN_ERROR;
    }

    private String headerAsString(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private Integer headerAsInt(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }

        return ByteBuffer.wrap(header.value()).getInt();
    }

    private Long headerAsLong(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }

        return ByteBuffer.wrap(header.value()).getLong();
    }
}
