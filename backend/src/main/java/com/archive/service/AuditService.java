package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.mapper.AuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

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
                    String businessType, Long businessId, Map<String, Object> detail) {
        try {
            Long userId = null;
            String actorType = "system";
            try {
                userId = AuthContext.getCurrentUserId();
                String ut = AuthContext.getUserType();
                if (ut != null) actorType = ut;
            } catch (Exception ignored) {}

            String detailJson = detail != null ? objectMapper.writeValueAsString(detail) : null;

            jdbcTemplate.update(
                    "INSERT INTO audit_logs (actor_user_id, actor_type, module_name, operation_type, " +
                            "business_type, business_id, detail, operated_at) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)",
                    userId, actorType, moduleName, operationType,
                    businessType, businessId, detailJson, OffsetDateTime.now());
        } catch (Exception e) {
            log.error("审计日志写入失败: module={}, op={}, biz={}/{}",
                    moduleName, operationType, businessType, businessId, e);
        }
    }

    /**
     * 记录审计日志（简化版，单 key-value 详情）。
     */
    public void log(String moduleName, String operationType,
                    String businessType, Long businessId,
                    String key, Object value) {
        log(moduleName, operationType, businessType, businessId, Map.of(key, value));
    }
}
