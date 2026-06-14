package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户类型不能为空")
    private String userType;

    @NotBlank(message = "登录名不能为空")
    private String loginName;

    private String employeeNo;
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private Long organizationId;
    private String departmentName;
    private Integer maxSecurityLevel = 0;
    private String dataScope = "own_org";

    /** 角色码列表，如 ["front_archivist"]。 */
    private java.util.List<String> roleCodes;
}
