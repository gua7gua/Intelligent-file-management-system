package com.archive.dto.request;

import lombok.Data;

/**
 * 删除或作废档案文件（10.8）。
 */
@Data
public class ArchiveFileDeleteRequest {

    private String reason;
}
