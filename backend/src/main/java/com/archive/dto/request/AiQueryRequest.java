package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiQueryRequest {
    @NotBlank(message = "检索内容不能为空")
    private String text;
}
