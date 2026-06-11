package com.archive.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应封装。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;

    private R() {}

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ErrorCode.OK.getCode();
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        R<T> r = new R<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getDefaultMessage();
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        R<T> r = new R<>();
        r.code = errorCode.getCode();
        r.message = message;
        return r;
    }

    public R<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}
