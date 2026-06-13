package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 操作审计日志。
 */
@Data
@TableName(value = "audit_logs", autoResultMap = true)
public class AuditLog {

    private Long id;

    private Long actorUserId;

    private String actorType;

    private String moduleName;

    private String operationType;

    private String businessType;

    private Long businessId;

    @TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.OTHER)
    private Map<String, Object> detail;

    private String ipAddress;

    private OffsetDateTime operatedAt;
}
