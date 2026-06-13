package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.ArchiveSearchQuery;
import com.archive.dto.response.ArchiveSummaryResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveFile;
import com.archive.enums.DataScope;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.archive.dto.response.ArchiveSearchDetailResponse;
import com.archive.enums.FileStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * 检索与利用服务（M08）。
 * 提供公众/内部检索、详情、预览/下载、访问日志、AI 检索 JSON 生成、dashboard。
 * 本 Task 实现检索部分；详情/预览/下载/AI/dashboard 在后续 Task 增量加入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ArchiveMapper archiveMapper;
    private final ArchiveFileMapper archiveFileMapper;
    private final ArchiveAccessLogMapper accessLogMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MinioService minioService;
    private final AiClient aiClient;

    // ==================== 5.2 公众检索 ====================

    public PageResult<ArchiveSummaryResponse> publicSearch(ArchiveSearchQuery query) {
        if (!isPublicSearchEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "公众检索未开放");
        }
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("security_level", 0)
         .eq("open_status", "open")
         .eq("lifecycle_status", "normal");
        applyCommonConditions(w, query);
        return queryAndMap(w, query.getPageNo(), query.getPageSize());
    }

    // ==================== 11.2 内部检索 ====================

    public PageResult<ArchiveSummaryResponse> internalSearch(ArchiveSearchQuery query) {
        QueryWrapper<Archive> w = buildInternalWrapper(query,
                AuthContext.getMaxSecurityLevel(),
                AuthContext.getDataScope(),
                AuthContext.getOrganizationId());
        return queryAndMap(w, query.getPageNo(), query.getPageSize());
    }

    /** 构建内部检索条件（接收权限参数，便于单元测试）。 */
    QueryWrapper<Archive> buildInternalWrapper(ArchiveSearchQuery query,
                                               int maxSecurityLevel,
                                               DataScope dataScope,
                                               Long organizationId) {
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("lifecycle_status", "normal");
        w.le("security_level", maxSecurityLevel);
        applyDataScope(w, dataScope, organizationId);
        applyCommonConditions(w, query);
        // 内部版用户主动筛选 securityLevel / openStatus
        if (query.getSecurityLevel() != null) {
            w.eq("security_level", query.getSecurityLevel());
        }
        if (query.getOpenStatus() != null && !query.getOpenStatus().isBlank()) {
            w.eq("open_status", query.getOpenStatus());
        }
        return w;
    }

    /** 公众/内部通用查询条件。 */
    private void applyCommonConditions(QueryWrapper<Archive> w, ArchiveSearchQuery q) {
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            String kw = q.getKeyword();
            w.and(x -> x.like("title", kw).or().like("archive_no", kw).or().like("responsible_text", kw));
        }
        if (q.getArchiveNo() != null && !q.getArchiveNo().isBlank()) w.like("archive_no", q.getArchiveNo());
        if (q.getTitle() != null && !q.getTitle().isBlank()) w.like("title", q.getTitle());
        if (q.getCategoryId() != null) w.eq("category_id", q.getCategoryId());
        if (q.getFormedYearStart() != null) w.ge("formed_year", q.getFormedYearStart());
        if (q.getFormedYearEnd() != null) w.le("formed_year", q.getFormedYearEnd());
        if (q.getResponsibleText() != null && !q.getResponsibleText().isBlank())
            w.like("responsible_text", q.getResponsibleText());
        if (q.getTagIds() != null && !q.getTagIds().isBlank()) {
            List<Long> tagIdList = parseLongCsv(q.getTagIds());
            if (!tagIdList.isEmpty()) {
                String idList = tagIdList.stream().map(String::valueOf).collect(Collectors.joining(","));
                w.inSql("id", "SELECT archive_id FROM archive_tags WHERE tag_id IN (" + idList + ")");
            }
        }
        if (q.getSourceType() != null && !q.getSourceType().isBlank()) w.eq("source_type", q.getSourceType());
        if (q.getCarrierStatus() != null && !q.getCarrierStatus().isBlank())
            w.eq("carrier_status", q.getCarrierStatus());
        if (Boolean.TRUE.equals(q.getHasElectronicFile())) {
            w.inSql("id", "SELECT archive_id FROM archive_files WHERE file_status = 'normal'");
        }
    }

    /** 内部数据范围过滤（不按 open_status 硬过滤）。 */
    private void applyDataScope(QueryWrapper<Archive> w, DataScope scope, Long organizationId) {
        if (scope == DataScope.all || organizationId == null) return;
        if (scope == DataScope.own_org) {
            w.eq("organization_id", organizationId);
        } else if (scope == DataScope.own_fonds) {
            w.inSql("fonds_id", "SELECT id FROM fonds WHERE organization_id = " + organizationId);
        }
    }

    private PageResult<ArchiveSummaryResponse> queryAndMap(QueryWrapper<Archive> w, int pageNo, int pageSize) {
        Map<Integer, String> categoryNameById = loadCategoryNames();
        Page<Archive> page = new Page<>(pageNo, pageSize);
        archiveMapper.selectPage(page, w);
        List<ArchiveSummaryResponse> records = page.getRecords().stream()
                .map(a -> toSummary(a, categoryNameById))
                .toList();
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private ArchiveSummaryResponse toSummary(Archive a, Map<Integer, String> categoryNameById) {
        Long fileCount = archiveFileMapper.selectCount(new QueryWrapper<ArchiveFile>()
                .eq("archive_id", a.getId()).eq("file_status", "normal"));
        return new ArchiveSummaryResponse(
                a.getId(), a.getArchiveNo(), a.getTitle(), a.getResponsibleText(),
                a.getFormedYear(), categoryNameById.get(a.getCategoryId()),
                a.getCarrierStatus() == null ? null : a.getCarrierStatus().name(),
                fileCount != null && fileCount > 0);
    }

    private Map<Integer, String> loadCategoryNames() {
        Map<Integer, String> map = new HashMap<>();
        categoryMapper.selectList(null).forEach(c -> {
            if (c.getId() != null && c.getCategoryName() != null) {
                map.put((int) c.getId(), c.getCategoryName());
            }
        });
        return map;
    }

    private boolean isPublicSearchEnabled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_configs WHERE config_key = 'public_search.enabled' AND config_value = 'true'",
                Integer.class);
        return count != null && count > 0;
    }

    private List<Long> parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> { try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; } })
                .filter(Objects::nonNull)
                .toList();
    }

    // ==================== 5.3 / 11.3 详情 ====================

    public ArchiveSearchDetailResponse publicDetail(Long archiveId, Long userId, String userType,
                                                    HttpServletRequest req) {
        Archive a = archiveMapper.selectOne(new QueryWrapper<Archive>()
                .eq("id", archiveId)
                .eq("security_level", 0).eq("open_status", "open").eq("lifecycle_status", "normal"));
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在或不公开");
        }
        logAccess(archiveId, null, "view_metadata", userId, userType, req);
        return toDetail(a, false);
    }

    public ArchiveSearchDetailResponse internalDetail(Long archiveId, int maxSecurityLevel,
                                                     DataScope dataScope, Long organizationId,
                                                     Long userId, HttpServletRequest req) {
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("id", archiveId).eq("lifecycle_status", "normal").le("security_level", maxSecurityLevel);
        applyDataScope(w, dataScope, organizationId);
        Archive a = archiveMapper.selectOne(w);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在或无权访问");
        }
        logAccess(archiveId, null, "view_metadata", userId, "internal", req);
        return toDetail(a, true);
    }

    private ArchiveSearchDetailResponse toDetail(Archive a, boolean includeSensitive) {
        List<ArchiveFile> files = archiveFileMapper.selectList(new QueryWrapper<ArchiveFile>()
                .eq("archive_id", a.getId()).eq("file_status", "normal"));
        List<ArchiveSearchDetailResponse.FileSummary> fileSummaries = files.stream()
                .map(f -> new ArchiveSearchDetailResponse.FileSummary(
                        f.getId(), f.getOriginalFilename(), f.getFileExt(),
                        f.getFileSize(), f.getMimeType(),
                        f.getFileRole() == null ? null : f.getFileRole().name()))
                .toList();
        String categoryName = loadCategoryNames().get(a.getCategoryId());
        return new ArchiveSearchDetailResponse(
                a.getId(), a.getArchiveNo(), a.getTitle(), a.getResponsibleText(),
                a.getFormedYear(), a.getFormedDate(), categoryName,
                a.getCarrierStatus() == null ? null : a.getCarrierStatus().name(),
                includeSensitive ? a.getSecurityLevel() : null,
                includeSensitive ? a.getOpenStatus() : null,
                (includeSensitive && a.getRetentionPeriod() != null) ? a.getRetentionPeriod().getDbValue() : null,
                fileSummaries);
    }

    // ==================== 5.4 / 5.5 / 11.5 / 11.6 预览/下载 ====================

    public String publicPreview(Long fileId, Long userId, String userType, HttpServletRequest req) {
        ArchiveFile file = loadVisiblePublicFile(fileId);
        logAccess(file.getArchiveId(), fileId, "preview", userId, userType, req);
        return minioService.getPresignedUrl(file.getBucketName(), file.getObjectKey());
    }

    public String publicDownload(Long fileId, Long userId, String userType, HttpServletRequest req) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "下载需要登录");
        }
        ArchiveFile file = loadVisiblePublicFile(fileId);
        logAccess(file.getArchiveId(), fileId, "download", userId, userType, req);
        return minioService.getPresignedUrl(file.getBucketName(), file.getObjectKey());
    }

    public String internalPreview(Long fileId, int maxSecurityLevel, DataScope dataScope,
                                  Long organizationId, Long userId, HttpServletRequest req) {
        ArchiveFile file = loadVisibleInternalFile(fileId, maxSecurityLevel, dataScope, organizationId);
        logAccess(file.getArchiveId(), fileId, "preview", userId, "internal", req);
        return minioService.getPresignedUrl(file.getBucketName(), file.getObjectKey());
    }

    public String internalDownload(Long fileId, int maxSecurityLevel, DataScope dataScope,
                                   Long organizationId, Long userId, HttpServletRequest req) {
        ArchiveFile file = loadVisibleInternalFile(fileId, maxSecurityLevel, dataScope, organizationId);
        logAccess(file.getArchiveId(), fileId, "download", userId, "internal", req);
        return minioService.getPresignedUrl(file.getBucketName(), file.getObjectKey());
    }

    /** 加载公众可见文件：文件存在 + 所属档案非密公开正常 + file_status=normal。 */
    ArchiveFile loadVisiblePublicFile(Long fileId) {
        ArchiveFile file = archiveFileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案文件不存在");
        }
        Archive a = archiveMapper.selectOne(new QueryWrapper<Archive>()
                .eq("id", file.getArchiveId())
                .eq("security_level", 0).eq("open_status", "open").eq("lifecycle_status", "normal"));
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在或不公开");
        }
        if (file.getFileStatus() != FileStatus.normal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "文件不可用");
        }
        return file;
    }

    /** 加载内部可见文件：文件存在 + 所属档案在当前用户权限范围 + file_status=normal。 */
    ArchiveFile loadVisibleInternalFile(Long fileId, int maxSecurityLevel, DataScope dataScope, Long organizationId) {
        ArchiveFile file = archiveFileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案文件不存在");
        }
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("id", file.getArchiveId()).eq("lifecycle_status", "normal").le("security_level", maxSecurityLevel);
        applyDataScope(w, dataScope, organizationId);
        Archive a = archiveMapper.selectOne(w);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在或无权访问");
        }
        if (file.getFileStatus() != FileStatus.normal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "文件不可用");
        }
        return file;
    }

    // ==================== 访问日志 ====================

    void logAccess(Long archiveId, Long fileId, String accessType,
                   Long userId, String userType, HttpServletRequest req) {
        accessLogMapper.insertAccessLog(userId, userType, archiveId, fileId,
                accessType, extractIp(req), OffsetDateTime.now());
    }

    private String extractIp(HttpServletRequest req) {
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
