package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增档案盒请求。
 */
@Data
public class ArchiveBoxCreateRequest {

    @NotNull(message = "架位不能为空")
    private Long locationId;

    private Integer categoryId;

    private Long fondsId;

    private String yearLabel;

    private String spineText;

    private Integer capacity;
}
