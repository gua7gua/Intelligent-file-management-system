package com.archive.dto.response;

import lombok.Data;

@Data
public class PublicRegisterResponse {
    private Long id;
    private String loginName;
    private String phone;
    private String realName;
    private String userType;
    private String status;
}
