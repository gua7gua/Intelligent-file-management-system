package com.archive.service;

import com.archive.exception.BusinessException;
import com.archive.dto.response.PublicStatsResponse;
import com.archive.entity.Category;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.IntakeBatchMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicStatsServiceTest {

    @Mock
    private ArchiveMapper archiveMapper;
    @Mock
    private IntakeBatchMapper intakeBatchMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PublicStatsService service;

    @Test
    void publicStats_检索开关关闭_抛业务异常() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        assertThatThrownBy(() -> service.publicStats())
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(archiveMapper, intakeBatchMapper, categoryMapper);
    }

    @Test
    void publicStats_开关开启_聚合四项统计与门类分组() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(archiveMapper.countPublicArchives()).thenReturn(128L);
        when(archiveMapper.countPublicElectronicFiles()).thenReturn(56L);
        when(archiveMapper.countPublicArchivesOpenedSince(any(OffsetDateTime.class))).thenReturn(12L);
        when(intakeBatchMapper.selectCount(any())).thenReturn(30L);

        Category cat = new Category();
        cat.setId((short) 1);
        cat.setCategoryName("文书");
        when(categoryMapper.selectList(isNull())).thenReturn(List.of(cat));

        Map<String, Object> row = new HashMap<>();
        row.put("category_id", 1);
        row.put("cnt", 80L);
        when(archiveMapper.countPublicByCategory()).thenReturn(List.of(row));

        PublicStatsResponse r = service.publicStats();

        assertThat(r.getOpenArchiveCount()).isEqualTo(128L);
        assertThat(r.getElectronicFileCount()).isEqualTo(56L);
        assertThat(r.getCollectionCount()).isEqualTo(30L);
        assertThat(r.getLatestOpenCount()).isEqualTo(12L);
        assertThat(r.getCategories()).hasSize(1);
        assertThat(r.getCategories().get(0).getName()).isEqualTo("文书");
        assertThat(r.getCategories().get(0).getCount()).isEqualTo(80L);
    }

    @Test
    void publicFigures_供dashboard复用_不检查检索开关() {
        when(archiveMapper.countPublicArchives()).thenReturn(5L);
        when(archiveMapper.countPublicElectronicFiles()).thenReturn(2L);
        when(archiveMapper.countPublicArchivesOpenedSince(any(OffsetDateTime.class))).thenReturn(1L);
        when(intakeBatchMapper.selectCount(any())).thenReturn(3L);
        when(categoryMapper.selectList(isNull())).thenReturn(List.of());
        when(archiveMapper.countPublicByCategory()).thenReturn(List.of());

        PublicStatsResponse r = service.publicFigures();

        assertThat(r.getOpenArchiveCount()).isEqualTo(5L);
        // dashboard 复用路径不应触发检索开关查询
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void publicStats_门类行缺少id或计数时跳过() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(archiveMapper.countPublicArchives()).thenReturn(0L);
        when(archiveMapper.countPublicElectronicFiles()).thenReturn(0L);
        when(archiveMapper.countPublicArchivesOpenedSince(any(OffsetDateTime.class))).thenReturn(0L);
        when(intakeBatchMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.selectList(isNull())).thenReturn(List.of());

        Map<String, Object> noId = new HashMap<>();
        noId.put("category_id", null);
        noId.put("cnt", 7L);
        Map<String, Object> noCnt = new HashMap<>();
        noCnt.put("category_id", 2);
        noCnt.put("cnt", null);
        when(archiveMapper.countPublicByCategory()).thenReturn(List.of(noId, noCnt));

        PublicStatsResponse r = service.publicStats();

        assertThat(r.getCategories()).isEmpty();
    }
}
