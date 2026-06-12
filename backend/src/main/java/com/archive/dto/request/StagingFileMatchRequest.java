package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手工匹配暂存文件请求。
 */
@Data
public class StagingFileMatchRequest {

    @NotNull(message = "目标条目 ID 不能为空")
    private Long itemId;

    private String note;
}
