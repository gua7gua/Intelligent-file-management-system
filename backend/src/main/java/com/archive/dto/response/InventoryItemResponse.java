package com.archive.dto.response;

import lombok.Data;

@Data
public class InventoryItemResponse {

    private Long id;
    private Long archiveId;
    private Long boxId;
    private Long expectedLocationId;
    private String actualLocationCode;
    private String checkResult;
    private String note;
}
