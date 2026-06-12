package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 操作审计日志。
 */
@Data
@TableName("audit_logs")
public class AuditLog {

    private Long id;

    private Long actorUserId;

    private String actorType;

    private String moduleName;

    private String operationType;

    private String businessType;

    private Long businessId;

    private Object detail;

    private String ipAddress;

    private OffsetDateTime operatedAt;
}
