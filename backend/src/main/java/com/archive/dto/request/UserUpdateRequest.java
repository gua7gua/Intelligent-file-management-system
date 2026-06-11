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
}
