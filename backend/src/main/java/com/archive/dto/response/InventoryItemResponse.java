package com.archive.dto.response;

import lombok.Data;

@Data
public class InventoryItemResponse {

    private Long id;
    private Long archiveId;
    private String archiveNo;
    private String title;
    private Long boxId;
    private Long expectedLocationId;
    private String expectedLocationCode;
    private String actualLocationCode;
    private String checkResult;
    private String note;
    private String loanStatus;
}
