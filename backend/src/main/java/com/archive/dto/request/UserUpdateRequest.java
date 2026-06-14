package com.archive.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String phone;
    private String realName;
    private Long organizationId;
    private String departmentName;
    private Integer maxSecurityLevel;
    private String dataScope;
    private String status;

    /** 角色码列表；非空时整体覆盖用户角色。 */
    private java.util.List<String> roleCodes;
}
