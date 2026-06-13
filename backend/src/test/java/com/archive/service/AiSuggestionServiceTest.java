package com.archive.service;

import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeItem;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.IntakeItemMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiSuggestionServiceTest {

    private AiSuggestionService service;
    private IntakeItemMapper intakeItemMapper;
    private CategoryMapper categoryMapper;
    private final ObjectMapper om = new ObjectMapper();

    /** 五类 code 与中文名都映射到 id，模拟 loadCategoryMap() 结果 */
    private Map<String, Integer> categoryMap() {
        Map<String, Integer> m = new HashMap<>();
        m.put("document", 1); m.put("文书档案", 1);
        m.put("technology", 2); m.put("科技档案", 2);
        m.put("accounting", 3); m.put("会计档案", 3);
        m.put("audio_video", 4); m.put("音像档案", 4);
        m.put("personnel", 5); m.put("人事档案", 5);
        return m;
    }

    private JsonNode json(String s) throws Exception {
        return om.readTree(s);
    }

    @BeforeEach
    void setup() {
        intakeItemMapper = mock(IntakeItemMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        service = new AiSuggestionService(intakeItemMapper, categoryMapper);
    }

    @Test
    void 正常items全部字段保留() throws Exception {
        JsonNode result = json("{\"ruleType\":\"fieldCompletion\",\"items\":[{\"listItemId\":1,\"fields\":{\"title\":\"标题\",\"responsible\":\"责任者\",\"formedDate\":\"2025-01-01\",\"category\":\"document\",\"tags\":[\"a\",\"b\"]}}]}");
        Map<Long, Map<String, Object>> out = service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap());
        assertThat(out).containsKey(1L);
        Map<String, Object> f = out.get(1L);
        assertThat(f).containsEntry("title", "标题")
                .containsEntry("responsible", "责任者")
                .containsEntry("formedDate", "2025-01-01")
                .containsEntry("categoryId", 1)
                .containsEntry("tags", List.of("a", "b"));
    }

    @Test
    void 受保护字段全部剥离() throws Exception {
        JsonNode result = json("{\"ruleType\":\"fieldCompletion\",\"items\":[{\"listItemId\":1,\"fields\":{" +
                "\"title\":\"保留\"," +
                "\"securityLevel\":2,\"retentionPeriod\":\"30y\",\"openStatus\":\"closed\"," +
                "\"allowDigitization\":false,\"archiveNo\":\"ARC-1\",\"warehouseLocation\":\"A-1\",\"destructionStatus\":\"normal\"}}]}");
        Map<Long, Map<String, Object>> out = service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap());
        Map<String, Object> f = out.get(1L);
        assertThat(f).containsOnlyKeys("title");
    }

    @Test
    void category非法跳过该字段() throws Exception {
        JsonNode result = json("{\"ruleType\":\"fieldCompletion\",\"items\":[{\"listItemId\":1,\"fields\":{\"title\":\"保留\",\"category\":\"不存在的分类\"}}]}");
        Map<Long, Map<String, Object>> out = service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap());
        assertThat(out.get(1L)).containsOnlyKeys("title");
    }

    @Test
    void category中文名归一化为categoryId() throws Exception {
        JsonNode result = json("{\"ruleType\":\"fieldCompletion\",\"items\":[{\"listItemId\":1,\"fields\":{\"category\":\"会计档案\"}}]}");
        Map<Long, Map<String, Object>> out = service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap());
        assertThat(out.get(1L)).containsEntry("categoryId", 3);
    }

    @Test
    void listItemId不在targetIds整条丢弃() throws Exception {
        JsonNode result = json("{\"ruleType\":\"fieldCompletion\",\"items\":[{\"listItemId\":99,\"fields\":{\"title\":\"x\"}}]}");
        Map<Long, Map<String, Object>> out = service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap());
        assertThat(out).isEmpty();
    }

    @Test
    void formedDate格式非法跳过() throws Exception {
        JsonNode result = json("{\"ruleType\":\"fieldCompletion\",\"items\":[{\"listItemId\":1,\"fields\":{\"title\":\"保留\",\"formedDate\":\"2025/01/01\"}}]}");
        Map<Long, Map<String, Object>> out = service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap());
        assertThat(out.get(1L)).containsOnlyKeys("title");
    }

    @Test
    void tags非数组跳过() throws Exception {
        JsonNode result = json("{\"ruleType\":\"fieldCompletion\",\"items\":[{\"listItemId\":1,\"fields\":{\"title\":\"保留\",\"tags\":\"不是数组\"}}]}");
        Map<Long, Map<String, Object>> out = service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap());
        assertThat(out.get(1L)).containsOnlyKeys("title");
    }

    @Test
    void ruleType错误返回空() throws Exception {
        JsonNode result = json("{\"ruleType\":\"something_else\",\"items\":[{\"listItemId\":1,\"fields\":{\"title\":\"x\"}}]}");
        assertThat(service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap())).isEmpty();
    }

    @Test
    void items非数组返回空() throws Exception {
        JsonNode result = json("{\"ruleType\":\"fieldCompletion\",\"items\":\"oops\"}");
        assertThat(service.parseAndFilterItems(result, new HashSet<>(Set.of(1L)), categoryMap())).isEmpty();
    }

    @Test
    void validateAndPersist_合法结果写入ai_suggestion并返回条目数() throws Exception {
        JsonNode aiResult = json("{\"ruleType\":\"fieldCompletion\",\"items\":[{\"listItemId\":1,\"fields\":{\"title\":\"新标题\"}}]}");
        AiTaskBatch batch = new AiTaskBatch();
        batch.setId(10L);
        batch.setTargetIds(List.of(1L));

        IntakeItem item = new IntakeItem();
        item.setId(1L);
        when(intakeItemMapper.selectById(1L)).thenReturn(item);
        when(intakeItemMapper.updateById(any(IntakeItem.class))).thenReturn(1);
        when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        int count = service.validateAndPersist(aiResult, batch);

        assertThat(count).isEqualTo(1);
        assertThat(item.getAiSuggestion()).containsEntry("title", "新标题");
        assertThat(batch.getValidatedResult()).isNotNull();
        verify(intakeItemMapper).updateById(item);
    }
}
