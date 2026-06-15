package com.archive.dto.request;

import com.archive.enums.InventoryCheckResult;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryItemUpdateRequest {

    private String actualLocationCode;

    @NotNull(message = "核对结果不能为空")
    private InventoryCheckResult checkResult;

    private String note;
}
