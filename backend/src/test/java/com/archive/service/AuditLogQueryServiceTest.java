package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.AuditLogQuery;
import com.archive.dto.response.AuditLogResponse;
import com.archive.entity.AuditLog;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.AuditLogMapper;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogQueryServiceTest {

    private AuditLogQueryService service;
    private AuditLogMapper auditLogMapper;

    @BeforeEach
    void setup() {
        auditLogMapper = mock(AuditLogMapper.class);
        service = new AuditLogQueryService(auditLogMapper, mock(UserMapper.class), mock(ArchiveMapper.class));
    }

    @Test
    void 查询_满页时hasNext为true并返回nextCursor() {
        when(auditLogMapper.selectList(any())).thenReturn(List.of(
                log(3, "2026-06-15T10:00:03Z"),
                log(2, "2026-06-15T10:00:02Z"),
                log(1, "2026-06-15T10:00:01Z")));

        AuditLogQuery q = new AuditLogQuery();
        q.setLimit(2);
        CursorResult<AuditLogResponse> r = service.query(q);

        assertThat(r.getRecords()).hasSize(2);
        assertThat(r.getHasNext()).isTrue();
        CursorCodec.Decoded d = CursorCodec.decode(r.getNextCursor());
        assertThat(d).isNotNull();
        assertThat(d.id()).isEqualTo(2L);
    }

    @Test
    void 查询_不足一页时hasNext为false且无nextCursor() {
        when(auditLogMapper.selectList(any())).thenReturn(List.of(log(1, "2026-06-15T10:00:01Z")));
        AuditLogQuery q = new AuditLogQuery();
        q.setLimit(20);
        CursorResult<AuditLogResponse> r = service.query(q);
        assertThat(r.getRecords()).hasSize(1);
        assertThat(r.getHasNext()).isFalse();
        assertThat(r.getNextCursor()).isNull();
    }

    @Test
    void 响应保留detail为Map() {
        AuditLog l = log(1, "2026-06-15T10:00:01Z");
        l.setDetail(Map.of("phone", "13800000005"));
        when(auditLogMapper.selectList(any())).thenReturn(List.of(l));
        AuditLogQuery q = new AuditLogQuery();
        q.setLimit(20);
        CursorResult<AuditLogResponse> r = service.query(q);
        assertThat(r.getRecords().get(0).getDetail()).containsEntry("phone", "13800000005");
    }

    private AuditLog log(long id, String iso) {
        AuditLog l = new AuditLog();
        l.setId(id);
        l.setActorType("system");
        l.setModuleName("M01");
        l.setOperationType("reset_password");
        l.setOperatedAt(OffsetDateTime.parse(iso));
        return l;
    }
}
