package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 架位停用/启用请求。
 */
@Data
public class LocationStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    private String reason;
}
