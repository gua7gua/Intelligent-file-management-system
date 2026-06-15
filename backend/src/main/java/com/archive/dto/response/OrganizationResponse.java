package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class OrganizationResponse {
    private Long id;
    private String orgName;
    private String orgType;
    private String contactName;
    private String contactPhone;
    private String status;
    private OffsetDateTime createdAt;
}
