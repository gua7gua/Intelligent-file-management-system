package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryTaskCreateRequest {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotNull(message = "库房不能为空")
    private Long roomId;

    @NotNull(message = "门类不能为空")
    private Integer categoryId;
}
