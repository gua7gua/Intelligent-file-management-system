package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryTaskQuery extends PageRequest {

    private String status;

    private Long roomId;

    private Integer categoryId;
}
