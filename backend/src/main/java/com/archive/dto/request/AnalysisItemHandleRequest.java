package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalysisItemHandleRequest {

    /** adopted 或 rejected。 */
    @NotBlank(message = "处理动作不能为空")
    private String action;

    private String note;
}
