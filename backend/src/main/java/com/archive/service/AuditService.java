package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.entity.AuditLog;
import com.archive.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 操作审计日志服务。
 * 所有模块统一调用本服务记录审计日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 记录审计日志。
     *
     * @param moduleName    模块名称（如 M03）
     * @param operationType 操作类型（如 upload、delete、scan_reject）
     * @param businessType  业务对象类型（如 staging_file）
     * @param businessId    业务对象 ID
     * @param detail        详细信息
     */
    public void log(String moduleName, String operationType,
                    String businessType, Long businessId, Object detail) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setModuleName(moduleName);
            auditLog.setOperationType(operationType);
            auditLog.setBusinessType(businessType);
            auditLog.setBusinessId(businessId);
            auditLog.setDetail(detail);
            auditLog.setOperatedAt(OffsetDateTime.now());

            // 尝试获取当前用户信息
            try {
                Long userId = AuthContext.getCurrentUserId();
                auditLog.setActorUserId(userId);
                String userType = AuthContext.getUserType();
                auditLog.setActorType(userType != null ? userType : "system");
            } catch (Exception e) {
                auditLog.setActorType("system");
            }

            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            // 审计日志写入失败不应阻断业务流程，只记录错误
            log.error("审计日志写入失败: module={}, op={}, biz={}/{}",
                    moduleName, operationType, businessType, businessId, e);
        }
    }

    /**
     * 记录审计日志（Map 详情简化版）。
     */
    public void log(String moduleName, String operationType,
                    String businessType, Long businessId,
                    String key, Object value) {
        log(moduleName, operationType, businessType, businessId, Map.of(key, value));
    }
}
