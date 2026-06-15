package com.archive.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 组织更新请求（§17.6）。
 * 支持部分更新：仅传入的字段会被更新，未传入（null）保持原值。
 * status 支持 active/disabled——停用而非物理删除，保留历史档案归属。
 */
@Data
public class OrganizationUpdateRequest {

    @Size(max = 200, message = "组织名称长度不能超过 200")
    private String orgName;

    @Size(max = 32, message = "组织类型长度不能超过 32")
    private String orgType;

    @Size(max = 64, message = "联系人长度不能超过 64")
    private String contactName;

    @Size(max = 32, message = "联系电话长度不能超过 32")
    private String contactPhone;

    @Pattern(regexp = "active|disabled", message = "status 只能为 active 或 disabled")
    private String status;
}
