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

    /**
     * 初始密码（接口文档 §22.2 字段名）。前端按文档传 initialPassword。
     * 保留旧 password 字段以兼容历史调用方，两者取其一即可（initialPassword 优先）。
     */
    private String initialPassword;

    /** @deprecated 改用 {@link #initialPassword}（对齐接口文档 §22.2）。 */
    @Deprecated
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private Long organizationId;
    private String departmentName;
    private Integer maxSecurityLevel = 0;
    private String dataScope = "own_org";

    /** 角色码列表，如 ["front_archivist"]。 */
    private java.util.List<String> roleCodes;

    /** 取初始密码：initialPassword 优先，兼容旧 password 字段。 */
    public String resolvePassword() {
        if (initialPassword != null && !initialPassword.isBlank()) {
            return initialPassword;
        }
        return password;
    }
}
