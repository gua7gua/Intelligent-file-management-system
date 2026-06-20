package com.archive.service;

import com.archive.common.ErrorCode;
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FondsServiceTest {

    private FondsService service;
    private FondsMapper fondsMapper;
    private OrganizationMapper organizationMapper;
    private AuditService auditService;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        fondsMapper = mock(FondsMapper.class);
        organizationMapper = mock(OrganizationMapper.class);
        auditService = mock(AuditService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new FondsService(fondsMapper, organizationMapper, auditService, jdbcTemplate);
    }

    @Test
    void createFonds_成功并写审计() {
        when(fondsMapper.selectOne(any())).thenReturn(null);
        Organization org = new Organization();
        org.setId(2L);
        org.setOrgName("苏州市财政局");
        when(organizationMapper.selectById(2L)).thenReturn(org);
        when(fondsMapper.insert(any(Fonds.class))).thenAnswer(inv -> {
            ((Fonds) inv.getArgument(0)).setId(10L);
            return 1;
        });

        FondsCreateRequest req = new FondsCreateRequest();
        req.setFondsNo("F001");
        req.setFondsName("财政局全宗");
        req.setOrganizationId(2L);
        FondsResponse resp = service.createFonds(req);

        assertThat(resp.getId()).isEqualTo(10L);
        verify(fondsMapper).insert(any(Fonds.class));
        verify(auditService).log(eq("M06"), eq("create_fonds"), eq("fonds"), eq(10L), any());
    }

    @Test
    void createFonds_全宗号重复抛冲突() {
        Fonds exist = new Fonds();
        exist.setId(1L);
        exist.setFondsNo("F001");
        when(fondsMapper.selectOne(any())).thenReturn(exist);

        FondsCreateRequest req = new FondsCreateRequest();
        req.setFondsNo("F001");
        req.setFondsName("x");
        req.setOrganizationId(2L);

        assertThatThrownBy(() -> service.createFonds(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("全宗号已存在");
        verify(fondsMapper, never()).insert(any(Fonds.class));
    }

    @Test
    void createFonds_组织不存在抛冲突() {
        when(fondsMapper.selectOne(any())).thenReturn(null);
        when(organizationMapper.selectById(2L)).thenReturn(null);

        FondsCreateRequest req = new FondsCreateRequest();
        req.setFondsNo("F002");
        req.setFondsName("x");
        req.setOrganizationId(2L);

        assertThatThrownBy(() -> service.createFonds(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("所属组织不存在");
    }

    @Test
    void updateFonds_有业务关联时仅更新状态() {
        Fonds f = newFonds(10L, "F001", "旧名", 2L);
        when(fondsMapper.selectById(10L)).thenReturn(f);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(10L))).thenReturn(5L);

        FondsUpdateRequest req = new FondsUpdateRequest();
        req.setFondsName("新名");
        req.setStatus("disabled");

        service.updateFonds(10L, req);

        assertThat(f.getFondsName()).isEqualTo("旧名");
        assertThat(f.getStatus()).isEqualTo("disabled");
        verify(fondsMapper).updateById(any(Fonds.class));
        verify(auditService).log(eq("M06"), eq("update_fonds"), eq("fonds"), eq(10L), any());
    }

    @Test
    void updateFonds_无业务关联时全字段更新() {
        Fonds f = newFonds(10L, "F001", "旧名", 2L);
        when(fondsMapper.selectById(10L)).thenReturn(f);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(10L))).thenReturn(0L);

        FondsUpdateRequest req = new FondsUpdateRequest();
        req.setFondsName("新名");
        req.setDescription("新备注");

        service.updateFonds(10L, req);

        assertThat(f.getFondsName()).isEqualTo("新名");
        assertThat(f.getDescription()).isEqualTo("新备注");
    }

    @Test
    void updateFonds_不存在抛404() {
        when(fondsMapper.selectById(99L)).thenReturn(null);
        FondsUpdateRequest req = new FondsUpdateRequest();
        req.setFondsName("x");
        assertThatThrownBy(() -> service.updateFonds(99L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void listFonds_分页并补充归档数量与组织名() {
        Fonds f = newFonds(10L, "F001", "财政局全宗", 2L);
        Page<Fonds> page = new Page<>(1, 20);
        page.setRecords(List.of(f));
        page.setTotal(1L);
        when(fondsMapper.selectPage(any(Page.class), any())).thenReturn(page);
        Organization org = new Organization();
        org.setId(2L);
        org.setOrgName("苏州市财政局");
        when(organizationMapper.selectBatchIds(any())).thenReturn(List.of(org));
        when(jdbcTemplate.queryForList(anyString())).thenReturn(
                List.of(Map.of("fid", 10L, "cnt", 3L)));

        FondsQuery q = new FondsQuery();
        q.setPageNo(1);
        q.setPageSize(20);
        var result = service.listFonds(q);

        assertThat(result.getRecords()).hasSize(1);
        FondsResponse r = result.getRecords().get(0);
        assertThat(r.getArchiveCount()).isEqualTo(3L);
        assertThat(r.getOrganizationName()).isEqualTo("苏州市财政局");
    }

    @Test
    void getDetail_带归档与档案盒数量() {
        Fonds f = newFonds(10L, "F001", "财政局全宗", 2L);
        when(fondsMapper.selectById(10L)).thenReturn(f);
        Organization org = new Organization();
        org.setId(2L);
        org.setOrgName("苏州市财政局");
        when(organizationMapper.selectById(2L)).thenReturn(org);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(10L)))
                .thenReturn(7L)
                .thenReturn(2L);

        FondsDetailResponse d = service.getFondsDetail(10L);
        assertThat(d.getArchiveCount()).isEqualTo(7L);
        assertThat(d.getBoxCount()).isEqualTo(2L);
    }

    @Test
    void deleteFonds_无关联_置空引用后物理删除() {
        Fonds fonds = new Fonds();
        fonds.setId(5L);
        fonds.setFondsName("误建全宗");
        when(fondsMapper.selectById(5L)).thenReturn(fonds);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM archives WHERE fonds_id = ?"),
                eq(Long.class), eq(5L))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM archive_boxes WHERE fonds_id = ?"),
                eq(Long.class), eq(5L))).thenReturn(0L);

        service.deleteFonds(5L);

        verify(jdbcTemplate).update(eq("UPDATE archives SET fonds_id = NULL WHERE fonds_id = ?"), eq(5L));
        verify(jdbcTemplate).update(eq("UPDATE archive_boxes SET fonds_id = NULL WHERE fonds_id = ?"), eq(5L));
        verify(fondsMapper).deleteById(5L);
        verify(auditService).log(eq("M06"), eq("delete_fonds"), eq("fonds"), eq(5L), any());
    }

    @Test
    void deleteFonds_有关联_仍置空引用后删除() {
        Fonds fonds = new Fonds();
        fonds.setId(6L);
        fonds.setFondsName("在用全宗");
        when(fondsMapper.selectById(6L)).thenReturn(fonds);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM archives WHERE fonds_id = ?"),
                eq(Long.class), eq(6L))).thenReturn(3L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM archive_boxes WHERE fonds_id = ?"),
                eq(Long.class), eq(6L))).thenReturn(2L);

        service.deleteFonds(6L);

        verify(jdbcTemplate).update(eq("UPDATE archives SET fonds_id = NULL WHERE fonds_id = ?"), eq(6L));
        verify(jdbcTemplate).update(eq("UPDATE archive_boxes SET fonds_id = NULL WHERE fonds_id = ?"), eq(6L));
        verify(fondsMapper).deleteById(6L);
    }

    @Test
    void deleteFonds_全宗不存在_抛NOT_FOUND() {
        when(fondsMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.deleteFonds(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    private Fonds newFonds(Long id, String no, String name, Long orgId) {
        Fonds f = new Fonds();
        f.setId(id);
        f.setFondsNo(no);
        f.setFondsName(name);
        f.setOrganizationId(orgId);
        f.setStatus("active");
        return f;
    }
}
