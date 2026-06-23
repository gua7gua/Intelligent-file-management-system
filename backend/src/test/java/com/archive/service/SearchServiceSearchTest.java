package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.ArchiveSearchQuery;
import com.archive.dto.response.ArchiveSummaryResponse;
import com.archive.entity.Archive;
import com.archive.entity.Category;
import com.archive.enums.DataScope;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SearchServiceSearchTest {

    private SearchService service;
    private ArchiveMapper archiveMapper;
    private ArchiveFileMapper archiveFileMapper;
    private ArchiveAccessLogMapper accessLogMapper;
    private CategoryMapper categoryMapper;
    private TagMapper tagMapper;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        archiveFileMapper = mock(ArchiveFileMapper.class);
        accessLogMapper = mock(ArchiveAccessLogMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        tagMapper = mock(TagMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new SearchService(archiveMapper, archiveFileMapper, accessLogMapper,
                categoryMapper, tagMapper, jdbcTemplate, null, mock(BorrowService.class));
    }

    @Test
    void buildInternalWrapper_own_org_含本单位与他单位非密的OR分支() {
        ArchiveSearchQuery q = new ArchiveSearchQuery();
        QueryWrapper<Archive> w = service.buildInternalWrapper(q, 2, DataScope.own_org, 100L);
        String sql = w.getCustomSqlSegment().toUpperCase();
        // 改造后 own_org 引入 (organization_id=? OR (organization_id<>? AND security_level=0)) 结构
        assertThat(sql).contains("ORGANIZATION_ID");
        assertThat(sql).contains("SECURITY_LEVEL");
        assertThat(sql).contains("OR");
    }

    @Test
    void buildInternalWrapper_all_不施加组织过滤() {
        ArchiveSearchQuery q = new ArchiveSearchQuery();
        QueryWrapper<Archive> w = service.buildInternalWrapper(q, 4, DataScope.all, 100L);
        String sql = w.getCustomSqlSegment().toUpperCase();
        assertThat(sql).doesNotContain("ORGANIZATION_ID");
    }

    @Test
    void 公众检索强制过滤非密公开正常() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(archiveMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<Archive> p = inv.getArgument(0);
            p.setRecords(List.of());
            p.setTotal(0L);
            return p;
        });
        when(categoryMapper.selectList(any())).thenReturn(List.of());
        when(archiveFileMapper.selectCount(any())).thenReturn(0L);

        service.publicSearch(new ArchiveSearchQuery());

        ArgumentCaptor<QueryWrapper<Archive>> cap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), cap.capture());
        assertThat(cap.getValue().getCustomSqlSegment()).contains("security_level", "open_status", "lifecycle_status");
    }

    @Test
    void 公众检索未开放抛冲突() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        assertThatThrownBy(() -> service.publicSearch(new ArchiveSearchQuery()))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void 内部检索强制生命周期正常并按密级上限过滤() {
        QueryWrapper<Archive> w = service.buildInternalWrapper(new ArchiveSearchQuery(), 2, DataScope.all, null);
        assertThat(w.getCustomSqlSegment()).contains("lifecycle_status", "security_level");
    }

    @Test
    void 内部检索own_org数据范围追加组织过滤() {
        QueryWrapper<Archive> w = service.buildInternalWrapper(new ArchiveSearchQuery(), 2, DataScope.own_org, 100L);
        assertThat(w.getCustomSqlSegment()).contains("organization_id");
    }

    @Test
    void 内部检索own_fonds追加全宗子查询() {
        QueryWrapper<Archive> w = service.buildInternalWrapper(new ArchiveSearchQuery(), 2, DataScope.own_fonds, 100L);
        assertThat(w.getCustomSqlSegment()).contains("fonds_id");
    }

    @Test
    void tagIds生成archive_tags子查询() {
        ArchiveSearchQuery q = new ArchiveSearchQuery();
        q.setTagIds("1,2,3");
        QueryWrapper<Archive> w = service.buildInternalWrapper(q, 4, DataScope.all, null);
        assertThat(w.getCustomSqlSegment()).contains("archive_tags");
    }

    @Test
    void hasElectronicFile生成archive_files子查询() {
        ArchiveSearchQuery q = new ArchiveSearchQuery();
        q.setHasElectronicFile(true);
        QueryWrapper<Archive> w = service.buildInternalWrapper(q, 4, DataScope.all, null);
        assertThat(w.getCustomSqlSegment()).contains("archive_files");
    }

    @Test
    void 检索结果正确映射为摘要() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        Archive a = new Archive();
        a.setId(7L);
        a.setArchiveNo("ARC-000007");
        a.setTitle("测试档案");
        a.setResponsibleText("责任者");
        a.setFormedYear(2025);
        a.setCategoryId(1);
        when(archiveMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<Archive> p = inv.getArgument(0);
            p.setRecords(List.of(a));
            p.setTotal(1L);
            return p;
        });
        Category c = new Category();
        c.setId((short) 1);
        c.setCategoryName("文书档案");
        when(categoryMapper.selectList(any())).thenReturn(List.of(c));
        when(archiveFileMapper.selectCount(any())).thenReturn(1L);

        PageResult<ArchiveSummaryResponse> result = service.publicSearch(new ArchiveSearchQuery());

        assertThat(result.getRecords()).hasSize(1);
        ArchiveSummaryResponse s = result.getRecords().get(0);
        assertThat(s.getArchiveId()).isEqualTo(7L);
        assertThat(s.getCategoryName()).isEqualTo("文书档案");
        assertThat(s.getHasElectronicFile()).isTrue();
    }
}
