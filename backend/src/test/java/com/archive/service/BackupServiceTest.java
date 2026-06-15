package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.dto.request.BackupTaskCreateRequest;
import com.archive.dto.response.BackupTaskResponse;
import com.archive.entity.BackupTask;
import com.archive.enums.BackupScope;
import com.archive.enums.BackupStatus;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.BackupTaskMapper;
import com.archive.util.BackupTaskNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @TempDir
    Path tempDir;

    private BackupService service;
    private BackupTaskMapper backupTaskMapper;
    private JdbcTemplate jdbcTemplate;
    private MinioService minioService;
    private BackupTaskNoUtil noUtil;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        backupTaskMapper = mock(BackupTaskMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        minioService = mock(MinioService.class);
        noUtil = mock(BackupTaskNoUtil.class);
        auditService = mock(AuditService.class);
        service = new BackupService(backupTaskMapper, jdbcTemplate, minioService, noUtil, auditService);
        ReflectionTestUtils.setField(service, "backupDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "fileBucket", "archive-files");
    }

    private void asBack(Runnable body) {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(() -> AuthContext.hasRole(RoleCode.back_archivist)).thenReturn(true);
            body.run();
        }
    }

    @Test
    void create_数据库范围生成sql且sha256非空() {
        when(noUtil.generate()).thenReturn("BAK-000001");
        when(backupTaskMapper.insert(any(BackupTask.class))).thenAnswer(inv -> {
            ((BackupTask) inv.getArgument(0)).setId(1L);
            return 1;
        });
        when(backupTaskMapper.selectCount(any())).thenReturn(0L);
        // 所有表的 queryForList 返回一行示例
        when(jdbcTemplate.queryForList(anyString())).thenReturn(
                List.of(Map.of("id", 1, "name", "demo")));

        BackupTaskCreateRequest req = new BackupTaskCreateRequest();
        req.setBackupScope(BackupScope.database);

        asBack(() -> {
            BackupTaskResponse resp = service.create(req);
            assertThat(resp.getStatus()).isEqualTo(BackupStatus.success.name());
            assertThat(resp.getSha256()).hasSize(64);
            assertThat(resp.getBackupPath()).endsWith("-db.sql");
            assertThat(resp.getFileSize()).isPositive();
        });
    }

    @Test
    void create_已有running任务抛冲突() {
        when(backupTaskMapper.selectCount(any())).thenReturn(1L);
        BackupTaskCreateRequest req = new BackupTaskCreateRequest();
        req.setBackupScope(BackupScope.database);
        asBack(() -> assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("运行中"));
    }
}
