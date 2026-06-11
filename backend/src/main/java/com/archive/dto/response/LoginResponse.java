package com.archive.dto.response;

import lombok.Data;

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
        private List<String> roles;
        private Long organizationId;
        private Integer maxSecurityLevel;
        private String dataScope;
    }
}
