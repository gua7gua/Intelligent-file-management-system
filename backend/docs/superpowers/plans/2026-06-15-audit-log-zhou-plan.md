# 日志审计查询/导出 实施计划（feat/audit-log-zhou）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 §23.1 / §23.2 + 导出：审计日志、档案访问日志的游标分页查询（按 §23 契约 cursor/limit/nextCursor/hasNext）+ xlsx 导出。只读、不可删改。

**Architecture:** 新建通用游标组件 `CursorPage`/`CursorResult`/`CursorCodec`（cursor 编码 `(时间戳,id)` 复合键，按时间 DESC+id DESC 稳定排序，查 `limit+1` 判 hasNext）。两个查询 Service 各自拼 QueryWrapper（筛选 + 游标条件），复用 `AuditLogMapper`/`ArchiveAccessLogMapper`（均 `BaseMapper`，直接 `selectPage`/`selectList`）。导出复用 pom 已有 `poi-ooxml`，流式写 xlsx。权限在 Controller 守卫。

**Tech Stack:** Spring Boot 3.5.3 / MyBatis-Plus 3.5.7 / Apache POI 5.5.1 / JUnit5 + Mockito。

**分支：** 从最新 `develop` 切 `feat/audit-log-zhou`。

**依据设计：** [2026-06-15-supplement-backend-design.md §3](../specs/2026-06-15-supplement-backend-design.md)

---

## 文件结构

| 文件 | 责任 |
|------|------|
| `common/CursorPage.java` | 游标分页请求：limit（1-100，默认 20）+ cursor（可空） |
| `common/CursorResult.java` | 游标分页响应：records / nextCursor / hasNext |
| `common/CursorCodec.java` | cursor 编解码：`encode(OffsetDateTime ts, Long id)` ↔ `decode` |
| `dto/request/AuditLogQuery.java` | §23.1 筛选 + CursorPage |
| `dto/request/ArchiveAccessLogQuery.java` | §23.2 筛选 + CursorPage |
| `dto/response/AuditLogResponse.java` | 审计日志响应（detail 保留 Map） |
| `dto/response/ArchiveAccessLogResponse.java` | 访问日志响应 |
| `service/AuditLogQueryService.java` | 审计日志查询 + 导出数据源 |
| `service/ArchiveAccessLogQueryService.java` | 访问日志查询 + 导出数据源 |
| `controller/AuditLogController.java` | `/api/admin/audit-logs` GET + `/export` |
| `controller/ArchiveAccessLogController.java` | `/api/admin/archive-access-logs` GET + `/export` |
| 测试：`CursorCodecTest`、`AuditLogQueryServiceTest`、`ArchiveAccessLogQueryServiceTest` |

**关键约束：**
- 只读：Controller 仅 GET，Service 无写方法；
- 游标按 `(时间戳 DESC, id DESC)` 稳定排序；
- 审计日志角色 sys_admin；访问日志 back_archivist 或 sys_admin；
- 导出与查询共用筛选，上限 10000 行；
- `detail`（JSONB→Map）导出时序列化为 JSON 字符串列。

## Task 1: 游标分页组件

**Files:**
- Create: `src/main/java/com/archive/common/CursorPage.java`
- Create: `src/main/java/com/archive/common/CursorResult.java`
- Create: `src/main/java/com/archive/common/CursorCodec.java`
- Test: `src/test/java/com/archive/common/CursorCodecTest.java`

- [ ] **Step 1: 写失败测试 `CursorCodecTest`**

```java
package com.archive.common;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CursorCodecTest {

    @Test
    void 编解码往返一致() {
        OffsetDateTime ts = OffsetDateTime.of(2026, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        String cursor = CursorCodec.encode(ts, 123L);
        CursorCodec.Decoded d = CursorCodec.decode(cursor);
        assertThat(d.id()).isEqualTo(123L);
        assertThat(d.timestamp()).isEqualTo(ts);
    }

    @Test
    void decode非法cursor返回null() {
        assertThat(CursorCodec.decode("!!!不是base64分隔!!!")).isNull();
        assertThat(CursorCodec.decode(null)).isNull();
        assertThat(CursorCodec.decode("")).isNull();
    }
}
```

