package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.AiQueryResponse;
import com.archive.dto.response.InternalDashboardResponse;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.TagMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SearchServiceAiTest {

    private SearchService service;
    private ArchiveMapper archiveMapper;
    private ArchiveAccessLogMapper accessLogMapper;
    private JdbcTemplate jdbcTemplate;
    private AiClient aiClient;
    private BorrowService borrowService;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        accessLogMapper = mock(ArchiveAccessLogMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        aiClient = mock(AiClient.class);
        borrowService = mock(BorrowService.class);
        when(borrowService.dashboardMine(anyLong(), anyInt())).thenReturn(List.of());
        when(borrowService.dashboardCurrent(anyLong(), anyInt())).thenReturn(List.of());
        when(borrowService.dashboardOverdue(anyLong(), anyInt())).thenReturn(List.of());
        service = new SearchService(archiveMapper, mock(ArchiveFileMapper.class), accessLogMapper,
                mock(CategoryMapper.class), mock(TagMapper.class), jdbcTemplate, null, aiClient, borrowService);
        service.setObjectMapper(om);
    }

    private JsonNode conditions(String ruleType, String conditionsJson) throws Exception {
        return om.readTree("{\"ruleType\":\"" + ruleType + "\",\"conditions\":" + conditionsJson + "}");
    }

    @Test
    void 重试_第一次ruleType错第二次通过() throws Exception {
        JsonNode bad = conditions("oops", "{}");
        JsonNode good = conditions("query", "{\"keyword\":\"财政\"}");
        when(aiClient.getSearchMaxRetries()).thenReturn(3);
        when(aiClient.callAndExtractJson(anyString(), anyString())).thenReturn(bad, good);

        JsonNode result = service.callSearchAiWithRetry("sys", "user");

        assertThat(result.get("ruleType").asText()).isEqualTo("query");
        verify(aiClient, times(2)).callAndExtractJson(anyString(), anyString());
    }

    @Test
    void 重试_三次ruleType都错抛异常() throws Exception {
        JsonNode bad = conditions("oops", "{}");
        when(aiClient.getSearchMaxRetries()).thenReturn(3);
        when(aiClient.callAndExtractJson(anyString(), anyString())).thenReturn(bad);

        assertThatThrownBy(() -> service.callSearchAiWithRetry("sys", "user"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()));
        verify(aiClient, times(3)).callAndExtractJson(anyString(), anyString());
    }

    @Test
    void 重试_AiClient抛异常不重试直接抛() {
        when(aiClient.getSearchMaxRetries()).thenReturn(3);
        when(aiClient.callAndExtractJson(anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 不可用"));

        assertThatThrownBy(() -> service.callSearchAiWithRetry("sys", "user"))
                .isInstanceOf(BusinessException.class);
        verify(aiClient, times(1)).callAndExtractJson(anyString(), anyString());
    }

    @Test
    void publicAiQuery_AI未启用抛外部异常() {
        when(aiClient.isAvailable()).thenReturn(false);
        assertThatThrownBy(() -> service.publicAiQuery("查档案"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()));
    }

    @Test
    void publicAiQuery_公众检索未开放抛冲突() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        assertThatThrownBy(() -> service.publicAiQuery("查档案"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void publicAiQuery_强制覆盖公众公开条件() throws Exception {
        when(aiClient.isAvailable()).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("财政", "会计"));
        when(aiClient.getSearchMaxRetries()).thenReturn(3);
        when(aiClient.callAndExtractJson(anyString(), anyString()))
                .thenReturn(conditions("query", "{\"keyword\":\"财政\",\"securityLevelMax\":3,\"openStatus\":\"closed\"}"));

        AiQueryResponse resp = service.publicAiQuery("查财政档案");

        assertThat(resp.getRuleType()).isEqualTo("query");
        assertThat(resp.getConditions().get("securityLevelMax").asInt()).isEqualTo(0);   // 强制覆盖
        assertThat(resp.getConditions().get("openStatus").asText()).isEqualTo("open");   // 强制覆盖
    }

    @Test
    void getInternalDashboard_返回最近查阅借阅部分空() {
        Map<String, Object> row = new HashMap<>();
        row.put("archiveId", 7L);
        row.put("archiveNo", "ARC-000007");
        row.put("title", "标题");
        row.put("accessedAt", OffsetDateTime.now());
        when(accessLogMapper.findRecentViews(5L)).thenReturn(List.of(row));

        InternalDashboardResponse d = service.getInternalDashboard(5L);

        assertThat(d.getRecentViews()).hasSize(1);
        assertThat(d.getRecentViews().get(0).getArchiveNo()).isEqualTo("ARC-000007");
        assertThat(d.getMyBorrowRequests()).isEmpty();
        assertThat(d.getCurrentBorrows()).isEmpty();
        assertThat(d.getOverdueReminders()).isEmpty();
    }

    @Test
    void getInternalDashboard_无查阅记录返回空列表() {
        when(accessLogMapper.findRecentViews(5L)).thenReturn(List.of());
        InternalDashboardResponse d = service.getInternalDashboard(5L);
        assertThat(d.getRecentViews()).isEmpty();
    }
}
