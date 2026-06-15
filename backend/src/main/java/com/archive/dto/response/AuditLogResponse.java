package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class AuditLogResponse {
    private Long id;
    private Long actorUserId;
    private String actorType;
    private String moduleName;
    private String operationType;
    private String businessType;
    private Long businessId;
    private Map<String, Object> detail;
    private String ipAddress;
    private OffsetDateTime operatedAt;
}
