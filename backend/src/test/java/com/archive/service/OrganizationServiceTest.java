package com.archive.service;

import com.archive.dto.request.OrganizationCreateRequest;
import com.archive.dto.request.OrganizationQuery;
import com.archive.dto.response.OrganizationResponse;
import com.archive.entity.Organization;
import com.archive.enums.OrgType;
import com.archive.exception.BusinessException;
import com.archive.mapper.OrganizationMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrganizationServiceTest {

    private OrganizationService service;
    private OrganizationMapper organizationMapper;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        organizationMapper = mock(OrganizationMapper.class);
        auditService = mock(AuditService.class);
        service = new OrganizationService(organizationMapper, auditService);
    }

    @Test
    void createOrganization_成功并写审计() {
        when(organizationMapper.selectOne(any())).thenReturn(null);
        when(organizationMapper.insert(any(Organization.class))).thenAnswer(inv -> {
            ((Organization) inv.getArgument(0)).setId(20L);
            return 1;
        });

        OrganizationCreateRequest req = new OrganizationCreateRequest();
        req.setOrgName("苏州市财政局");
        req.setOrgType("government");
        req.setContactName("张伟");
        OrganizationResponse resp = service.createOrganization(req);

        assertThat(resp.getId()).isEqualTo(20L);
        assertThat(resp.getOrgType()).isEqualTo("government");
        verify(organizationMapper).insert(any(Organization.class));
        verify(auditService).log(eq("M06"), eq("create_organization"), eq("organization"), eq(20L), any());
    }

    @Test
    void createOrganization_名称重复抛冲突() {
        Organization exist = new Organization();
        exist.setId(1L);
        exist.setOrgName("苏州市财政局");
        when(organizationMapper.selectOne(any())).thenReturn(exist);

        OrganizationCreateRequest req = new OrganizationCreateRequest();
        req.setOrgName("苏州市财政局");
        req.setOrgType("government");
        assertThatThrownBy(() -> service.createOrganization(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("组织名称已存在");
        verify(organizationMapper, never()).insert(any(Organization.class));
    }

    @Test
    void createOrganization_非法orgType抛异常() {
        when(organizationMapper.selectOne(any())).thenReturn(null);
        OrganizationCreateRequest req = new OrganizationCreateRequest();
        req.setOrgName("x");
        req.setOrgType("not_a_type");
        assertThatThrownBy(() -> service.createOrganization(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listOrganizations_分页过滤() {
        Organization o = new Organization();
        o.setId(1L);
        o.setOrgName("苏州市财政局");
        o.setOrgType(OrgType.government);
        o.setStatus("active");
        Page<Organization> page = new Page<>(1, 20);
        page.setRecords(List.of(o));
        page.setTotal(1L);
        when(organizationMapper.selectPage(any(), any())).thenReturn(page);

        OrganizationQuery q = new OrganizationQuery();
        q.setPageNo(1);
        q.setPageSize(20);
        var result = service.listOrganizations(q);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getOrgName()).isEqualTo("苏州市财政局");
    }
}
