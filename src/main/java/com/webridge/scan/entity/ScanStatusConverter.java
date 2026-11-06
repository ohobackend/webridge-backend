package com.webridge.scan.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ScanStatusConverter implements AttributeConverter<ScanStatus, String> {

    @Override
    public String convertToDatabaseColumn(ScanStatus status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public ScanStatus convertToEntityAttribute(String value) {
        return value == null ? null : ScanStatus.from(value);
    }
}
