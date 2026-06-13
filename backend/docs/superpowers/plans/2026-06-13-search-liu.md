# feat/search-liu 检索与 AI 补全模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 AI 接收清单补全（异步批处理 + 白名单）、内部/公众档案检索（权限过滤）、电子文件预览/下载（下载审计），共 14 个接口。

**Architecture:** 按职责拆 3 个 Service —— `AiSuggestionService`（白名单校验与提示词）、`AiTaskService`（ai_tasks/ai_task_batches 管理 + `@Async` 异步批处理 + 重试）、`SearchService`（公众/内部检索 + 权限过滤 + 访问日志 + AI 检索 + dashboard）。复用已有 `AiClient`、`AuthContext`、`ArchiveMapper`、`MinioService`、`AuditService`，不新增数据库迁移。

**Tech Stack:** Spring Boot 3、MyBatis-Plus 3.5.7、PostgreSQL 17（JSONB + INET）、MinIO、Sa-Token、JUnit 5 + Mockito（spring-boot-starter-test）。

**设计规格：** [backend/docs/superpowers/specs/2026-06-13-search-liu-design.md](../specs/2026-06-13-search-liu-design.md)

---

## 文件结构总览

### 新增 Entity（backend/src/main/java/com/archive/entity/）
| 文件 | 职责 |
|------|------|
| `AiTask.java` | ai_tasks 任务主表，继承 BaseEntity |
| `AiTaskBatch.java` | ai_task_batches 任务批次，继承 BaseEntity，JSONB 字段用 JacksonTypeHandler |
| `ArchiveAccessLog.java` | archive_access_logs 访问日志，**不继承** BaseEntity |

### 新增 Mapper（backend/src/main/java/com/archive/mapper/）
| 文件 | 职责 |
|------|------|
| `AiTaskMapper.java` | ai_tasks CRUD |
| `AiTaskBatchMapper.java` | ai_task_batches CRUD |
| `ArchiveAccessLogMapper.java` | archive_access_logs 插入 + dashboard 最近查阅查询 |

### 新增 Util / Config
| 文件 | 职责 |
|------|------|
| `util/AiTaskNoUtil.java` | `seq_ai_task_no` 生成 `AIT-{6位}` |
| `config/AsyncConfig.java` | `@EnableAsync` + `aiTaskExecutor` 线程池 |

### 新增/改 Service（backend/src/main/java/com/archive/service/）
| 文件 | 状态 | 职责 |
|------|------|------|
| `AiSuggestionService.java` | 新增 | 清单补全提示词、白名单校验、写 ai_suggestion |
| `AiTaskService.java` | 新增 | 任务/批次 CRUD、@Async 异步执行、状态汇总、重试、查询 |
| `SearchService.java` | 新增 | 公众/内部检索、详情、预览/下载、访问日志、AI 检索、dashboard |

### 新增/改 Controller（backend/src/main/java/com/archive/controller/）
| 文件 | 状态 | 路径前缀 |
|------|------|----------|
| `PublicSearchController.java` | 新增 | /api/public |
| `SearchController.java` | 新增 | /api/internal |
| `PendingArchiveController.java` | 改（加 9.3） | /api/admin/pending-archive |
| `AiTaskController.java` | 改（补 9.4/9.5） | /api/admin/ai-tasks |

### 新增 DTO（backend/src/main/java/com/archive/dto/）
| 文件 | 用途 |
|------|------|
| `request/AiQueryRequest.java` | 5.6/11.4 AI 检索输入 |
| `request/ArchiveSearchQuery.java` | 5.2/11.2 检索查询参数（@ModelAttribute） |
| `response/AiTaskStartResponse.java` | 9.3 返回 |
| `response/AiTaskResponse.java` | 9.4 返回 |
| `response/AiQueryResponse.java` | 5.6/11.4 返回 |
| `response/ArchiveSummaryResponse.java` | 5.2/11.2 列表项 |
| `response/ArchiveSearchDetailResponse.java` | 5.3/11.3 详情 |
| `response/InternalDashboardResponse.java` | 11.1 返回 |

### 测试（backend/src/test/java/com/archive/）
| 文件 | 覆盖 |
|------|------|
| `service/AiSuggestionServiceTest.java` | 白名单过滤、category/listItemId/formedDate/tags 校验 |
| `service/AiTaskServiceTest.java` | 批次拆分、状态汇总、重试 |
| `service/SearchServiceTest.java` | 权限过滤条件、AI 检索重试与降级、访问日志 |

---

## Task 1: 基础 Entity 与 Mapper

**Files:**
- Create: `backend/src/main/java/com/archive/entity/AiTask.java`
- Create: `backend/src/main/java/com/archive/entity/AiTaskBatch.java`
- Create: `backend/src/main/java/com/archive/entity/ArchiveAccessLog.java`
- Create: `backend/src/main/java/com/archive/mapper/AiTaskMapper.java`
- Create: `backend/src/main/java/com/archive/mapper/AiTaskBatchMapper.java`
- Create: `backend/src/main/java/com/archive/mapper/ArchiveAccessLogMapper.java`

- [ ] **Step 1: 创建 AiTask Entity**

`backend/src/main/java/com/archive/entity/AiTask.java`：

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * AI 任务主表。记录检索类与批处理类 AI 调用的任务汇总。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_tasks")
public class AiTask extends BaseEntity {

    /** 任务号，AIT-{6位序号} */
    private String taskNo;
    /** 任务类型：intake_completion / internal_search_query / public_search_query / archive_analysis */
    private String taskType;
    /** 业务对象类型：intake_batch / analysis_task / none */
    private String businessType;
    /** 业务对象 ID；检索类可空 */
    private Long businessId;
    /** 任务状态：running / partial_completed / completed / failed */
    private String status;
    /** 批大小 */
    private Integer batchSize;
    /** 总批次数 */
    private Integer totalBatches;
    /** 成功批次数 */
    private Integer successBatches;
    /** 失败批次数 */
    private Integer failedBatches;
    /** 开始时间 */
    private OffsetDateTime startedAt;
    /** 完成时间 */
    private OffsetDateTime completedAt;
    /** 任务级错误摘要 */
    private String errorMessage;
}
```

- [ ] **Step 2: 创建 AiTaskBatch Entity**

`backend/src/main/java/com/archive/entity/AiTaskBatch.java`：

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 任务批次。每个批次独立调用 AI、独立校验、独立记录成败。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_task_batches", autoResultMap = true)
public class AiTaskBatch extends BaseEntity {

    /** 关联 ai_tasks.id */
    private Long taskId;
    /** 任务内批次序号，从 1 递增 */
    private Integer batchNo;
    /** 批次状态：pending / running / success / failed */
    private String status;
    /** 本批处理的清单条目 ID 或档案 ID */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> targetIds;
    /** 本批注入的上下文摘要，不含敏感密钥 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> requestContext;
    /** AI 原始响应，便于排错 */
    private String rawResponse;
    /** 通过校验后的结果 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validatedResult;
    /** 批次错误说明 */
    private String errorMessage;
    /** 尝试次数 */
    private Integer attemptCount;
    /** 批次开始时间 */
    private OffsetDateTime startedAt;
    /** 批次完成时间 */
    private OffsetDateTime completedAt;
}
```

- [ ] **Step 3: 创建 ArchiveAccessLog Entity**

`backend/src/main/java/com/archive/entity/ArchiveAccessLog.java`（不继承 BaseEntity，表无 created_by/updated_by/deleted_at）：

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 档案访问日志。记录公众/内部对档案元数据的查看、文件预览、文件下载。
 */
@Data
@TableName("archive_access_logs")
public class ArchiveAccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户 ID；未登录公众为 null */
    private Long userId;
    /** 用户类型：internal / public / anonymous */
    private String userType;
    /** 档案 ID */
    private Long archiveId;
    /** 档案文件 ID；元数据查看时为 null */
    private Long archiveFileId;
    /** 访问类型：view_metadata / preview / download */
    private String accessType;
    /** IP 地址（写入 PG INET） */
    private String ipAddress;
    /** 访问时间 */
    private OffsetDateTime accessedAt;
    /** 创建时间（数据库 DEFAULT now()） */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private OffsetDateTime createdAt;
    /** 更新时间（数据库 DEFAULT now()） */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
```

- [ ] **Step 4: 创建 3 个 Mapper**

`backend/src/main/java/com/archive/mapper/AiTaskMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.AiTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiTaskMapper extends BaseMapper<AiTask> {
}
```

`backend/src/main/java/com/archive/mapper/AiTaskBatchMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.AiTaskBatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiTaskBatchMapper extends BaseMapper<AiTaskBatch> {
}
```

`backend/src/main/java/com/archive/mapper/ArchiveAccessLogMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.ArchiveAccessLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArchiveAccessLogMapper extends BaseMapper<ArchiveAccessLog> {

    /**
     * 查询某用户最近查阅的档案（每个 archive_id 取最近一次，去重，取前 10）。
     * 返回每条：archiveId、archiveNo、title、accessedAt。
     */
    @Select("""
            SELECT DISTINCT ON (a.id) a.id AS "archiveId", a.archive_no AS "archiveNo",
                   a.title AS "title", l.accessed_at AS "accessedAt"
            FROM archive_access_logs l JOIN archives a ON a.id = l.archive_id
            WHERE l.user_id = #{userId} AND l.access_type = 'view_metadata'
            ORDER BY a.id, l.accessed_at DESC
            LIMIT 10
            """)
    List<Map<String, Object>> findRecentViews(@Param("userId") Long userId);
}
```

- [ ] **Step 5: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS，无编译错误。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/entity/AiTask.java \
  backend/src/main/java/com/archive/entity/AiTaskBatch.java \
  backend/src/main/java/com/archive/entity/ArchiveAccessLog.java \
  backend/src/main/java/com/archive/mapper/AiTaskMapper.java \
  backend/src/main/java/com/archive/mapper/AiTaskBatchMapper.java \
  backend/src/main/java/com/archive/mapper/ArchiveAccessLogMapper.java
git commit -m "feat(search): 添加 AI 任务与访问日志 Entity 和 Mapper"
```

---

## Task 2: AiTaskNoUtil 与 AsyncConfig

**Files:**
- Create: `backend/src/main/java/com/archive/util/AiTaskNoUtil.java`
- Create: `backend/src/main/java/com/archive/config/AsyncConfig.java`
- Test: `backend/src/test/java/com/archive/util/AiTaskNoUtilTest.java`

- [ ] **Step 1: 写失败测试**

`backend/src/test/java/com/archive/util/AiTaskNoUtilTest.java`：

```java
package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiTaskNoUtilTest {

    @Test
    void generate_序列号格式化为AIT前缀6位() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_ai_task_no')"), eq(Long.class)))
                .thenReturn(7L);

        AiTaskNoUtil util = new AiTaskNoUtil(jdbc);

        assertThat(util.generate()).isEqualTo("AIT-000007");
        verify(jdbc, times(1)).queryForObject(eq("SELECT nextval('seq_ai_task_no')"), eq(Long.class));
    }

    @Test
    void generate_序号超过6位仍正确显示() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1234567L);

        assertThat(new AiTaskNoUtil(jdbc).generate()).isEqualTo("AIT-1234567");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=AiTaskNoUtilTest`
Expected: 编译失败（AiTaskNoUtil 不存在）。

- [ ] **Step 3: 实现 AiTaskNoUtil**

`backend/src/main/java/com/archive/util/AiTaskNoUtil.java`：

```java
package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 任务号生成工具。调用 seq_ai_task_no 序列，格式 AIT-{6位序号}。
 * 与 ArchiveNoUtil 模式一致。
 */
@Component
@RequiredArgsConstructor
public class AiTaskNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('seq_ai_task_no')", Long.class);
        return String.format("AIT-%06d", seq);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=AiTaskNoUtilTest`
Expected: Tests run: 2, Failures: 0。

- [ ] **Step 5: 实现 AsyncConfig**

