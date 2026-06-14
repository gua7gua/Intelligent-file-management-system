package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserStatusRequest {

    /** active / disabled。 */
    @NotBlank(message = "状态不能为空")
    private String status;

    private String reason;
}
