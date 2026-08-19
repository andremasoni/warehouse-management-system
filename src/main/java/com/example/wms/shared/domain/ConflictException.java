package com.example.wms.shared.domain;

public final class ConflictException extends BusinessException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
