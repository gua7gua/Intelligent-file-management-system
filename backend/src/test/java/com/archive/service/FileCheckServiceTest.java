package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.dto.request.FileCheckTriggerRequest;
import com.archive.dto.response.FileCheckRecordResponse;
import com.archive.entity.ArchiveFile;
import com.archive.entity.FileCheckRecord;
import com.archive.enums.FileCheckResult;
import com.archive.enums.FileCheckType;
import com.archive.enums.RoleCode;
import com.archive.mapper.FileCheckRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileCheckServiceTest {

    private FileCheckService service;
    private ArchiveFileService archiveFileService;
    private MinioService minioService;
    private ClamAvScanner clamAvScanner;
    private FileCheckRecordMapper recordMapper;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        archiveFileService = mock(ArchiveFileService.class);
        minioService = mock(MinioService.class);
        clamAvScanner = mock(ClamAvScanner.class);
        recordMapper = mock(FileCheckRecordMapper.class);
        auditService = mock(AuditService.class);
        service = new FileCheckService(archiveFileService, minioService, clamAvScanner, recordMapper, auditService);
    }

    private void asBack(Runnable body) {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(() -> AuthContext.hasRole(RoleCode.back_archivist)).thenReturn(true);
            body.run();
        }
    }

    private ArchiveFile file() {
        ArchiveFile f = new ArchiveFile();
        f.setId(7L);
        f.setBucketName("archive-files");
        f.setObjectKey("archive-files/ARC-000001/7/x.pdf");
        f.setSha256("0");
        return f;
    }

    @Test
    void 真实性恒为not_configured() {
        when(archiveFileService.findById(7L)).thenReturn(file());
        FileCheckTriggerRequest req = new FileCheckTriggerRequest();
        req.setCheckTypes(List.of(FileCheckType.authenticity));
        asBack(() -> {
            List<FileCheckRecordResponse> out = service.trigger(7L, req);
            assertThat(out).hasSize(1);
            assertThat(out.get(0).getCheckResult()).isEqualTo(FileCheckResult.not_configured.name());
            assertThat(out.get(0).getSignatureResult()).isEqualTo("not_configured");
        });
        verify(recordMapper).insert(any(FileCheckRecord.class));
    }

    @Test
    void 完整性哈希不一致则failed() {
        when(archiveFileService.findById(7L)).thenReturn(file());
        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(minioService.getObjectStream("archive-files", "archive-files/ARC-000001/7/x.pdf")).thenReturn(stream);
        FileCheckTriggerRequest req = new FileCheckTriggerRequest();
        req.setCheckTypes(List.of(FileCheckType.integrity));
        asBack(() -> {
            List<FileCheckRecordResponse> out = service.trigger(7L, req);
            assertThat(out.get(0).getCheckResult()).isEqualTo(FileCheckResult.failed.name());
        });
    }

    @Test
    void 安全性扫描异常则failed不放行() {
        when(archiveFileService.findById(7L)).thenReturn(file());
        InputStream stream = new ByteArrayInputStream(new byte[]{1});
        when(minioService.getObjectStream("archive-files", "archive-files/ARC-000001/7/x.pdf")).thenReturn(stream);
        when(clamAvScanner.scan(any(InputStream.class))).thenThrow(new RuntimeException("clamd 不可达"));
        FileCheckTriggerRequest req = new FileCheckTriggerRequest();
        req.setCheckTypes(List.of(FileCheckType.security));
        asBack(() -> {
            List<FileCheckRecordResponse> out = service.trigger(7L, req);
            assertThat(out.get(0).getCheckResult()).isEqualTo(FileCheckResult.failed.name());
            assertThat(out.get(0).getMessage()).contains("不放行");
        });
    }
}
