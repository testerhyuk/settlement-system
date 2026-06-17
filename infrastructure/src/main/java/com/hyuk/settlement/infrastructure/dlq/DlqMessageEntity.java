package com.hyuk.settlement.infrastructure.dlq;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DlqMessageEntity {
    @Id
    private String dlqMessageId;
    private String sourceTopic;
    private Integer sourcePartition;
    private Long sourceOffset;
    private String exceptionClass;
    private String exceptionMessage;
    @Enumerated(EnumType.STRING)
    private DlqErrorType dlqErrorType;
    private String rawMessage;
    @Enumerated(EnumType.STRING)
    private DlqStatus dlqStatus;
    private Integer retryCount;
    private LocalDateTime createdAt;

    public static DlqMessageEntity create(String sourceTopic, Integer sourcePartition, Long sourceOffset,
                                          String exceptionClass, String exceptionMessage, DlqErrorType dlqErrorType,
                                          String rawMessage, DlqStatus dlqStatus, Integer retryCount) {
        DlqMessageEntity dlqMessageEntity = new DlqMessageEntity();

        dlqMessageEntity.dlqMessageId = UUID.randomUUID().toString();
        dlqMessageEntity.sourceTopic = sourceTopic;
        dlqMessageEntity.sourcePartition = sourcePartition;
        dlqMessageEntity.sourceOffset = sourceOffset;
        dlqMessageEntity.exceptionClass = exceptionClass;
        dlqMessageEntity.exceptionMessage = exceptionMessage;
        dlqMessageEntity.dlqErrorType = dlqErrorType;
        dlqMessageEntity.rawMessage = rawMessage;
        dlqMessageEntity.dlqStatus = dlqStatus;
        dlqMessageEntity.retryCount = retryCount;
        dlqMessageEntity.createdAt = LocalDateTime.now();

        return dlqMessageEntity;
    }

    public void changeDlqStatus(DlqStatus dlqStatus) {
        this.dlqStatus = dlqStatus;
    }

    public void plusRetryCount() {
        this.retryCount++;
    }

    public void markReplayFailed(DlqErrorType errorType, String exceptionMessage) {
        this.dlqStatus = DlqStatus.REPLAY_FAILED;
        this.dlqErrorType = errorType;
        this.exceptionMessage = exceptionMessage;
    }
}
