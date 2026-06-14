package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.SystemConfigUpdateRequest;
import com.archive.dto.response.SystemConfigResponse;
import com.archive.entity.SystemConfig;
import com.archive.mapper.SystemConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SystemConfigServiceTest {

    private SystemConfigService service;
    private SystemConfigMapper mapper;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        mapper = mock(SystemConfigMapper.class);
        auditService = mock(AuditService.class);
        service = new SystemConfigService(mapper, auditService);
    }

    @Test
    void listConfigs_敏感键值脱敏() {
        SystemConfig normal = cfg(1L, "upload.max_file_size_mb", "100", "number", true);
        SystemConfig secret = cfg(2L, "ai.secret_key", "sk-xxx", "string", true);
        when(mapper.selectList(any())).thenReturn(List.of(normal, secret));

        List<SystemConfigResponse> r = service.listConfigs();

        assertThat(r).extracting(SystemConfigResponse::getConfigKey)
                .contains("upload.max_file_size_mb", "ai.secret_key");
        assertThat(r.stream().filter(x -> "ai.secret_key".equals(x.getConfigKey()))
                .findFirst().orElseThrow().getConfigValue()).isEqualTo("***");
        assertThat(r.stream().filter(x -> "upload.max_file_size_mb".equals(x.getConfigKey()))
                .findFirst().orElseThrow().getConfigValue()).isEqualTo("100");
    }

    @Test
    void updateConfig_不可编辑抛冲突() {
        when(mapper.selectOne(any())).thenReturn(cfg(1L, "system.locked", "1", "string", false));

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("2");

        assertThatThrownBy(() -> service.updateConfig("system.locked", req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void updateConfig_number类型非数字抛校验失败() {
        when(mapper.selectOne(any())).thenReturn(cfg(1L, "upload.max_file_size_mb", "100", "number", true));

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("abc");

        assertThatThrownBy(() -> service.updateConfig("upload.max_file_size_mb", req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void updateConfig_boolean类型非法值抛校验失败() {
        when(mapper.selectOne(any())).thenReturn(cfg(1L, "ai.enabled", "true", "boolean", true));

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("yes");

        assertThatThrownBy(() -> service.updateConfig("ai.enabled", req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void updateConfig_合法值更新成功并审计() {
        when(mapper.selectOne(any())).thenReturn(cfg(1L, "upload.max_file_size_mb", "100", "number", true));

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("200");

        SystemConfigResponse r = service.updateConfig("upload.max_file_size_mb", req);

        assertThat(r.getConfigValue()).isEqualTo("200");
        verify(mapper).updateById(any(SystemConfig.class));
        verify(auditService).log(eq("M14"), eq("update_config"), eq("system_config"), any(), any());
    }

    private SystemConfig cfg(Long id, String key, String value, String type, boolean editable) {
        SystemConfig c = new SystemConfig();
        c.setId(id);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setValueType(type);
        c.setEditable(editable);
        return c;
    }
}
