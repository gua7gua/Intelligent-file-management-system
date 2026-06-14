package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.config.FileProperties;
import com.archive.dto.request.CompilationArchiveRequest;
import com.archive.dto.request.CompilationQuery;
import com.archive.dto.request.CompilationSaveRequest;
import com.archive.dto.response.CompilationDetailResponse;
import com.archive.dto.response.CompilationMaterialResponse;
import com.archive.dto.response.CompilationResponse;
import com.archive.entity.*;
import com.archive.enums.*;
import com.archive.exception.BusinessException;
import com.archive.mapper.*;
import com.archive.util.ArchiveNoUtil;
import com.archive.util.CompilationNoUtil;
import com.archive.util.PdfGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 档案编研服务（M13）。
 * 生成正文 PDF 附件挂 business_attachments；成果入库为纯电子档案（sourceType=compilation）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationService {

    private final CompilationMapper compilationMapper;
    private final CompilationMaterialMapper materialMapper;
    private final ArchiveMapper archiveMapper;
    private final ArchiveFileMapper archiveFileMapper;
    private final TagMapper tagMapper;
    private final ArchiveTagMapper archiveTagMapper;
    private final MinioService minioService;
    private final FileProperties fileProperties;
    private final PdfGenerator pdfGenerator;
    private final CompilationNoUtil noUtil;
    private final ArchiveNoUtil archiveNoUtil;
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    private void requireRole() {
        if (!AuthContext.hasRole(RoleCode.back_archivist)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }

    /** 19.1 查询编研成果 */
    public PageResult<CompilationResponse> list(CompilationQuery q) {
        requireRole();
        QueryWrapper<Compilation> w = new QueryWrapper<>();
        w.isNull("deleted_at");
        if (q.getStatus() != null) w.eq("status", q.getStatus());
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            w.and(x -> x.like("title", q.getKeyword()).or().like("compilation_no", q.getKeyword()));
        }
        w.orderByDesc("created_at");
        Page<Compilation> p = compilationMapper.selectPage(new Page<>(q.getPageNo(), q.getPageSize()), w);
        return new PageResult<>(p.getRecords().stream().map(this::toListResponse).toList(),
                q.getPageNo(), q.getPageSize(), p.getTotal());
    }

    /** 19.2 / 19.4 创建或更新草稿 */
    @Transactional
    public CompilationDetailResponse save(Long existingId, CompilationSaveRequest req) {
        requireRole();
        Compilation c;
        if (existingId == null) {
            c = new Compilation();
            c.setCompilationNo(noUtil.generate());
            c.setStatus(CompilationStatus.draft);
        } else {
            c = compilationMapper.selectById(existingId);
            if (c == null || c.getDeletedAt() != null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "编研成果不存在");
            }
            if (c.getStatus() == CompilationStatus.archived) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "已入库的编研成果不可编辑");
            }
        }
        c.setTitle(req.getTitle());
        c.setCompilationType(req.getCompilationType());
        c.setDateRangeText(req.getDateRangeText());
        c.setKeywords(req.getKeywords());
        c.setSummary(req.getSummary());
        c.setContentHtml(req.getContentHtml());
        if (existingId == null) {
            compilationMapper.insert(c);
        } else {
            compilationMapper.updateById(c);
            materialMapper.delete(new QueryWrapper<CompilationMaterial>().eq("compilation_id", existingId));
        }

        if (req.getMaterialArchiveIds() != null) {
            int sort = 1;
            for (Long aid : req.getMaterialArchiveIds()) {
                Archive a = archiveMapper.selectById(aid);
                if (a == null || a.getLifecycleStatus() == LifecycleStatus.destroyed) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "素材档案不可用: " + aid);
                }
                CompilationMaterial m = new CompilationMaterial();
                m.setCompilationId(c.getId());
                m.setArchiveId(aid);
                m.setSortNo(sort++);
                materialMapper.insert(m);
            }
        }
        return getDetail(c.getId());
    }

    /** 19.3 详情 */
    public CompilationDetailResponse getDetail(Long id) {
        requireRole();
        Compilation c = compilationMapper.selectById(id);
        if (c == null || c.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "编研成果不存在");
        }
        CompilationDetailResponse r = new CompilationDetailResponse();
        r.setId(c.getId());
        r.setCompilationNo(c.getCompilationNo());
        r.setTitle(c.getTitle());
        r.setCompilationType(c.getCompilationType());
        r.setStatus(c.getStatus() != null ? c.getStatus().name() : null);
        r.setCreatedAt(c.getCreatedAt());
        r.setDateRangeText(c.getDateRangeText());
        r.setKeywords(c.getKeywords());
        r.setSummary(c.getSummary());
        r.setContentHtml(c.getContentHtml());
        r.setGeneratedFileAttachmentId(c.getGeneratedFileAttachmentId());
        r.setGeneratedArchiveId(c.getGeneratedArchiveId());
        r.setMaterials(materialMapper.selectList(new QueryWrapper<CompilationMaterial>()
                .eq("compilation_id", id).isNull("deleted_at").orderByAsc("sort_no")).stream().map(m -> {
            CompilationMaterialResponse mr = new CompilationMaterialResponse();
            mr.setId(m.getId());
            mr.setArchiveId(m.getArchiveId());
            mr.setSortNo(m.getSortNo());
            mr.setQuoteNote(m.getQuoteNote());
            return mr;
        }).toList());
        return r;
    }

    /** 19.5 生成正文附件（draft -> generated） */
    @Transactional
    public CompilationDetailResponse generate(Long id) {
        requireRole();
        Compilation c = mustGet(id);
        if (c.getStatus() != CompilationStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "仅草稿状态可生成正文");
        }
        byte[] pdf = pdfGenerator.generateCompilationPdf(c.getTitle(), c.getContentHtml());
        String bucket = fileProperties.getBucket();
        String key = "compilations/" + c.getId() + "/body.pdf";
        minioService.putObject(bucket, key, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");
        String sha = sha256(pdf);

        // 用 Object[] 重载（非变参），避免 Mockito 变参匹配歧义
        Object[] params = {c.getId(), bucket, key, pdf.length, sha, AuthContext.getCurrentUserId()};
        Long attachId = jdbcTemplate.queryForObject(
                "INSERT INTO business_attachments (business_type, business_id, attachment_type, bucket_name, object_key, " +
                        "original_filename, file_ext, mime_type, file_size, sha256, scan_result, file_status, uploaded_by, uploaded_at) " +
                        "VALUES ('compilation', ?, 'report', ?, ?, '编研正文.pdf', 'pdf', 'application/pdf', ?, ?, 'safe', 'normal', ?, now()) RETURNING id",
                params, Long.class);

        c.setGeneratedFileAttachmentId(attachId);
        c.setStatus(CompilationStatus.generated);
        compilationMapper.updateById(c);
        auditService.log("M13", "generate_compilation", "compilation", id, Map.of("attachmentId", attachId));
        return getDetail(id);
    }

    /** 19.6 入库（镜像 PendingArchiveService.confirmArchive） */
    @Transactional
    public Long archive(Long id, CompilationArchiveRequest req) {
        requireRole();
        Compilation c = mustGet(id);
        if (c.getStatus() != CompilationStatus.generated) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "需先生成正文附件再入库");
        }
        if (c.getGeneratedFileAttachmentId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "缺少正文附件，无法入库");
        }

        // 读回正文附件的 bucket/key/sha256/size
        Map<String, Object> att = jdbcTemplate.queryForMap(
                "SELECT bucket_name, object_key, sha256, file_size FROM business_attachments WHERE id = ?",
                c.getGeneratedFileAttachmentId());
        String srcBucket = String.valueOf(att.get("bucket_name"));
        String srcKey = String.valueOf(att.get("object_key"));
        String sha = att.get("sha256") != null ? String.valueOf(att.get("sha256")) : null;
        long size = att.get("file_size") != null ? ((Number) att.get("file_size")).longValue() : 0L;

        String archiveNo = archiveNoUtil.generate();
        Archive a = new Archive();
        a.setArchiveNo(archiveNo);
        a.setTitle(c.getTitle());
        a.setFormedDate(req.getFormedDate());
        a.setFormedYear(req.getFormedDate() != null ? req.getFormedDate().getYear() : null);
        a.setCategoryId(req.getCategoryId());
        a.setSourceType(SourceType.compilation);
        a.setSourceCompilationId(c.getId());
        a.setFondsId(req.getFondsId());
        a.setCarrierStatus(CarrierStatus.electronic);
        a.setRetentionPeriod(req.getRetentionPeriod());
        a.setSecurityLevel(0);
        a.setOpenStatus(req.getOpenStatus());
        a.setAllowDigitization(false);
        a.setLifecycleStatus(LifecycleStatus.normal);
        a.setLoanStatus(LoanStatus.available);
        a.setConditionStatus(ConditionStatus.normal);
        a.setArchivedAt(OffsetDateTime.now());
        archiveMapper.insert(a);

        // 复制正文为正式 archive_files（fileRole=compilation_body）
        String bucket = fileProperties.getBucket();
        ArchiveFile af = new ArchiveFile();
        af.setArchiveId(a.getId());
        af.setFileRole(FileRole.compilation_body);
        af.setBucketName(bucket);
        af.setObjectKey("");
        af.setOriginalFilename("编研正文.pdf");
        af.setFileExt("pdf");
        af.setMimeType("application/pdf");
        af.setFileSize(size);
        af.setSha256(sha);
        af.setScanResult(ScanResult.safe);
        af.setUsabilityResult(UsabilityResult.passed);
        af.setFileStatus(FileStatus.normal);
        archiveFileMapper.insert(af);
        String formalKey = String.format("archive-files/%s/%d/compilation-body.pdf", archiveNo, af.getId());
        af.setObjectKey(formalKey);
        archiveFileMapper.updateById(af);
        minioService.copyObject(srcBucket, srcKey, bucket, formalKey);

        // 标签 upsert（仿 confirmArchive）
        if (req.getTagNames() != null) {
            for (String name : req.getTagNames()) {
                Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("tag_name", name));
                if (tag == null) {
                    tag = new Tag();
                    tag.setTagName(name);
                    tag.setCreatedAt(OffsetDateTime.now());
                    tagMapper.insert(tag);
                }
                ArchiveTag at = new ArchiveTag();
                at.setArchiveId(a.getId());
                at.setTagId(tag.getId());
                at.setCreatedAt(OffsetDateTime.now());
                archiveTagMapper.insert(at);
            }
        }

        c.setGeneratedArchiveId(a.getId());
        c.setGeneratedArchiveFileId(af.getId());
        c.setStatus(CompilationStatus.archived);
        compilationMapper.updateById(c);
        auditService.log("M13", "archive_compilation", "compilation", id,
                Map.of("archiveId", a.getId(), "archiveNo", archiveNo));
        return a.getId();
    }

    private Compilation mustGet(Long id) {
        Compilation c = compilationMapper.selectById(id);
        if (c == null || c.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "编研成果不存在");
        }
        return c;
    }

    private CompilationResponse toListResponse(Compilation c) {
        CompilationResponse r = new CompilationResponse();
        r.setId(c.getId());
        r.setCompilationNo(c.getCompilationNo());
        r.setTitle(c.getTitle());
        r.setCompilationType(c.getCompilationType());
        r.setStatus(c.getStatus() != null ? c.getStatus().name() : null);
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(data)) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
