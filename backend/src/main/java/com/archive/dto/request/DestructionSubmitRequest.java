package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DestructionSubmitRequest {

    @NotBlank(message = "申请理由不能为空")
    private String reason;
}
