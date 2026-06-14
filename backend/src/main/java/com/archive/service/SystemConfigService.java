package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.SystemConfigBatchUpdateRequest;
import com.archive.dto.request.SystemConfigUpdateRequest;
import com.archive.dto.response.SystemConfigResponse;
import com.archive.entity.SystemConfig;
import com.archive.exception.BusinessException;
import com.archive.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务。
 * 敏感密钥脱敏返回；按 valueType 校验值；editable 控制可改性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;
    private final AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 22.8 查询系统配置 ====================

    public List<SystemConfigResponse> listConfigs() {
        List<SystemConfig> configs = configMapper.selectList(null);
        return configs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private SystemConfigResponse toResponse(SystemConfig c) {
        SystemConfigResponse r = new SystemConfigResponse();
        r.setId(c.getId());
        r.setConfigKey(c.getConfigKey());
        r.setConfigValue(isSensitive(c.getConfigKey()) ? "***" : c.getConfigValue());
        r.setValueType(c.getValueType());
        r.setDescription(c.getDescription());
        r.setEditable(c.getEditable());
        return r;
    }

    private boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return lower.contains("key") || lower.contains("secret")
                || lower.contains("password") || lower.contains("token");
    }

    // ==================== 22.9 更新系统配置 ====================

    @Transactional
    public SystemConfigResponse updateConfig(String configKey, SystemConfigUpdateRequest req) {
        SystemConfig cfg = loadByKey(configKey);
        if (!Boolean.TRUE.equals(cfg.getEditable())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "该配置项不可编辑");
        }
        validateValueType(cfg.getValueType(), req.getConfigValue());

        cfg.setConfigValue(req.getConfigValue());
        configMapper.updateById(cfg);

        auditService.log("M14", "update_config", "system_config", cfg.getId(),
                Map.of("configKey", configKey));
        return toResponse(cfg);
    }

    // ==================== 22.10 批量更新系统配置 ====================

    @Transactional
    public List<SystemConfigResponse> batchUpdate(SystemConfigBatchUpdateRequest req) {
        return req.getItems().stream()
                .map(it -> {
                    SystemConfigUpdateRequest single = new SystemConfigUpdateRequest();
                    single.setConfigValue(it.getConfigValue());
                    return updateConfig(it.getConfigKey(), single);
                })
                .collect(Collectors.toList());
    }

    private SystemConfig loadByKey(String configKey) {
        SystemConfig cfg = configMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", configKey));
        if (cfg == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置项不存在: " + configKey);
        }
        return cfg;
    }

    private void validateValueType(String valueType, String value) {
        if (valueType == null || "string".equals(valueType)) {
            return;
        }
        switch (valueType) {
            case "number" -> {
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "配置值不是合法数字");
                }
            }
            case "boolean" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "配置值必须为 true 或 false");
                }
            }
            case "json" -> {
                try {
                    objectMapper.readTree(value);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "配置值不是合法 JSON");
                }
            }
            default -> {
                // 未知类型按 string 语义放行
            }
        }
    }
}
