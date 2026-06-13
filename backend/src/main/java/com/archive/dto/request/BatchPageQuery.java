package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 清单分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchPageQuery extends PageRequest {

    private String status;

    private String keyword;

    private Integer archiveYear;

    /** 过滤来源类型。 */
    private String sourceType;
}
