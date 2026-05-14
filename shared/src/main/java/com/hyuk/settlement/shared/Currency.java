package com.hyuk.settlement.shared;

public enum Currency {
    KRW(0),
    USD(2),
    EUR(2);

    private final int scale;

    Currency(int scale) {
        this.scale = scale;
    }

    public int getScale() {
        return scale;
    }
}
