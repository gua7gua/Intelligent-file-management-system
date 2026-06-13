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

    /** 纸质相关档案必填。 */
    private Long locationId;

    private Integer sortNo;

    private Integer pageCount;
}
