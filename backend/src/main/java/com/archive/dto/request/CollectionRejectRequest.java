package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 征集拒绝请求。
 */
@Data
public class CollectionRejectRequest {

    @NotBlank(message = "拒绝原因不能为空")
    private String rejectReason;
}
