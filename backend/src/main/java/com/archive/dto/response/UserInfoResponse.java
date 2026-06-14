package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class UserInfoResponse {

    private Long id;
    private String userType;
    private String loginName;
    private String employeeNo;
    private String phone;
    private String realName;
    private Long organizationId;
    private String organizationName;
    private String departmentName;
    private Integer maxSecurityLevel;
    private String dataScope;
    private String status;
    private List<String> roles;
    private OffsetDateTime createdAt;

    /** 最近操作摘要（仅用户详情接口填充）。 */
    private List<RecentOperation> recentOperations;

    @lombok.Data
    public static class RecentOperation {
        private String moduleName;
        private String operationType;
        private OffsetDateTime operatedAt;
    }
}
