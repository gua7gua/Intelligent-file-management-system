package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.OrganizationCreateRequest;
import com.archive.dto.request.OrganizationQuery;
import com.archive.dto.request.OrganizationUpdateRequest;
import com.archive.dto.response.OrganizationResponse;
import com.archive.entity.Fonds;
import com.archive.entity.Organization;
import com.archive.entity.User;
import com.archive.enums.OrgType;
import com.archive.exception.BusinessException;
import com.archive.mapper.FondsMapper;
import com.archive.mapper.OrganizationMapper;
import com.archive.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final FondsMapper fondsMapper;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    /** 17.4 查询组织 */
    public PageResult<OrganizationResponse> listOrganizations(OrganizationQuery query) {
        QueryWrapper<Organization> qw = new QueryWrapper<>();
        qw.isNull("deleted_at");
        if (query.getOrgType() != null && !query.getOrgType().isBlank()) {
            qw.eq("org_type", query.getOrgType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            qw.eq("status", query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.like("org_name", query.getKeyword());
        }
        qw.orderByDesc("created_at");

        Page<Organization> result = organizationMapper.selectPage(
                new Page<>(query.getPageNo(), query.getPageSize()), qw);
        List<OrganizationResponse> voList = result.getRecords().stream()
                .map(this::toResponse).toList();
        return new PageResult<>(voList, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    /** 17.5 新增组织（名称唯一 §17.6） */
    @Transactional
    public OrganizationResponse createOrganization(OrganizationCreateRequest req) {
        Organization existing = organizationMapper.selectOne(
                new QueryWrapper<Organization>().eq("org_name", req.getOrgName()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "组织名称已存在");
        }

        Organization org = new Organization();
        org.setOrgName(req.getOrgName());
        org.setOrgType(OrgType.valueOf(req.getOrgType())); // 非法值抛 IllegalArgumentException → 全局 400
        org.setContactName(req.getContactName());
        org.setContactPhone(req.getContactPhone());
        org.setStatus("active");
        organizationMapper.insert(org);

        auditService.log("M06", "create_organization", "organization", org.getId(),
                Map.of("orgName", org.getOrgName(), "orgType", org.getOrgType().name()));
        return toResponse(org);
    }

    /** 17.6 更新组织（部分更新；名称唯一排除自身；停用不物理删除，保留历史档案归属） */
    @Transactional
    public OrganizationResponse updateOrganization(Long id, OrganizationUpdateRequest req) {
        Organization existing = organizationMapper.selectById(id);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组织不存在");
        }

        if (req.getOrgName() != null && !req.getOrgName().equals(existing.getOrgName())) {
            Organization conflict = organizationMapper.selectOne(
                    new QueryWrapper<Organization>().eq("org_name", req.getOrgName()));
            if (conflict != null && !conflict.getId().equals(id)) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "组织名称已存在");
            }
            existing.setOrgName(req.getOrgName());
        }
        if (req.getOrgType() != null && !req.getOrgType().isBlank()) {
            existing.setOrgType(OrgType.valueOf(req.getOrgType())); // 非法值抛 IllegalArgumentException → 全局 400
        }
        if (req.getContactName() != null) {
            existing.setContactName(req.getContactName());
        }
        if (req.getContactPhone() != null) {
            existing.setContactPhone(req.getContactPhone());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        organizationMapper.updateById(existing);

        auditService.log("M06", "update_organization", "organization", id,
                Map.of("orgName", existing.getOrgName(),
                        "status", existing.getStatus() != null ? existing.getStatus() : "active"));
        return toResponse(existing);
    }

    /** 17.8 删除组织：无关联全宗和用户时软删除；有关联则拒绝，提示改用停用 */
    @Transactional
    public void deleteOrganization(Long id) {
        Organization existing = organizationMapper.selectById(id);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组织不存在");
        }
        long fondsCount = fondsMapper.selectCount(
                new QueryWrapper<Fonds>().eq("organization_id", id).isNull("deleted_at"));
        long userCount = userMapper.selectCount(
                new QueryWrapper<User>().eq("organization_id", id).isNull("deleted_at"));
        Long archiveRaw = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM archives WHERE organization_id = ?", Long.class, id);
        long archiveCount = archiveRaw != null ? archiveRaw : 0L;
        Long intakeRaw = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM intake_batches WHERE organization_id = ?", Long.class, id);
        long intakeCount = intakeRaw != null ? intakeRaw : 0L;
        if (fondsCount > 0 || userCount > 0 || archiveCount > 0 || intakeCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "存在关联全宗、用户、档案或移交批次，无法删除，请改用停用");
        }
        // 硬删除（物理删除）：关联校验通过后真删，释放 org_name 唯一约束，便于同名组织重建
        organizationMapper.deleteById(id);
        auditService.log("M06", "delete_organization", "organization", id,
                Map.of("orgName", existing.getOrgName()));
    }

    private OrganizationResponse toResponse(Organization o) {
        OrganizationResponse vo = new OrganizationResponse();
        vo.setId(o.getId());
        vo.setOrgName(o.getOrgName());
        vo.setOrgType(o.getOrgType() != null ? o.getOrgType().name() : null);
        vo.setContactName(o.getContactName());
        vo.setContactPhone(o.getContactPhone());
        vo.setStatus(o.getStatus());
        vo.setCreatedAt(o.getCreatedAt());
        // 关联计数：前端据此决定显示「删除」还是「停用」（组织列表通常较小，逐条统计可接受）
        vo.setFondsCount(fondsMapper.selectCount(
                new QueryWrapper<Fonds>().eq("organization_id", o.getId()).isNull("deleted_at")));
        vo.setUserCount(userMapper.selectCount(
                new QueryWrapper<User>().eq("organization_id", o.getId()).isNull("deleted_at")));
        return vo;
    }
}
