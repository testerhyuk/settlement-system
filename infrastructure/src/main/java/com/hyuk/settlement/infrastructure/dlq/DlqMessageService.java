package com.hyuk.settlement.infrastructure.dlq;

import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Getter
public class DlqMessageService {
    private final DlqMessageRepository dlqMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public DlqMessageService(
            DlqMessageRepository dlqMessageRepository,
            @Qualifier("dltKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.dlqMessageRepository = dlqMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void markAsReplayed(String dlqMessageId) {
        DlqMessageEntity entity = dlqMessageRepository.findById(dlqMessageId).orElseThrow(() -> new RuntimeException("DlqMessageId not found"));

        if (!entity.getDlqStatus().equals(DlqStatus.REPLAY_REQUESTED)) {
            throw new IllegalStateException("REPLAY_REQUESTED 상태만 REPLAYED로 변경할 수 있습니다");
        }

        entity.changeDlqStatus(DlqStatus.REPLAYED);
    }

    @Transactional
    public void replay(String dlqMessageId) {
        DlqMessageEntity entity = dlqMessageRepository.findById(dlqMessageId).orElseThrow(() -> new RuntimeException("DlqMessageId not found"));

        if (!entity.getDlqStatus().equals(DlqStatus.PENDING)) {
            throw new IllegalStateException("DlqStatus is not PENDING");
        }

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(entity.getSourceTopic(), entity.getRawMessage());
            record.headers().add(
                    "x-dlq-message-id",
                    dlqMessageId.getBytes(StandardCharsets.UTF_8)
            );

            kafkaTemplate.send(record).get();
            entity.changeDlqStatus(DlqStatus.REPLAY_REQUESTED);
            entity.plusRetryCount();
        } catch (Exception e) {
            throw new IllegalStateException("DLQ 메시지 재발행 실패 -> dlqMessageId : " + entity.getDlqMessageId(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DlqResponseDto> getDlqMessage() {
        List<DlqMessageEntity> entities = dlqMessageRepository.findByDlqStatus(DlqStatus.PENDING);

        return entities.stream()
                .map(entity -> DlqResponseDto.builder()
                        .dlqMessageId(entity.getDlqMessageId())
                        .sourceTopic(entity.getSourceTopic())
                        .sourcePartition(entity.getSourcePartition())
                        .sourceOffset(entity.getSourceOffset())
                        .exceptionClass(entity.getExceptionClass())
                        .exceptionMessage(entity.getExceptionMessage())
                        .dlqErrorType(entity.getDlqErrorType())
                        .rawMessage(entity.getRawMessage())
                        .dlqStatus(entity.getDlqStatus())
                        .createdAt(entity.getCreatedAt())
                        .retryCount(entity.getRetryCount())
                        .build())
                .toList();
    }

    @Transactional
    public void discardDlqMessage(String dlqMessageId) {
        DlqMessageEntity entity = dlqMessageRepository.findById(dlqMessageId).orElseThrow(() -> new RuntimeException("DlqMessageId not found"));
        entity.changeDlqStatus(DlqStatus.DISCARDED);
    }

    @Transactional
    public DlqResponseDto save(DlqRequestDto request) {
        if (request.getDlqMessageId() != null) {
            DlqMessageEntity entity = dlqMessageRepository.findById(request.getDlqMessageId()).orElseThrow(() -> new RuntimeException("DlqMessageId not found"));
            entity.markReplayFailed(
                    request.getDlqErrorType(),
                    request.getExceptionMessage()
            );

            return toResponse(entity);
        }

        DlqMessageEntity entity = DlqMessageEntity.create(
                request.getSourceTopic(),
                request.getSourcePartition(),
                request.getSourceOffset(),
                request.getExceptionClass(),
                request.getExceptionMessage(),
                request.getDlqErrorType(),
                request.getRawMessage(),
                request.getDlqStatus(),
                0
        );

        dlqMessageRepository.save(entity);

        return toResponse(entity);
    }

    private DlqResponseDto toResponse(DlqMessageEntity entity) {
        return DlqResponseDto.builder()
                .dlqMessageId(entity.getDlqMessageId())
                .sourceTopic(entity.getSourceTopic())
                .sourcePartition(entity.getSourcePartition())
                .sourceOffset(entity.getSourceOffset())
                .exceptionClass(entity.getExceptionClass())
                .exceptionMessage(entity.getExceptionMessage())
                .dlqErrorType(entity.getDlqErrorType())
                .rawMessage(entity.getRawMessage())
                .dlqStatus(entity.getDlqStatus())
                .createdAt(entity.getCreatedAt())
                .retryCount(entity.getRetryCount())
                .build();
    }
}
