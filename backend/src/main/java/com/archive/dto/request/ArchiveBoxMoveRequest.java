package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移动档案盒请求。
 */
@Data
public class ArchiveBoxMoveRequest {

    @NotNull(message = "目标架位不能为空")
    private Long targetLocationId;

    private String reason;
}
