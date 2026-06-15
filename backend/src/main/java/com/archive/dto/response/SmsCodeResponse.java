package com.archive.dto.response;

import lombok.Data;

@Data
public class SmsCodeResponse {
    /** 是否走了 fallback（固定码）路径，便于前端调试 */
    private boolean fallback;
    private String message;

    public static SmsCodeResponse of(boolean fallback, String message) {
        SmsCodeResponse r = new SmsCodeResponse();
        r.fallback = fallback;
        r.message = message;
        return r;
    }
}
