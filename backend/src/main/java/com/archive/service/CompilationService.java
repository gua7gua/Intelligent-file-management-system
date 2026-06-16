package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.config.FileProperties;
import com.archive.dto.request.CompilationArchiveRequest;
import com.archive.dto.request.CompilationQuery;
import com.archive.dto.request.CompilationSaveRequest;
import com.archive.dto.response.CompilationAttachmentResponse;
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
import java.util.Set;
import java.util.stream.Collectors;

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

    /** JDBC 查询 uploaded_at 返回 Timestamp/OffsetDateTime/LocalDateTime 不一，统一转为 OffsetDateTime。 */
    private OffsetDateTime toOffsetDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof OffsetDateTime o) return o;
        if (v instanceof java.sql.Timestamp t) return t.toInstant().atOffset(java.time.ZoneOffset.UTC);
        if (v instanceof java.time.LocalDateTime l) return l.atOffset(java.time.ZoneOffset.UTC);
        if (v instanceof java.util.Date d) return d.toInstant().atOffset(java.time.ZoneOffset.UTC);
        return null;
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
        List<Compilation> rows = p.getRecords();
        List<CompilationResponse> resp = rows.stream().map(this::toListResponse).toList();
        if (!resp.isEmpty()) {
            enrichListResponses(resp, rows);
        }
        return new PageResult<>(resp, q.getPageNo(), q.getPageSize(), p.getTotal());
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
        r.setUpdatedAt(c.getUpdatedAt());
        r.setDateRangeText(c.getDateRangeText());
        r.setKeywords(c.getKeywords());
        r.setSummary(c.getSummary());
        r.setContentHtml(c.getContentHtml());
        r.setGeneratedFileAttachmentId(c.getGeneratedFileAttachmentId());
        r.setGeneratedArchiveId(c.getGeneratedArchiveId());
        r.setArchiveId(c.getGeneratedArchiveId());

        List<CompilationMaterial> materials = materialMapper.selectList(new QueryWrapper<CompilationMaterial>()
                .eq("compilation_id", id).isNull("deleted_at").orderByAsc("sort_no"));
        r.setMaterialCount(materials.size());

        // 素材档案 archiveNo/title 批量补全
        Set<Long> matArchiveIds = materials.stream().map(CompilationMaterial::getArchiveId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Archive> matArchiveMap = matArchiveIds.isEmpty() ? Map.of() :
                archiveMapper.selectList(new QueryWrapper<Archive>().in("id", matArchiveIds)
                        .select("id", "archive_no", "title")).stream()
                        .collect(Collectors.toMap(Archive::getId, a -> a, (a, b) -> a));
        r.setMaterials(materials.stream().map(m -> {
            CompilationMaterialResponse mr = new CompilationMaterialResponse();
            mr.setId(m.getId());
            mr.setArchiveId(m.getArchiveId());
            mr.setSortNo(m.getSortNo());
            mr.setQuoteNote(m.getQuoteNote());
            Archive ma = matArchiveMap.get(m.getArchiveId());
            if (ma != null) {
                mr.setArchiveNo(ma.getArchiveNo());
                mr.setTitle(ma.getTitle());
            }
            return mr;
        }).toList());

        // 入库档号 + 正文附件摘要
        if (c.getGeneratedArchiveId() != null) {
            Archive ga = archiveMapper.selectById(c.getGeneratedArchiveId());
            if (ga != null) r.setArchiveNo(ga.getArchiveNo());
        }
        if (c.getGeneratedFileAttachmentId() != null) {
            try {
                Map<String, Object> att = jdbcTemplate.queryForMap(
                        "SELECT id, original_filename, uploaded_at FROM business_attachments WHERE id = ?",
                        c.getGeneratedFileAttachmentId());
                CompilationAttachmentResponse a = new CompilationAttachmentResponse();
                a.setId(c.getGeneratedFileAttachmentId());
                a.setFileName(att.get("original_filename") != null ? String.valueOf(att.get("original_filename")) : "编研正文.pdf");
                a.setFileUrl("/api/admin/compilations/" + id + "/attachment/" + c.getGeneratedFileAttachmentId());
                a.setAttachmentType("report");
                a.setGeneratedAt(toOffsetDateTime(att.get("uploaded_at")));
                if (a.getGeneratedAt() == null) a.setGeneratedAt(c.getCreatedAt());
                r.setAttachment(a);
            } catch (Exception ignore) {
                // 附件记录缺失时不阻断详情返回
            }
        }
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
    public CompilationDetailResponse archive(Long id, CompilationArchiveRequest req) {
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
        return getDetail(id);
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
        r.setUpdatedAt(c.getUpdatedAt());
        r.setArchiveId(c.getGeneratedArchiveId());
        return r;
    }

    /** 批量补全列表字段：archiveNo、materialCount、attachment。避免 N+1 查询。 */
    private void enrichListResponses(List<CompilationResponse> resp, List<Compilation> rows) {
        // 1. 归档的档案 archiveNo（一次查询所有 generated_archive_id）
        Set<Long> archiveIds = rows.stream()
                .map(Compilation::getGeneratedArchiveId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> archiveNoMap = archiveIds.isEmpty() ? Map.of() :
                archiveMapper.selectList(new QueryWrapper<Archive>().in("id", archiveIds)
                        .select("id", "archive_no")).stream()
                        .collect(Collectors.toMap(Archive::getId, Archive::getArchiveNo, (a, b) -> a));
        // 2. materialCount（按 compilation_id 分组聚合）
        Set<Long> compIds = rows.stream().map(Compilation::getId).collect(Collectors.toSet());
        Map<Long, Long> materialCountMap = compIds.isEmpty() ? Map.of() :
                materialMapper.selectList(new QueryWrapper<CompilationMaterial>()
                        .in("compilation_id", compIds).isNull("deleted_at")).stream()
                        .collect(Collectors.groupingBy(CompilationMaterial::getCompilationId, Collectors.counting()));
        // 3. attachment 信息（business_attachments 一次查询）
        Set<Long> attachIds = rows.stream()
                .map(Compilation::getGeneratedFileAttachmentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Map<String, Object>> attachMap = attachIds.isEmpty() ? Map.of() :
                jdbcTemplate.queryForList("SELECT id, original_filename, uploaded_at FROM business_attachments WHERE id IN (" +
                        attachIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")").stream()
                        .collect(Collectors.toMap(m -> ((Number) m.get("id")).longValue(), m -> m, (a, b) -> a));

        for (int i = 0; i < resp.size(); i++) {
            Compilation c = rows.get(i);
            CompilationResponse r = resp.get(i);
            if (c.getGeneratedArchiveId() != null) {
                r.setArchiveNo(archiveNoMap.get(c.getGeneratedArchiveId()));
            }
            r.setMaterialCount(materialCountMap.getOrDefault(c.getId(), 0L).intValue());
            if (c.getGeneratedFileAttachmentId() != null) {
                Map<String, Object> att = attachMap.get(c.getGeneratedFileAttachmentId());
                if (att != null) {
                    CompilationAttachmentResponse a = new CompilationAttachmentResponse();
                    a.setId(c.getGeneratedFileAttachmentId());
                    a.setFileName(att.get("original_filename") != null ? String.valueOf(att.get("original_filename")) : "编研正文.pdf");
                    a.setFileUrl("/api/admin/compilations/" + c.getId() + "/attachment/" + c.getGeneratedFileAttachmentId());
                    a.setAttachmentType("report");
                    a.setGeneratedAt(toOffsetDateTime(att.get("uploaded_at")));
                    if (a.getGeneratedAt() == null) a.setGeneratedAt(c.getCreatedAt());
                    r.setAttachment(a);
                }
            }
        }
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
