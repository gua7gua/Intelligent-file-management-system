package com.archive.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求基类。
 */
@Data
public class PageRequest {

    @Min(value = 1, message = "pageNo 最小为 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    private Integer pageSize = 20;

    private String sortBy;

    private String sortOrder = "asc";
}
