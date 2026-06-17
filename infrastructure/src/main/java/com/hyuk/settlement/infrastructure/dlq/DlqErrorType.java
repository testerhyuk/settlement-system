package com.hyuk.settlement.infrastructure.dlq;

public enum DlqErrorType {
    DESERIALIZATION_ERROR,
    DB_ERROR,
    VALIDATION_ERROR,
    UNKNOWN_ERROR
}
