package com.hyuk.settlement.settlement;

public enum Status {
    PENDING,
    CALCULATED,
    PAID,
    FAILED,
    NEGATIVE_SETTLEMENT,
    RECOVERY_REQUIRED,
    RECOVERED,
    RETRY_EXHAUSTED
}
