package com.archive.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.archive.common.ErrorCode;
import com.archive.common.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 捕获所有未处理异常并转换为统一 R 响应格式，附带 traceId。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常 — 由 Service 层主动抛出，携带 ErrorCode。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode ec = e.getErrorCode();
        log.warn("业务异常: code={}, message={}", ec.getCode(), e.getMessage());
        return ResponseEntity
                .status(ec.getHttpStatus())
                .body(R.<Void>fail(ec, e.getMessage()).traceId(getTraceId(request)));
    }

    /**
     * Sa-Token 未登录异常。
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("未登录访问: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(R.<Void>fail(ErrorCode.UNAUTHORIZED, "未登录或 token 无效").traceId(getTraceId(request)));
    }

    /**
     * 请求体参数校验失败（@Valid + @RequestBody）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.<Void>fail(ErrorCode.BAD_REQUEST, message).traceId(getTraceId(request)));
    }

    /**
     * 路径/查询参数校验失败（@Validated + @PathVariable/@RequestParam）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数约束违反: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.<Void>fail(ErrorCode.BAD_REQUEST, message).traceId(getTraceId(request)));
    }

    /**
     * 请求体 JSON 解析失败。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.<Void>fail(ErrorCode.BAD_REQUEST, "请求体格式错误").traceId(getTraceId(request)));
    }

    /**
     * 兜底 — 所有未捕获的异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("未预期异常: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.<Void>fail(ErrorCode.INTERNAL_ERROR).traceId(getTraceId(request)));
    }

    private String getTraceId(HttpServletRequest request) {
        return (String) request.getAttribute("traceId");
    }
}