`backend/src/main/java/com/archive/config/AsyncConfig.java`：

```java
package com.archive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置。提供 AI 批处理专用线程池 aiTaskExecutor。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * AI 任务专用线程池：核心 2、最大 4、队列 10。
     * 拒绝策略 CallerRunsPolicy——队列满时由调用线程同步执行，避免任务丢失。
     */
    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("ai-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 6: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS。

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/archive/util/AiTaskNoUtil.java \
  backend/src/main/java/com/archive/config/AsyncConfig.java \
  backend/src/test/java/com/archive/util/AiTaskNoUtilTest.java
git commit -m "feat(search): 添加 AiTaskNoUtil 和 AsyncConfig 异步线程池"
```

---

## Task 3: AiSuggestionService（白名单校验、提示词、写 ai_suggestion）

**Files:**
- Create: `backend/src/main/java/com/archive/service/AiSuggestionService.java`
- Test: `backend/src/test/java/com/archive/service/AiSuggestionServiceTest.java`

**核心方法：**
- `buildSystemPrompt()` — AI 交互架构 4.1 场景一系统提示词（固定）
- `buildUserMessage(List<IntakeItem> items)` — 注入清单条目元数据
- `parseAndFilterItems(JsonNode, Set<Long>, Map<String,Integer>)` — 纯逻辑：过滤受保护字段、校验 category/formedDate/tags/listItemId
- `validateAndPersist(JsonNode, AiTaskBatch)` — 调 parseAndFilterItems + 写 `intake_items.ai_suggestion`，返回合法条目数

- [ ] **Step 1: 写白名单过滤与校验的失败测试**

`backend/src/test/java/com/archive/service/AiSuggestionServiceTest.java`：

```java
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
        when(intakeItemMapper.updateById(any())).thenReturn(1);
        when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        int count = service.validateAndPersist(aiResult, batch);

        assertThat(count).isEqualTo(1);
        assertThat(item.getAiSuggestion()).containsEntry("title", "新标题");
        assertThat(batch.getValidatedResult()).isNotNull();
        verify(intakeItemMapper).updateById(item);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=AiSuggestionServiceTest`
Expected: 编译失败（AiSuggestionService 不存在）。

- [ ] **Step 3: 实现 AiSuggestionService**

`backend/src/main/java/com/archive/service/AiSuggestionService.java`：

```java
package com.archive.service;

import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeItem;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.IntakeItemMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * AI 接收清单补全服务。
 * 组装提示词、白名单校验、把合法字段建议写入 intake_items.ai_suggestion。
 * 不直接写 archives 等业务表，AI 结果必须经管理员确认（9.6）后入库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSuggestionService {

    private final IntakeItemMapper intakeItemMapper;
    private final CategoryMapper categoryMapper;

    /** AI 可写的字段白名单 */
    private static final Set<String> ALLOWED_FIELDS = Set.of("title", "responsible", "formedDate", "category", "tags");
    /** 受保护字段，出现时必须剥离 */
    private static final Set<String> PROTECTED_FIELDS = Set.of(
            "securityLevel", "retentionPeriod", "openStatus", "allowDigitization",
            "archiveNo", "warehouseLocation", "destructionStatus");

    public String buildSystemPrompt() {
        return """
                你是档案管理系统的 AI 助手，帮助档案管理员从清单已有字段中补全正式入库所需的元数据。

                约束：
                - 你只能根据清单字段、文件名、移交单位、移交部门等元数据推测。
                - 你不能读取电子文件正文。
                - 你不能覆盖密级、保管期限、是否公开、是否允许数字化等业务判断字段。
                - 分类必须从系统给定分类中选择（document/technology/accounting/audio_video/personnel）。
                - 标签优先从系统给定标签中选择，必要时可建议新增标签。
                - 每次请求最多处理 50 条清单条目。
                - 输出必须是 JSON，并使用 <JSON> 标签包裹。

                允许补全字段：title、responsible、formedDate、category、tags

                输出格式：
                <JSON>
                {
                  "ruleType": "fieldCompletion",
                  "items": [
                    {
                      "listItemId": 清单条目ID,
                      "fields": {
                        "title": "正式题名",
                        "responsible": "责任者",
                        "formedDate": "yyyy-MM-dd",
                        "category": "分类code或中文名",
                        "tags": ["标签1", "标签2"]
                      }
                    }
                  ]
                }
                </JSON>
                """;
    }

    /** 组装用户消息：注入本批条目的元数据，受保护字段只作上下文参考。 */
    public String buildUserMessage(List<IntakeItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下清单条目补全元数据（分类请使用 code：document/technology/accounting/audio_video/personnel）：\n");
        for (IntakeItem it : items) {
            sb.append("- listItemId: ").append(it.getId()).append('\n');
            if (it.getInputTitle() != null) sb.append("  标题: ").append(it.getInputTitle()).append('\n');
            if (it.getExpectedFilename() != null) sb.append("  文件名: ").append(it.getExpectedFilename()).append('\n');
            if (it.getFormedDate() != null) sb.append("  形成日期: ").append(it.getFormedDate()).append('\n');
            if (it.getCarrierStatus() != null) sb.append("  载体状态: ").append(it.getCarrierStatus()).append('\n');
            if (it.getPageCount() != null) sb.append("  页数: ").append(it.getPageCount()).append('\n');
            if (it.getSecurityLevel() != null) sb.append("  密级(仅参考,不可写): ").append(it.getSecurityLevel()).append('\n');
            if (it.getRetentionPeriod() != null) sb.append("  保管期限(仅参考,不可写): ").append(it.getRetentionPeriod()).append('\n');
        }
        return sb.toString();
    }

    /**
     * 解析 AI 返回 JSON，过滤受保护字段、校验枚举与 ID 归属。
     *
     * @param aiResult    AiClient 提取的 JSON 节点
     * @param targetIds   本批合法的清单条目 ID
     * @param categoryMap 分类 code/中文名 → id 映射（由 loadCategoryMap 提供）
     * @return listItemId → 合法字段 Map；无合法内容则空
     */
    public Map<Long, Map<String, Object>> parseAndFilterItems(JsonNode aiResult, Set<Long> targetIds,
                                                              Map<String, Integer> categoryMap) {
        Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
        if (aiResult == null) return out;
        JsonNode ruleType = aiResult.get("ruleType");
        if (ruleType == null || !"fieldCompletion".equals(ruleType.asText())) return out;
        JsonNode items = aiResult.get("items");
        if (items == null || !items.isArray()) return out;

        for (JsonNode item : items) {
            JsonNode idNode = item.get("listItemId");
            if (idNode == null || !idNode.canConvertToLong()) continue;
            long itemId = idNode.asLong();
            if (!targetIds.contains(itemId)) continue;
            JsonNode fields = item.get("fields");
            if (fields == null || !fields.isObject()) continue;

            Map<String, Object> clean = new LinkedHashMap<>();
            Iterator<String> names = fields.fieldNames();
            while (names.hasNext()) {
                String fname = names.next();
                if (PROTECTED_FIELDS.contains(fname)) continue;   // 剥离受保护字段
                if (!ALLOWED_FIELDS.contains(fname)) continue;   // 非白名单丢弃
                JsonNode fval = fields.get(fname);
                switch (fname) {
                    case "formedDate" -> {
                        if (fval.isTextual() && isValidDate(fval.asText())) clean.put("formedDate", fval.asText());
                    }
                    case "category" -> {
                        if (fval.isTextual()) {
                            Integer catId = categoryMap.get(fval.asText());
                            if (catId != null) clean.put("categoryId", catId);
                        }
                    }
                    case "tags" -> {
                        if (fval.isArray()) {
                            List<String> tags = new ArrayList<>();
                            fval.forEach(t -> { if (t.isTextual()) tags.add(t.asText()); });
                            if (!tags.isEmpty()) clean.put("tags", tags);
                        }
                    }
                    default -> { // title、responsible
                        if (fval.isTextual() && !fval.asText().isBlank()) clean.put(fname, fval.asText());
                    }
                }
            }
            if (!clean.isEmpty()) {
                out.merge(itemId, clean, (a, b) -> { a.putAll(b); return a; });
            }
        }
        return out;
    }

    /**
     * 校验 AI 结果并写入 intake_items.ai_suggestion，更新 batch.validatedResult。
     * 返回通过校验并落库的条目数。
     */
    public int validateAndPersist(JsonNode aiResult, AiTaskBatch batch) {
        Map<String, Integer> categoryMap = loadCategoryMap();
        Map<Long, Map<String, Object>> filtered = parseAndFilterItems(
                aiResult, new HashSet<>(batch.getTargetIds() != null ? batch.getTargetIds() : List.of()), categoryMap);

        // 组装 validatedResult（通过校验的 JSON）
        List<Map<String, Object>> itemList = new ArrayList<>();
        filtered.forEach((itemId, fields) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("listItemId", itemId);
            entry.put("fields", fields);
            itemList.add(entry);
        });
        Map<String, Object> validated = new LinkedHashMap<>();
        validated.put("ruleType", "fieldCompletion");
        validated.put("items", itemList);
        batch.setValidatedResult(validated);

        // 写入 ai_suggestion，只覆盖允许字段，不清空管理员已确认字段
        for (Map.Entry<Long, Map<String, Object>> e : filtered.entrySet()) {
            Long itemId = e.getKey();
            IntakeItem item = intakeItemMapper.selectById(itemId);
            if (item == null) continue;
            Map<String, Object> suggestion = item.getAiSuggestion() != null
                    ? new LinkedHashMap<>(item.getAiSuggestion()) : new LinkedHashMap<>();
            suggestion.putAll(e.getValue());
            item.setAiSuggestion(suggestion);
            intakeItemMapper.updateById(item);
        }
        return filtered.size();
    }

    /** 查询固定五类，返回 code 和 category_name → id 的合并映射。 */
    private Map<String, Integer> loadCategoryMap() {
        Map<String, Integer> map = new HashMap<>();
        categoryMapper.selectList(null).forEach(c -> {
            if (c.getId() != null) {
                if (c.getCategoryCode() != null) map.put(c.getCategoryCode(), c.getId());
                if (c.getCategoryName() != null) map.put(c.getCategoryName(), c.getId());
            }
        });
        return map;
    }

    private boolean isValidDate(String s) {
        try {
            LocalDate.parse(s); // ISO yyyy-MM-dd
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=AiSuggestionServiceTest`
Expected: Tests run: 10, Failures: 0。

> 若 `Category` entity 的字段名不是 `categoryCode`/`categoryName`/`id`，按 `backend/src/main/java/com/archive/entity/Category.java` 实际字段调整 `loadCategoryMap()`。实施时先读该 entity 确认。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/service/AiSuggestionService.java \
  backend/src/test/java/com/archive/service/AiSuggestionServiceTest.java
