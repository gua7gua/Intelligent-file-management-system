package com.archive.dto.request;

import lombok.Data;

/**
 * 完成批次验收请求。
 */
@Data
public class BatchCompleteRequest {

    private String acceptanceNote;
}
