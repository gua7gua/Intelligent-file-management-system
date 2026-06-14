package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.config.FileProperties;
import com.archive.dto.request.CompilationArchiveRequest;
import com.archive.dto.request.CompilationSaveRequest;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveFile;
import com.archive.entity.Compilation;
import com.archive.entity.CompilationMaterial;
import com.archive.enums.CarrierStatus;
import com.archive.enums.CompilationStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.RetentionPeriod;
import com.archive.enums.RoleCode;
import com.archive.enums.SourceType;
import com.archive.exception.BusinessException;
import com.archive.mapper.*;
import com.archive.util.ArchiveNoUtil;
import com.archive.util.CompilationNoUtil;
import com.archive.util.PdfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceTest {

    private CompilationService service;
    private CompilationMapper compilationMapper;
    private CompilationMaterialMapper materialMapper;
    private ArchiveMapper archiveMapper;
    private ArchiveFileMapper archiveFileMapper;
    private TagMapper tagMapper;
    private ArchiveTagMapper archiveTagMapper;
    private MinioService minioService;
    private PdfGenerator pdfGenerator;
    private CompilationNoUtil noUtil;
    private ArchiveNoUtil archiveNoUtil;
    private JdbcTemplate jdbcTemplate;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        compilationMapper = mock(CompilationMapper.class);
        materialMapper = mock(CompilationMaterialMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        archiveFileMapper = mock(ArchiveFileMapper.class);
        tagMapper = mock(TagMapper.class);
        archiveTagMapper = mock(ArchiveTagMapper.class);
        minioService = mock(MinioService.class);
        pdfGenerator = mock(PdfGenerator.class);
        noUtil = mock(CompilationNoUtil.class);
        archiveNoUtil = mock(ArchiveNoUtil.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        auditService = mock(AuditService.class);
        service = new CompilationService(compilationMapper, materialMapper, archiveMapper, archiveFileMapper,
                tagMapper, archiveTagMapper, minioService, new FileProperties(), pdfGenerator,
                noUtil, archiveNoUtil, jdbcTemplate, auditService);
    }

    private void asBack(Runnable body) {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(() -> AuthContext.hasRole(RoleCode.back_archivist)).thenReturn(true);
            a.when(AuthContext::getCurrentUserId).thenReturn(60L);
            body.run();
        }
    }

    @Test
    void save_创建草稿并写素材() {
        when(noUtil.generate()).thenReturn("CMP-000001");
        when(compilationMapper.insert(any(Compilation.class))).thenAnswer(inv -> {
            ((Compilation) inv.getArgument(0)).setId(1L);
            return 1;
        });
        when(archiveMapper.selectById(2L)).thenReturn(validArchive());
        when(compilationMapper.selectById(1L)).thenReturn(draft());
        when(materialMapper.selectList(any())).thenReturn(List.of());

        CompilationSaveRequest req = new CompilationSaveRequest();
        req.setTitle("财政改革专题");
        req.setMaterialArchiveIds(List.of(2L));

        asBack(() -> service.save(null, req));

        verify(materialMapper).insert(any(CompilationMaterial.class));
    }

    @Test
    void save_archived只读抛冲突() {
        Compilation c = draft();
        c.setStatus(CompilationStatus.archived);
        when(compilationMapper.selectById(1L)).thenReturn(c);

        CompilationSaveRequest req = new CompilationSaveRequest();
        req.setTitle("x");
        asBack(() -> assertThatThrownBy(() -> service.save(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可编辑"));
    }

    @Test
    void generate_写business_attachment且状态draft转generated() {
        Compilation c = draft();
        when(compilationMapper.selectById(1L)).thenReturn(c);
        when(pdfGenerator.generateCompilationPdf(anyString(), anyString())).thenReturn(new byte[]{1, 2, 3});
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class))).thenReturn(99L);
        when(materialMapper.selectList(any())).thenReturn(List.of());

        asBack(() -> service.generate(1L));

        ArgumentCaptor<Compilation> cap = ArgumentCaptor.forClass(Compilation.class);
        verify(compilationMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(CompilationStatus.generated);
        assertThat(cap.getValue().getGeneratedFileAttachmentId()).isEqualTo(99L);
    }

    @Test
    void archive_生成正式档案sourceType为compilation() {
        Compilation c = generated();
        when(compilationMapper.selectById(1L)).thenReturn(c);
        when(jdbcTemplate.queryForMap(anyString(), eq(5L))).thenReturn(Map.of(
                "bucket_name", "archive-files", "object_key", "compilations/1/body.pdf",
                "sha256", "abc", "file_size", 100L));
        when(archiveNoUtil.generate()).thenReturn("ARC-000001");
        when(archiveMapper.insert(any(Archive.class))).thenAnswer(inv -> {
            ((Archive) inv.getArgument(0)).setId(8L);
            return 1;
        });
        when(archiveFileMapper.insert(any(ArchiveFile.class))).thenAnswer(inv -> {
            ((ArchiveFile) inv.getArgument(0)).setId(9L);
            return 1;
        });

        CompilationArchiveRequest req = new CompilationArchiveRequest();
        req.setFondsId(3L);
        req.setCategoryId(1);
        req.setFormedDate(LocalDate.of(2026, 5, 30));
        req.setRetentionPeriod(RetentionPeriod.permanent);
        req.setOpenStatus("open");

        asBack(() -> service.archive(1L, req));

        ArgumentCaptor<Archive> cap = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper).insert(cap.capture());
        assertThat(cap.getValue().getSourceType()).isEqualTo(SourceType.compilation);
        assertThat(cap.getValue().getLifecycleStatus()).isEqualTo(LifecycleStatus.normal);
        assertThat(cap.getValue().getSourceCompilationId()).isEqualTo(1L);
        verify(minioService).copyObject(anyString(), anyString(), anyString(), anyString());
    }

    private Compilation draft() {
        Compilation c = new Compilation();
        c.setId(1L);
        c.setCompilationNo("CMP-000001");
        c.setTitle("财政改革专题");
        c.setContentHtml("<p>编研正文</p>");
        c.setStatus(CompilationStatus.draft);
        return c;
    }

    private Compilation generated() {
        Compilation c = draft();
        c.setStatus(CompilationStatus.generated);
        c.setGeneratedFileAttachmentId(5L);
        return c;
    }

    private Archive validArchive() {
        Archive a = new Archive();
        a.setId(2L);
        a.setCarrierStatus(CarrierStatus.electronic);
        a.setLifecycleStatus(LifecycleStatus.normal);
        return a;
    }
}
