package com.archive.dto.response;

import lombok.Data;

@Data
public class RoleResponse {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
}
