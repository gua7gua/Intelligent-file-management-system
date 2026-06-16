package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.PublicStatsResponse;
import com.archive.entity.Category;
import com.archive.entity.IntakeBatch;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开馆藏统计（§5.1.1 GET /api/public/stats）。
 * 统计口径与 §5.2 公开检索一致：security_level=0 AND open_status=open AND lifecycle_status=normal。
 * 与 SearchService.isPublicSearchEnabled 保持同一 system_configs 查询口径。
 */
@Service
@RequiredArgsConstructor
public class PublicStatsService {

    private static final String PUBLIC_SEARCH_ENABLED_SQL =
            "SELECT COUNT(*) FROM system_configs WHERE config_key = 'public_search.enabled' AND config_value = 'true'";
    private static final int LATEST_OPEN_DAYS = 30;

    private final ArchiveMapper archiveMapper;
    private final IntakeBatchMapper intakeBatchMapper;
    private final CategoryMapper categoryMapper;
    private final JdbcTemplate jdbcTemplate;

    /** /api/public/stats 端点：公众检索开关关闭时抛 BUSINESS_CONFLICT（与 §5.2 一致）。 */
    public PublicStatsResponse publicStats() {
        if (!isPublicSearchEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "公众检索未开放");
        }
        return buildStats();
    }

    /** 公开统计（不检查检索开关），供公众概览 dashboard 复用。 */
    public PublicStatsResponse publicFigures() {
        return buildStats();
    }

    private PublicStatsResponse buildStats() {
        PublicStatsResponse r = new PublicStatsResponse();
        r.setOpenArchiveCount(archiveMapper.countPublicArchives());
        r.setElectronicFileCount(archiveMapper.countPublicElectronicFiles());
        r.setCollectionCount(intakeBatchMapper.selectCount(
                new QueryWrapper<IntakeBatch>().isNull("deleted_at")));
        r.setLatestOpenCount(archiveMapper.countPublicArchivesOpenedSince(
                OffsetDateTime.now().minusDays(LATEST_OPEN_DAYS)));
        r.setCategories(buildCategories());
        return r;
    }

    private List<PublicStatsResponse.CategoryCount> buildCategories() {
        Map<Integer, String> nameById = new HashMap<>();
        for (Category c : categoryMapper.selectList(null)) {
            if (c.getId() != null && c.getCategoryName() != null) {
                nameById.put((int) c.getId(), c.getCategoryName());
            }
        }
        List<PublicStatsResponse.CategoryCount> out = new ArrayList<>();
        for (Map<String, Object> row : archiveMapper.countPublicByCategory()) {
            Object idRaw = row.get("category_id");
            Object cntRaw = row.get("cnt");
            // category_id 或计数缺失（含 NULL 门类）的行跳过，不影响 openArchiveCount 全量口径
            if (!(idRaw instanceof Number) || !(cntRaw instanceof Number)) {
                continue;
            }
            int categoryId = ((Number) idRaw).intValue();
            long count = ((Number) cntRaw).longValue();
            out.add(new PublicStatsResponse.CategoryCount(nameById.get(categoryId), count));
        }
        return out;
    }

    private boolean isPublicSearchEnabled() {
        Integer count = jdbcTemplate.queryForObject(PUBLIC_SEARCH_ENABLED_SQL, Integer.class);
        return count != null && count > 0;
    }
}
