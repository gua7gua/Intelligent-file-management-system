package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FondsCreateRequest {
    @Size(max = 64, message = "全宗号长度不能超过 64")
    private String fondsNo;

    @NotBlank(message = "全宗名称不能为空")
    @Size(max = 200, message = "全宗名称长度不能超过 200")
    private String fondsName;

    @NotNull(message = "所属组织不能为空")
    private Long organizationId;

    @Size(max = 1000, message = "备注长度不能超过 1000")
    private String description;
}