git commit -m "feat(search): 添加 AiSuggestionService 白名单校验与清单补全"
```

---

## Task 4: AiTaskService 与 AiTaskAsyncRunner

异步执行拆为独立 bean `AiTaskAsyncRunner`（持有 `@Async` 方法），避免同类 self-invocation 导致 `@Async` 失效。`AiTaskService` 负责任务 CRUD/查询/拆批/触发，不持有 `@Async` 方法；`AiTaskAsyncRunner` 不反向依赖 `AiTaskService`，无循环依赖。

**Files:**
- Create: `backend/src/main/java/com/archive/dto/response/AiTaskStartResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/AiTaskResponse.java`
- Create: `backend/src/main/java/com/archive/service/AiTaskService.java`
- Create: `backend/src/main/java/com/archive/service/AiTaskAsyncRunner.java`
- Test: `backend/src/test/java/com/archive/service/AiTaskServiceTest.java`

- [ ] **Step 1: 创建两个响应 DTO**

`backend/src/main/java/com/archive/dto/response/AiTaskStartResponse.java`：

```java
package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiTaskStartResponse {
    private Long aiTaskId;
    private String taskNo;
    private String status;
    private Integer batchSize;
    private Integer totalBatches;
}
```

`backend/src/main/java/com/archive/dto/response/AiTaskResponse.java`：

```java
package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class AiTaskResponse {
    private Long aiTaskId;
    private String taskNo;
    private String status;
    private Integer totalBatches;
    private Integer successBatches;
    private Integer failedBatches;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
```

- [ ] **Step 2: 写失败测试（拆批与状态汇总纯逻辑 + 启动/重试）**

`backend/src/test/java/com/archive/service/AiTaskServiceTest.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.AiTaskStartResponse;
import com.archive.entity.AiTask;
import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeBatch;
import com.archive.entity.IntakeItem;
import com.archive.exception.BusinessException;
import com.archive.mapper.AiTaskBatchMapper;
import com.archive.mapper.AiTaskMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.IntakeItemMapper;
import com.archive.util.AiTaskNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiTaskServiceTest {

    private AiTaskService service;
    private AiTaskMapper aiTaskMapper;
    private AiTaskBatchMapper aiTaskBatchMapper;
    private IntakeItemMapper intakeItemMapper;
    private IntakeBatchMapper intakeBatchMapper;
    private AiClient aiClient;
    private AiTaskNoUtil aiTaskNoUtil;
    private AuditService auditService;
    private AiTaskAsyncRunner asyncRunner;

    /** AiTaskService.summarizeStatus 包可见静态方法的反射访问辅助：这里直接测 splitIntoBatches */
    @BeforeEach
    void setup() {
        aiTaskMapper = mock(AiTaskMapper.class);
        aiTaskBatchMapper = mock(AiTaskBatchMapper.class);
        intakeItemMapper = mock(IntakeItemMapper.class);
        intakeBatchMapper = mock(IntakeBatchMapper.class);
        aiClient = mock(AiClient.class);
        aiTaskNoUtil = mock(AiTaskNoUtil.class);
        auditService = mock(AuditService.class);
        asyncRunner = mock(AiTaskAsyncRunner.class);
        service = new AiTaskService(aiTaskMapper, aiTaskBatchMapper, intakeItemMapper,
                intakeBatchMapper, aiClient, aiTaskNoUtil, auditService, asyncRunner);
    }

    @Test
    void splitIntoBatches_整除拆分() {
        List<Long> ids = LongStream.rangeClosed(1, 100).boxed().toList();
        List<List<Long>> out = AiTaskService.splitIntoBatches(ids, 50);
        assertThat(out).hasSize(2);
        assertThat(out.get(0)).hasSize(50);
        assertThat(out.get(1)).hasSize(50);
    }

    @Test
    void splitIntoBatches_非整除末批取剩余() {
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();
        List<List<Long>> out = AiTaskService.splitIntoBatches(ids, 50);
        assertThat(out).hasSize(3);
        assertThat(out.get(2)).hasSize(1);
    }

    @Test
    void splitIntoBatches_空列表返回空() {
        assertThat(AiTaskService.splitIntoBatches(List.of(), 50)).isEmpty();
    }

    @Test
    void startIntakeCompletion_AI未启用抛外部异常() {
        when(aiClient.isAvailable()).thenReturn(false);
        assertThatThrownBy(() -> service.startIntakeCompletion(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()));
        verifyNoInteractions(asyncRunner);
    }

    @Test
    void startIntakeCompletion_无条目抛冲突() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(intakeBatchMapper.selectById(1L)).thenReturn(new IntakeBatch());
        when(intakeItemMapper.selectList(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.startIntakeCompletion(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void startIntakeCompletion_有运行中任务抛冲突() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(intakeBatchMapper.selectById(1L)).thenReturn(new IntakeBatch());
        List<IntakeItem> items = new ArrayList<>();
        IntakeItem it = new IntakeItem();
        it.setId(10L);
        items.add(it);
        when(intakeItemMapper.selectList(any())).thenReturn(items);
        when(aiTaskMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.startIntakeCompletion(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void startIntakeCompletion_正常创建任务批次并触发异步() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(aiClient.getBatchSize()).thenReturn(50);
        when(aiTaskNoUtil.generate()).thenReturn("AIT-000001");
        when(intakeBatchMapper.selectById(1L)).thenReturn(new IntakeBatch());
        IntakeItem it = new IntakeItem();
        it.setId(10L);
        when(intakeItemMapper.selectList(any())).thenReturn(List.of(it));
        when(aiTaskMapper.selectCount(any())).thenReturn(0L);
        when(aiTaskMapper.insert(any())).thenAnswer(inv -> {
            ((AiTask) inv.getArgument(0)).setId(99L);
            return 1;
        });

        AiTaskStartResponse resp = service.startIntakeCompletion(1L);

        assertThat(resp.getAiTaskId()).isEqualTo(99L);
        assertThat(resp.getStatus()).isEqualTo("running");
        assertThat(resp.getTotalBatches()).isEqualTo(1);
        verify(aiTaskBatchMapper, times(1)).insert(any(AiTaskBatch.class));
        verify(asyncRunner, times(1)).executeIntakeCompletion(99L);
    }

    @Test
    void retryFailed_非终态任务拒绝重试() {
        AiTask task = new AiTask();
        task.setStatus("running");
        when(aiTaskMapper.selectById(1L)).thenReturn(task);
        assertThatThrownBy(() -> service.retryFailed(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void retryFailed_无失败批次拒绝() {
        AiTask task = new AiTask();
        task.setStatus("partial_completed");
        when(aiTaskMapper.selectById(1L)).thenReturn(task);
        when(aiTaskBatchMapper.selectList(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.retryFailed(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void retryFailed_重置失败批次并触发异步() {
        AiTask task = new AiTask();
        task.setId(1L);
        task.setTaskType("intake_completion");
        task.setStatus("partial_completed");
        when(aiTaskMapper.selectById(1L)).thenReturn(task);
        AiTaskBatch failed = new AiTaskBatch();
        failed.setId(5L);
        failed.setStatus("failed");
        when(aiTaskBatchMapper.selectList(any())).thenReturn(List.of(failed));
        when(aiTaskMapper.selectCount(any())).thenReturn(0L);

        service.retryFailed(1L);

        assertThat(failed.getStatus()).isEqualTo("pending");
        assertThat(task.getStatus()).isEqualTo("running");
        verify(asyncRunner).executeIntakeCompletion(1L);
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=AiTaskServiceTest`
Expected: 编译失败（AiTaskService / AiTaskAsyncRunner 不存在）。

- [ ] **Step 4: 实现 AiTaskService**

`backend/src/main/java/com/archive/service/AiTaskService.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.AiTaskResponse;
import com.archive.dto.response.AiTaskStartResponse;
import com.archive.entity.AiTask;
import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeBatch;
import com.archive.entity.IntakeItem;
import com.archive.exception.BusinessException;
import com.archive.mapper.AiTaskBatchMapper;
import com.archive.mapper.AiTaskMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.IntakeItemMapper;
import com.archive.util.AiTaskNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 任务管理服务。
 * 负责任务/批次创建、拆批、查询、失败重试编排；真正的异步逐批执行在 AiTaskAsyncRunner。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskService {

    private final AiTaskMapper aiTaskMapper;
    private final AiTaskBatchMapper aiTaskBatchMapper;
    private final IntakeItemMapper intakeItemMapper;
    private final IntakeBatchMapper intakeBatchMapper;
    private final AiClient aiClient;
    private final AiTaskNoUtil aiTaskNoUtil;
    private final AuditService auditService;
    private final AiTaskAsyncRunner asyncRunner;

    /** 把 id 列表按 batchSize 拆成多批，纯逻辑。 */
    static List<List<Long>> splitIntoBatches(List<Long> ids, int batchSize) {
        List<List<Long>> out = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += batchSize) {
            out.add(new ArrayList<>(ids.subList(i, Math.min(i + batchSize, ids.size()))));
        }
        return out;
    }

    /** 9.3 启动 AI 补全。 */
    public AiTaskStartResponse startIntakeCompletion(Long batchId) {
        if (!aiClient.isAvailable()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 功能未启用");
        }
        IntakeBatch batch = intakeBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单批次不存在");
        }
        List<IntakeItem> items = intakeItemMapper.selectList(new QueryWrapper<IntakeItem>()
                .eq("batch_id", batchId)
                .in("status", "accepted", "pending_archive")
                .isNull("generated_archive_id"));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "批次没有可补全的条目");
        }
        Long running = aiTaskMapper.selectCount(new QueryWrapper<AiTask>()
                .eq("business_type", "intake_batch").eq("business_id", batchId).eq("status", "running"));
        if (running != null && running > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "该批次已有运行中的 AI 任务");
        }

        int batchSize = aiClient.getBatchSize();
        List<List<Long>> batches = splitIntoBatches(
                items.stream().map(IntakeItem::getId).toList(), batchSize);

        AiTask task = new AiTask();
        task.setTaskNo(aiTaskNoUtil.generate());
        task.setTaskType("intake_completion");
        task.setBusinessType("intake_batch");
        task.setBusinessId(batchId);
        task.setStatus("running");
        task.setBatchSize(batchSize);
        task.setTotalBatches(batches.size());
        task.setSuccessBatches(0);
        task.setFailedBatches(0);
        task.setStartedAt(OffsetDateTime.now());
        aiTaskMapper.insert(task);

        for (int i = 0; i < batches.size(); i++) {
            AiTaskBatch b = new AiTaskBatch();
            b.setTaskId(task.getId());
            b.setBatchNo(i + 1);
            b.setStatus("pending");
            b.setTargetIds(batches.get(i));
            b.setAttemptCount(0);
            aiTaskBatchMapper.insert(b);
        }

        auditService.log("M04", "start_ai_completion", "intake_batch", batchId,
                Map.of("aiTaskId", task.getId(), "totalBatches", batches.size()));

        asyncRunner.executeIntakeCompletion(task.getId());

        return new AiTaskStartResponse(task.getId(), task.getTaskNo(), task.getStatus(),
                batchSize, batches.size());
    }

    /** 9.5 重试失败批次。 */
    public AiTaskResponse retryFailed(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 任务不存在");
        }
        if (!"intake_completion".equals(task.getTaskType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非清单补全任务");
        }
        if (!"partial_completed".equals(task.getStatus()) && !"failed".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "当前任务状态不允许重试");
        }
        List<AiTaskBatch> failedBatches = aiTaskBatchMapper.selectList(new QueryWrapper<AiTaskBatch>()
                .eq("task_id", taskId).eq("status", "failed"));
        if (failedBatches.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "没有可重试的失败批次");
        }
        for (AiTaskBatch b : failedBatches) {
            b.setStatus("pending");
            aiTaskBatchMapper.updateById(b);
        }
        task.setStatus("running");
        task.setErrorMessage(null);
        aiTaskMapper.updateById(task);
        auditService.log("M04", "retry_ai_failed", "ai_task", taskId,
                Map.of("failedBatches", failedBatches.size()));
        asyncRunner.executeIntakeCompletion(taskId);
        return getTask(taskId);
    }

    /** 9.4 查询任务。 */
    public AiTaskResponse getTask(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 任务不存在");
        }
        return new AiTaskResponse(task.getId(), task.getTaskNo(), task.getStatus(),
                task.getTotalBatches(), task.getSuccessBatches(), task.getFailedBatches(),
                task.getErrorMessage(), task.getStartedAt(), task.getCompletedAt());
    }
}
```

- [ ] **Step 5: 实现 AiTaskAsyncRunner**

`backend/src/main/java/com/archive/service/AiTaskAsyncRunner.java`：

```java
package com.archive.service;

import com.archive.entity.AiTask;
import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeItem;
import com.archive.mapper.AiTaskBatchMapper;
import com.archive.mapper.AiTaskMapper;
import com.archive.mapper.IntakeItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AI 补全异步执行器。
 * 独立 bean 持有 @Async 方法，避免同类 self-invocation 导致异步失效。
 * 逐批调用 AiClient + AiSuggestionService，独立记录成败，最后汇总任务状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskAsyncRunner {

    private final AiTaskMapper aiTaskMapper;
    private final AiTaskBatchMapper aiTaskBatchMapper;
    private final IntakeItemMapper intakeItemMapper;
    private final AiClient aiClient;
    private final AiSuggestionService aiSuggestionService;

    /** 异步执行任务的所有 pending/failed 批次。 */
    @Async("aiTaskExecutor")
    public void executeIntakeCompletion(Long taskId) {
        try {
            List<AiTaskBatch> batches = aiTaskBatchMapper.selectList(new QueryWrapper<AiTaskBatch>()
                    .eq("task_id", taskId)
                    .in("status", "pending", "failed")
                    .orderByAsc("batch_no"));
            for (AiTaskBatch batch : batches) {
                processBatch(batch);
            }
            summarizeTask(taskId);
        } catch (Exception e) {
            log.error("AI 任务 {} 异步执行异常", taskId, e);
            AiTask t = aiTaskMapper.selectById(taskId);
            if (t != null && "running".equals(t.getStatus())) {
                t.setStatus("failed");
                t.setErrorMessage("任务执行异常: " + e.getMessage());
                t.setCompletedAt(OffsetDateTime.now());
                aiTaskMapper.updateById(t);
            }
        }
    }

    private void processBatch(AiTaskBatch batch) {
        batch.setStatus("running");
        batch.setStartedAt(OffsetDateTime.now());
        batch.setAttemptCount((batch.getAttemptCount() == null ? 0 : batch.getAttemptCount()) + 1);
        aiTaskBatchMapper.updateById(batch);
        try {
            List<IntakeItem> items = intakeItemMapper.selectBatchIds(batch.getTargetIds());
            String systemPrompt = aiSuggestionService.buildSystemPrompt();
            String userMessage = aiSuggestionService.buildUserMessage(items);
            JsonNode aiResult = aiClient.callAndExtractJson(systemPrompt, userMessage);
            batch.setRawResponse(aiResult.toString());
            aiSuggestionService.validateAndPersist(aiResult, batch); // 内部设置 validatedResult
            batch.setStatus("success");
        } catch (Exception e) {
            log.warn("AI 批次 {} 执行失败: {}", batch.getId(), e.getMessage());
            batch.setStatus("failed");
            batch.setErrorMessage(e.getMessage());
        }
        batch.setCompletedAt(OffsetDateTime.now());
        aiTaskBatchMapper.updateById(batch);
    }

    private void summarizeTask(Long taskId) {
        List<AiTaskBatch> all = aiTaskBatchMapper.selectList(
                new QueryWrapper<AiTaskBatch>().eq("task_id", taskId));
        int success = 0, failed = 0;
        for (AiTaskBatch b : all) {
            if ("success".equals(b.getStatus())) success++;
            else if ("failed".equals(b.getStatus())) failed++;
        }
        AiTask t = aiTaskMapper.selectById(taskId);
        if (t == null) return;
        t.setSuccessBatches(success);
        t.setFailedBatches(failed);
        t.setStatus(summarizeStatus(success, failed));
        t.setCompletedAt(OffsetDateTime.now());
        aiTaskMapper.updateById(t);
    }

    /** 纯逻辑：根据成功/失败批次数计算任务状态。 */
    static String summarizeStatus(int success, int failed) {
        if (success > 0 && failed > 0) return "partial_completed";
        if (success > 0) return "completed";
        return "failed";
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=AiTaskServiceTest`
Expected: Tests run: 9, Failures: 0。

- [ ] **Step 7: 编译验证（含 @Async 织入）**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS。

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/archive/dto/response/AiTaskStartResponse.java \
  backend/src/main/java/com/archive/dto/response/AiTaskResponse.java \
  backend/src/main/java/com/archive/service/AiTaskService.java \
  backend/src/main/java/com/archive/service/AiTaskAsyncRunner.java \
  backend/src/test/java/com/archive/service/AiTaskServiceTest.java
git commit -m "feat(search): 添加 AiTaskService 任务管理与异步批处理执行"
```

---

## Task 5: SearchService 检索（公众强制过滤 + 内部权限过滤）

为可测试，把权限条件构建抽成接收 `maxSecurityLevel/dataScope/orgId` 参数的方法；公开方法 `internalSearch` 内部读 `AuthContext` 后传入。`publicSearch`/`internalSearch` 对应 5.2/11.2。

**Files:**
- Create: `backend/src/main/java/com/archive/dto/request/ArchiveSearchQuery.java`
- Create: `backend/src/main/java/com/archive/dto/response/ArchiveSummaryResponse.java`
- Create: `backend/src/main/java/com/archive/service/SearchService.java`
- Test: `backend/src/test/java/com/archive/service/SearchServiceSearchTest.java`

- [ ] **Step 1: 创建检索查询参数 DTO**

`backend/src/main/java/com/archive/dto/request/ArchiveSearchQuery.java`：

```java
package com.archive.dto.request;

import lombok.Data;

/**
 * 5.2/11.2 检索查询参数。用 @ModelAttribute 绑定。
 * 内部版（11.2）额外接受 securityLevel、openStatus 作为用户主动筛选。
 */
@Data
public class ArchiveSearchQuery {

    /** 题名/档号/责任者 模糊检索 */
    private String keyword;
    private String archiveNo;
    private String title;
    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    private String responsibleText;
    /** 逗号分隔的标签 ID */
    private String tagIds;
    private String sourceType;
    private String carrierStatus;
    private Boolean hasElectronicFile;

    /** 内部版额外筛选 */
    private Integer securityLevel;
    private String openStatus;

    private int pageNo = 1;
    private int pageSize = 20;
}
```

- [ ] **Step 2: 创建检索摘要响应 DTO**

`backend/src/main/java/com/archive/dto/response/ArchiveSummaryResponse.java`：

```java
package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArchiveSummaryResponse {
    private Long archiveId;
    private String archiveNo;
    private String title;
    private String responsibleText;
    private Integer formedYear;
    private String categoryName;
    private String carrierStatus;
    private Boolean hasElectronicFile;
}
```

- [ ] **Step 3: 写检索权限过滤的失败测试**

`backend/src/test/java/com/archive/service/SearchServiceSearchTest.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.ArchiveSearchQuery;
import com.archive.dto.response.ArchiveSummaryResponse;
import com.archive.common.PageResult;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SearchServiceSearchTest {

    private SearchService service;
    private ArchiveMapper archiveMapper;
    private ArchiveFileMapper archiveFileMapper;
    private ArchiveAccessLogMapper accessLogMapper;
    private CategoryMapper categoryMapper;
    private TagMapper tagMapper;
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        archiveFileMapper = mock(ArchiveFileMapper.class);
        accessLogMapper = mock(ArchiveAccessLogMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        tagMapper = mock(TagMapper.class);
        jdbcTemplate = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        service = new SearchService(archiveMapper, archiveFileMapper, accessLogMapper,
                categoryMapper, tagMapper, jdbcTemplate, null, null);
    }

    private void mockSelectPageEmpty() {
        when(archiveMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<Archive> p = inv.getArgument(0);
            p.setRecords(List.of());
            p.setTotal(0L);
            return p;
        });
        when(categoryMapper.selectList(any())).thenReturn(List.of());
        when(archiveFileMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void 公众检索强制过滤非密公开正常() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        mockSelectPageEmpty();

        service.publicSearch(new ArchiveSearchQuery());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<QueryWrapper> cap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), cap.capture());
        String sql = cap.getValue().getCustomSqlSegment();
        assertThat(sql).contains("security_level", "open_status", "lifecycle_status");
    }

    @Test
    void 公众检索未开放抛冲突() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        assertThatThrownBy(() -> service.publicSearch(new ArchiveSearchQuery()))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void 内部检索强制生命周期正常并按密级上限过滤() {
        mockSelectPageEmpty();

        service.buildInternalWrapper(new ArchiveSearchQuery(), 2, DataScope.all, null);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<QueryWrapper> cap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), cap.capture());
        String sql = cap.getValue().getCustomSqlSegment();
        assertThat(sql).contains("lifecycle_status", "security_level");
    }

    @Test
    void 内部检索own_org数据范围追加组织过滤() {
        mockSelectPageEmpty();

        service.buildInternalWrapper(new ArchiveSearchQuery(), 2, DataScope.own_org, 100L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<QueryWrapper> cap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), cap.capture());
        String sql = cap.getValue().getCustomSqlSegment();
        assertThat(sql).contains("organization_id");
    }

    @Test
    void 内部检索own_fonds追加全宗子查询() {
        mockSelectPageEmpty();

        service.buildInternalWrapper(new ArchiveSearchQuery(), 2, DataScope.own_fonds, 100L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<QueryWrapper> cap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), cap.capture());
        String sql = cap.getValue().getCustomSqlSegment();
        assertThat(sql).contains("fonds_id");
    }

    @Test
    void tagIds生成archive_tags子查询() {
        mockSelectPageEmpty();
        ArchiveSearchQuery q = new ArchiveSearchQuery();
        q.setTagIds("1,2,3");

        service.buildInternalWrapper(q, 4, DataScope.all, null);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<QueryWrapper> cap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), cap.capture());
        assertThat(cap.getValue().getCustomSqlSegment()).contains("archive_tags");
    }

    @Test
    void hasElectronicFile生成archive_files子查询() {
        mockSelectPageEmpty();
        ArchiveSearchQuery q = new ArchiveSearchQuery();
        q.setHasElectronicFile(true);

        service.buildInternalWrapper(q, 4, DataScope.all, null);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<QueryWrapper> cap = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), cap.capture());
        assertThat(cap.getValue().getCustomSqlSegment()).contains("archive_files");
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
        a.setCarrierStatus("electronic");
        when(archiveMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<Archive> p = inv.getArgument(0);
            p.setRecords(List.of(a));
            p.setTotal(1L);
            return p;
        });
        Category c = new Category();
        c.setId(1);
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
```

- [ ] **Step 4: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=SearchServiceSearchTest`
Expected: 编译失败（SearchService 不存在）。

- [ ] **Step 5: 实现 SearchService（检索部分）**

`backend/src/main/java/com/archive/service/SearchService.java`：

```java
package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.ArchiveSearchQuery;
import com.archive.dto.response.ArchiveSummaryResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveFile;
import com.archive.enums.DataScope;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索与利用服务（M08）。
 * 提供公众/内部检索、详情、预览/下载、访问日志、AI 检索 JSON 生成、dashboard。
 * 本 Task 实现检索部分；详情/预览/下载/AI/dashboard 在后续 Task 增量加入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ArchiveMapper archiveMapper;
    private final ArchiveFileMapper archiveFileMapper;
    private final ArchiveAccessLogMapper accessLogMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MinioService minioService;
    private final AiClient aiClient;

    // ==================== 5.2 公众检索 ====================

    public PageResult<ArchiveSummaryResponse> publicSearch(ArchiveSearchQuery query) {
        if (!isPublicSearchEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "公众检索未开放");
        }
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("security_level", 0)
         .eq("open_status", "open")
         .eq("lifecycle_status", "normal");
        applyCommonConditions(w, query);
        return queryAndMap(w, query.getPageNo(), query.getPageSize());
    }

    // ==================== 11.2 内部检索 ====================

    public PageResult<ArchiveSummaryResponse> internalSearch(ArchiveSearchQuery query) {
        QueryWrapper<Archive> w = buildInternalWrapper(query,
                AuthContext.getMaxSecurityLevel(),
                AuthContext.getDataScope(),
                AuthContext.getOrganizationId());
        return queryAndMap(w, query.getPageNo(), query.getPageSize());
    }

    /** 构建内部检索条件（接收权限参数，便于单元测试）。 */
    QueryWrapper<Archive> buildInternalWrapper(ArchiveSearchQuery query,
                                               int maxSecurityLevel,
                                               DataScope dataScope,
                                               Long organizationId) {
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("lifecycle_status", "normal");
        w.le("security_level", maxSecurityLevel);
        applyDataScope(w, dataScope, organizationId);
        applyCommonConditions(w, query);
        // 内部版用户主动筛选 securityLevel / openStatus
        if (query.getSecurityLevel() != null) {
            w.eq("security_level", query.getSecurityLevel());
        }
        if (query.getOpenStatus() != null && !query.getOpenStatus().isBlank()) {
            w.eq("open_status", query.getOpenStatus());
        }
        return w;
    }

    /** 公众/内部通用查询条件。 */
    private void applyCommonConditions(QueryWrapper<Archive> w, ArchiveSearchQuery q) {
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            String kw = q.getKeyword();
            w.and(x -> x.like("title", kw).or().like("archive_no", kw).or().like("responsible_text", kw));
        }
        if (q.getArchiveNo() != null && !q.getArchiveNo().isBlank()) w.like("archive_no", q.getArchiveNo());
        if (q.getTitle() != null && !q.getTitle().isBlank()) w.like("title", q.getTitle());
        if (q.getCategoryId() != null) w.eq("category_id", q.getCategoryId());
        if (q.getFormedYearStart() != null) w.ge("formed_year", q.getFormedYearStart());
        if (q.getFormedYearEnd() != null) w.le("formed_year", q.getFormedYearEnd());
        if (q.getResponsibleText() != null && !q.getResponsibleText().isBlank())
            w.like("responsible_text", q.getResponsibleText());
        if (q.getTagIds() != null && !q.getTagIds().isBlank()) {
            List<Long> tagIdList = parseLongCsv(q.getTagIds());
            if (!tagIdList.isEmpty()) {
                String idList = tagIdList.stream().map(String::valueOf).collect(Collectors.joining(","));
                w.inSql("id", "SELECT archive_id FROM archive_tags WHERE tag_id IN (" + idList + ")");
            }
        }
        if (q.getSourceType() != null && !q.getSourceType().isBlank()) w.eq("source_type", q.getSourceType());
        if (q.getCarrierStatus() != null && !q.getCarrierStatus().isBlank())
            w.eq("carrier_status", q.getCarrierStatus());
        if (Boolean.TRUE.equals(q.getHasElectronicFile())) {
            w.inSql("id", "SELECT archive_id FROM archive_files WHERE file_status = 'normal'");
        }
    }

    /** 内部数据范围过滤（不按 open_status 硬过滤）。 */
    private void applyDataScope(QueryWrapper<Archive> w, DataScope scope, Long organizationId) {
        if (scope == DataScope.all || organizationId == null) return;
        if (scope == DataScope.own_org) {
            w.eq("organization_id", organizationId);
        } else if (scope == DataScope.own_fonds) {
            w.inSql("fonds_id", "SELECT id FROM fonds WHERE organization_id = " + organizationId);
        }
    }

    private PageResult<ArchiveSummaryResponse> queryAndMap(QueryWrapper<Archive> w, int pageNo, int pageSize) {
        Map<Integer, String> categoryNameById = loadCategoryNames();
        Page<Archive> page = new Page<>(pageNo, pageSize);
        archiveMapper.selectPage(page, w);
        List<ArchiveSummaryResponse> records = page.getRecords().stream()
                .map(a -> toSummary(a, categoryNameById))
                .toList();
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private ArchiveSummaryResponse toSummary(Archive a, Map<Integer, String> categoryNameById) {
        Long fileCount = archiveFileMapper.selectCount(new QueryWrapper<ArchiveFile>()
                .eq("archive_id", a.getId()).eq("file_status", "normal"));
        return new ArchiveSummaryResponse(
                a.getId(), a.getArchiveNo(), a.getTitle(), a.getResponsibleText(),
                a.getFormedYear(), categoryNameById.get(a.getCategoryId()),
                a.getCarrierStatus(), fileCount != null && fileCount > 0);
    }

    private Map<Integer, String> loadCategoryNames() {
        Map<Integer, String> map = new HashMap<>();
        categoryMapper.selectList(null).forEach(c -> {
            if (c.getId() != null && c.getCategoryName() != null) map.put(c.getId(), c.getCategoryName());
        });
        return map;
    }

    private boolean isPublicSearchEnabled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_configs WHERE config_key = 'public_search.enabled' AND config_value = 'true'",
                Integer.class);
        return count != null && count > 0;
    }

    private List<Long> parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> { try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; } })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
```

> SearchService 构造注入了 `MinioService` 和 `AiClient`（后续 Task 用）。本 Task 测试中传入 `null`（检索路径用不到），Task 6/7/8 补充预览/下载/AI 方法后这两者才被调用。`MinioService` 为项目内 `@Service`，能被 Spring 注入。

- [ ] **Step 6: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=SearchServiceSearchTest`
Expected: Tests run: 8, Failures: 0。

> 若 `Archive`/`Category` entity 字段名（如 `formedYear`/`categoryId`/`categoryName`）与实际不符，按 entity 源码调整。若 `getCustomSqlSegment()` 在无 MyBatis-Plus 上下文时返回空，改用 `cap.getValue().getExpression().getNormal().sqlSegment()` 验证。

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/archive/dto/request/ArchiveSearchQuery.java \
  backend/src/main/java/com/archive/dto/response/ArchiveSummaryResponse.java \
  backend/src/main/java/com/archive/service/SearchService.java \
  backend/src/test/java/com/archive/service/SearchServiceSearchTest.java
git commit -m "feat(search): 实现 SearchService 公众与内部检索权限过滤"
```

---

## Task 6: SearchService 详情、预览/下载、访问日志

对应 5.3/5.4/5.5/11.3/11.5/11.6。身份信息（`userId`/`userType`）由 Controller 解析 `AuthContext` 后传入，SearchService 不直接调 `AuthContext`，保证可单元测试。`ip_address` 写 PG `INET` 用自定义 `@Insert` + `::inet` cast。

**Files:**
- Create: `backend/src/main/java/com/archive/dto/response/ArchiveSearchDetailResponse.java`
- Modify: `backend/src/main/java/com/archive/mapper/ArchiveAccessLogMapper.java`（加 `insertAccessLog`）
- Modify: `backend/src/main/java/com/archive/service/SearchService.java`（加详情/预览/下载/logAccess）
- Test: `backend/src/test/java/com/archive/service/SearchServiceAccessTest.java`

- [ ] **Step 1: 创建详情响应 DTO**

`backend/src/main/java/com/archive/dto/response/ArchiveSearchDetailResponse.java`（公众版不填 `securityLevel`/`openStatus`/`retentionPeriod`，配合 `R` 的 `@JsonInclude(NON_NULL)` 自动脱敏）：

```java
package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class ArchiveSearchDetailResponse {
    private Long archiveId;
    private String archiveNo;
    private String title;
    private String responsibleText;
    private Integer formedYear;
    private LocalDate formedDate;
    private String categoryName;
    private String carrierStatus;
    /** 内部版可见，公众版不填 */
    private Integer securityLevel;
    /** 内部版可见，公众版不填 */
    private String openStatus;
    /** 内部版可见，公众版不填 */
    private String retentionPeriod;
    /** 可预览/可下载文件摘要 */
    private List<FileSummary> files;

    @Data
    @AllArgsConstructor
    public static class FileSummary {
        private Long fileId;
        private String originalFilename;
        private String fileExt;
        private Long fileSize;
        private String mimeType;
        private String fileRole;
    }
}
```

- [ ] **Step 2: 给 ArchiveAccessLogMapper 加 insertAccessLog**

在 `backend/src/main/java/com/archive/mapper/ArchiveAccessLogMapper.java` 的接口体内追加方法（保留已有的 `findRecentViews`）：

```java
    /**
     * 写入一条访问日志。ip_address 用 ::inet cast 写入 PG INET。
     */
    @org.apache.ibatis.annotations.Insert("""
            INSERT INTO archive_access_logs (user_id, user_type, archive_id, archive_file_id,
                access_type, ip_address, accessed_at)
            VALUES (#{userId}, #{userType}, #{archiveId}, #{fileId}, #{accessType}, #{ip}::inet, #{at})
            """)
    void insertAccessLog(@org.apache.ibatis.annotations.Param("userId") Long userId,
                         @org.apache.ibatis.annotations.Param("userType") String userType,
                         @org.apache.ibatis.annotations.Param("archiveId") Long archiveId,
                         @org.apache.ibatis.annotations.Param("fileId") Long fileId,
                         @org.apache.ibatis.annotations.Param("accessType") String accessType,
                         @org.apache.ibatis.annotations.Param("ip") String ip,
                         @org.apache.ibatis.annotations.Param("at") java.time.OffsetDateTime at);
```

- [ ] **Step 3: 写详情/预览/下载/访问日志的失败测试**

`backend/src/test/java/com/archive/service/SearchServiceAccessTest.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.ArchiveSearchDetailResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveFile;
import com.archive.enums.DataScope;
import com.archive.enums.FileStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SearchServiceAccessTest {

    private SearchService service;
    private ArchiveMapper archiveMapper;
    private ArchiveFileMapper archiveFileMapper;
    private ArchiveAccessLogMapper accessLogMapper;
    private MinioService minioService;
    private HttpServletRequest req;

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        archiveFileMapper = mock(ArchiveFileMapper.class);
        accessLogMapper = mock(ArchiveAccessLogMapper.class);
        minioService = mock(MinioService.class);
        req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("192.168.1.1");
        service = new SearchService(archiveMapper, archiveFileMapper, accessLogMapper,
                mock(CategoryMapper.class), mock(TagMapper.class),
                mock(org.springframework.jdbc.core.JdbcTemplate.class), minioService, null);
    }

    @Test
    void logAccess_解析参数写入访问日志() {
        OffsetDateTime before = OffsetDateTime.now();
        service.logAccess(7L, 99L, "download", 5L, "public", req);

        ArgumentCaptor<OffsetDateTime> atCap = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(accessLogMapper).insertAccessLog(eq(5L), eq("public"), eq(7L), eq(99L),
                eq("download"), eq("192.168.1.1"), atCap.capture());
        assertThat(atCap.getValue()).isAfterOrEqualTo(before);
    }

    @Test
    void logAccess_匿名用户userId为空() {
        service.logAccess(7L, null, "view_metadata", null, "anonymous", req);
        verify(accessLogMapper).insertAccessLog(isNull(), eq("anonymous"), eq(7L), isNull(),
                eq("view_metadata"), anyString(), any());
    }

    @Test
    void loadVisiblePublicFile_档案不公开抛NOT_FOUND() {
        ArchiveFile file = new ArchiveFile();
        file.setId(1L);
        file.setArchiveId(7L);
        file.setFileStatus(FileStatus.normal);
        when(archiveFileMapper.selectById(1L)).thenReturn(file);
        when(archiveMapper.selectOne(any())).thenReturn(null); // 不满足公开过滤

        assertThatThrownBy(() -> service.loadVisiblePublicFile(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void loadVisiblePublicFile_文件非normal抛冲突() {
        ArchiveFile file = new ArchiveFile();
        file.setId(1L);
        file.setArchiveId(7L);
        file.setFileStatus(FileStatus.failed);
        Archive a = new Archive();
        a.setId(7L);
        when(archiveFileMapper.selectById(1L)).thenReturn(file);
        when(archiveMapper.selectOne(any())).thenReturn(a);

        assertThatThrownBy(() -> service.loadVisiblePublicFile(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void loadVisiblePublicFile_文件不存在抛NOT_FOUND() {
        when(archiveFileMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.loadVisiblePublicFile(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void publicPreview_正常返回预签名URL并写日志() {
        ArchiveFile file = new ArchiveFile();
        file.setId(1L);
        file.setArchiveId(7L);
        file.setFileStatus(FileStatus.normal);
        file.setBucketName("archive-files");
        file.setObjectKey("archive-files/ARC-1/1/a.pdf");
        when(archiveFileMapper.selectById(1L)).thenReturn(file);
        when(archiveMapper.selectOne(any())).thenReturn(new Archive());
        when(minioService.getPresignedUrl("archive-files", "archive-files/ARC-1/1/a.pdf"))
                .thenReturn("https://minio/presigned");

        String url = service.publicPreview(1L, 5L, "public", req);

        assertThat(url).isEqualTo("https://minio/presigned");
        verify(accessLogMapper).insertAccessLog(eq(5L), eq("public"), eq(7L), eq(1L),
                eq("preview"), anyString(), any());
    }

    @Test
    void publicDownload_未登录抛UNAUTHORIZED() {
        assertThatThrownBy(() -> service.publicDownload(1L, null, "anonymous", req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.UNAUTHORIZED.getCode()));
        verifyNoInteractions(minioService);
    }

    @Test
    void loadVisibleInternalFile_档案超密级抛NOT_FOUND() {
        ArchiveFile file = new ArchiveFile();
        file.setId(1L);
        file.setArchiveId(7L);
        file.setFileStatus(FileStatus.normal);
        when(archiveFileMapper.selectById(1L)).thenReturn(file);
        when(archiveMapper.selectOne(any())).thenReturn(null); // 不在权限范围

        assertThatThrownBy(() -> service.loadVisibleInternalFile(1L, 1, DataScope.all, null))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void publicDetail_查到返回脱敏详情且写日志() {
        Archive a = new Archive();
        a.setId(7L);
        a.setArchiveNo("ARC-000007");
        a.setTitle("标题");
        a.setCarrierStatus("electronic");
        a.setSecurityLevel(0);
        when(archiveMapper.selectOne(any())).thenReturn(a);
        when(archiveFileMapper.selectList(any())).thenReturn(List.of());

        ArchiveSearchDetailResponse detail = service.publicDetail(7L, null, "anonymous", req);

        assertThat(detail.getArchiveId()).isEqualTo(7L);
        assertThat(detail.getSecurityLevel()).isNull(); // 公众脱敏
        verify(accessLogMapper).insertAccessLog(isNull(), eq("anonymous"), eq(7L), isNull(),
                eq("view_metadata"), anyString(), any());
    }

    @Test
    void publicDetail_查不到抛NOT_FOUND() {
        when(archiveMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.publicDetail(7L, null, "anonymous", req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.NOT_FOUND.getCode()));
    }
}
```

- [ ] **Step 4: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=SearchServiceAccessTest`
Expected: 编译失败（SearchService 缺方法）。

- [ ] **Step 5: 在 SearchService 中追加详情/预览/下载/logAccess 方法**

在 `backend/src/main/java/com/archive/service/SearchService.java` 顶部 import 区追加：

```java
import com.archive.dto.response.ArchiveSearchDetailResponse;
import com.archive.entity.ArchiveFile;
import com.archive.enums.FileStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
```

在类体内追加方法（与 Task 5 的方法并列）：

```java
    // ==================== 5.3 / 11.3 详情 ====================

    public ArchiveSearchDetailResponse publicDetail(Long archiveId, Long userId, String userType,
                                                    HttpServletRequest req) {
        Archive a = archiveMapper.selectOne(new QueryWrapper<Archive>()
                .eq("id", archiveId)
                .eq("security_level", 0).eq("open_status", "open").eq("lifecycle_status", "normal"));
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在或不公开");
        }
        logAccess(archiveId, null, "view_metadata", userId, userType, req);
        return toDetail(a, false);
    }

    public ArchiveSearchDetailResponse internalDetail(Long archiveId, int maxSecurityLevel,
                                                     DataScope dataScope, Long organizationId,
                                                     Long userId, HttpServletRequest req) {
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("id", archiveId).eq("lifecycle_status", "normal").le("security_level", maxSecurityLevel);
        applyDataScope(w, dataScope, organizationId);
        Archive a = archiveMapper.selectOne(w);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在或无权访问");
        }
        logAccess(archiveId, null, "view_metadata", userId, "internal", req);
        return toDetail(a, true);
    }

    private ArchiveSearchDetailResponse toDetail(Archive a, boolean includeSensitive) {
        List<ArchiveFile> files = archiveFileMapper.selectList(new QueryWrapper<ArchiveFile>()
                .eq("archive_id", a.getId()).eq("file_status", "normal"));
        List<ArchiveSearchDetailResponse.FileSummary> fileSummaries = files.stream()
                .map(f -> new ArchiveSearchDetailResponse.FileSummary(
                        f.getId(), f.getOriginalFilename(), f.getFileExt(),
                        f.getFileSize(), f.getMimeType(),
                        f.getFileRole() == null ? null : f.getFileRole().name()))
                .toList();
        String categoryName = loadCategoryNames().get(a.getCategoryId());
        return new ArchiveSearchDetailResponse(
                a.getId(), a.getArchiveNo(), a.getTitle(), a.getResponsibleText(),
                a.getFormedYear(), a.getFormedDate(), categoryName, a.getCarrierStatus(),
                includeSensitive ? a.getSecurityLevel() : null,
                includeSensitive ? a.getOpenStatus() : null,
                includeSensitive ? a.getRetentionPeriod() : null,
                fileSummaries);
    }

    // ==================== 5.4 / 5.5 / 11.5 / 11.6 预览/下载 ====================

    public String publicPreview(Long fileId, Long userId, String userType, HttpServletRequest req) {
        ArchiveFile file = loadVisiblePublicFile(fileId);
        logAccess(file.getArchiveId(), fileId, "preview", userId, userType, req);
        return minioService.getPresignedUrl(file.getBucketName(), file.getObjectKey());
    }

    public String publicDownload(Long fileId, Long userId, String userType, HttpServletRequest req) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "下载需要登录");
        }
        ArchiveFile file = loadVisiblePublicFile(fileId);
        logAccess(file.getArchiveId(), fileId, "download", userId, userType, req);
        return minioService.getPresignedUrl(file.getBucketName(), file.getObjectKey());
    }

    public String internalPreview(Long fileId, int maxSecurityLevel, DataScope dataScope,
                                  Long organizationId, Long userId, HttpServletRequest req) {
        ArchiveFile file = loadVisibleInternalFile(fileId, maxSecurityLevel, dataScope, organizationId);
        logAccess(file.getArchiveId(), fileId, "preview", userId, "internal", req);
        return minioService.getPresignedUrl(file.getBucketName(), file.getObjectKey());
    }

    public String internalDownload(Long fileId, int maxSecurityLevel, DataScope dataScope,
                                   Long organizationId, Long userId, HttpServletRequest req) {
        ArchiveFile file = loadVisibleInternalFile(fileId, maxSecurityLevel, dataScope, organizationId);
        logAccess(file.getArchiveId(), fileId, "download", userId, "internal", req);
        return minioService.getPresignedUrl(file.getBucketName(), file.getObjectKey());
    }

    /** 加载公众可见文件：文件存在 + 所属档案非密公开正常 + file_status=normal。 */
    ArchiveFile loadVisiblePublicFile(Long fileId) {
        ArchiveFile file = archiveFileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案文件不存在");
        }
        Archive a = archiveMapper.selectOne(new QueryWrapper<Archive>()
                .eq("id", file.getArchiveId())
                .eq("security_level", 0).eq("open_status", "open").eq("lifecycle_status", "normal"));
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在或不公开");
        }
        if (file.getFileStatus() != FileStatus.normal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "文件不可用");
        }
        return file;
    }

    /** 加载内部可见文件：文件存在 + 所属档案在当前用户权限范围 + file_status=normal。 */
    ArchiveFile loadVisibleInternalFile(Long fileId, int maxSecurityLevel, DataScope dataScope, Long organizationId) {
        ArchiveFile file = archiveFileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案文件不存在");
        }
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("id", file.getArchiveId()).eq("lifecycle_status", "normal").le("security_level", maxSecurityLevel);
        applyDataScope(w, dataScope, organizationId);
        Archive a = archiveMapper.selectOne(w);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在或无权访问");
        }
        if (file.getFileStatus() != FileStatus.normal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "文件不可用");
        }
        return file;
    }

    // ==================== 访问日志 ====================

    void logAccess(Long archiveId, Long fileId, String accessType,
                   Long userId, String userType, HttpServletRequest req) {
        accessLogMapper.insertAccessLog(userId, userType, archiveId, fileId,
                accessType, extractIp(req), OffsetDateTime.now());
    }

    private String extractIp(HttpServletRequest req) {
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
```

- [ ] **Step 6: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=SearchServiceAccessTest`
Expected: Tests run: 10, Failures: 0。

> `ArchiveFile.getFileStatus()` 返回 `FileStatus` 枚举（MybatisEnumTypeHandler）。若实际为 String，调整为字符串比较并删除 `FileStatus` import。

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/archive/dto/response/ArchiveSearchDetailResponse.java \
  backend/src/main/java/com/archive/mapper/ArchiveAccessLogMapper.java \
  backend/src/main/java/com/archive/service/SearchService.java \
  backend/src/test/java/com/archive/service/SearchServiceAccessTest.java
git commit -m "feat(search): 实现 SearchService 详情预览下载与访问日志审计"
```

---

## Task 7: SearchService AI 检索与内部工作台

对应 5.6/11.4（AI 检索 JSON 生成）+ 11.1（dashboard）。AI 检索同步调用，按 AI 交互架构 2.5 不留 `ai_tasks`。重试规则：`AiClient` 抛异常不重试（直接抛），仅 `ruleType` 校验失败才重试 `searchMaxRetries` 次。

**Files:**
- Create: `backend/src/main/java/com/archive/dto/request/AiQueryRequest.java`
- Create: `backend/src/main/java/com/archive/dto/response/AiQueryResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/InternalDashboardResponse.java`
- Modify: `backend/src/main/java/com/archive/service/SearchService.java`（加 AI 检索/dashboard）
- Test: `backend/src/test/java/com/archive/service/SearchServiceAiTest.java`

- [ ] **Step 1: 创建 AI 检索请求/响应与 dashboard 响应 DTO**

`backend/src/main/java/com/archive/dto/request/AiQueryRequest.java`：

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiQueryRequest {
    @NotBlank(message = "检索内容不能为空")
    private String text;
}
```

`backend/src/main/java/com/archive/dto/response/AiQueryResponse.java`：

```java
package com.archive.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiQueryResponse {
    private String ruleType;
    private JsonNode conditions;
    private JsonNode rawJson;
}
```

`backend/src/main/java/com/archive/dto/response/InternalDashboardResponse.java`：

```java
package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class InternalDashboardResponse {
    /** 最近查阅（来自 archive_access_logs） */
    private List<RecentView> recentViews;
    /** 我的借阅申请 —— 本次返回空，待 feat/borrow-liu 补齐 */
    private List<Object> myBorrowRequests;
    /** 当前借阅 —— 本次返回空 */
    private List<Object> currentBorrows;
    /** 逾期提示 —— 本次返回空 */
    private List<Object> overdueReminders;

    @Data
    @AllArgsConstructor
    public static class RecentView {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private OffsetDateTime accessedAt;
    }
}
```

- [ ] **Step 2: 写 AI 检索重试、降级、dashboard 的失败测试**

`backend/src/test/java/com/archive/service/SearchServiceAiTest.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.AiQueryResponse;
import com.archive.dto.response.InternalDashboardResponse;
import com.archive.entity.AiTask;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SearchServiceAiTest {

    private SearchService service;
    private ArchiveMapper archiveMapper;
    private ArchiveAccessLogMapper accessLogMapper;
    private JdbcTemplate jdbcTemplate;
    private AiClient aiClient;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        accessLogMapper = mock(ArchiveAccessLogMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        aiClient = mock(AiClient.class);
        service = new SearchService(archiveMapper, mock(ArchiveFileMapper.class), accessLogMapper,
                mock(CategoryMapper.class), mock(TagMapper.class), jdbcTemplate, null, aiClient);
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
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()));
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
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()));
    }

    @Test
    void publicAiQuery_公众检索未开放抛冲突() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        assertThatThrownBy(() -> service.publicAiQuery("查档案"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
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
        Map<String, Object> row = new java.util.HashMap<>();
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
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=SearchServiceAiTest`
Expected: 编译失败（SearchService 缺 AI/dashboard 方法 + setObjectMapper）。

- [ ] **Step 4: 在 SearchService 追加 AI 检索与 dashboard 方法**

在 `backend/src/main/java/com/archive/service/SearchService.java` 顶部 import 区追加：

```java
import com.archive.dto.response.AiQueryResponse;
import com.archive.dto.response.InternalDashboardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
```

在类体内字段区追加（`@RequiredArgsConstructor` 不纳入非 final 字段，不影响 Task 5/6 构造）：

```java
    @Autowired
    private ObjectMapper objectMapper;

    /** 单元测试注入用。 */
    void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
```

在类体内方法区追加：

```java
    // ==================== 5.6 / 11.4 AI 检索 JSON 生成 ====================

    public AiQueryResponse publicAiQuery(String text) {
        if (!aiClient.isAvailable()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 功能未启用");
        }
        if (!isPublicSearchEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "公众检索未开放");
        }
        List<String> tags = loadVisibleTagsForPublic();
        JsonNode result = callSearchAiWithRetry(buildSearchSystemPrompt(true),
                buildSearchUserMessage(text, tags));
        ObjectNode conditions = ensureObjectNode(result.get("conditions"));
        conditions.put("securityLevelMax", 0);     // 公众强制
        conditions.put("openStatus", "open");      // 公众强制
        return new AiQueryResponse("query", conditions, result);
    }

    public AiQueryResponse internalAiQuery(String text, int maxSecurityLevel,
                                           DataScope dataScope, Long organizationId) {
        if (!aiClient.isAvailable()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 功能未启用");
        }
        List<String> tags = loadVisibleTagsForInternal(maxSecurityLevel, dataScope, organizationId);
        JsonNode result = callSearchAiWithRetry(buildSearchSystemPrompt(false),
                buildSearchUserMessage(text, tags));
        JsonNode conditions = result.get("conditions") != null ? result.get("conditions") : objectMapper.createObjectNode();
        return new AiQueryResponse("query", conditions, result);
    }

    /**
     * 同步调用 AI 并校验 ruleType；AiClient 抛异常不重试（直接传播），
     * 仅 ruleType != query 时重试，最多 searchMaxRetries 次。
     */
    JsonNode callSearchAiWithRetry(String systemPrompt, String userMessage) {
        int max = aiClient.getSearchMaxRetries();
        for (int i = 0; i < max; i++) {
            JsonNode result = aiClient.callAndExtractJson(systemPrompt, userMessage);
            JsonNode ruleType = result.get("ruleType");
            if (ruleType != null && "query".equals(ruleType.asText())) {
                return result;
            }
        }
        throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 检索结果格式异常，请改用普通筛选");
    }

    private String buildSearchSystemPrompt(boolean isPublic) {
        return """
                你是档案管理系统的 AI 助手，将用户的自然语言检索需求转换为结构化查询条件。

                约束：
                - 分类必须从系统给定分类中选择（document/technology/accounting/audio_video/personnel）。
                - 标签必须优先从系统给定的可见标签集合中选择。
                - 日期格式统一为 yyyy-MM-dd。
                - 如用户为公众用户，必须附加 securityLevelMax = 0 且 openStatus = "open"。
                - 你只生成查询条件，不执行查询。

                输出格式：
                <JSON>
                {
                  "ruleType": "query",
                  "conditions": {
                    "keywords": ["关键词"],
                    "category": "分类code",
                    "formedDateRange": ["yyyy-MM-dd","yyyy-MM-dd"],
                    "responsible": "责任者",
                    "securityLevelMax": 0,
                    "openStatus": "open",
                    "carrierStatus": "electronic / paper_electronic / paper"
                  }
                }
                </JSON>
                """;
    }

    private String buildSearchUserMessage(String text, List<String> tags) {
        return "用户检索需求：" + text + "\n" +
                "可用分类：document/technology/accounting/audio_video/personnel\n" +
                "可见标签：" + String.join("、", tags);
    }

    private ObjectNode ensureObjectNode(JsonNode node) {
        if (node instanceof ObjectNode on) return on;
        return objectMapper.createObjectNode();
    }

    /** 公众可见标签：非密公开正常档案的 distinct tag_name。 */
    private List<String> loadVisibleTagsForPublic() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT t.tag_name FROM tags t " +
                "JOIN archive_tags at ON at.tag_id = t.id " +
                "JOIN archives a ON a.id = at.archive_id " +
                "WHERE a.security_level = 0 AND a.open_status = 'open' AND a.lifecycle_status = 'normal'",
                String.class);
    }

    /** 内部可见标签：按密级上限 + 数据范围 + 生命周期过滤后的 distinct tag_name。 */
    private List<String> loadVisibleTagsForInternal(int maxSecurityLevel, DataScope scope, Long organizationId) {
        String orgFilter = (scope == DataScope.all || organizationId == null) ? ""
                : (scope == DataScope.own_org
                    ? " AND a.organization_id = " + organizationId
                    : " AND a.fonds_id IN (SELECT id FROM fonds WHERE organization_id = " + organizationId + ")");
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT t.tag_name FROM tags t " +
                "JOIN archive_tags at ON at.tag_id = t.id " +
                "JOIN archives a ON a.id = at.archive_id " +
                "WHERE a.lifecycle_status = 'normal' AND a.security_level <= " + maxSecurityLevel + orgFilter,
                String.class);
    }

    // ==================== 11.1 内部工作台 ====================

    public InternalDashboardResponse getInternalDashboard(Long userId) {
        List<InternalDashboardResponse.RecentView> recentViews = new ArrayList<>();
        for (Map<String, Object> row : accessLogMapper.findRecentViews(userId)) {
            recentViews.add(new InternalDashboardResponse.RecentView(
                    toLong(row.get("archiveId")),
                    (String) row.get("archiveNo"),
                    (String) row.get("title"),
                    row.get("accessedAt") instanceof OffsetDateTime odt ? odt : null));
        }
        // 借阅部分本次返回空占位，待 feat/borrow-liu
        return new InternalDashboardResponse(
                recentViews, List.of(), List.of(), List.of());
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=SearchServiceAiTest`
Expected: Tests run: 8, Failures: 0。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/dto/request/AiQueryRequest.java \
  backend/src/main/java/com/archive/dto/response/AiQueryResponse.java \
  backend/src/main/java/com/archive/dto/response/InternalDashboardResponse.java \
  backend/src/main/java/com/archive/service/SearchService.java \
  backend/src/test/java/com/archive/service/SearchServiceAiTest.java
git commit -m "feat(search): 实现 SearchService AI 检索与内部工作台"
```

---

## Task 8: Controller 层

Controller 只做参数绑定、身份解析（`AuthContext`）、HTTP 响应封装；业务与权限在 Service。声明式代码，无单元测试（逻辑已在 Service 测试覆盖），靠编译 + Task 9 集成验证。

**Files:**
- Create: `backend/src/main/java/com/archive/controller/PublicSearchController.java`
- Create: `backend/src/main/java/com/archive/controller/SearchController.java`
- Modify: `backend/src/main/java/com/archive/controller/PendingArchiveController.java`（加 9.3）
- Modify: `backend/src/main/java/com/archive/controller/AiTaskController.java`（补 9.4/9.5，去 501）

- [ ] **Step 1: 创建 PublicSearchController（5.2-5.6）**

`backend/src/main/java/com/archive/controller/PublicSearchController.java`：

```java
package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.AiQueryRequest;
import com.archive.dto.request.ArchiveSearchQuery;
import com.archive.dto.response.AiQueryResponse;
import com.archive.dto.response.ArchiveSearchDetailResponse;
import com.archive.dto.response.ArchiveSummaryResponse;
import com.archive.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 公众检索接口（5.2-5.6）。
 * /api/public/** 在 SaTokenConfig 免登录；5.5 下载在 Service 层校验登录。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "公众检索", description = "公开检索、详情、预览、下载、AI 检索")
public class PublicSearchController {

    private final SearchService searchService;

    @GetMapping("/archives/search")
    @Operation(summary = "公开档案检索")
    public R<PageResult<ArchiveSummaryResponse>> search(@ModelAttribute ArchiveSearchQuery query) {
        return R.ok(searchService.publicSearch(query));
    }

    @GetMapping("/archives/{archiveId}")
    @Operation(summary = "公开档案详情")
    public R<ArchiveSearchDetailResponse> detail(@PathVariable Long archiveId, HttpServletRequest req) {
        Long uid = currentPublicUserId();
        return R.ok(searchService.publicDetail(archiveId, uid, uid != null ? "public" : "anonymous", req));
    }

    @GetMapping("/archive-files/{fileId}/preview")
    @Operation(summary = "公开档案预览")
    public ResponseEntity<Void> preview(@PathVariable Long fileId, HttpServletRequest req) {
        Long uid = currentPublicUserId();
        String url = searchService.publicPreview(fileId, uid, uid != null ? "public" : "anonymous", req);
        return redirect(url);
    }

    @GetMapping("/archive-files/{fileId}/download")
    @Operation(summary = "公开档案下载")
    public ResponseEntity<Void> download(@PathVariable Long fileId, HttpServletRequest req) {
        Long uid = currentPublicUserId();
        // publicDownload 内部校验 uid==null 抛 UNAUTHORIZED
        String url = searchService.publicDownload(fileId, uid, uid != null ? "public" : "anonymous", req);
        return redirect(url);
    }

    @PostMapping("/archives/ai-query")
    @Operation(summary = "公众 AI 检索 JSON 生成")
    public R<AiQueryResponse> aiQuery(@RequestBody @Valid AiQueryRequest req) {
        return R.ok(searchService.publicAiQuery(req.getText()));
    }

    /** 公众端可能未登录；未登录返回 null。 */
    private Long currentPublicUserId() {
        try {
            return AuthContext.isAuthenticated() ? AuthContext.getCurrentUserId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<Void> redirect(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
```

- [ ] **Step 2: 创建 SearchController（11.1-11.6）**

`backend/src/main/java/com/archive/controller/SearchController.java`：

```java
package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.AiQueryRequest;
import com.archive.dto.request.ArchiveSearchQuery;
import com.archive.dto.response.AiQueryResponse;
import com.archive.dto.response.ArchiveSearchDetailResponse;
import com.archive.dto.response.ArchiveSummaryResponse;
import com.archive.dto.response.InternalDashboardResponse;
import com.archive.enums.DataScope;
import com.archive.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 内部查阅者检索接口（11.1-11.6）。
 * /api/internal/** 需登录（SaTokenConfig 默认拦截 /api/**）。
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Tag(name = "内部检索", description = "内部工作台、检索、详情、预览、下载、AI 检索")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/dashboard")
    @Operation(summary = "内部工作台")
    public R<InternalDashboardResponse> dashboard() {
        return R.ok(searchService.getInternalDashboard(AuthContext.getCurrentUserId()));
    }

    @GetMapping("/archives/search")
    @Operation(summary = "内部档案检索")
    public R<PageResult<ArchiveSummaryResponse>> search(@ModelAttribute ArchiveSearchQuery query) {
        // internalSearch 内部读取 AuthContext 做权限过滤
        return R.ok(searchService.internalSearch(query));
    }

    @GetMapping("/archives/{archiveId}")
    @Operation(summary = "内部档案详情")
    public R<ArchiveSearchDetailResponse> detail(@PathVariable Long archiveId, HttpServletRequest req) {
        return R.ok(searchService.internalDetail(archiveId,
                AuthContext.getMaxSecurityLevel(), AuthContext.getDataScope(),
                AuthContext.getOrganizationId(), AuthContext.getCurrentUserId(), req));
    }

    @GetMapping("/archive-files/{fileId}/preview")
    @Operation(summary = "内部预览电子文件")
    public ResponseEntity<Void> preview(@PathVariable Long fileId, HttpServletRequest req) {
        String url = searchService.internalPreview(fileId,
                AuthContext.getMaxSecurityLevel(), AuthContext.getDataScope(),
                AuthContext.getOrganizationId(), AuthContext.getCurrentUserId(), req);
        return redirect(url);
    }

    @GetMapping("/archive-files/{fileId}/download")
    @Operation(summary = "内部下载电子文件")
    public ResponseEntity<Void> download(@PathVariable Long fileId, HttpServletRequest req) {
        String url = searchService.internalDownload(fileId,
                AuthContext.getMaxSecurityLevel(), AuthContext.getDataScope(),
                AuthContext.getOrganizationId(), AuthContext.getCurrentUserId(), req);
        return redirect(url);
    }

    @PostMapping("/archives/ai-query")
    @Operation(summary = "内部 AI 检索 JSON 生成")
    public R<AiQueryResponse> aiQuery(@RequestBody @Valid AiQueryRequest req) {
        return R.ok(searchService.internalAiQuery(req.getText(),
                AuthContext.getMaxSecurityLevel(), AuthContext.getDataScope(),
                AuthContext.getOrganizationId()));
    }

    private ResponseEntity<Void> redirect(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
```

- [ ] **Step 3: 在 PendingArchiveController 加 9.3 启动 AI 补全**

在 `backend/src/main/java/com/archive/controller/PendingArchiveController.java`：
- import 区追加 `com.archive.dto.response.AiTaskStartResponse` 和 `com.archive.service.AiTaskService`
- 字段区追加 `private final AiTaskService aiTaskService;`（已有的 `@RequiredArgsConstructor` 会自动注入）
- 类体内追加方法：

```java
    @PostMapping("/batches/{batchId}/ai-completion")
    @Operation(summary = "启动 AI 补全")
    public R<AiTaskStartResponse> startAiCompletion(@PathVariable Long batchId) {
        return R.ok(aiTaskService.startIntakeCompletion(batchId));
    }
```

- [ ] **Step 4: 改 AiTaskController 补 9.4/9.5（替换 501 空壳）**

`backend/src/main/java/com/archive/controller/AiTaskController.java` 全文替换为：

```java
package com.archive.controller;

import com.archive.common.R;
import com.archive.dto.response.AiTaskResponse;
import com.archive.service.AiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 任务接口（9.4 查询、9.5 重试失败批次）。
 */
@RestController
@RequestMapping("/api/admin/ai-tasks")
@RequiredArgsConstructor
@Tag(name = "AI 任务", description = "AI 补全任务查询与重试")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    @GetMapping("/{taskId}")
    @Operation(summary = "查询 AI 补全任务")
    public R<AiTaskResponse> getAiTask(@PathVariable Long taskId) {
        return R.ok(aiTaskService.getTask(taskId));
    }

    @PostMapping("/{taskId}/retry-failed")
    @Operation(summary = "重试失败 AI 批次")
    public R<AiTaskResponse> retryAiTask(@PathVariable Long taskId) {
        return R.ok(aiTaskService.retryFailed(taskId));
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/controller/PublicSearchController.java \
  backend/src/main/java/com/archive/controller/SearchController.java \
  backend/src/main/java/com/archive/controller/PendingArchiveController.java \
  backend/src/main/java/com/archive/controller/AiTaskController.java
git commit -m "feat(search): 添加公众/内部检索 Controller 与 AI 补全接口"
```

---

## Task 9: 集成验证、AI 降级与最终构建

**Files:** 无新增（本任务为验证与收尾）。

- [ ] **Step 1: 全量单元测试**

Run: `cd backend && mvn -q test`
Expected: Tests run 全部，Failures: 0, Errors: 0。包含本模块新增的 `AiTaskNoUtilTest`、`AiSuggestionServiceTest`、`AiTaskServiceTest`、`SearchServiceSearchTest`、`SearchServiceAccessTest`、`SearchServiceAiTest`，以及既有的 `ArchiveApplicationTests`。

若有失败，按失败用例定位修复；不得为使测试通过而弱化断言（见项目 backend/CLAUDE.md 测试完整性约束）。

- [ ] **Step 2: 全量编译打包**

Run: `cd backend && mvn -q -DskipTests package`
Expected: BUILD SUCCESS，生成 `target/*.jar`。

- [ ] **Step 3: AI 降级静态确认**

降级路径已由单元测试覆盖，确认两点：

1. `AiTaskServiceTest#startIntakeCompletion_AI未启用抛外部异常` —— 9.3 在 `ai.enabled=false` 时返回 502，不创建任务。
2. `SearchServiceAiTest#publicAiQuery_AI未启用抛外部异常` 与 `#重试_AiClient抛异常不重试直接抛` —— 5.6/11.4 AI 入口降级。
3. 检索主流程 `publicSearch`/`internalSearch` **不依赖** `AiClient`，AI 不可用时检索仍可用（Service 代码确认：检索方法未调用 `aiClient`）。

- [ ] **Step 4: 集成验证（环境就绪时执行）**

前置：PostgreSQL 17、MinIO、`DEEPSEEK_API_KEY` 环境变量就绪（见 backend/CLAUDE.md 开发环境检查）。

启动：`cd backend && mvn spring-boot:run`（或 IDE 启动 `ArchiveApplication`）。

按以下清单验证（任一条失败回到对应 Task 修复）：

| # | 操作 | 预期 |
|---|------|------|
| 1 | `GET /api/public/archives/search` 未登录 | 200，返回 `security_level=0/open=open/lifecycle=normal` 的档案摘要 |
| 2 | `GET /api/public/archive-files/{fileId}/download` 未登录 | 401 UNAUTHORIZED |
| 3 | 登录 `internal_reader` 后 `GET /api/internal/archives/search` | 200，结果受 `maxSecurityLevel`/`dataScope` 过滤 |
| 4 | `GET /api/internal/archives/{archiveId}` | 200，`archive_access_logs` 新增 `view_metadata` 记录（`select * from archive_access_logs order by id desc limit 1`） |
| 5 | `POST /api/admin/pending-archive/batches/{batchId}/ai-completion`（`ai.enabled=true` 且有已接收未入库条目） | 200，返回 `status=running`；轮询 `GET /api/admin/ai-tasks/{taskId}` 到终态；`select ai_suggestion from intake_items where id=?` 有写入 |
| 6 | `ai.enabled=false` 时重复 #5 | 502，且不创建 `ai_tasks` 记录 |
| 7 | `GET /api/internal/dashboard` | 200，`recentViews` 来自 `archive_access_logs`，借阅部分为空数组 |
| 8 | 公众预览/内部下载后 | `archive_access_logs` 有 `preview`/`download` 记录，`ip_address` 正确写入（`select ip_address::text from archive_access_logs order by id desc limit 1`） |

- [ ] **Step 5: 收尾提交（如有修复）**

若 Step 1-4 有修复，提交修复；否则跳过。

```bash
git add -A
git commit -m "fix(search): 集成验证修复"   # 仅有修复时
```

- [ ] **Step 6: 推送与 PR**

```bash
git push -u origin feat/search-liu
```

随后在 GitHub 以刘星（Lx123759）账号向 `develop` 发起 PR，标题 `feat(search): 检索与 AI 补全模块（内部/公众检索、下载审计、AI 补全白名单）`，由方江苏（项目经理）review 合并。

---

## 计划自审（writing-plans self-review）

**1. Spec 覆盖检查**

| Spec 章节 | 覆盖 Task |
|-----------|-----------|
| 1.1 接口 9.3/9.4/9.5（AI 补全） | Task 4（Service）、Task 8（Controller：PendingArchiveController 9.3、AiTaskController 9.4/9.5） |
| 1.1 接口 5.2-5.6（公众检索） | Task 5/6/7（Service）、Task 8（PublicSearchController） |
| 1.1 接口 11.1-11.6（内部检索） | Task 5/6/7（Service）、Task 8（SearchController） |
| 2 文件结构（3 Entity/3 Mapper/3 Service/4 Controller/Config/DTO/Util） | Task 1-8 全覆盖 |
| 3.1 AI 补全异步批处理 | Task 4（AiTaskService + AiTaskAsyncRunner） |
| 3.2 AI 白名单校验 | Task 3（AiSuggestionService.parseAndFilterItems） |
| 3.3 公众检索权限过滤 | Task 5（publicSearch 强制过滤） |
| 3.4 内部检索权限过滤 | Task 5（buildInternalWrapper + applyDataScope） |
| 3.5 下载审计（archive_access_logs） | Task 6（logAccess + insertAccessLog） |
| 3.6 AI 检索 JSON 生成 | Task 7（callSearchAiWithRetry + public/internalAiQuery） |
| 3.7 dashboard 最近查阅 | Task 7（getInternalDashboard + findRecentViews） |
| 6 错误处理（502/401/409/404） | 各 Task 对应 BusinessException + ErrorCode |
| 7 测试策略 | Task 1-7 单元测试 + Task 9 集成验证 |

无遗漏。

**2. 占位符扫描**

计划中无 `TODO`/`TBD`/`implement later`。几处"若字段名不符则按 entity 源码调整""若 INET 写入报错则在 Mapper 用 CAST（Task 6 已直接用 `::inet`）""若 getCustomSqlSegment 返回空则改用 getExpression"是具体的实施容错指引（给出明确替代方法名），不属于占位符。

**3. 类型一致性检查**

- `AiSuggestionService.parseAndFilterItems` / `validateAndPersist` / `buildSystemPrompt` / `buildUserMessage` —— Task 3 定义，Task 4 `AiTaskAsyncRunner.processBatch` 调用，签名一致。
- `AiTaskService.startIntakeCompletion` / `retryFailed` / `getTask` / `splitIntoBatches` —— Task 4 定义，Task 8 Controller 调用，签名一致。
- `SearchService` 方法（`publicSearch`/`internalSearch`/`buildInternalWrapper`/`publicDetail`/`publicPreview`/`publicDownload`/`internalPreview`/`internalDownload`/`loadVisiblePublicFile`/`loadVisibleInternalFile`/`logAccess`/`callSearchAiWithRetry`/`publicAiQuery`/`internalAiQuery`/`getInternalDashboard`/`setObjectMapper`）—— Task 5/6/7 增量定义，Task 8 Controller 调用，签名一致。
- DTO 字段名跨 Task 一致（`AiTaskStartResponse.aiTaskId/taskNo/status/batchSize/totalBatches`、`AiTaskResponse`、`AiQueryResponse.ruleType/conditions/rawJson` 等）。
- SearchService 构造为 8 参数（Task 5 定义），Task 6/7 测试 mock 对应依赖，`objectMapper` 通过字段注入 + `setObjectMapper`（不改构造），Task 5/6 测试不受影响。

**实施注意：**
- Task 6/7 对 SearchService 的 import 与方法为"追加"，实施时与 Task 5 已有 import 合并去重，避免重复 import 编译错误。
- `Archive`/`ArchiveFile`/`Category` entity 的字段名以实际源码为准（如 `Archive.getRetentionPeriod()`、`ArchiveFile.getFileRole()` 返回枚举还是 String），按需微调 `toDetail`/`loadVisibleXxx` 中的 getter 与比较方式。
- `archive_access_logs.ip_address` 的 `::inet` cast（Task 6 `insertAccessLog`）已在 SQL 内置，无需额外 TypeHandler。

---

## 执行交接

计划已完成并保存至 `backend/docs/superpowers/plans/2026-06-13-search-liu.md`。下一步执行有两种方式：

**1. Subagent-Driven（推荐）** —— 每个 Task 派发独立 subagent 执行，任务间 review，快速迭代。
**2. Inline Execution** —— 在当前会话用 executing-plans 批量执行，带 checkpoint review。

请选择执行方式；或先暂停，由你 review 计划后再决定。
