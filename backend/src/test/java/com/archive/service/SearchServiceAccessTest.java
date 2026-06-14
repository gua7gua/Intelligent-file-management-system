package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.ArchiveSearchDetailResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveFile;
import com.archive.enums.DataScope;
import com.archive.enums.FileStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.TagMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SearchServiceAccessTest {

    private SearchService service;
    private ArchiveMapper archiveMapper;
    private ArchiveFileMapper archiveFileMapper;
    private ArchiveAccessLogMapper accessLogMapper;
    private CategoryMapper categoryMapper;
    private MinioService minioService;
    private HttpServletRequest req;

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        archiveFileMapper = mock(ArchiveFileMapper.class);
        accessLogMapper = mock(ArchiveAccessLogMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        minioService = mock(MinioService.class);
        req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("192.168.1.1");
        service = new SearchService(archiveMapper, archiveFileMapper, accessLogMapper,
                categoryMapper, mock(TagMapper.class), mock(JdbcTemplate.class), minioService, null,
                mock(BorrowService.class));
    }

    @Test
    void logAccess_解析参数写入访问日志() {
        OffsetDateTime before = OffsetDateTime.now();
        service.logAccess(7L, 99L, "download", 5L, "public", req);

        ArgumentCaptor<OffsetDateTime> atCap = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(accessLogMapper).insertAccessLog(eq(5L), eq("public"), eq(7L), eq(99L),
                eq("download"), eq("192.168.1.1"), atCap.capture());
        assertThat(atCap.getValue()).isAfterOrEqualTo(before);
    }

    @Test
    void logAccess_匿名用户userId为空() {
        service.logAccess(7L, null, "view_metadata", null, "anonymous", req);
        verify(accessLogMapper).insertAccessLog(isNull(), eq("anonymous"), eq(7L), isNull(),
                eq("view_metadata"), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void loadVisiblePublicFile_档案不公开抛NOT_FOUND() {
        ArchiveFile file = new ArchiveFile();
        file.setId(1L);
        file.setArchiveId(7L);
        file.setFileStatus(FileStatus.normal);
        when(archiveFileMapper.selectById(1L)).thenReturn(file);
        when(archiveMapper.selectOne(any())).thenReturn(null); // 不满足公开过滤

        assertThatThrownBy(() -> service.loadVisiblePublicFile(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void loadVisiblePublicFile_文件非normal抛冲突() {
        ArchiveFile file = new ArchiveFile();
        file.setId(1L);
        file.setArchiveId(7L);
        file.setFileStatus(FileStatus.failed);
        Archive a = new Archive();
        a.setId(7L);
        when(archiveFileMapper.selectById(1L)).thenReturn(file);
        when(archiveMapper.selectOne(any())).thenReturn(a);

        assertThatThrownBy(() -> service.loadVisiblePublicFile(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void loadVisiblePublicFile_文件不存在抛NOT_FOUND() {
        when(archiveFileMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.loadVisiblePublicFile(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void publicPreview_正常返回预签名URL并写日志() {
        ArchiveFile file = new ArchiveFile();
        file.setId(1L);
        file.setArchiveId(7L);
        file.setFileStatus(FileStatus.normal);
        file.setBucketName("archive-files");
        file.setObjectKey("archive-files/ARC-1/1/a.pdf");
        when(archiveFileMapper.selectById(1L)).thenReturn(file);
        when(archiveMapper.selectOne(any())).thenReturn(new Archive());
        when(minioService.getPresignedUrl("archive-files", "archive-files/ARC-1/1/a.pdf"))
                .thenReturn("https://minio/presigned");

        String url = service.publicPreview(1L, 5L, "public", req);

        assertThat(url).isEqualTo("https://minio/presigned");
        verify(accessLogMapper).insertAccessLog(eq(5L), eq("public"), eq(7L), eq(1L),
                eq("preview"), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void publicDownload_未登录抛UNAUTHORIZED() {
        assertThatThrownBy(() -> service.publicDownload(1L, null, "anonymous", req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.UNAUTHORIZED.getCode()));
        verifyNoInteractions(minioService);
    }

    @Test
    void loadVisibleInternalFile_档案超密级抛NOT_FOUND() {
        ArchiveFile file = new ArchiveFile();
        file.setId(1L);
        file.setArchiveId(7L);
        file.setFileStatus(FileStatus.normal);
        when(archiveFileMapper.selectById(1L)).thenReturn(file);
        when(archiveMapper.selectOne(any())).thenReturn(null); // 不在权限范围

        assertThatThrownBy(() -> service.loadVisibleInternalFile(1L, 1, DataScope.all, null))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void publicDetail_查到返回脱敏详情且写日志() {
        Archive a = new Archive();
        a.setId(7L);
        a.setArchiveNo("ARC-000007");
        a.setTitle("标题");
        a.setCarrierStatus(null);
        a.setSecurityLevel(0);
        when(archiveMapper.selectOne(any())).thenReturn(a);
        when(archiveFileMapper.selectList(any())).thenReturn(List.of());
        when(categoryMapper.selectList(any())).thenReturn(List.of());

        ArchiveSearchDetailResponse detail = service.publicDetail(7L, null, "anonymous", req);

        assertThat(detail.getArchiveId()).isEqualTo(7L);
        assertThat(detail.getSecurityLevel()).isNull(); // 公众脱敏
        verify(accessLogMapper).insertAccessLog(isNull(), eq("anonymous"), eq(7L), isNull(),
                eq("view_metadata"), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void publicDetail_查不到抛NOT_FOUND() {
        when(archiveMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.publicDetail(7L, null, "anonymous", req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.NOT_FOUND.getCode()));
    }
}
