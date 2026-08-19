package com.example.wms.shared.domain;

public final class ForbiddenOperationException extends BusinessException {

    public ForbiddenOperationException(String code, String message) {
        super(code, message);
    }
}
