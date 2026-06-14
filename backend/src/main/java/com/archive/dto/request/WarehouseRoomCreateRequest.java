package com.archive.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增库房请求。
 */
@Data
public class WarehouseRoomCreateRequest {

    @NotBlank(message = "库房号不能为空")
    private String roomNo;

    @NotBlank(message = "库房名称不能为空")
    private String roomName;

    @NotNull(message = "机架数量不能为空")
    @Min(value = 1, message = "机架数量必须大于 0")
    private Integer rackCount;

    @NotNull(message = "单机架层数不能为空")
    @Min(value = 1, message = "单机架层数必须大于 0")
    private Integer layersPerRack;

    @NotNull(message = "每层盒数不能为空")
    @Min(value = 1, message = "每层盒数必须大于 0")
    private Integer boxesPerLayer;

    /** 占用告警阈值，空时服务层取默认 0.85。 */
    private BigDecimal warningThreshold;
}
