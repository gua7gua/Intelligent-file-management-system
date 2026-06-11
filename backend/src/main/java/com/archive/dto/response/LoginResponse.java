package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class LoginResponse {

    private String token;
    private UserInfoResponse user;
    private String defaultRoute;

    @Data
    public static class UserInfoResponse {
        private Long id;
        private String realName;
        private String userType;
        private String loginName;
        private String employeeNo;
        private String phone;
        private List<String> roles;
        private Long organizationId;
        private String departmentName;
        private Integer maxSecurityLevel;
        private String dataScope;
        private String status;
        private OffsetDateTime createdAt;
    }
}
