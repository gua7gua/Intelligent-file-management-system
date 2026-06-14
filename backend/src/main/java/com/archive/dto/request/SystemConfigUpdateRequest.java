package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemConfigUpdateRequest {

    @NotBlank(message = "配置值不能为空")
    private String configValue;
}
