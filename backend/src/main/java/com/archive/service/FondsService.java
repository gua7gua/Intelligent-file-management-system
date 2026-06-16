package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.FondsCreateRequest;
import com.archive.dto.request.FondsQuery;
import com.archive.dto.request.FondsUpdateRequest;
import com.archive.dto.response.FondsDetailResponse;
import com.archive.dto.response.FondsResponse;
import com.archive.entity.Fonds;
import com.archive.entity.Organization;
import com.archive.exception.BusinessException;
import com.archive.mapper.FondsMapper;
import com.archive.mapper.OrganizationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FondsService {

    private final FondsMapper fondsMapper;
    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    /** 17.1 查询全宗 */
    public PageResult<FondsResponse> listFonds(FondsQuery query) {
        QueryWrapper<Fonds> qw = new QueryWrapper<>();
        qw.isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            qw.eq("status", query.getStatus());
        }
        if (query.getOrganizationId() != null) {
            qw.eq("organization_id", query.getOrganizationId());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(w -> w.like("fonds_no", query.getKeyword())
                    .or().like("fonds_name", query.getKeyword()));
        }
        qw.orderByDesc("created_at");

        Page<Fonds> result = fondsMapper.selectPage(
                new Page<>(query.getPageNo(), query.getPageSize()), qw);

        List<Fonds> rows = result.getRecords();
        Map<Long, Long> archiveCounts = countByFonds("archives",
                rows.stream().map(Fonds::getId).toList());
        Map<Long, Long> boxCounts = countByFonds("archive_boxes",
                rows.stream().map(Fonds::getId).toList());
        Map<Long, Organization> orgMap = loadOrganizations(
                rows.stream().map(Fonds::getOrganizationId).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<FondsResponse> voList = rows.stream().map(f -> {
            FondsResponse vo = toResponse(f);
            vo.setArchiveCount(archiveCounts.getOrDefault(f.getId(), 0L));
            vo.setBoxCount(boxCounts.getOrDefault(f.getId(), 0L));
            Organization o = orgMap.get(f.getOrganizationId());
            vo.setOrganizationName(o != null ? o.getOrgName() : null);
            return vo;
        }).toList();
        return new PageResult<>(voList, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    /** 17.2 新增全宗 */
    @Transactional
    public FondsResponse createFonds(FondsCreateRequest req) {
        Fonds existing = fondsMapper.selectOne(new QueryWrapper<Fonds>().eq("fonds_no", req.getFondsNo()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "全宗号已存在");
        }
        Organization org = organizationMapper.selectById(req.getOrganizationId());
        if (org == null || org.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "所属组织不存在");
        }

        Fonds fonds = new Fonds();
        fonds.setFondsNo(req.getFondsNo());
        fonds.setFondsName(req.getFondsName());
        fonds.setOrganizationId(req.getOrganizationId());
        fonds.setDescription(req.getDescription());
        fonds.setStatus("active");
        fondsMapper.insert(fonds);

        auditService.log("M06", "create_fonds", "fonds", fonds.getId(),
                Map.of("fondsNo", fonds.getFondsNo(), "fondsName", fonds.getFondsName()));
        return toResponse(fonds);
    }

    /** 17.3 更新全宗（fondsNo 不可改；有业务关联时仅更新状态） */
    @Transactional
    public void updateFonds(Long id, FondsUpdateRequest req) {
        Fonds fonds = fondsMapper.selectById(id);
        if (fonds == null || fonds.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全宗不存在");
        }

        long assoc = countAssoc(id);
        boolean onlyStatus = assoc > 0L;

        if (onlyStatus) {
            // 有档案/档案盒关联：仅允许停用，忽略其余业务字段
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                fonds.setStatus(req.getStatus());
            }
            fondsMapper.updateById(fonds);
            auditService.log("M06", "update_fonds", "fonds", id,
                    Map.of("onlyStatus", true, "assoc", assoc,
                            "status", req.getStatus() != null ? req.getStatus() : fonds.getStatus()));
            return;
        }

        if (req.getFondsName() != null) fonds.setFondsName(req.getFondsName());
        if (req.getOrganizationId() != null) {
            Organization org = organizationMapper.selectById(req.getOrganizationId());
            if (org == null || org.getDeletedAt() != null) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "所属组织不存在");
            }
            fonds.setOrganizationId(req.getOrganizationId());
        }
        if (req.getDescription() != null) fonds.setDescription(req.getDescription());
        if (req.getStatus() != null && !req.getStatus().isBlank()) fonds.setStatus(req.getStatus());
        fondsMapper.updateById(fonds);
        auditService.log("M06", "update_fonds", "fonds", id, Map.of());
    }

    /** 详情 */
    public FondsDetailResponse getFondsDetail(Long id) {
        Fonds fonds = fondsMapper.selectById(id);
        if (fonds == null || fonds.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全宗不存在");
        }
        FondsDetailResponse vo = new FondsDetailResponse();
        vo.setId(fonds.getId());
        vo.setFondsNo(fonds.getFondsNo());
        vo.setFondsName(fonds.getFondsName());
        vo.setOrganizationId(fonds.getOrganizationId());
        vo.setDescription(fonds.getDescription());
        vo.setStatus(fonds.getStatus());
        vo.setCreatedAt(fonds.getCreatedAt());
        vo.setUpdatedAt(fonds.getUpdatedAt());
        if (fonds.getOrganizationId() != null) {
            Organization o = organizationMapper.selectById(fonds.getOrganizationId());
            vo.setOrganizationName(o != null ? o.getOrgName() : null);
        }
        vo.setArchiveCount(countWhere("archives", id));
        vo.setBoxCount(countWhere("archive_boxes", id));
        return vo;
    }

    // ===== 内部辅助 =====

    private long countAssoc(Long fondsId) {
        long a = countWhere("archives", fondsId);
        long b = countWhere("archive_boxes", fondsId);
        return a + b;
    }

    private long countWhere(String table, Long fondsId) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE fonds_id = ?", Long.class, fondsId);
        return cnt != null ? cnt : 0L;
    }

    private Map<Long, Long> countByFonds(String table, Collection<Long> fondsIds) {
        if (fondsIds.isEmpty()) return Map.of();
        String in = fondsIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT fonds_id AS fid, COUNT(*) AS cnt FROM " + table
                        + " WHERE fonds_id IN (" + in + ") GROUP BY fonds_id");
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(((Number) row.get("fid")).longValue(), ((Number) row.get("cnt")).longValue());
        }
        return result;
    }

    private Map<Long, Organization> loadOrganizations(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        List<Organization> list = organizationMapper.selectBatchIds(ids);
        Map<Long, Organization> map = new HashMap<>();
        for (Organization o : list) map.put(o.getId(), o);
        return map;
    }

    private FondsResponse toResponse(Fonds f) {
        FondsResponse vo = new FondsResponse();
        vo.setId(f.getId());
        vo.setFondsNo(f.getFondsNo());
        vo.setFondsName(f.getFondsName());
        vo.setOrganizationId(f.getOrganizationId());
        vo.setDescription(f.getDescription());
        vo.setStatus(f.getStatus());
        vo.setCreatedAt(f.getCreatedAt());
        return vo;
    }
}
