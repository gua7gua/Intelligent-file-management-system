package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.ArchiveAccessLogQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.entity.ArchiveAccessLog;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArchiveAccessLogQueryServiceTest {

    private ArchiveAccessLogQueryService service;
    private ArchiveAccessLogMapper mapper;

    @BeforeEach
    void setup() {
        mapper = mock(ArchiveAccessLogMapper.class);
        service = new ArchiveAccessLogQueryService(mapper, mock(UserMapper.class), mock(ArchiveMapper.class));
    }

    @Test
    void 查询_满页hasNext并带nextCursor() {
        when(mapper.selectList(any())).thenReturn(List.of(
                access(3, "2026-06-15T11:00:03Z"),
                access(2, "2026-06-15T11:00:02Z"),
                access(1, "2026-06-15T11:00:01Z")));
        ArchiveAccessLogQuery q = new ArchiveAccessLogQuery();
        q.setLimit(2);
        CursorResult<ArchiveAccessLogResponse> r = service.query(q);
        assertThat(r.getRecords()).hasSize(2);
        assertThat(r.getHasNext()).isTrue();
        assertThat(CursorCodec.decode(r.getNextCursor()).id()).isEqualTo(2L);
    }

    private ArchiveAccessLog access(long id, String iso) {
        ArchiveAccessLog l = new ArchiveAccessLog();
        l.setId(id);
        l.setUserType("public");
        l.setArchiveId(100L);
        l.setAccessType("view_metadata");
        l.setAccessedAt(OffsetDateTime.parse(iso));
        return l;
    }
}
