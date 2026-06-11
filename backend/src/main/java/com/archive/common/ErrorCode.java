package com.archive.common;

import lombok.Getter;

/**
 * 统一错误码枚举。
 */
@Getter
public enum ErrorCode {

    OK(200, "OK", "操作成功"),
    BAD_REQUEST(400, "BAD_REQUEST", "请求参数错误"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录或 token 无效"),
    FORBIDDEN(403, "FORBIDDEN", "权限不足"),
    NOT_FOUND(404, "NOT_FOUND", "资源不存在"),
    BUSINESS_CONFLICT(409, "BUSINESS_CONFLICT", "状态冲突"),
    PAYLOAD_TOO_LARGE(413, "PAYLOAD_TOO_LARGE", "上传文件超过限制"),
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "不支持的文件类型"),
    VALIDATION_FAILED(422, "VALIDATION_FAILED", "业务校验失败"),
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "请求过于频繁"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "系统内部错误"),
    EXTERNAL_SERVICE_ERROR(502, "EXTERNAL_SERVICE_ERROR", "外部服务异常");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
