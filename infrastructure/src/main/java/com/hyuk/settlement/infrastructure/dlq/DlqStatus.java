package com.hyuk.settlement.infrastructure.dlq;

public enum DlqStatus {
    PENDING,
    REPLAY_REQUESTED,
    REPLAY_FAILED,
    REPLAYED,
    DISCARDED
}
