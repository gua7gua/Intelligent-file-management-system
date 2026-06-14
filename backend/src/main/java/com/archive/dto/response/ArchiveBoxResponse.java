package com.archive.dto.response;

import lombok.Data;

/**
 * 档案盒列表项。
 */
@Data
public class ArchiveBoxResponse {

    private Long id;
    private String boxNo;
    private Long locationId;
    private String locationCode;
    private String roomNo;
    private Integer categoryId;
    private Long fondsId;
    private String yearLabel;
    private String spineText;
    private Integer capacity;
    private Integer usedCount;
    private String status;
}