- [ ] **Step 2: 实现 `CursorCodec`**

```java
package com.archive.common;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * 游标编解码：cursor = Base64( "ISO时间戳|id" )，URL 安全。
 * 游标内含 (时间戳, id) 复合键，配合按 (时间戳 DESC, id DESC) 排序实现稳定翻页。
 */
public final class CursorCodec {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorCodec() {}

    public static String encode(OffsetDateTime ts, Long id) {
        String raw = ts.toString() + "|" + id;
        return ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** 解码失败返回 null（非法/空 cursor） */
    public static Decoded decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String raw = new String(DECODER.decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.lastIndexOf('|');
            if (sep < 0) return null;
            OffsetDateTime ts = OffsetDateTime.parse(raw.substring(0, sep));
            Long id = Long.parseLong(raw.substring(sep + 1));
            return new Decoded(ts, id);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return null;
        }
    }

    public record Decoded(OffsetDateTime timestamp, Long id) {}
}
```

- [ ] **Step 3: 实现 `CursorPage`、`CursorResult`**

`CursorPage.java`：
```java
package com.archive.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CursorPage {
    @Min(value = 1, message = "limit 最小为 1")
    @Max(value = 100, message = "limit 最大为 100")
    private Integer limit = 20;

    /** 上一页返回的 nextCursor；首页为空 */
    private String cursor;
}
```

`CursorResult.java`：
```java
package com.archive.common;

import lombok.Data;
import java.util.List;

@Data
public class CursorResult<T> {
    private List<T> records;
    private String nextCursor;
    private Boolean hasNext;

    public CursorResult() {}

    public CursorResult(List<T> records, String nextCursor, boolean hasNext) {
        this.records = records;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `mvn -q -Dtest=CursorCodecTest test`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/common/CursorPage.java src/main/java/com/archive/common/CursorResult.java src/main/java/com/archive/common/CursorCodec.java src/test/java/com/archive/common/CursorCodecTest.java
git commit -m "feat(audit-log): 新增通用游标分页组件"
```

## Task 2: DTO

**Files:**
- Create: `src/main/java/com/archive/dto/request/AuditLogQuery.java`
- Create: `src/main/java/com/archive/dto/request/ArchiveAccessLogQuery.java`
- Create: `src/main/java/com/archive/dto/response/AuditLogResponse.java`
- Create: `src/main/java/com/archive/dto/response/ArchiveAccessLogResponse.java`

- [ ] **Step 1: 新建 4 个 DTO**

`AuditLogQuery.java`（§23.1 筛选 + CursorPage）：
```java
package com.archive.dto.request;

import com.archive.common.CursorPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQuery extends CursorPage {
    private Long actorUserId;
    private String actorType;
    private String moduleName;
    private String operationType;
    private String businessType;
    private Long businessId;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
```

`ArchiveAccessLogQuery.java`（§23.2 + CursorPage）：
```java
package com.archive.dto.request;

import com.archive.common.CursorPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveAccessLogQuery extends CursorPage {
    private Long userId;
    private Long archiveId;
    private String accessType;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
```

`AuditLogResponse.java`：
```java
package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class AuditLogResponse {
    private Long id;
    private Long actorUserId;
    private String actorType;
    private String moduleName;
    private String operationType;
    private String businessType;
    private Long businessId;
    private Map<String, Object> detail;
    private String ipAddress;
    private OffsetDateTime operatedAt;
}
```

`ArchiveAccessLogResponse.java`：
```java
package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ArchiveAccessLogResponse {
    private Long id;
    private Long userId;
    private String userType;
    private Long archiveId;
    private Long archiveFileId;
    private String accessType;
    private String ipAddress;
    private OffsetDateTime accessedAt;
}
```

