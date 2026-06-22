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
    /** 模块代号对应的中文标签（如 M14 -> 系统配置），未命中返回 null。 */
    private String moduleLabel;
    private String operationType;
    /** 操作类型对应的中文标签（如 approve -> 审批通过），未命中返回 null。 */
    private String operationLabel;
    private String businessType;
    private Long businessId;
    private Map<String, Object> detail;
    private String ipAddress;
    private OffsetDateTime operatedAt;
    /** 操作人姓名（联 users.real_name，system 类型固定「系统」）。 */
    private String actorName;
    /** 关联档案档号（当 business_type=archive 时联 archives.archive_no）。 */
    private String archiveNo;
}
