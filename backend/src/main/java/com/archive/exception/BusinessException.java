package com.archive.exception;

import com.archive.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常，由 Service 层抛出，由 GlobalExceptionHandler 统一捕获。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
