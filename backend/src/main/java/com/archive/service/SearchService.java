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
import com.archive.dto.response.AiQueryResponse;
import com.archive.dto.response.InternalDashboardResponse;
import com.archive.enums.FileStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
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
    private final BorrowService borrowService;

    @Autowired
    private ObjectMapper objectMapper;

    /** 单元测试注入用。 */
    void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        List<Archive> archives = page.getRecords();
        // 批量加载标签，避免逐条 N+1 查询
        Map<Long, List<String>> tagNamesByArchive = loadTagNamesByArchive(archives);
        List<ArchiveSummaryResponse> records = archives.stream()
                .map(a -> toSummary(a, categoryNameById,
                        tagNamesByArchive.getOrDefault(a.getId(), List.of())))
                .toList();
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private ArchiveSummaryResponse toSummary(Archive a, Map<Integer, String> categoryNameById,
                                             List<String> tagNames) {
        Long fileCount = archiveFileMapper.selectCount(new QueryWrapper<ArchiveFile>()
                .eq("archive_id", a.getId()).eq("file_status", "normal"));
        return new ArchiveSummaryResponse(
                a.getId(), a.getArchiveNo(), a.getTitle(), a.getResponsibleText(),
                a.getFormedYear(), categoryNameById.get(a.getCategoryId()),
                a.getCarrierStatus() == null ? null : a.getCarrierStatus().name(),
                fileCount != null && fileCount > 0,
                a.getSourceType() == null ? null : a.getSourceType().name(),
                a.getSecurityLevel(),
                tagNames);
    }

    /** 批量查询给定档案集合的标签名（archive_id → tag_name 列表）。ids 来自库内记录，非用户输入。 */
    private Map<Long, List<String>> loadTagNamesByArchive(List<Archive> archives) {
        if (archives == null || archives.isEmpty()) {
            return Map.of();
        }
        String inClause = archives.stream().map(a -> String.valueOf(a.getId()))
                .collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT at.archive_id AS aid, t.tag_name AS name FROM archive_tags at " +
                        "JOIN tags t ON t.id = at.tag_id " +
                        "WHERE at.archive_id IN (" + inClause + ")");
        Map<Long, List<String>> map = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Long aid = ((Number) r.get("aid")).longValue();
            String name = (String) r.get("name");
            map.computeIfAbsent(aid, k -> new ArrayList<>()).add(name);
        }
        return map;
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
        // 电子件可访问性：内部版（includeSensitive=true）已在 internalDetail 按 maxSecurityLevel+数据范围
        // 校验过权限，故内部用户对权限内档案（含 closed）均可预览/下载；公众版仅 open 档案可访问。
        // （open_status 表示"是否对外开放"，约束公众；不约束内部在密级+数据范围权限内的访问。）
        boolean accessible = includeSensitive || "open".equals(a.getOpenStatus());
        List<ArchiveSearchDetailResponse.FileSummary> fileSummaries = files.stream()
                .map(f -> new ArchiveSearchDetailResponse.FileSummary(
                        f.getId(), f.getOriginalFilename(), f.getFileExt(),
                        f.getFileSize(), f.getMimeType(),
                        f.getFileRole() == null ? null : f.getFileRole().name(),
                        accessible, accessible))
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

    /** 加载内部可见文件：文件存在 + 所属档案在当前用户密级上限+数据范围权限内 + file_status=normal。
     *  open_status 仅约束公众开放，不约束内部在权限内的访问（closed 档案内部可见元数据+电子件）。 */
    ArchiveFile loadVisibleInternalFile(Long fileId, int maxSecurityLevel, DataScope dataScope, Long organizationId) {
        ArchiveFile file = archiveFileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案文件不存在");
        }
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("id", file.getArchiveId()).eq("lifecycle_status", "normal")
                .le("security_level", maxSecurityLevel);
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

    // ==================== 5.6 / 11.4 AI 检索 JSON 生成 ====================

    public AiQueryResponse publicAiQuery(String text) {
        if (!aiClient.isAvailable()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 功能未启用");
        }
        if (!isPublicSearchEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "公众检索未开放");
        }
        List<String> tags = loadVisibleTagsForPublic();
        JsonNode result = callSearchAiWithRetry(buildSearchSystemPrompt(),
                buildSearchUserMessage(text, tags));
        ObjectNode conditions = ensureObjectNode(result.get("conditions"));
        conditions.put("securityLevelMax", 0);     // 公众强制
        conditions.put("openStatus", "open");      // 公众强制
        return new AiQueryResponse("query", conditions, result);
    }

    public AiQueryResponse internalAiQuery(String text, int maxSecurityLevel,
                                           DataScope dataScope, Long organizationId) {
        if (!aiClient.isAvailable()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 功能未启用");
        }
        List<String> tags = loadVisibleTagsForInternal(maxSecurityLevel, dataScope, organizationId);
        JsonNode result = callSearchAiWithRetry(buildSearchSystemPrompt(),
                buildSearchUserMessage(text, tags));
        JsonNode conditions = result.get("conditions") != null
                ? result.get("conditions") : objectMapper.createObjectNode();
        return new AiQueryResponse("query", conditions, result);
    }

    /**
     * 同步调用 AI 并校验 ruleType；AiClient 抛异常不重试（直接传播），
     * 仅 ruleType != query 时重试，最多 searchMaxRetries 次。
     */
    JsonNode callSearchAiWithRetry(String systemPrompt, String userMessage) {
        int max = aiClient.getSearchMaxRetries();
        for (int i = 0; i < max; i++) {
            JsonNode result = aiClient.callAndExtractJson(systemPrompt, userMessage);
            JsonNode ruleType = result.get("ruleType");
            if (ruleType != null && "query".equals(ruleType.asText())) {
                return result;
            }
        }
        throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 检索结果格式异常，请改用普通筛选");
    }

    private String buildSearchSystemPrompt() {
        return """
                你是档案管理系统的 AI 助手，将用户的自然语言检索需求转换为结构化查询条件。

                约束：
                - 分类必须从系统给定分类中选择（document/technology/accounting/audio_video/personnel）。
                - 标签必须优先从系统给定的可见标签集合中选择。
                - 日期格式统一为 yyyy-MM-dd。
                - 如用户为公众用户，必须附加 securityLevelMax = 0 且 openStatus = "open"。
                - 你只生成查询条件，不执行查询。

                输出格式：
                <JSON>
                {
                  "ruleType": "query",
                  "conditions": {
                    "keywords": ["关键词"],
                    "category": "分类code",
                    "formedDateRange": ["yyyy-MM-dd","yyyy-MM-dd"],
                    "responsible": "责任者",
                    "securityLevelMax": 0,
                    "openStatus": "open",
                    "carrierStatus": "electronic / paper_electronic / paper"
                  }
                }
                </JSON>
                """;
    }

    private String buildSearchUserMessage(String text, List<String> tags) {
        return "用户检索需求：" + text + "\n" +
                "可用分类：document/technology/accounting/audio_video/personnel\n" +
                "可见标签：" + String.join("、", tags);
    }

    private ObjectNode ensureObjectNode(JsonNode node) {
        if (node instanceof ObjectNode on) return on;
        return objectMapper.createObjectNode();
    }

    /** 公众可见标签：非密公开正常档案的 distinct tag_name。 */
    private List<String> loadVisibleTagsForPublic() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT t.tag_name FROM tags t " +
                "JOIN archive_tags at ON at.tag_id = t.id " +
                "JOIN archives a ON a.id = at.archive_id " +
                "WHERE a.security_level = 0 AND a.open_status = 'open' AND a.lifecycle_status = 'normal'",
                String.class);
    }

    /** 内部可见标签：按密级上限 + 数据范围 + 生命周期过滤后的 distinct tag_name。 */
    private List<String> loadVisibleTagsForInternal(int maxSecurityLevel, DataScope scope, Long organizationId) {
        String orgFilter = (scope == DataScope.all || organizationId == null) ? ""
                : (scope == DataScope.own_org
                    ? " AND a.organization_id = " + organizationId
                    : " AND a.fonds_id IN (SELECT id FROM fonds WHERE organization_id = " + organizationId + ")");
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT t.tag_name FROM tags t " +
                "JOIN archive_tags at ON at.tag_id = t.id " +
                "JOIN archives a ON a.id = at.archive_id " +
                "WHERE a.lifecycle_status = 'normal' AND a.security_level <= " + maxSecurityLevel + orgFilter,
                String.class);
    }

    // ==================== 11.1 内部工作台 ====================

    public InternalDashboardResponse getInternalDashboard(Long userId) {
        List<InternalDashboardResponse.RecentView> recentViews = new ArrayList<>();
        for (Map<String, Object> row : accessLogMapper.findRecentViews(userId)) {
            recentViews.add(new InternalDashboardResponse.RecentView(
                    toLong(row.get("archiveId")),
                    (String) row.get("archiveNo"),
                    (String) row.get("title"),
                    row.get("accessedAt") instanceof java.sql.Timestamp t ? t.toInstant().atOffset(java.time.ZoneOffset.ofHours(8)) : (row.get("accessedAt") instanceof OffsetDateTime odt ? odt : null)));
        }
        return new InternalDashboardResponse(
                recentViews,
                borrowService.dashboardMine(userId, 5),
                borrowService.dashboardCurrent(userId, 5),
                borrowService.dashboardOverdue(userId, 5));
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }
}
