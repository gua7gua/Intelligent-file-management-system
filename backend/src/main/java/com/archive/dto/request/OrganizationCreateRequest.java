package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationCreateRequest {
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 200, message = "组织名称长度不能超过 200")
    private String orgName;

    @NotBlank(message = "组织类型不能为空")
    private String orgType;

    @Size(max = 64, message = "联系人长度不能超过 64")
    private String contactName;

    @Size(max = 32, message = "联系电话长度不能超过 32")
    private String contactPhone;
}
