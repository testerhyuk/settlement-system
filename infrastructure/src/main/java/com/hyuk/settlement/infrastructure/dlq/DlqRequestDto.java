package com.hyuk.settlement.infrastructure.dlq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqRequestDto {
    private String dlqMessageId;
    private String sourceTopic;
    private Integer sourcePartition;
    private Long sourceOffset;
    private String exceptionClass;
    private String exceptionMessage;
    private DlqErrorType dlqErrorType;
    private String rawMessage;
    private DlqStatus dlqStatus;
}