- [ ] **Step 2: 编译**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/archive/dto/request/AuditLogQuery.java src/main/java/com/archive/dto/request/ArchiveAccessLogQuery.java src/main/java/com/archive/dto/response/AuditLogResponse.java src/main/java/com/archive/dto/response/ArchiveAccessLogResponse.java
git commit -m "feat(audit-log): 新增审计与访问日志查询响应DTO"
```

## Task 3: AuditLogQueryService（TDD）

**Files:**
- Create: `src/main/java/com/archive/service/AuditLogQueryService.java`
- Test: `src/test/java/com/archive/service/AuditLogQueryServiceTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.AuditLogQuery;
import com.archive.dto.response.AuditLogResponse;
import com.archive.entity.AuditLog;
import com.archive.mapper.AuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        service = new AuditLogQueryService(auditLogMapper);
    }

    @Test
    void 查询_满页时hasNext为true并返回nextCursor() {
        // limit=2，返回 3 条 → hasNext=true，records 取前 2 条，nextCursor 由第 2 条构造
        when(auditLogMapper.selectList(any())).thenReturn(List.of(
                log(3, "2026-06-15T10:00:03Z"),
                log(2, "2026-06-15T10:00:02Z"),
                log(1, "2026-06-15T10:00:01Z")));

        AuditLogQuery q = new AuditLogQuery();
        q.setLimit(2);
        CursorResult<AuditLogResponse> r = service.query(q);

        assertThat(r.getRecords()).hasSize(2);
        assertThat(r.getHasNext()).isTrue();
        // nextCursor 由第 2 条（id=2）构造，可正常解码
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
    void 导出_带筛选返回全部() {
        when(auditLogMapper.selectList(any())).thenReturn(List.of(log(1, "2026-06-15T10:00:01Z")));
        AuditLogQuery q = new AuditLogQuery();
        q.setOperationType("reset_password");
        List<AuditLogResponse> rows = service.listForExport(q);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getOperationType()).isEqualTo("reset_password");
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
```

- [ ] **Step 2: 实现 `AuditLogQueryService`**

```java
package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.AuditLogQuery;
import com.archive.dto.response.AuditLogResponse;
import com.archive.entity.AuditLog;
import com.archive.mapper.AuditLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private static final int EXPORT_MAX = 10000;

    private final AuditLogMapper auditLogMapper;

    /** 23.1 查询审计日志（游标分页） */
    public CursorResult<AuditLogResponse> query(AuditLogQuery q) {
        int limit = q.getLimit() != null ? q.getLimit() : 20;
        QueryWrapper<AuditLog> qw = baseFilters(q);
        applyCursor(qw, q.getCursor(), "operated_at");
        qw.orderByDesc("operated_at").orderByDesc("id").last("LIMIT " + (limit + 1));

        List<AuditLog> rows = auditLogMapper.selectList(qw);
        boolean hasNext = rows.size() > limit;
        List<AuditLog> page = hasNext ? rows.subList(0, limit) : rows;

        List<AuditLogResponse> records = page.stream().map(this::toResponse).toList();
        String nextCursor = null;
        if (hasNext) {
            AuditLog last = page.get(page.size() - 1);
            nextCursor = CursorCodec.encode(last.getOperatedAt(), last.getId());
        }
        return new CursorResult<>(records, nextCursor, hasNext);
    }

    /** 导出数据源（同筛选，无游标，上限 10000） */
    public List<AuditLogResponse> listForExport(AuditLogQuery q) {
        QueryWrapper<AuditLog> qw = baseFilters(q);
        qw.orderByDesc("operated_at").orderByDesc("id").last("LIMIT " + EXPORT_MAX);
        return auditLogMapper.selectList(qw).stream().map(this::toResponse).toList();
    }

    private QueryWrapper<AuditLog> baseFilters(AuditLogQuery q) {
        QueryWrapper<AuditLog> qw = new QueryWrapper<>();
        if (q.getActorUserId() != null) qw.eq("actor_user_id", q.getActorUserId());
        if (q.getActorType() != null && !q.getActorType().isBlank()) qw.eq("actor_type", q.getActorType());
        if (q.getModuleName() != null && !q.getModuleName().isBlank()) qw.eq("module_name", q.getModuleName());
        if (q.getOperationType() != null && !q.getOperationType().isBlank()) qw.eq("operation_type", q.getOperationType());
        if (q.getBusinessType() != null && !q.getBusinessType().isBlank()) qw.eq("business_type", q.getBusinessType());
        if (q.getBusinessId() != null) qw.eq("business_id", q.getBusinessId());
        if (q.getStartedAt() != null) qw.ge("operated_at", q.getStartedAt());
        if (q.getEndedAt() != null) qw.le("operated_at", q.getEndedAt());
        return qw;
    }

    private void applyCursor(QueryWrapper<AuditLog> qw, String cursor, String tsColumn) {
        CursorCodec.Decoded c = CursorCodec.decode(cursor);
        if (c == null) return;
        qw.and(w -> w.lt(tsColumn, c.timestamp())
                .or(o -> o.eq(tsColumn, c.timestamp()).lt("id", c.id())));
    }

    private AuditLogResponse toResponse(AuditLog l) {
        AuditLogResponse vo = new AuditLogResponse();
        vo.setId(l.getId());
        vo.setActorUserId(l.getActorUserId());
        vo.setActorType(l.getActorType());
        vo.setModuleName(l.getModuleName());
        vo.setOperationType(l.getOperationType());
        vo.setBusinessType(l.getBusinessType());
        vo.setBusinessId(l.getBusinessId());
        vo.setDetail(l.getDetail());
        vo.setIpAddress(l.getIpAddress());
        vo.setOperatedAt(l.getOperatedAt());
        return vo;
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `mvn -q -Dtest=AuditLogQueryServiceTest test`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/archive/service/AuditLogQueryService.java src/test/java/com/archive/service/AuditLogQueryServiceTest.java
git commit -m "feat(audit-log): 实现审计日志游标分页查询与导出"
```

## Task 4: ArchiveAccessLogQueryService（TDD）

**Files:**
- Create: `src/main/java/com/archive/service/ArchiveAccessLogQueryService.java`
- Test: `src/test/java/com/archive/service/ArchiveAccessLogQueryServiceTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.ArchiveAccessLogQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.entity.ArchiveAccessLog;
import com.archive.mapper.ArchiveAccessLogMapper;
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
        service = new ArchiveAccessLogQueryService(mapper);
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

    @Test
    void 导出_按archiveId筛选() {
        when(mapper.selectList(any())).thenReturn(List.of(access(1, "2026-06-15T11:00:01Z")));
        ArchiveAccessLogQuery q = new ArchiveAccessLogQuery();
        q.setArchiveId(100L);
        List<ArchiveAccessLogResponse> rows = service.listForExport(q);
        assertThat(rows).hasSize(1);
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
```

- [ ] **Step 2: 实现 `ArchiveAccessLogQueryService`**

```java
package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.ArchiveAccessLogQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.entity.ArchiveAccessLog;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveAccessLogQueryService {

    private static final int EXPORT_MAX = 10000;

    private final ArchiveAccessLogMapper archiveAccessLogMapper;

    /** 23.2 查询档案访问日志（游标分页） */
    public CursorResult<ArchiveAccessLogResponse> query(ArchiveAccessLogQuery q) {
        int limit = q.getLimit() != null ? q.getLimit() : 20;
        QueryWrapper<ArchiveAccessLog> qw = baseFilters(q);
        applyCursor(qw, q.getCursor(), "accessed_at");
        qw.orderByDesc("accessed_at").orderByDesc("id").last("LIMIT " + (limit + 1));

        List<ArchiveAccessLog> rows = archiveAccessLogMapper.selectList(qw);
        boolean hasNext = rows.size() > limit;
        List<ArchiveAccessLog> page = hasNext ? rows.subList(0, limit) : rows;

        List<ArchiveAccessLogResponse> records = page.stream().map(this::toResponse).toList();
        String nextCursor = hasNext
                ? CursorCodec.encode(page.get(page.size() - 1).getAccessedAt(),
                        page.get(page.size() - 1).getId())
                : null;
        return new CursorResult<>(records, nextCursor, hasNext);
    }

    public List<ArchiveAccessLogResponse> listForExport(ArchiveAccessLogQuery q) {
        QueryWrapper<ArchiveAccessLog> qw = baseFilters(q);
        qw.orderByDesc("accessed_at").orderByDesc("id").last("LIMIT " + EXPORT_MAX);
        return archiveAccessLogMapper.selectList(qw).stream().map(this::toResponse).toList();
    }

    private QueryWrapper<ArchiveAccessLog> baseFilters(ArchiveAccessLogQuery q) {
        QueryWrapper<ArchiveAccessLog> qw = new QueryWrapper<>();
        if (q.getUserId() != null) qw.eq("user_id", q.getUserId());
        if (q.getArchiveId() != null) qw.eq("archive_id", q.getArchiveId());
        if (q.getAccessType() != null && !q.getAccessType().isBlank()) qw.eq("access_type", q.getAccessType());
        if (q.getStartedAt() != null) qw.ge("accessed_at", q.getStartedAt());
        if (q.getEndedAt() != null) qw.le("accessed_at", q.getEndedAt());
        return qw;
    }

    private void applyCursor(QueryWrapper<ArchiveAccessLog> qw, String cursor, String tsColumn) {
        CursorCodec.Decoded c = CursorCodec.decode(cursor);
        if (c == null) return;
        qw.and(w -> w.lt(tsColumn, c.timestamp())
                .or(o -> o.eq(tsColumn, c.timestamp()).lt("id", c.id())));
    }

    private ArchiveAccessLogResponse toResponse(ArchiveAccessLog l) {
        ArchiveAccessLogResponse vo = new ArchiveAccessLogResponse();
        vo.setId(l.getId());
        vo.setUserId(l.getUserId());
        vo.setUserType(l.getUserType());
        vo.setArchiveId(l.getArchiveId());
        vo.setArchiveFileId(l.getArchiveFileId());
        vo.setAccessType(l.getAccessType());
        vo.setIpAddress(l.getIpAddress());
        vo.setAccessedAt(l.getAccessedAt());
        return vo;
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `mvn -q -Dtest=ArchiveAccessLogQueryServiceTest test`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/archive/service/ArchiveAccessLogQueryService.java src/test/java/com/archive/service/ArchiveAccessLogQueryServiceTest.java
git commit -m "feat(audit-log): 实现档案访问日志游标分页查询与导出"
```

## Task 5: Excel 导出工具 + Controller

**Files:**
- Create: `src/main/java/com/archive/util/LogExcelExporter.java`
- Create: `src/main/java/com/archive/controller/AuditLogController.java`
- Create: `src/main/java/com/archive/controller/ArchiveAccessLogController.java`

- [ ] **Step 1: 实现 `LogExcelExporter`（共享 SXSSF 流式导出）**

```java
package com.archive.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 日志导出工具：SXSSF 流式写 xlsx，避免大数据量内存膨胀。
 */
public final class LogExcelExporter {

    private LogExcelExporter() {}

    public static void write(HttpServletResponse response, String filename,
                             String[] headers, List<String[]> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try (Workbook wb = new SXSSFWorkbook(100); OutputStream out = response.getOutputStream()) {
            Sheet sheet = wb.createSheet("logs");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (String[] row : rows) {
                Row r = sheet.createRow(rowIdx++);
                for (int i = 0; i < row.length; i++) {
                    r.createCell(i).setCellValue(row[i] != null ? row[i] : "");
                }
            }
            wb.write(out);
        }
    }
}
```

- [ ] **Step 2: 实现 `AuditLogController`（查询 + 导出）**

```java
package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.CursorResult;
import com.archive.common.ErrorCode;
import com.archive.common.R;
import com.archive.dto.request.AuditLogQuery;
import com.archive.dto.response.AuditLogResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.service.AuditLogQueryService;
import com.archive.util.LogExcelExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "操作审计日志查询/导出（系统管理员，只读）")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "查询审计日志（游标分页）")
    public R<CursorResult<AuditLogResponse>> list(@Valid AuditLogQuery query) {
        requireSysAdmin();
        return R.ok(auditLogQueryService.query(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出审计日志(xlsx)")
    public void export(@Valid AuditLogQuery query, HttpServletResponse response) throws IOException {
        requireSysAdmin();
        List<AuditLogResponse> rows = auditLogQueryService.listForExport(query);
        String[] headers = {"ID", "操作时间", "操作人类型", "操作人ID", "模块", "操作类型", "业务类型", "业务ID", "详情", "IP"};
        List<String[]> data = rows.stream().map(l -> new String[]{
                String.valueOf(l.getId()),
                l.getOperatedAt() != null ? l.getOperatedAt().toString() : "",
                l.getActorType() != null ? l.getActorType() : "",
                l.getActorUserId() != null ? String.valueOf(l.getActorUserId()) : "",
                l.getModuleName() != null ? l.getModuleName() : "",
                l.getOperationType() != null ? l.getOperationType() : "",
                l.getBusinessType() != null ? l.getBusinessType() : "",
                l.getBusinessId() != null ? String.valueOf(l.getBusinessId()) : "",
                detailToJson(l),
                l.getIpAddress() != null ? l.getIpAddress() : ""
        }).toList();
        LogExcelExporter.write(response, "audit-logs.xlsx", headers, data);
    }

    private String detailToJson(AuditLogResponse l) {
        try {
            return l.getDetail() != null ? objectMapper.writeValueAsString(l.getDetail()) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void requireSysAdmin() {
        if (!AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅系统管理员可查询审计日志");
        }
    }
}
```

- [ ] **Step 3: 实现 `ArchiveAccessLogController`**

```java
package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.CursorResult;
import com.archive.common.ErrorCode;
import com.archive.common.R;
import com.archive.dto.request.ArchiveAccessLogQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.service.ArchiveAccessLogQueryService;
import com.archive.util.LogExcelExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/archive-access-logs")
@RequiredArgsConstructor
@Tag(name = "档案访问日志", description = "档案访问日志查询/导出（后台档案管理员、系统管理员，只读）")
public class ArchiveAccessLogController {

    private final ArchiveAccessLogQueryService archiveAccessLogQueryService;

    @GetMapping
    @Operation(summary = "查询档案访问日志（游标分页）")
    public R<CursorResult<ArchiveAccessLogResponse>> list(@Valid ArchiveAccessLogQuery query) {
        requireBackOrAdmin();
        return R.ok(archiveAccessLogQueryService.query(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出档案访问日志(xlsx)")
    public void export(@Valid ArchiveAccessLogQuery query, HttpServletResponse response) throws IOException {
        requireBackOrAdmin();
        List<ArchiveAccessLogResponse> rows = archiveAccessLogQueryService.listForExport(query);
        String[] headers = {"ID", "访问时间", "用户类型", "用户ID", "档案ID", "档案文件ID", "访问类型", "IP"};
        List<String[]> data = rows.stream().map(l -> new String[]{
                String.valueOf(l.getId()),
                l.getAccessedAt() != null ? l.getAccessedAt().toString() : "",
                l.getUserType() != null ? l.getUserType() : "",
                l.getUserId() != null ? String.valueOf(l.getUserId()) : "",
                l.getArchiveId() != null ? String.valueOf(l.getArchiveId()) : "",
                l.getArchiveFileId() != null ? String.valueOf(l.getArchiveFileId()) : "",
                l.getAccessType() != null ? l.getAccessType() : "",
                l.getIpAddress() != null ? l.getIpAddress() : ""
        }).toList();
        LogExcelExporter.write(response, "archive-access-logs.xlsx", headers, data);
    }

    private void requireBackOrAdmin() {
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.sys_admin))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }
}
```

- [ ] **Step 4: 编译 + 全量单测**

Run: `mvn -q test`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/util/LogExcelExporter.java src/main/java/com/archive/controller/AuditLogController.java src/main/java/com/archive/controller/ArchiveAccessLogController.java
git commit -m "feat(audit-log): 新增审计与访问日志查询导出接口"
```

## Task 6: 运行时验证

- [ ] **Step 1: 启动后端 + 制造日志数据**

Run: `mvn spring-boot:run`
> audit_logs 已有各业务写入的记录（M01/M03/M06/M11/M14 等）；若空，触发一次登录或用户操作产生记录。archive_access_logs 可暂无数据（空表导出也需验证不报错）。

- [ ] **Step 2: sys_admin 登录拿 token `$T`**

- [ ] **Step 3: 游标分页首页**

```bash
curl -s "http://localhost:8080/api/admin/audit-logs?limit=5" -H "Authorization: Bearer $T" | jq '{hasNext, nextCursor, n: (.data.records|length)}'
```
Expected: records 数 ≤ 5；有更多数据时 `hasNext=true` + 非空 `nextCursor`。

- [ ] **Step 4: 翻页连续无重复**

```bash
NEXT=<上一步 nextCursor>
curl -s "http://localhost:8080/api/admin/audit-logs?limit=5&cursor=$NEXT" -H "Authorization: Bearer $T" | jq '.data.records[].id'
```
Expected: 第二页 id 全部小于第一页最小 id（按时间倒序，无重复）。

- [ ] **Step 5: 筛选**

```bash
curl -s "http://localhost:8080/api/admin/audit-logs?operationType=reset_password" -H "Authorization: Bearer $T" | jq '.data.records[].operationType'
```
Expected: 全部为 reset_password（若模块二已验证，应有记录）。

- [ ] **Step 6: 导出 xlsx**

```bash
curl -s -o /tmp/audit.xlsx -w "%{http_code} %{size_download}" "http://localhost:8080/api/admin/audit-logs/export" -H "Authorization: Bearer $T"
file /tmp/audit.xlsx
```
Expected: 200 + 非零字节数；`file` 识别为 Microsoft Excel 2007+。

- [ ] **Step 7: 访问日志查询 + 导出**

```bash
curl -s "http://localhost:8080/api/admin/archive-access-logs?limit=5" -H "Authorization: Bearer $T" | jq .
curl -s -o /tmp/access.xlsx -w "%{http_code}" "http://localhost:8080/api/admin/archive-access-logs/export" -H "Authorization: Bearer $T"
```
Expected: 查询返回（可空 records）；导出 200。

- [ ] **Step 8: 权限**

非 sys_admin（如 back_archivist）访问 `GET /api/admin/audit-logs` → 403；公众/未登录 → 401。

- [ ] **Step 9: 只读约束**

Swagger 确认无 POST/PUT/DELETE 端点。

- [ ] **Step 10: 记录验证报告并提交**

写入 `backend/docs/runtime-verification-audit-log-zhou-06-15.md`。
```bash
git add backend/docs/runtime-verification-audit-log-zhou-06-15.md
git commit -m "docs(audit-log): 记录日志查询导出运行时验证结果"
```

> 运行时验证全绿后，模块三视为基本完成。**push / PR 等用户查看后再操作。**



