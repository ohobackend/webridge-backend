package com.webridge.scan.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ScanStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String value;

    ScanStatus(String v) {
        this.value = v;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static ScanStatus from(String v) {
        if (v == null) return null;
        return Arrays.stream(values())
            .filter(s -> s.value.equalsIgnoreCase(v))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScanStatus: " + v));
    }

    public static ScanStatus fromValue(String v) {
        return from(v);
    }
}
