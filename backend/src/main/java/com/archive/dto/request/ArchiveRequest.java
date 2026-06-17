package com.archive.dto.request;

import lombok.Data;

/**
 * 确认入库（9.7）。
 */
@Data
public class ArchiveRequest {

    private Long fondsId;

    /** 纸质相关档案必填。 */
    private Long boxId;

    /**
     * 可选（保留兼容）。盒即架位，架位由档案盒的 locationId 决定，
     * 入库时以盒为准，不再单独要求传 locationId（B5-D1 方案 A）。
     */
    private Long locationId;

    private Integer sortNo;

    private Integer pageCount;
}
