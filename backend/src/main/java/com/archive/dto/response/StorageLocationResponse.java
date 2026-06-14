package com.archive.dto.response;

import lombok.Data;

/**
 * 架位列表项，含当前档案盒与盒内件数。
 */
@Data
public class StorageLocationResponse {

    private Long id;
    private Long roomId;
    private Integer rackNo;
    private Integer layerNo;
    private Integer boxSlotNo;
    private String locationCode;
    private String status;

    /** 是否被档案盒占用。 */
    private Boolean occupied;
    private Long currentBoxId;
    private String currentBoxNo;
    /** 当前盒内件数。 */
    private Integer boxItemCount;
}
