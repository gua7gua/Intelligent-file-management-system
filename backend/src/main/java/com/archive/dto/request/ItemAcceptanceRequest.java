package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 条目验收结果请求。
 */
@Data
public class ItemAcceptanceRequest {

    /** accepted 或 rejected。 */
    @NotBlank(message = "验收结果不能为空")
    private String result;

    private String acceptanceNote;

    /** 回退时必填。 */
    private String rejectReason;
}
