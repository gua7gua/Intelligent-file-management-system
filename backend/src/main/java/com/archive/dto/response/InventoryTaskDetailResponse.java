package com.archive.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryTaskDetailResponse extends InventoryTaskResponse {

    private List<InventoryItemResponse> items;
    private InventoryTaskStatsResponse stats;
}
