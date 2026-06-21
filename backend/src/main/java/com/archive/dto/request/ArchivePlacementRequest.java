package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 档案换盒请求（P0-1）：把档案从当前档案盒换到目标档案盒（同分类）。
 */
@Data
public class ArchivePlacementRequest {

    @NotNull(message = "目标档案盒不能为空")
    private Long boxId;
}
