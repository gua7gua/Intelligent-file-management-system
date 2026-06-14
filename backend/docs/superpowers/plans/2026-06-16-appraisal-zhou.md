# M08/M10/M11/M14 鉴定·销毁·审批·用户配置模块（feat/appraisal-zhou）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现接口文档第 13（审批工作台）、14（鉴定）、15（销毁）、22（用户/角色/系统配置）章共 24 个 REST 端点 + 1 个序列迁移，打通「鉴定→销毁清册→馆领导审批→确认销毁」闭环。

**Architecture:** 复用已存在的 `ApprovalRequest(+Mapper)`、`User/Role/UserRole/Organization(+Mapper)`、`Archive/ArchiveFile/ArchiveBox/ArchiveBoxItem/ArchiveChangeLog(+Mapper)`。新建鉴定/销毁/系统配置/业务附件的实体与 Mapper、4 个 Service、6 个 Controller（含 `UserController` 迁移到 `/api/admin/users`）、编号工具与 DTO。审批生效采用 Approach A：`ApprovalService` 单事务内按类型窄变更目标表 + 写 change_log + 审计。聚合查询用 `JdbcTemplate`，与 `WarehouseService` 同款。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、PostgreSQL、SaToken、MinIO、ClamAV；测试 JUnit5 + Mockito + AssertJ。

**分支：** `feat/appraisal-zhou`（从最新 `develop` 拉出，spec 与本计划为首两个提交）。任务按依赖顺序执行，每个 Service 方法先写测试（红）再实现（绿）。

**设计依据：** [2026-06-16-appraisal-zhou-design.md](../specs/2026-06-16-appraisal-zhou-design.md)

测试目录前缀：`backend/src/test/java/com/archive/`
主代码目录前缀：`backend/src/main/java/com/archive/`
迁移目录：`backend/src/main/resources/db/migration/`

---

## 文件结构

| 文件 | 职责 | 动作 |
|------|------|------|
| `db/migration/V17__add-appraisal-batch-sequence.sql` | 新增 `seq_appraisal_batch_no` 序列 | 新建 |
| `enums/AppraisalBatchStatus.java` | draft/completed | 新建 |
| `enums/AppraisalResult.java` | extend/destroy | 新建 |
| `enums/DestructionListStatus.java` | draft/pending_approval/pending_destroy/destroyed | 新建 |
| `enums/DestroyMethod.java` | shredding/burning/entrusted | 新建 |
| `enums/ConfigValueType.java` | string/number/boolean/json | 新建 |
| `entity/AppraisalBatch.java` | 鉴定批次 | 新建 |
| `entity/AppraisalItem.java` | 鉴定明细 | 新建 |
| `entity/DestructionList.java` | 销毁清册 | 新建 |
| `entity/DestructionItem.java` | 销毁明细（快照） | 新建 |
| `entity/SystemConfig.java` | 系统配置项 | 新建 |
| `entity/BusinessAttachment.java` | 业务附件（销毁照片） | 新建 |
| `mapper/AppraisalBatchMapper.java` 等 6 个 | BaseMapper | 新建 |
| `util/AppraisalNoUtil.java` | `APP-{6位序号}` | 新建 |
| `util/DestructionNoUtil.java` | `DES-{6位序号}` | 新建 |
| `service/AppraisalService.java` | 14.1-14.5 | 新建 |
| `service/DestructionService.java` | 15.1-15.5 | 新建 |
| `service/ApprovalService.java` | 13.1-13.4 | 新建 |
| `service/SystemConfigService.java` | 22.8-22.10 | 新建 |
| `service/RoleService.java` | 22.7 | 新建 |
| `service/UserService.java` | 22.1-22.6（迁移+扩展） | 改造 |
| `controller/AppraisalController.java` | 5 端点 | 新建 |
| `controller/DestructionController.java` | 5 端点 | 新建 |
| `controller/ApprovalController.java` | 4 端点 | 新建 |
| `controller/SystemConfigController.java` | 3 端点 | 新建 |
| `controller/RoleController.java` | 1 端点 | 新建 |
| `controller/UserController.java` | 迁移路径 + 扩展至 6 端点 | 改造 |
| DTO 请求/响应若干 | 见 Task 6/7 | 新建/扩展 |

---

## Task 1: V17 序列迁移

**Files:**
- Create: `backend/src/main/resources/db/migration/V17__add-appraisal-batch-sequence.sql`

- [ ] **Step 1: 创建迁移脚本**

`backend/src/main/resources/db/migration/V17__add-appraisal-batch-sequence.sql`：

```sql
-- 鉴定批次号序列（编号规范 APP-{自增序号}，V14 缺失，本迁移补齐）
CREATE SEQUENCE IF NOT EXISTS seq_appraisal_batch_no START WITH 1;

-- 对齐已有演示/种子数据（如 APP-000001），避免 uk_appraisal_batches_batch_no 唯一索引冲突
SELECT setval('seq_appraisal_batch_no',
  GREATEST(1, COALESCE(
    (SELECT MAX(CAST(SUBSTRING(batch_no FROM '[0-9]+$') AS bigint)) FROM appraisal_batches),
    0)));
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/resources/db/migration/V17__add-appraisal-batch-sequence.sql
git commit -m "feat(appraisal): 新增鉴定批次号序列 seq_appraisal_batch_no"
```

---

## Task 2: 枚举（5 个）

枚举为纯常量，以编译通过为验证。命名/风格对齐 `ApprovalType`（`@Getter` + `displayName`，实体字段用 `@EnumValue`）。

**Files:**
- Create: `backend/src/main/java/com/archive/enums/AppraisalBatchStatus.java`
- Create: `backend/src/main/java/com/archive/enums/AppraisalResult.java`
- Create: `backend/src/main/java/com/archive/enums/DestructionListStatus.java`
- Create: `backend/src/main/java/com/archive/enums/DestroyMethod.java`
- Create: `backend/src/main/java/com/archive/enums/ConfigValueType.java`

- [ ] **Step 1: AppraisalBatchStatus**

```java
package com.archive.enums;

import lombok.Getter;

/** 鉴定批次状态。 */
@Getter
public enum AppraisalBatchStatus {
    draft("草稿"),
    completed("已完成");

    private final String displayName;

    AppraisalBatchStatus(String displayName) {
        this.displayName = displayName;
    }
}
```

- [ ] **Step 2: AppraisalResult**

```java
package com.archive.enums;

import lombok.Getter;

/** 鉴定结论。 */
@Getter
public enum AppraisalResult {
    extend("延长保管"),
    destroy("销毁");

    private final String displayName;

    AppraisalResult(String displayName) {
        this.displayName = displayName;
    }
}
```

- [ ] **Step 3: DestructionListStatus**

```java
package com.archive.enums;

import lombok.Getter;

/** 销毁清册状态。 */
@Getter
public enum DestructionListStatus {
    draft("草稿"),
    pending_approval("待审批"),
    pending_destroy("待销毁"),
    destroyed("已销毁");

    private final String displayName;

    DestructionListStatus(String displayName) {
        this.displayName = displayName;
    }
}
```

- [ ] **Step 4: DestroyMethod**

```java
package com.archive.enums;

import lombok.Getter;

/** 销毁方式。 */
@Getter
public enum DestroyMethod {
    shredding("粉碎"),
    burning("焚毁"),
    entrusted("委托销毁");

    private final String displayName;

    DestroyMethod(String displayName) {
        this.displayName = displayName;
    }
}
```

- [ ] **Step 5: ConfigValueType**

```java
package com.archive.enums;

import lombok.Getter;

/** 系统配置值类型。 */
@Getter
public enum ConfigValueType {
    string,
    number,
    boolean,
    json
}
```

- [ ] **Step 6: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/archive/enums/
git commit -m "feat(appraisal): 新增鉴定销毁审批配置相关枚举"
```

---

## Task 3: 实体（6 个）

字段严格对齐数据库设计第 8 章（鉴定/销毁）、11.3（system_configs）、6.4（business_attachments）。继承 `BaseEntity` 的表含审计字段；`ArchiveChangeLog` 风格（自带 id/时间）的实体不继承。

**Files:**
- Create: 6 个实体文件（见下）

- [ ] **Step 1: AppraisalBatch（继承 BaseEntity）**

```java
package com.archive.entity;

import com.archive.enums.AppraisalBatchStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/** 鉴定批次。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appraisal_batches")
public class AppraisalBatch extends BaseEntity {

    private String batchNo;
    private String batchName;
    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;

    @EnumValue
    private AppraisalBatchStatus status;

    private OffsetDateTime completedAt;
}
```

- [ ] **Step 2: AppraisalItem（继承 BaseEntity）**

```java
package com.archive.entity;

import com.archive.enums.AppraisalResult;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 鉴定明细。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appraisal_items")
public class AppraisalItem extends BaseEntity {

    private Long batchId;
    private Long archiveId;

    @EnumValue
    private AppraisalResult appraisalResult;

    private String newRetentionPeriod;
    private LocalDate newRetentionUntil;
    private String opinion;
    private Long appraisedBy;
    private OffsetDateTime appraisedAt;
}
```

- [ ] **Step 3: DestructionList（继承 BaseEntity）**

```java
package com.archive.entity;

import com.archive.enums.DestroyMethod;
import com.archive.enums.DestructionListStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/** 销毁清册。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("destruction_lists")
public class DestructionList extends BaseEntity {

    private String listNo;
    private String listName;
    private Long appraisalBatchId;

    @EnumValue
    private DestructionListStatus status;

    private Long approvalRequestId;
    private OffsetDateTime destroyedAt;

    @EnumValue
    private DestroyMethod destroyMethod;

    private String supervisorName1;
    private String supervisorName2;
    private String destroyNote;
}
```

- [ ] **Step 4: DestructionItem（继承 BaseEntity）**

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/** 销毁明细（快照）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("destruction_items")
public class DestructionItem extends BaseEntity {

    private Long destructionListId;
    private Long archiveId;
    private String archiveNoSnapshot;
    private String titleSnapshot;
    private String categorySnapshot;
    private Integer pageCountSnapshot;
    private String retentionSnapshot;
    private Integer securityLevelSnapshot;
    private String appraisalOpinionSnapshot;

    /** not_started / deleted / failed。 */
    private String fileDeleteStatus;
    private OffsetDateTime fileDeletedAt;
}
```

- [ ] **Step 5: SystemConfig（不继承 BaseEntity，自带 id；对齐 11.3）**

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 系统配置项。 */
@Data
@TableName("system_configs")
public class SystemConfig {

    private Long id;
    private String configKey;
    private String configValue;

    /** string / number / boolean / json。 */
    private String valueType;

    private String description;
    private Boolean editable;
}
```

- [ ] **Step 6: BusinessAttachment（继承 BaseEntity；对齐 6.4）**

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 业务附件（回执/签字件/协议/现场照片/报告等，多态 business_type+business_id）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("business_attachments")
public class BusinessAttachment extends BaseEntity {

    /** destruction_list / borrow_request / intake_batch 等。 */
    private String businessType;
    private Long businessId;

    /** destruction_photo / borrow_voucher_pdf 等。 */
    private String attachmentType;

    private String bucketName;
    private String objectKey;
    private String originalFilename;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String sha256;
}
```

> 说明：`business_attachments` 表结构在实现 Task 8（销毁照片）前需确认其列名（`business_type/business_id/attachment_type` + 对象存储字段）。若与上方字段不符，以实际 V6 迁移建表为准调整实体。

- [ ] **Step 7: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/archive/entity/
git commit -m "feat(appraisal): 新增鉴定销毁系统配置业务附件实体"
```

---

## Task 4: Mapper（6 个，均继承 BaseMapper）

**Files:**
- Create: `backend/src/main/java/com/archive/mapper/AppraisalBatchMapper.java`
- Create: `backend/src/main/java/com/archive/mapper/AppraisalItemMapper.java`
- Create: `backend/src/main/java/com/archive/mapper/DestructionListMapper.java`
- Create: `backend/src/main/java/com/archive/mapper/DestructionItemMapper.java`
- Create: `backend/src/main/java/com/archive/mapper/SystemConfigMapper.java`
- Create: `backend/src/main/java/com/archive/mapper/BusinessAttachmentMapper.java`

- [ ] **Step 1: 创建 6 个 Mapper（模板一致，仅泛型不同）**

`AppraisalBatchMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.AppraisalBatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface AppraisalBatchMapper extends BaseMapper<AppraisalBatch> {
}
```

`AppraisalItemMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.AppraisalItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface AppraisalItemMapper extends BaseMapper<AppraisalItem> {
}
```

`DestructionListMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.DestructionList;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface DestructionListMapper extends BaseMapper<DestructionList> {
}
```

`DestructionItemMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.DestructionItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface DestructionItemMapper extends BaseMapper<DestructionItem> {
}
```

`SystemConfigMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.SystemConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
}
```

`BusinessAttachmentMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.BusinessAttachment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface BusinessAttachmentMapper extends BaseMapper<BusinessAttachment> {
}
```

- [ ] **Step 2: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/archive/mapper/
git commit -m "feat(appraisal): 新增鉴定销毁配置附件 Mapper"
```

---

## Task 5: 编号工具（2 个）+ 单测

**Files:**
- Create: `backend/src/main/java/com/archive/util/AppraisalNoUtil.java`
- Create: `backend/src/main/java/com/archive/util/DestructionNoUtil.java`
- Test: `backend/src/test/java/com/archive/util/AppraisalNoUtilTest.java`
- Test: `backend/src/test/java/com/archive/util/DestructionNoUtilTest.java`

- [ ] **Step 1: 写失败测试 AppraisalNoUtilTest**

```java
package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppraisalNoUtilTest {

    @Test
    void generate_使用序列生成APP加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_appraisal_batch_no')"), eq(Long.class)))
                .thenReturn(1L);

        assertThat(new AppraisalNoUtil(jdbc).generate()).isEqualTo("APP-000001");
    }

    @Test
    void generate_大序号正确补零() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_appraisal_batch_no')"), eq(Long.class)))
                .thenReturn(23456L);

        assertThat(new AppraisalNoUtil(jdbc).generate()).isEqualTo("APP-023456");
    }
}
```

- [ ] **Step 2: 写失败测试 DestructionNoUtilTest**

```java
package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DestructionNoUtilTest {

    @Test
    void generate_使用序列生成DES加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_destruction_list_no')"), eq(Long.class)))
                .thenReturn(7L);

        assertThat(new DestructionNoUtil(jdbc).generate()).isEqualTo("DES-000007");
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=AppraisalNoUtilTest,DestructionNoUtilTest`
Expected: 编译失败（类不存在）

- [ ] **Step 4: 写实现 AppraisalNoUtil**

```java
package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 鉴定批次号生成工具。调用 seq_appraisal_batch_no，格式 APP-{6位序号}。 */
@Component
@RequiredArgsConstructor
public class AppraisalNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_appraisal_batch_no')", Long.class);
        return String.format("APP-%06d", seq);
    }
}
```

- [ ] **Step 5: 写实现 DestructionNoUtil**

```java
package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 销毁清册号生成工具。调用 seq_destruction_list_no，格式 DES-{6位序号}。 */
@Component
@RequiredArgsConstructor
public class DestructionNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_destruction_list_no')", Long.class);
        return String.format("DES-%06d", seq);
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=AppraisalNoUtilTest,DestructionNoUtilTest`
Expected: PASS（3 个测试通过）

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/archive/util/AppraisalNoUtil.java backend/src/main/java/com/archive/util/DestructionNoUtil.java backend/src/test/java/com/archive/util/AppraisalNoUtilTest.java backend/src/test/java/com/archive/util/DestructionNoUtilTest.java
git commit -m "feat(appraisal): 新增鉴定批次号与销毁清册号生成工具"
```

---

## Task 6: 请求 DTO（8 个，其中 2 个扩展现有）

纯数据载体，以编译通过为验证。

**Files:**
- Create: `backend/src/main/java/com/archive/dto/request/AppraisalBatchCreateRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/AppraisalItemSaveRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/DestructionSubmitRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/DestructionDestroyRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/ApprovalOpinionRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/SystemConfigUpdateRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/SystemConfigBatchUpdateRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/UserStatusRequest.java`
- Modify: `backend/src/main/java/com/archive/dto/request/UserCreateRequest.java`（追加 `roleCodes`）
- Modify: `backend/src/main/java/com/archive/dto/request/UserUpdateRequest.java`（追加 `roleCodes`）

- [ ] **Step 1: AppraisalBatchCreateRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppraisalBatchCreateRequest {

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;
}
```

- [ ] **Step 2: AppraisalItemSaveRequest（含内嵌条目）**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AppraisalItemSaveRequest {

    @NotNull(message = "明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "档案不能为空")
        private Long archiveId;

        /** extend / destroy。 */
        @NotNull(message = "鉴定结论不能为空")
        private String appraisalResult;

        /** 延长后的保管期限 10y/30y/permanent。 */
        private String newRetentionPeriod;

        private String opinion;
    }
}
```

- [ ] **Step 3: DestructionSubmitRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DestructionSubmitRequest {

    @NotBlank(message = "申请理由不能为空")
    private String reason;
}
```

- [ ] **Step 4: DestructionDestroyRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DestructionDestroyRequest {

    /** shredding / burning / entrusted。 */
    @NotBlank(message = "销毁方式不能为空")
    private String destroyMethod;

    @NotBlank(message = "监销人1不能为空")
    private String supervisorName1;

    @NotBlank(message = "监销人2不能为空")
    private String supervisorName2;

    private String destroyNote;
}
```

- [ ] **Step 5: ApprovalOpinionRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalOpinionRequest {

    /** 审批通过可空；退回必填（服务层校验）。 */
    private String opinion;
}
```

- [ ] **Step 6: SystemConfigUpdateRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemConfigUpdateRequest {

    @NotBlank(message = "配置值不能为空")
    private String configValue;
}
```

- [ ] **Step 7: SystemConfigBatchUpdateRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SystemConfigBatchUpdateRequest {

    @NotEmpty(message = "配置项不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        private String configKey;
        private String configValue;
    }
}
```

- [ ] **Step 8: UserStatusRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserStatusRequest {

    /** active / disabled。 */
    @NotBlank(message = "状态不能为空")
    private String status;

    private String reason;
}
```

- [ ] **Step 9: 扩展现有 UserCreateRequest / UserUpdateRequest**

在两个类中各追加角色码字段（其余字段保持现状，以实际文件为准；若已存在 `userType/dataScope/maxSecurityLevel` 则不重复添加）：

```java
    /** 角色码列表，如 ["front_archivist"]。 */
    private java.util.List<String> roleCodes;
```

> 实现时先读取现有 `UserCreateRequest`/`UserUpdateRequest` 全文，仅追加缺失的 `roleCodes`（及 §22.2 要求但缺失的 `userType/dataScope/maxSecurityLevel`），不改动已有字段顺序与校验注解。

- [ ] **Step 10: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 11: 提交**

```bash
git add backend/src/main/java/com/archive/dto/request/
git commit -m "feat(appraisal): 新增鉴定销毁审批配置请求 DTO 并扩展用户角色字段"
```

---

## Task 7: 响应 DTO（9 个，其中 1 个扩展现有）

**Files:**
- Create: `backend/src/main/java/com/archive/dto/response/AppraisalBatchResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/AppraisalBatchDetailResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/DestructionListResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/DestructionListDetailResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/ApprovalResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/ApprovalDetailResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/SystemConfigResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/RoleResponse.java`
- Modify: `backend/src/main/java/com/archive/dto/response/UserInfoResponse.java`（追加 `recentOperations`）

- [ ] **Step 1: AppraisalBatchResponse**

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AppraisalBatchResponse {

    private Long id;
    private String batchNo;
    private String batchName;
    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    private String status;
    private Integer itemCount;
    private OffsetDateTime completedAt;
}
```

- [ ] **Step 2: AppraisalBatchDetailResponse（含明细视图）**

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class AppraisalBatchDetailResponse {

    private Long id;
    private String batchNo;
    private String batchName;
    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    private String status;
    private OffsetDateTime completedAt;

    private List<ItemView> items;

    @Data
    public static class ItemView {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private Integer formedYear;
        private String retentionPeriod;
        private LocalDate retentionUntil;
        /** extend / destroy / null（未鉴定）。 */
        private String appraisalResult;
        private String newRetentionPeriod;
        private String opinion;
    }
}
```

- [ ] **Step 3: DestructionListResponse**

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DestructionListResponse {

    private Long id;
    private String listNo;
    private String listName;
    private Long appraisalBatchId;
    private String status;
    private Integer itemCount;
    private OffsetDateTime destroyedAt;
}
```

- [ ] **Step 4: DestructionListDetailResponse（含明细+审批摘要+照片）**

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class DestructionListDetailResponse {

    private Long id;
    private String listNo;
    private String listName;
    private Long appraisalBatchId;
    private String status;
    private Long approvalRequestId;
    private String approvalStatus;
    private String destroyMethod;
    private String supervisorName1;
    private String supervisorName2;
    private String destroyNote;
    private OffsetDateTime destroyedAt;

    private List<ItemView> items;
    private List<PhotoView> photos;

    @Data
    public static class ItemView {
        private Long archiveId;
        private String archiveNoSnapshot;
        private String titleSnapshot;
        private String categorySnapshot;
        private Integer pageCountSnapshot;
        private String retentionSnapshot;
        private Integer securityLevelSnapshot;
        private String appraisalOpinionSnapshot;
        private String fileDeleteStatus;
    }

    @Data
    public static class PhotoView {
        private Long id;
        private String originalFilename;
        private String mimeType;
        private Long fileSize;
        private String sha256;
    }
}
```

- [ ] **Step 5: ApprovalResponse**

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovalResponse {

    private Long id;
    private String approvalType;
    private String targetType;
    private Long targetId;
    private String status;
    private String reason;
    private Long submittedBy;
    private OffsetDateTime submittedAt;
    private String approvalOpinion;
    /** 目标摘要（档号/清册号）。 */
    private String targetSummary;
}
```

- [ ] **Step 6: ApprovalDetailResponse（含调整前后值与凭证）**

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovalDetailResponse {

    private Long id;
    private String approvalType;
    private String targetType;
    private Long targetId;
    private Long evidenceArchiveId;
    private String evidenceArchiveNo;
    private String oldValue;
    private String newValue;
    private String reason;
    private String status;
    private Long submittedBy;
    private OffsetDateTime submittedAt;
    private Long approvedBy;
    private OffsetDateTime approvedAt;
    private String approvalOpinion;
    private String targetSummary;
}
```

- [ ] **Step 7: SystemConfigResponse**

```java
package com.archive.dto.response;

import lombok.Data;

@Data
public class SystemConfigResponse {

    private Long id;
    private String configKey;
    /** 敏感键返回 ***。 */
    private String configValue;
    private String valueType;
    private String description;
    private Boolean editable;
}
```

- [ ] **Step 8: RoleResponse**

```java
package com.archive.dto.response;

import lombok.Data;

@Data
public class RoleResponse {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
}
```

> 实现时确认 `Role` 实体字段（`roleCode` 及是否含 `roleName/description` 列）；若 Role 仅有 `roleCode`，则 `roleName/description` 取 `RoleCode` 枚举的 `displayName` 填充，`description` 留空或取枚举说明。

- [ ] **Step 9: 扩展现有 UserInfoResponse**

在现有 `UserInfoResponse` 中追加「最近操作」字段（仅 22.3 详情接口填充）：

```java
    private java.util.List<RecentOperation> recentOperations;

    @lombok.Data
    public static class RecentOperation {
        private String moduleName;
        private String operationType;
        private java.time.OffsetDateTime operatedAt;
    }
```

> 不改动现有字段；`recentOperations` 在列表/创建/更新响应中保持 null。

- [ ] **Step 10: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 11: 提交**

```bash
git add backend/src/main/java/com/archive/dto/response/
git commit -m "feat(appraisal): 新增鉴定销毁审批配置响应 DTO"
```

---

## Task 8: AppraisalService 骨架 + 创建鉴定批次（14.2）

命中查询改用 MyBatis-Plus `QueryWrapper.apply("retention_until < CURRENT_DATE")`（而非 spec 描述的 JdbcTemplate），理由：与 `archiveMapper.selectList` 一致、Mockito 单测更稳定（避免 varargs 匹配）。

**Files:**
- Create: `backend/src/main/java/com/archive/service/AppraisalService.java`
- Test: `backend/src/test/java/com/archive/service/AppraisalServiceTest.java`

- [ ] **Step 1: 写失败测试（含共享 @BeforeEach）**

`backend/src/test/java/com/archive/service/AppraisalServiceTest.java`：

```java
package com.archive.service;

import com.archive.dto.request.AppraisalBatchCreateRequest;
import com.archive.dto.response.AppraisalBatchDetailResponse;
import com.archive.entity.AppraisalBatch;
import com.archive.entity.AppraisalItem;
import com.archive.entity.Archive;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.RetentionPeriod;
import com.archive.mapper.AppraisalBatchMapper;
import com.archive.mapper.AppraisalItemMapper;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.util.AppraisalNoUtil;
import com.archive.util.DestructionNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppraisalServiceTest {

    private AppraisalService service;
    private AppraisalBatchMapper batchMapper;
    private AppraisalItemMapper itemMapper;
    private ArchiveMapper archiveMapper;
    private DestructionListMapper destructionListMapper;
    private DestructionItemMapper destructionItemMapper;
    private ArchiveChangeLogMapper changeLogMapper;
    private JdbcTemplate jdbcTemplate;
    private AppraisalNoUtil appraisalNoUtil;
    private DestructionNoUtil destructionNoUtil;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        batchMapper = mock(AppraisalBatchMapper.class);
        itemMapper = mock(AppraisalItemMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        destructionListMapper = mock(DestructionListMapper.class);
        destructionItemMapper = mock(DestructionItemMapper.class);
        changeLogMapper = mock(ArchiveChangeLogMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        appraisalNoUtil = mock(AppraisalNoUtil.class);
        destructionNoUtil = mock(DestructionNoUtil.class);
        auditService = mock(AuditService.class);

        service = new AppraisalService(batchMapper, itemMapper, archiveMapper,
                destructionListMapper, destructionItemMapper, changeLogMapper,
                jdbcTemplate, appraisalNoUtil, destructionNoUtil, auditService);
    }

    @Test
    void createBatch_年度范围反了抛校验失败() {
        AppraisalBatchCreateRequest req = new AppraisalBatchCreateRequest();
        req.setBatchName("批次");
        req.setFormedYearStart(2020);
        req.setFormedYearEnd(2019);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createBatch(req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(com.archive.common.ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void createBatch_生成批次号并命中到期档案写入明细() {
        AppraisalBatchCreateRequest req = new AppraisalBatchCreateRequest();
        req.setBatchName("2026 到期会计鉴定");
        req.setCategoryId(3);
        req.setFormedYearStart(2014);
        req.setFormedYearEnd(2014);
        when(appraisalNoUtil.generate()).thenReturn("APP-000001");

        Archive a1 = archive(10L, "ARC-000010", "凭证A", 2014, LocalDate.of(2014, 1, 1),
                RetentionPeriod.ten_y, LocalDate.of(2024, 12, 31));
        Archive a2 = archive(11L, "ARC-000011", "凭证B", 2014, LocalDate.of(2014, 6, 1),
                RetentionPeriod.ten_y, LocalDate.of(2024, 6, 1));
        when(archiveMapper.selectList(any())).thenReturn(List.of(a1, a2));

        ArgumentCaptor<AppraisalItem> captor = ArgumentCaptor.forClass(AppraisalItem.class);

        AppraisalBatchDetailResponse resp = service.createBatch(req);

        assertThat(resp.getBatchNo()).isEqualTo("APP-000001");
        assertThat(resp.getStatus()).isEqualTo("draft");
        assertThat(resp.getItems()).hasSize(2);
        verify(batchMapper).insert(any(AppraisalBatch.class));
        verify(itemMapper, times(2)).insert(captor.capture());
        assertThat(captor.getValue().getArchiveId()).isIn(10L, 11L);
        assertThat(captor.getValue().getAppraisalResult()).isNull(); // 待填
        verify(auditService).log(eq("M10"), eq("create_appraisal_batch"), eq("appraisal_batch"), any(), any());
    }

    private Archive archive(long id, String no, String title, int year, LocalDate formed,
                            RetentionPeriod rp, LocalDate until) {
        Archive a = new Archive();
        a.setId(id);
        a.setArchiveNo(no);
        a.setTitle(title);
        a.setFormedYear(year);
        a.setFormedDate(formed);
        a.setRetentionPeriod(rp);
        a.setRetentionUntil(until);
        a.setLifecycleStatus(LifecycleStatus.normal);
        return a;
    }
}
```

> 注：`RetentionPeriod.ten_y` 为枚举值；实现前确认枚举确切常量名（可能为 `y10`/`ten_y`），以实际 `RetentionPeriod.java` 为准，测试同步修正。

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=AppraisalServiceTest`
Expected: 编译失败（`AppraisalService` 类不存在）

- [ ] **Step 3: 写 AppraisalService 骨架与 createBatch**

`backend/src/main/java/com/archive/service/AppraisalService.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.AppraisalBatchCreateRequest;
import com.archive.dto.response.AppraisalBatchDetailResponse;
import com.archive.entity.AppraisalBatch;
import com.archive.entity.AppraisalItem;
import com.archive.entity.Archive;
import com.archive.enums.AppraisalBatchStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.AppraisalBatchMapper;
import com.archive.mapper.AppraisalItemMapper;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.util.AppraisalNoUtil;
import com.archive.util.DestructionNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 档案鉴定服务。
 * 负责鉴定批次创建/查询/明细保存/完成（完成时按 destroy 结论生成销毁清册）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppraisalService {

    private final AppraisalBatchMapper batchMapper;
    private final AppraisalItemMapper itemMapper;
    private final ArchiveMapper archiveMapper;
    private final DestructionListMapper destructionListMapper;
    private final DestructionItemMapper destructionItemMapper;
    private final ArchiveChangeLogMapper changeLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AppraisalNoUtil appraisalNoUtil;
    private final DestructionNoUtil destructionNoUtil;
    private final AuditService auditService;

    // ==================== 14.2 创建鉴定批次 ====================

    @Transactional
    public AppraisalBatchDetailResponse createBatch(AppraisalBatchCreateRequest req) {
        if (req.getFormedYearStart() != null && req.getFormedYearEnd() != null
                && req.getFormedYearStart() > req.getFormedYearEnd()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "年度起不能大于年度止");
        }

        AppraisalBatch batch = new AppraisalBatch();
        batch.setBatchNo(appraisalNoUtil.generate());
        batch.setBatchName(req.getBatchName());
        batch.setCategoryId(req.getCategoryId());
        batch.setFormedYearStart(req.getFormedYearStart());
        batch.setFormedYearEnd(req.getFormedYearEnd());
        batch.setStatus(AppraisalBatchStatus.draft);
        batchMapper.insert(batch);

        List<Archive> hits = findDueArchives(req.getCategoryId(),
                req.getFormedYearStart(), req.getFormedYearEnd());
        for (Archive a : hits) {
            AppraisalItem item = new AppraisalItem();
            item.setBatchId(batch.getId());
            item.setArchiveId(a.getId());
            itemMapper.insert(item);
        }

        auditService.log("M10", "create_appraisal_batch", "appraisal_batch", batch.getId(),
                Map.of("batchNo", batch.getBatchNo(), "hitCount", hits.size()));

        return toDetail(batch, hits);
    }

    /** 命中 retention_until 到期且未销毁的档案。 */
    private List<Archive> findDueArchives(Integer categoryId, Integer yearStart, Integer yearEnd) {
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.isNotNull("retention_until");
        w.apply("retention_until < CURRENT_DATE");
        w.ne("lifecycle_status", "destroyed");
        if (categoryId != null) {
            w.eq("category_id", categoryId);
        }
        if (yearStart != null) {
            w.ge("formed_year", yearStart);
        }
        if (yearEnd != null) {
            w.le("formed_year", yearEnd);
        }
        w.orderByAsc("id");
        return archiveMapper.selectList(w);
    }

    /** 由命中档案列表组装批次详情（明细 appraisalResult 暂为 null）。 */
    private AppraisalBatchDetailResponse toDetail(AppraisalBatch batch, List<Archive> archives) {
        AppraisalBatchDetailResponse resp = new AppraisalBatchDetailResponse();
        resp.setId(batch.getId());
        resp.setBatchNo(batch.getBatchNo());
        resp.setBatchName(batch.getBatchName());
        resp.setCategoryId(batch.getCategoryId());
        resp.setFormedYearStart(batch.getFormedYearStart());
        resp.setFormedYearEnd(batch.getFormedYearEnd());
        resp.setStatus(batch.getStatus() != null ? batch.getStatus().name() : null);
        resp.setCompletedAt(batch.getCompletedAt());

        List<AppraisalBatchDetailResponse.ItemView> items = new ArrayList<>();
        for (Archive a : archives) {
            AppraisalBatchDetailResponse.ItemView v = new AppraisalBatchDetailResponse.ItemView();
            v.setArchiveId(a.getId());
            v.setArchiveNo(a.getArchiveNo());
            v.setTitle(a.getTitle());
            v.setFormedYear(a.getFormedYear());
            v.setRetentionPeriod(a.getRetentionPeriod() != null ? a.getRetentionPeriod().name() : null);
            v.setRetentionUntil(a.getRetentionUntil());
            items.add(v);
        }
        resp.setItems(items);
        return resp;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=AppraisalServiceTest`
Expected: PASS（2 个测试通过）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/service/AppraisalService.java backend/src/test/java/com/archive/service/AppraisalServiceTest.java
git commit -m "feat(appraisal): 实现创建鉴定批次并命中到期档案"
```

---

## Task 9: AppraisalService 查询列表（14.1）+ 批次详情（14.3）

**Files:**
- Modify: `backend/src/main/java/com/archive/service/AppraisalService.java`（新增 `listBatches`、`getBatchDetail`、`toBatchResponse`、`toDetailFromItems`）
- Modify: `backend/src/test/java/com/archive/service/AppraisalServiceTest.java`

- [ ] **Step 1: 在 AppraisalService import 区补充**

```java
import com.archive.common.PageResult;
import com.archive.dto.response.AppraisalBatchResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.HashMap;
import java.util.stream.Collectors;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void listBatches_分页并回填明细数() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setBatchNo("APP-000001");
        b.setBatchName("批次");
        b.setStatus(AppraisalBatchStatus.draft);
        Page<AppraisalBatch> page = new Page<>(1, 20);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(batchMapper.selectPage(any(), any())).thenReturn(page);
        when(itemMapper.selectCount(any())).thenReturn(5L);

        PageResult<AppraisalBatchResponse> r = service.listBatches(null, null, null, null, 1, 20);

        assertThat(r.getRecords()).hasSize(1);
        assertThat(r.getRecords().get(0).getItemCount()).isEqualTo(5);
        assertThat(r.getRecords().get(0).getStatus()).isEqualTo("draft");
    }

    @Test
    void getBatchDetail_回填档案信息与鉴定结论() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setBatchNo("APP-000001");
        b.setBatchName("批次");
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItem it = new AppraisalItem();
        it.setId(100L);
        it.setBatchId(1L);
        it.setArchiveId(10L);
        it.setAppraisalResult(com.archive.enums.AppraisalResult.destroy);
        it.setOpinion("到期");
        when(itemMapper.selectList(any())).thenReturn(List.of(it));

        Archive a = archive(10L, "ARC-000010", "凭证A", 2014,
                LocalDate.of(2014, 1, 1), RetentionPeriod.ten_y, LocalDate.of(2024, 12, 31));
        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(a));

        AppraisalBatchDetailResponse resp = service.getBatchDetail(1L);

        assertThat(resp.getItems()).hasSize(1);
        AppraisalBatchDetailResponse.ItemView v = resp.getItems().get(0);
        assertThat(v.getArchiveNo()).isEqualTo("ARC-000010");
        assertThat(v.getAppraisalResult()).isEqualTo("destroy");
        assertThat(v.getOpinion()).isEqualTo("到期");
    }

    @Test
    void getBatchDetail_批次不存在抛404() {
        when(batchMapper.selectById(99L)).thenReturn(null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getBatchDetail(99L))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.NOT_FOUND);
    }
```

import 补充：`import com.archive.entity.AppraisalBatch;` 已存在；追加 `import com.archive.enums.AppraisalBatchStatus;`（如未导入）。

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=AppraisalServiceTest`
Expected: 编译失败（`listBatches`/`getBatchDetail` 不存在）

- [ ] **Step 4: 实现 listBatches / getBatchDetail**

在 `createBatch` 之后新增：

```java
    // ==================== 14.1 查询鉴定批次 ====================

    public PageResult<AppraisalBatchResponse> listBatches(
            String status, Integer categoryId, Integer yearStart, Integer yearEnd,
            int pageNo, int pageSize) {

        QueryWrapper<AppraisalBatch> w = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (categoryId != null) {
            w.eq("category_id", categoryId);
        }
        if (yearStart != null) {
            w.ge("formed_year_start", yearStart);
        }
        if (yearEnd != null) {
            w.le("formed_year_end", yearEnd);
        }
        w.orderByDesc("id");

        Page<AppraisalBatch> page = batchMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<AppraisalBatchResponse> records = page.getRecords().stream()
                .map(this::toBatchResponse)
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private AppraisalBatchResponse toBatchResponse(AppraisalBatch b) {
        AppraisalBatchResponse r = new AppraisalBatchResponse();
        r.setId(b.getId());
        r.setBatchNo(b.getBatchNo());
        r.setBatchName(b.getBatchName());
        r.setCategoryId(b.getCategoryId());
        r.setFormedYearStart(b.getFormedYearStart());
        r.setFormedYearEnd(b.getFormedYearEnd());
        r.setStatus(b.getStatus() != null ? b.getStatus().name() : null);
        r.setCompletedAt(b.getCompletedAt());

        QueryWrapper<AppraisalItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", b.getId());
        Long c = itemMapper.selectCount(iw);
        r.setItemCount(c != null ? c.intValue() : 0);
        return r;
    }

    // ==================== 14.3 获取鉴定批次详情 ====================

    public AppraisalBatchDetailResponse getBatchDetail(Long batchId) {
        AppraisalBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "鉴定批次不存在");
        }

        QueryWrapper<AppraisalItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", batchId);
        iw.orderByAsc("id");
        List<AppraisalItem> items = itemMapper.selectList(iw);

        List<Long> archiveIds = items.stream().map(AppraisalItem::getArchiveId)
                .collect(Collectors.toList());
        Map<Long, Archive> archiveMap = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
                archiveMap.put(a.getId(), a);
            }
        }
        return toDetailFromItems(batch, items, archiveMap);
    }

    /** 由已保存明细 + 档案组装详情（含鉴定结论）。 */
    private AppraisalBatchDetailResponse toDetailFromItems(
            AppraisalBatch batch, List<AppraisalItem> items, Map<Long, Archive> archiveMap) {
        AppraisalBatchDetailResponse resp = new AppraisalBatchDetailResponse();
        resp.setId(batch.getId());
        resp.setBatchNo(batch.getBatchNo());
        resp.setBatchName(batch.getBatchName());
        resp.setCategoryId(batch.getCategoryId());
        resp.setFormedYearStart(batch.getFormedYearStart());
        resp.setFormedYearEnd(batch.getFormedYearEnd());
        resp.setStatus(batch.getStatus() != null ? batch.getStatus().name() : null);
        resp.setCompletedAt(batch.getCompletedAt());

        List<AppraisalBatchDetailResponse.ItemView> views = new ArrayList<>();
        for (AppraisalItem it : items) {
            AppraisalBatchDetailResponse.ItemView v = new AppraisalBatchDetailResponse.ItemView();
            v.setArchiveId(it.getArchiveId());
            v.setAppraisalResult(it.getAppraisalResult() != null ? it.getAppraisalResult().name() : null);
            v.setNewRetentionPeriod(it.getNewRetentionPeriod());
            v.setOpinion(it.getOpinion());
            Archive a = archiveMap.get(it.getArchiveId());
            if (a != null) {
                v.setArchiveNo(a.getArchiveNo());
                v.setTitle(a.getTitle());
                v.setFormedYear(a.getFormedYear());
                v.setRetentionPeriod(a.getRetentionPeriod() != null ? a.getRetentionPeriod().name() : null);
                v.setRetentionUntil(a.getRetentionUntil());
            }
            views.add(v);
        }
        resp.setItems(views);
        return resp;
    }
```

> 注意：`Map` import 已在 createBatch 用到；确认 `com.archive.common.PageResult`、`com.baomidou.mybatisplus.extension.plugins.pagination.Page` 已 import。

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=AppraisalServiceTest`
Expected: PASS（5 个测试通过）

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/AppraisalService.java backend/src/test/java/com/archive/service/AppraisalServiceTest.java
git commit -m "feat(appraisal): 实现鉴定批次列表查询与详情"
```

---

## Task 10: AppraisalService 保存鉴定明细（14.4）

> 当前用户取值用 `safeCurrentUserId()`（try/catch 包裹 `AuthContext`，单测无 SaToken 上下文时返回 null），与 `AuthContext.isAuthenticated` 同款防御，避免单测需要静态 mock。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/AppraisalService.java`
- Modify: `backend/src/test/java/com/archive/service/AppraisalServiceTest.java`

- [ ] **Step 1: 在 import 区补充**

```java
import com.archive.common.AuthContext;
import com.archive.dto.request.AppraisalItemSaveRequest;
import com.archive.enums.AppraisalResult;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void saveItems_非草稿批次抛冲突() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setStatus(AppraisalBatchStatus.completed);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItemSaveRequest req = new AppraisalItemSaveRequest();
        req.setItems(java.util.Collections.emptyList());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.saveItems(1L, req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void saveItems_延长未填保管期限抛校验失败() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItemSaveRequest req = new AppraisalItemSaveRequest();
        AppraisalItemSaveRequest.Item it = new AppraisalItemSaveRequest.Item();
        it.setArchiveId(10L);
        it.setAppraisalResult("extend"); // 缺 newRetentionPeriod
        req.setItems(List.of(it));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.saveItems(1L, req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void saveItems_更新明细结论并返回详情() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setBatchNo("APP-000001");
        b.setBatchName("批次");
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItem existing = new AppraisalItem();
        existing.setId(100L);
        existing.setBatchId(1L);
        existing.setArchiveId(10L);
        // getBatchDetail 内部再查一次
        when(itemMapper.selectOne(any())).thenReturn(existing);
        when(itemMapper.selectList(any())).thenReturn(List.of(existing));
        Archive a = archive(10L, "ARC-000010", "凭证A", 2014,
                LocalDate.of(2014, 1, 1), RetentionPeriod.ten_y, LocalDate.of(2024, 12, 31));
        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(a));

        AppraisalItemSaveRequest req = new AppraisalItemSaveRequest();
        AppraisalItemSaveRequest.Item it = new AppraisalItemSaveRequest.Item();
        it.setArchiveId(10L);
        it.setAppraisalResult("destroy");
        it.setOpinion("到期销毁");
        req.setItems(List.of(it));

        AppraisalBatchDetailResponse resp = service.saveItems(1L, req);

        org.mockito.Mockito.verify(itemMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                i -> ((AppraisalItem) i).getAppraisalResult() == AppraisalResult.destroy));
        assertThat(resp.getItems().get(0).getAppraisalResult()).isEqualTo("destroy");
    }
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=AppraisalServiceTest`
Expected: 编译失败（`saveItems` 不存在）

- [ ] **Step 4: 实现 saveItems 与 safeCurrentUserId**

在 `getBatchDetail` 之后新增：

```java
    // ==================== 14.4 保存鉴定明细 ====================

    @Transactional
    public AppraisalBatchDetailResponse saveItems(Long batchId, AppraisalItemSaveRequest req) {
        AppraisalBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "鉴定批次不存在");
        }
        if (batch.getStatus() != AppraisalBatchStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "非草稿批次不可修改鉴定明细");
        }

        // 校验
        Set<Long> seen = new HashSet<>();
        for (AppraisalItemSaveRequest.Item it : req.getItems()) {
            String result = it.getAppraisalResult();
            if (!"extend".equals(result) && !"destroy".equals(result)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "鉴定结论必须为 extend 或 destroy");
            }
            if ("extend".equals(result)
                    && (it.getNewRetentionPeriod() == null || it.getNewRetentionPeriod().isBlank())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "延长保管必须填写新保管期限");
            }
            if (!seen.add(it.getArchiveId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "鉴定明细档案重复");
            }
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();
        for (AppraisalItemSaveRequest.Item it : req.getItems()) {
            AppraisalItem existing = itemMapper.selectOne(new QueryWrapper<AppraisalItem>()
                    .eq("batch_id", batchId).eq("archive_id", it.getArchiveId()));
            if (existing == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "档案不属于该鉴定批次");
            }
            existing.setAppraisalResult(AppraisalResult.valueOf(it.getAppraisalResult()));
            existing.setOpinion(it.getOpinion());
            if ("extend".equals(it.getAppraisalResult())) {
                existing.setNewRetentionPeriod(it.getNewRetentionPeriod());
            }
            existing.setAppraisedBy(uid);
            existing.setAppraisedAt(now);
            itemMapper.updateById(existing);
        }

        auditService.log("M10", "save_appraisal_items", "appraisal_batch", batchId,
                Map.of("count", req.getItems().size()));

        return getBatchDetail(batchId);
    }

    /** 安全取当前用户 ID，无登录上下文（如单测/异步）时返回 null。 */
    private Long safeCurrentUserId() {
        try {
            return AuthContext.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=AppraisalServiceTest`
Expected: PASS（8 个测试通过）

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/AppraisalService.java backend/src/test/java/com/archive/service/AppraisalServiceTest.java
git commit -m "feat(appraisal): 实现保存鉴定明细与状态校验"
```

---

## Task 11: AppraisalService 完成鉴定（14.5）— 生成销毁清册

> 保管期限推算规则（实现决策）：`permanent` → `retention_until=null`；否则 `years=parse(period)`（10y→10, 30y→30），`new_retention_until = LocalDate.of(formedYear + years, 12, 31)`；`formedYear` 缺失时 `retention_until=null`。`category_snapshot` 暂存分类 ID 字符串（前端按字典映射；如需分类名可后续接入 `CategoryMapper`）。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/AppraisalService.java`
- Modify: `backend/src/test/java/com/archive/service/AppraisalServiceTest.java`

- [ ] **Step 1: 在 import 区补充**

```java
import com.archive.entity.ArchiveChangeLog;
import com.archive.entity.DestructionItem;
import com.archive.entity.DestructionList;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.RetentionPeriod;
import java.time.LocalDate;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void completeBatch_存在未填结论的明细抛校验失败() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItem it = new AppraisalItem();
        it.setArchiveId(10L);
        it.setAppraisalResult(null); // 未填
        when(itemMapper.selectList(any())).thenReturn(List.of(it));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.completeBatch(1L))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void completeBatch_extend更新期限写日志且destroy生成销毁清册快照() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setBatchNo("APP-000001");
        b.setBatchName("2026 到期会计");
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItem ext = new AppraisalItem();
        ext.setArchiveId(10L);
        ext.setAppraisalResult(com.archive.enums.AppraisalResult.extend);
        ext.setNewRetentionPeriod("30y");
        ext.setOpinion("延长保管");
        AppraisalItem des = new AppraisalItem();
        des.setArchiveId(11L);
        des.setAppraisalResult(com.archive.enums.AppraisalResult.destroy);
        des.setOpinion("到期销毁");
        when(itemMapper.selectList(any())).thenReturn(List.of(ext, des));

        Archive a10 = archive(10L, "ARC-000010", "凭证A", 2014,
                LocalDate.of(2014, 1, 1), RetentionPeriod.ten_y, LocalDate.of(2024, 12, 31));
        Archive a11 = archive(11L, "ARC-000011", "凭证B", 2014,
                LocalDate.of(2014, 6, 1), RetentionPeriod.ten_y, LocalDate.of(2024, 6, 1));
        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(a10, a11));
        when(destructionNoUtil.generate()).thenReturn("DES-000001");

        service.completeBatch(1L);

        // extend: 档案期限更新 + 变更日志
        org.mockito.Mockito.verify(archiveMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                a -> a.getId() == 10L && a.getRetentionPeriod() == RetentionPeriod.y30));
        org.mockito.Mockito.verify(changeLogMapper).insert(org.mockito.ArgumentMatchers.argThat(
                l -> l.getArchiveId() == 10L && "retention_period".equals(l.getFieldName())));
        // destroy: 档案 pending_destruction + 销毁清册 + 快照
        org.mockito.Mockito.verify(archiveMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                a -> a.getId() == 11L && a.getLifecycleStatus() == LifecycleStatus.pending_destruction));
        org.mockito.Mockito.verify(destructionListMapper).insert(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(destructionItemMapper).insert(org.mockito.ArgumentMatchers.argThat(
                d -> d.getArchiveId() == 11L && "ARC-000011".equals(d.getArchiveNoSnapshot())
                        && "到期销毁".equals(d.getAppraisalOpinionSnapshot())));
        // 批次完成
        org.mockito.Mockito.verify(batchMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                bb -> bb.getStatus() == AppraisalBatchStatus.completed));
        org.mockito.Mockito.verify(auditService).log(eq("M10"), eq("complete_appraisal"),
                eq("appraisal_batch"), eq(1L), any());
    }
```

import 补充：`import static org.mockito.ArgumentMatchers.eq;`（如未导入）。`RetentionPeriod.y30` / `y10` 为枚举常量名占位——实现前确认 `RetentionPeriod` 枚举确切常量名（`y10`/`y30`/`permanent` 或 `ten_y` 等），测试与 `RetentionPeriod.valueOf("30y")` 必须一致；若枚举名与 DB 串（`10y/30y/permanent`）不同，则在 `completeBatch` 中用值映射而非 `valueOf`。

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=AppraisalServiceTest`
Expected: 编译失败（`completeBatch` 不存在）

- [ ] **Step 4: 实现 completeBatch 与辅助方法**

在 `saveItems` 之后新增：

```java
    // ==================== 14.5 完成鉴定 ====================

    @Transactional
    public AppraisalBatchDetailResponse completeBatch(Long batchId) {
        AppraisalBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "鉴定批次不存在");
        }
        if (batch.getStatus() != AppraisalBatchStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "非草稿批次不可完成鉴定");
        }

        QueryWrapper<AppraisalItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", batchId);
        List<AppraisalItem> items = itemMapper.selectList(iw);
        for (AppraisalItem it : items) {
            if (it.getAppraisalResult() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "存在未填写鉴定结论的明细");
            }
        }

        // 档案批量加载
        List<Long> archiveIds = items.stream().map(AppraisalItem::getArchiveId).collect(Collectors.toList());
        Map<Long, Archive> archMap = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
                archMap.put(a.getId(), a);
            }
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();
        List<AppraisalItem> destroyItems = new ArrayList<>();

        for (AppraisalItem it : items) {
            Archive a = archMap.get(it.getArchiveId());
            if (a == null) {
                continue;
            }
            if (it.getAppraisalResult() == com.archive.enums.AppraisalResult.extend) {
                RetentionPeriod oldPeriod = a.getRetentionPeriod();
                a.setRetentionPeriod(RetentionPeriod.valueOf(it.getNewRetentionPeriod()));
                a.setRetentionUntil(it.getNewRetentionUntil());
                archiveMapper.updateById(a);
                writeChangeLog(a.getId(), "retention_period",
                        oldPeriod != null ? oldPeriod.name() : null,
                        a.getRetentionPeriod() != null ? a.getRetentionPeriod().name() : null,
                        it.getOpinion(), "appraisal", null, uid, now);
            } else {
                a.setLifecycleStatus(LifecycleStatus.pending_destruction);
                archiveMapper.updateById(a);
                destroyItems.add(it);
            }
        }

        // 生成销毁清册（若有销毁结论）
        if (!destroyItems.isEmpty()) {
            DestructionList list = new DestructionList();
            list.setListNo(destructionNoUtil.generate());
            list.setListName(batch.getBatchName() + " 销毁清册");
            list.setAppraisalBatchId(batchId);
            list.setStatus(DestructionListStatus.draft);
            destructionListMapper.insert(list);

            for (AppraisalItem it : destroyItems) {
                Archive a = archMap.get(it.getArchiveId());
                DestructionItem di = new DestructionItem();
                di.setDestructionListId(list.getId());
                di.setArchiveId(a.getId());
                di.setArchiveNoSnapshot(a.getArchiveNo());
                di.setTitleSnapshot(a.getTitle());
                di.setCategorySnapshot(a.getCategoryId() != null ? String.valueOf(a.getCategoryId()) : null);
                di.setRetentionSnapshot(a.getRetentionPeriod() != null ? a.getRetentionPeriod().name() : null);
                di.setSecurityLevelSnapshot(a.getSecurityLevel());
                di.setAppraisalOpinionSnapshot(it.getOpinion());
                di.setFileDeleteStatus("not_started");
                destructionItemMapper.insert(di);
            }
        }

        batch.setStatus(AppraisalBatchStatus.completed);
        batch.setCompletedAt(now);
        batchMapper.updateById(batch);

        auditService.log("M10", "complete_appraisal", "appraisal_batch", batchId,
                Map.of("destroyCount", destroyItems.size()));

        return getBatchDetail(batchId);
    }

    private void writeChangeLog(Long archiveId, String fieldName, String oldValue, String newValue,
                                String reason, String source, Long approvalRequestId,
                                Long changedBy, OffsetDateTime changedAt) {
        ArchiveChangeLog log = new ArchiveChangeLog();
        log.setArchiveId(archiveId);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setChangeReason(reason);
        log.setChangeSource(source);
        log.setApprovalRequestId(approvalRequestId);
        log.setChangedBy(changedBy);
        log.setChangedAt(changedAt);
        changeLogMapper.insert(log);
    }
```

> `newRetentionUntil` 在 14.4 `saveItems` 中未计算（明细仅存 `newRetentionPeriod`）。`completeBatch` extend 分支用 `it.getNewRetentionUntil()`——需在 `saveItems` 中补充计算并写入明细：在 `saveItems` 的 extend 分支追加 `existing.setNewRetentionUntil(computeRetentionUntil(existingArchive 的 formedYear, it.getNewRetentionPeriod()))`。为避免 `saveItems` 重复加载档案，实现时在 `saveItems` 校验后批量加载 `req` 涉及档案，按 `formedYear` 调用下方 `computeRetentionUntil`。

- [ ] **Step 5: 补充 computeRetentionUntil（放入 AppraisalService，并在 saveItems extend 分支调用）**

```java
    /** 由形成年度与保管期限推算到期日；permanent 或无年度返回 null。 */
    private LocalDate computeRetentionUntil(Integer formedYear, String period) {
        if (formedYear == null || period == null) {
            return null;
        }
        if ("permanent".equals(period)) {
            return null;
        }
        String digits = period.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        int years = Integer.parseInt(digits);
        return LocalDate.of(formedYear + years, 12, 31);
    }
```

并在 `saveItems` 的 extend 分支 `existing.setNewRetentionPeriod(...)` 之后追加：

```java
                existing.setNewRetentionPeriod(it.getNewRetentionPeriod());
                // formedYear 取档案形成年度（saveItems 中需加载档案 archMap）
                Archive ea = extendArchMap.get(it.getArchiveId());
                existing.setNewRetentionUntil(computeRetentionUntil(
                        ea != null ? ea.getFormedYear() : null, it.getNewRetentionPeriod()));
```

其中 `extendArchMap` 为 `saveItems` 内批量加载的 `req` 涉及档案映射（仿 `completeBatch` 的 `selectBatchIds` → `archMap`）。

- [ ] **Step 6: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=AppraisalServiceTest`
Expected: PASS（10 个测试通过）

- [ ] **Step 7: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/archive/service/AppraisalService.java backend/src/test/java/com/archive/service/AppraisalServiceTest.java
git commit -m "feat(appraisal): 实现完成鉴定与销毁清册生成"
```

---

## Task 12: DestructionService 骨架 + 查询（15.1）+ 详情（15.2）+ 提交审批（15.3）

**Files:**
- Create: `backend/src/main/java/com/archive/service/DestructionService.java`
- Test: `backend/src/test/java/com/archive/service/DestructionServiceTest.java`

- [ ] **Step 1: 写失败测试（共享 @BeforeEach）**

`backend/src/test/java/com/archive/service/DestructionServiceTest.java`：

```java
package com.archive.service;

import com.archive.dto.request.DestructionSubmitRequest;
import com.archive.dto.response.DestructionListDetailResponse;
import com.archive.dto.response.DestructionListResponse;
import com.archive.common.PageResult;
import com.archive.entity.ApprovalRequest;
import com.archive.entity.BusinessAttachment;
import com.archive.entity.DestructionItem;
import com.archive.entity.DestructionList;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BusinessAttachmentMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.util.DestructionNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DestructionServiceTest {

    private DestructionService service;
    private DestructionListMapper listMapper;
    private DestructionItemMapper itemMapper;
    private ArchiveMapper archiveMapper;
    private ApprovalRequestMapper approvalMapper;
    private BusinessAttachmentMapper attachmentMapper;
    private DestructionNoUtil destructionNoUtil;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        listMapper = mock(DestructionListMapper.class);
        itemMapper = mock(DestructionItemMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        approvalMapper = mock(ApprovalRequestMapper.class);
        attachmentMapper = mock(BusinessAttachmentMapper.class);
        destructionNoUtil = mock(DestructionNoUtil.class);
        auditService = mock(AuditService.class);

        service = new DestructionService(listMapper, itemMapper, archiveMapper,
                approvalMapper, attachmentMapper, destructionNoUtil, auditService);
    }

    @Test
    void listLists_分页并回填明细数() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setListNo("DES-000001");
        l.setListName("清册");
        l.setStatus(DestructionListStatus.draft);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DestructionList> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(l));
        page.setTotal(1L);
        when(listMapper.selectPage(any(), any())).thenReturn(page);
        when(itemMapper.selectCount(any())).thenReturn(4L);

        PageResult<DestructionListResponse> r = service.listLists(null, null, 1, 20);

        assertThat(r.getRecords()).hasSize(1);
        assertThat(r.getRecords().get(0).getItemCount()).isEqualTo(4);
    }

    @Test
    void getListDetail_聚合明细审批与照片() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setListNo("DES-000001");
        l.setListName("清册");
        l.setStatus(DestructionListStatus.pending_destroy);
        l.setApprovalRequestId(7L);
        when(listMapper.selectById(1L)).thenReturn(l);

        DestructionItem it = new DestructionItem();
        it.setArchiveId(10L);
        it.setArchiveNoSnapshot("ARC-000010");
        it.setTitleSnapshot("凭证A");
        when(itemMapper.selectList(any())).thenReturn(List.of(it));

        ApprovalRequest ap = new ApprovalRequest();
        ap.setId(7L);
        ap.setStatus(ApprovalStatus.approved);
        when(approvalMapper.selectById(7L)).thenReturn(ap);

        BusinessAttachment photo = new BusinessAttachment();
        photo.setId(20L);
        photo.setOriginalFilename("p1.jpg");
        photo.setMimeType("image/jpeg");
        photo.setFileSize(1024L);
        when(attachmentMapper.selectList(any())).thenReturn(List.of(photo));

        DestructionListDetailResponse resp = service.getListDetail(1L);

        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getApprovalStatus()).isEqualTo("approved");
        assertThat(resp.getPhotos()).hasSize(1);
        assertThat(resp.getPhotos().get(0).getOriginalFilename()).isEqualTo("p1.jpg");
    }

    @Test
    void submitApproval_创建销毁审批单并置待审批() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setStatus(DestructionListStatus.draft);
        when(listMapper.selectById(1L)).thenReturn(l);

        DestructionItem it = new DestructionItem();
        it.setArchiveId(10L);
        when(itemMapper.selectList(any())).thenReturn(List.of(it));
        com.archive.entity.Archive a = new com.archive.entity.Archive();
        a.setId(10L);
        a.setLifecycleStatus(LifecycleStatus.pending_destruction);
        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(a));

        DestructionSubmitRequest req = new DestructionSubmitRequest();
        req.setReason("到期鉴定后提交销毁");

        ArgumentCaptor<ApprovalRequest> cap = ArgumentCaptor.forClass(ApprovalRequest.class);
        service.submitApproval(1L, req);

        verify(approvalMapper).insert(cap.capture());
        assertThat(cap.getValue().getApprovalType()).isEqualTo(ApprovalType.destruction);
        assertThat(cap.getValue().getTargetType()).isEqualTo("destruction_list");
        assertThat(cap.getValue().getTargetId()).isEqualTo(1L);
        assertThat(cap.getValue().getStatus()).isEqualTo(ApprovalStatus.pending);
        verify(listMapper).updateById(any(DestructionList.class));
    }

    @Test
    void submitApproval_非草稿清册抛冲突() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setStatus(DestructionListStatus.pending_approval);
        when(listMapper.selectById(1L)).thenReturn(l);

        DestructionSubmitRequest req = new DestructionSubmitRequest();
        req.setReason("x");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.submitApproval(1L, req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.BUSINESS_CONFLICT);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=DestructionServiceTest`
Expected: 编译失败（`DestructionService` 不存在）

- [ ] **Step 3: 写 DestructionService（list/detail/submit）**

`backend/src/main/java/com/archive/service/DestructionService.java`：

```java
package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.DestructionSubmitRequest;
import com.archive.dto.response.DestructionListDetailResponse;
import com.archive.dto.response.DestructionListResponse;
import com.archive.entity.ApprovalRequest;
import com.archive.entity.Archive;
import com.archive.entity.BusinessAttachment;
import com.archive.entity.DestructionItem;
import com.archive.entity.DestructionList;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BusinessAttachmentMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 档案销毁服务。
 * 负责销毁清册查询/详情/提交审批/上传现场照片/确认销毁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestructionService {

    private final DestructionListMapper listMapper;
    private final DestructionItemMapper itemMapper;
    private final ArchiveMapper archiveMapper;
    private final ApprovalRequestMapper approvalMapper;
    private final BusinessAttachmentMapper attachmentMapper;
    private final DestructionNoUtil destructionNoUtil;
    private final AuditService auditService;

    // ==================== 15.1 查询销毁清册 ====================

    public PageResult<DestructionListResponse> listLists(
            String status, String keyword, int pageNo, int pageSize) {
        QueryWrapper<DestructionList> w = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like("list_no", keyword).or().like("list_name", keyword));
        }
        w.orderByDesc("id");

        Page<DestructionList> page = listMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<DestructionListResponse> records = page.getRecords().stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private DestructionListResponse toListResponse(DestructionList l) {
        DestructionListResponse r = new DestructionListResponse();
        r.setId(l.getId());
        r.setListNo(l.getListNo());
        r.setListName(l.getListName());
        r.setAppraisalBatchId(l.getAppraisalBatchId());
        r.setStatus(l.getStatus() != null ? l.getStatus().name() : null);
        r.setDestroyedAt(l.getDestroyedAt());
        QueryWrapper<DestructionItem> iw = new QueryWrapper<>();
        iw.eq("destruction_list_id", l.getId());
        Long c = itemMapper.selectCount(iw);
        r.setItemCount(c != null ? c.intValue() : 0);
        return r;
    }

    // ==================== 15.2 获取销毁清册详情 ====================

    public DestructionListDetailResponse getListDetail(Long listId) {
        DestructionList list = listMapper.selectById(listId);
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }

        QueryWrapper<DestructionItem> iw = new QueryWrapper<>();
        iw.eq("destruction_list_id", listId).orderByAsc("id");
        List<DestructionItem> items = itemMapper.selectList(iw);

        String approvalStatus = null;
        if (list.getApprovalRequestId() != null) {
            ApprovalRequest ap = approvalMapper.selectById(list.getApprovalRequestId());
            if (ap != null) {
                approvalStatus = ap.getStatus() != null ? ap.getStatus().name() : null;
            }
        }

        QueryWrapper<BusinessAttachment> pw = new QueryWrapper<>();
        pw.eq("business_type", "destruction_list")
                .eq("business_id", listId)
                .eq("attachment_type", "destruction_photo");
        List<BusinessAttachment> photos = attachmentMapper.selectList(pw);

        return toDetail(list, items, approvalStatus, photos);
    }

    private DestructionListDetailResponse toDetail(DestructionList list, List<DestructionItem> items,
                                                   String approvalStatus, List<BusinessAttachment> photos) {
        DestructionListDetailResponse resp = new DestructionListDetailResponse();
        resp.setId(list.getId());
        resp.setListNo(list.getListNo());
        resp.setListName(list.getListName());
        resp.setAppraisalBatchId(list.getAppraisalBatchId());
        resp.setStatus(list.getStatus() != null ? list.getStatus().name() : null);
        resp.setApprovalRequestId(list.getApprovalRequestId());
        resp.setApprovalStatus(approvalStatus);
        resp.setDestroyMethod(list.getDestroyMethod() != null ? list.getDestroyMethod().name() : null);
        resp.setSupervisorName1(list.getSupervisorName1());
        resp.setSupervisorName2(list.getSupervisorName2());
        resp.setDestroyNote(list.getDestroyNote());
        resp.setDestroyedAt(list.getDestroyedAt());

        resp.setItems(items.stream().map(it -> {
            DestructionListDetailResponse.ItemView v = new DestructionListDetailResponse.ItemView();
            v.setArchiveId(it.getArchiveId());
            v.setArchiveNoSnapshot(it.getArchiveNoSnapshot());
            v.setTitleSnapshot(it.getTitleSnapshot());
            v.setCategorySnapshot(it.getCategorySnapshot());
            v.setPageCountSnapshot(it.getPageCountSnapshot());
            v.setRetentionSnapshot(it.getRetentionSnapshot());
            v.setSecurityLevelSnapshot(it.getSecurityLevelSnapshot());
            v.setAppraisalOpinionSnapshot(it.getAppraisalOpinionSnapshot());
            v.setFileDeleteStatus(it.getFileDeleteStatus());
            return v;
        }).collect(Collectors.toList()));

        resp.setPhotos(photos.stream().map(p -> {
            DestructionListDetailResponse.PhotoView pv = new DestructionListDetailResponse.PhotoView();
            pv.setId(p.getId());
            pv.setOriginalFilename(p.getOriginalFilename());
            pv.setMimeType(p.getMimeType());
            pv.setFileSize(p.getFileSize());
            pv.setSha256(p.getSha256());
            return pv;
        }).collect(Collectors.toList()));
        return resp;
    }

    // ==================== 15.3 提交销毁审批 ====================

    @Transactional
    public DestructionListDetailResponse submitApproval(Long listId, DestructionSubmitRequest req) {
        DestructionList list = listMapper.selectById(listId);
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }
        if (list.getStatus() != DestructionListStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "非草稿清册不可提交审批");
        }

        QueryWrapper<DestructionItem> iw = new QueryWrapper<>();
        iw.eq("destruction_list_id", listId);
        List<DestructionItem> items = itemMapper.selectList(iw);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "销毁清册无明细");
        }
        List<Long> archiveIds = items.stream().map(DestructionItem::getArchiveId).collect(Collectors.toList());
        for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
            if (a.getLifecycleStatus() != LifecycleStatus.pending_destruction) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "销毁明细存在非待销毁档案");
            }
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();

        ApprovalRequest ap = new ApprovalRequest();
        ap.setApprovalType(ApprovalType.destruction);
        ap.setTargetType("destruction_list");
        ap.setTargetId(listId);
        ap.setReason(req.getReason());
        ap.setStatus(ApprovalStatus.pending);
        ap.setSubmittedBy(uid);
        ap.setSubmittedAt(now);
        approvalMapper.insert(ap);

        list.setApprovalRequestId(ap.getId());
        list.setStatus(DestructionListStatus.pending_approval);
        listMapper.updateById(list);

        auditService.log("M11", "submit_destruction_approval", "destruction_list", listId,
                Map.of("approvalRequestId", ap.getId(), "reason", req.getReason()));

        return getListDetail(listId);
    }

    private Long safeCurrentUserId() {
        try {
            return AuthContext.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=DestructionServiceTest`
Expected: PASS（4 个测试通过）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/service/DestructionService.java backend/src/test/java/com/archive/service/DestructionServiceTest.java
git commit -m "feat(appraisal): 实现销毁清册查询详情与提交审批"
```

---

## Task 13: DestructionService 确认销毁（15.5）

> 构造器追加 `ArchiveFileMapper`、`ArchiveBoxItemMapper`、`ArchiveBoxMapper` 三个依赖；测试 `@BeforeEach` 同步 mock 并传入新构造器。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/DestructionService.java`
- Modify: `backend/src/test/java/com/archive/service/DestructionServiceTest.java`

- [ ] **Step 1: 在 DestructionService import 区补充**

```java
import com.archive.dto.request.DestructionDestroyRequest;
import com.archive.entity.ArchiveBox;
import com.archive.entity.ArchiveBoxItem;
import com.archive.entity.ArchiveFile;
import com.archive.enums.ConditionStatus;
import com.archive.enums.DestroyMethod;
import com.archive.enums.FileStatus;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveBoxMapper;
import com.archive.mapper.ArchiveFileMapper;
import java.util.ArrayList;
```

- [ ] **Step 2: 构造器追加 3 个 Mapper 字段**

在类字段区追加，并在 `@RequiredArgsConstructor` 生成的构造器中追加（构造器参数顺序：`..., archiveFileMapper, archiveBoxItemMapper, archiveBoxMapper`）：

```java
    private final ArchiveFileMapper archiveFileMapper;
    private final ArchiveBoxItemMapper archiveBoxItemMapper;
    private final ArchiveBoxMapper archiveBoxMapper;
```

- [ ] **Step 3: 测试 @BeforeEach 追加 mock 并更新构造调用**

```java
    private com.archive.mapper.ArchiveFileMapper archiveFileMapper;
    private com.archive.mapper.ArchiveBoxItemMapper archiveBoxItemMapper;
    private com.archive.mapper.ArchiveBoxMapper archiveBoxMapper;
```

在 `setup()` 内追加：

```java
        archiveFileMapper = mock(com.archive.mapper.ArchiveFileMapper.class);
        archiveBoxItemMapper = mock(com.archive.mapper.ArchiveBoxItemMapper.class);
        archiveBoxMapper = mock(com.archive.mapper.ArchiveBoxMapper.class);

        service = new DestructionService(listMapper, itemMapper, archiveMapper,
                approvalMapper, attachmentMapper, archiveFileMapper, archiveBoxItemMapper,
                archiveBoxMapper, destructionNoUtil, auditService);
```

- [ ] **Step 4: 新增测试**

```java
    @Test
    void confirmDestroy_非待销毁清册抛冲突() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setStatus(DestructionListStatus.pending_approval);
        when(listMapper.selectById(1L)).thenReturn(l);

        com.archive.dto.request.DestructionDestroyRequest req =
                new com.archive.dto.request.DestructionDestroyRequest();
        req.setDestroyMethod("shredding");
        req.setSupervisorName1("甲");
        req.setSupervisorName2("乙");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirmDestroy(1L, req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void confirmDestroy_置销毁并解除盒关系标记电子文件删除() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setStatus(DestructionListStatus.pending_destroy);
        l.setApprovalRequestId(7L);
        when(listMapper.selectById(1L)).thenReturn(l);

        ApprovalRequest ap = new ApprovalRequest();
        ap.setId(7L);
        ap.setStatus(ApprovalStatus.approved);
        when(approvalMapper.selectById(7L)).thenReturn(ap);

        DestructionItem it = new DestructionItem();
        it.setId(50L);
        it.setArchiveId(10L);
        when(itemMapper.selectList(any())).thenReturn(List.of(it));

        com.archive.entity.Archive a = new com.archive.entity.Archive();
        a.setId(10L);
        a.setLifecycleStatus(LifecycleStatus.pending_destruction);
        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(a));

        com.archive.entity.ArchiveFile f = new com.archive.entity.ArchiveFile();
        f.setId(80L);
        f.setArchiveId(10L);
        when(archiveFileMapper.selectList(any())).thenReturn(List.of(f));

        com.archive.entity.ArchiveBoxItem bi = new com.archive.entity.ArchiveBoxItem();
        bi.setId(90L);
        bi.setBoxId(5L);
        bi.setArchiveId(10L);
        when(archiveBoxItemMapper.selectList(any())).thenReturn(List.of(bi));
        com.archive.entity.ArchiveBox box = new com.archive.entity.ArchiveBox();
        box.setId(5L);
        box.setUsedCount(3);
        when(archiveBoxMapper.selectById(5L)).thenReturn(box);

        com.archive.dto.request.DestructionDestroyRequest req =
                new com.archive.dto.request.DestructionDestroyRequest();
        req.setDestroyMethod("shredding");
        req.setSupervisorName1("甲");
        req.setSupervisorName2("乙");
        req.setDestroyNote("现场粉碎");

        service.confirmDestroy(1L, req);

        // 档案置 destroyed
        org.mockito.Mockito.verify(archiveMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                x -> x.getId() == 10L && x.getLifecycleStatus() == LifecycleStatus.destroyed));
        // 电子文件标记删除
        org.mockito.Mockito.verify(archiveFileMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                x -> ((com.archive.entity.ArchiveFile) x).getFileStatus() == FileStatus.deleted));
        // 盒关系解除 + 盒内数量 -1
        org.mockito.Mockito.verify(archiveBoxItemMapper).delete(any());
        org.mockito.Mockito.verify(archiveBoxMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                b -> b.getUsedCount() == 2));
        // 清册置 destroyed
        org.mockito.Mockito.verify(listMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                ll -> ll.getStatus() == DestructionListStatus.destroyed
                        && ll.getDestroyMethod() == DestroyMethod.shredding));
        org.mockito.Mockito.verify(auditService).log(eq("M11"), eq("destroy"),
                eq("destruction_list"), eq(1L), any());
    }
```

import 补充：`import static org.mockito.ArgumentMatchers.eq;`（如未导入）。

> `FileStatus.deleted` 常量名以实际 `FileStatus.java` 为准（可能 `deleted`/`DELETED`）。

- [ ] **Step 5: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=DestructionServiceTest`
Expected: 编译失败（`confirmDestroy` 不存在）

- [ ] **Step 6: 实现 confirmDestroy**

在 `submitApproval` 之后新增：

```java
    // ==================== 15.5 确认销毁 ====================

    @Transactional
    public DestructionListDetailResponse confirmDestroy(Long listId, DestructionDestroyRequest req) {
        DestructionList list = listMapper.selectById(listId);
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }
        if (list.getStatus() != DestructionListStatus.pending_destroy) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "非待销毁清册不可确认销毁");
        }
        if (list.getApprovalRequestId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "销毁清册未关联审批单");
        }
        ApprovalRequest ap = approvalMapper.selectById(list.getApprovalRequestId());
        if (ap == null || ap.getStatus() != ApprovalStatus.approved) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "销毁审批未通过，不可确认销毁");
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();

        QueryWrapper<DestructionItem> iw = new QueryWrapper<>();
        iw.eq("destruction_list_id", listId);
        List<DestructionItem> items = itemMapper.selectList(iw);
        List<Long> archiveIds = items.stream().map(DestructionItem::getArchiveId)
                .collect(Collectors.toList());

        Map<Long, Archive> archMap = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
                archMap.put(a.getId(), a);
            }
        }

        // 1. 档案置 destroyed
        for (DestructionItem it : items) {
            Archive a = archMap.get(it.getArchiveId());
            if (a != null) {
                a.setLifecycleStatus(LifecycleStatus.destroyed);
                a.setConditionStatus(ConditionStatus.destroyed);
                archiveMapper.updateById(a);
            }
        }

        // 2. 电子文件标记删除
        if (!archiveIds.isEmpty()) {
            QueryWrapper<ArchiveFile> fw = new QueryWrapper<>();
            fw.in("archive_id", archiveIds).ne("file_status", "deleted");
            List<ArchiveFile> files = archiveFileMapper.selectList(fw);
            for (ArchiveFile f : files) {
                f.setFileStatus(FileStatus.deleted);
                f.setDeletedAt(now);
                f.setDeletedBy(uid);
                archiveFileMapper.updateById(f);
            }
            for (DestructionItem it : items) {
                it.setFileDeleteStatus("deleted");
                it.setFileDeletedAt(now);
                itemMapper.updateById(it);
            }
        }

        // 3. 纸质：解除盒内关系，盒内数量递减
        if (!archiveIds.isEmpty()) {
            QueryWrapper<ArchiveBoxItem> bw = new QueryWrapper<>();
            bw.in("archive_id", archiveIds);
            List<ArchiveBoxItem> boxItems = archiveBoxItemMapper.selectList(bw);
            Map<Long, Integer> boxDecr = new HashMap<>();
            for (ArchiveBoxItem bi : boxItems) {
                boxDecr.merge(bi.getBoxId(), 1, Integer::sum);
            }
            archiveBoxItemMapper.delete(bw);
            for (Map.Entry<Long, Integer> e : boxDecr.entrySet()) {
                ArchiveBox box = archiveBoxMapper.selectById(e.getKey());
                if (box != null) {
                    int used = box.getUsedCount() != null ? box.getUsedCount() : 0;
                    box.setUsedCount(Math.max(0, used - e.getValue()));
                    archiveBoxMapper.updateById(box);
                }
            }
        }

        // 4. 清册置 destroyed
        list.setStatus(DestructionListStatus.destroyed);
        list.setDestroyedAt(now);
        list.setDestroyMethod(DestroyMethod.valueOf(req.getDestroyMethod()));
        list.setSupervisorName1(req.getSupervisorName1());
        list.setSupervisorName2(req.getSupervisorName2());
        list.setDestroyNote(req.getDestroyNote());
        listMapper.updateById(list);

        auditService.log("M11", "destroy", "destruction_list", listId,
                Map.of("destroyMethod", req.getDestroyMethod(), "itemCount", items.size()));

        return getListDetail(listId);
    }
```

> 销毁清册、明细快照、审批单、审计、档案元数据均保留（不删除），满足「永久保留」要求。

- [ ] **Step 7: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=DestructionServiceTest`
Expected: PASS（6 个测试通过）

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/archive/service/DestructionService.java backend/src/test/java/com/archive/service/DestructionServiceTest.java
git commit -m "feat(appraisal): 实现确认销毁与档案盒电子文件处置"
```

---

## Task 14: DestructionService 上传销毁现场照片（15.4）

> **实现前必读**：本任务依赖 ClamAV 扫描 + MinIO 上传 + SHA-256 摘要的现有链路。开始编码前先完整阅读 `service/StagingFileService.java`（接口 7.3 上传暂存电子文件），复用其相同的扫描/上传/摘要处理：若已抽取为可复用方法则直接调用；否则在本任务中以相同逻辑内联（同一 `MinioService`、同一 ClamAV 客户端 Bean、同一扩展名/大小/MIME 校验来源 `system_configs`）。
>
> 本计划给出附件实体创建与业务编排的完整代码；扫描/上传调用以「复用 StagingFileService 同款」标注，实现时对齐其确切方法签名。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/DestructionService.java`
- Modify: `backend/src/test/java/com/archive/service/DestructionServiceTest.java`

- [ ] **Step 1: 构造器追加 MinIO/ClamAV 协作依赖**

按 `StagingFileService` 实际依赖注入（典型为 `MinioService` + ClamAV 客户端 Bean + `SystemConfigService` 或直接读 `system_configs`）。字段示例（以实际为准）：

```java
    private final com.archive.service.MinioService minioService;
    // ClamAV 客户端：按 StagingFileService 实际 Bean 注入（如 ClamavClient / IClamAVClient）
```

测试 `@BeforeEach` 同步 mock 这些依赖并加入构造器参数。

- [ ] **Step 2: 新增测试（覆盖校验分支与附件落库）**

```java
    @Test
    void uploadPhotos_清册不存在抛404() {
        when(listMapper.selectById(99L)).thenReturn(null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.uploadPhotos(99L, new org.springframework.web.multipart.MultipartFile[0]))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.NOT_FOUND);
    }

    @Test
    void uploadPhotos_扫描通过后创建destruction_photo附件() throws Exception {
        DestructionList l = new DestructionList();
        l.setId(1L);
        when(listMapper.selectById(1L)).thenReturn(l);

        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("scene1.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(2048L);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});

        // 复用 StagingFileService 同款：minioService 上传成功返回 objectKey、ClamAV 扫描通过
        // 具体 stub 以实际协作依赖为准（如 when(minioService.upload(...)).thenReturn("destruction/1/xxx")）

        java.util.List<com.archive.dto.response.DestructionListDetailResponse.PhotoView> result =
                service.uploadPhotos(1L, new org.springframework.web.multipart.MultipartFile[]{file});

        org.mockito.Mockito.verify(attachmentMapper).insert(org.mockito.ArgumentMatchers.argThat(
                a -> "destruction_list".equals(((com.archive.entity.BusinessAttachment) a).getBusinessType())
                        && "destruction_photo".equals(((com.archive.entity.BusinessAttachment) a).getAttachmentType())
                        && ((com.archive.entity.BusinessAttachment) a).getBusinessId() == 1L));
        assertThat(result).hasSize(1);
    }
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=DestructionServiceTest`
Expected: 编译失败（`uploadPhotos` 不存在）

- [ ] **Step 4: 实现 uploadPhotos**

在 `confirmDestroy` 之后新增（扫描/上传段标注「复用 StagingFileService 同款」）：

```java
    // ==================== 15.4 上传销毁现场照片 ====================

    @Transactional
    public List<DestructionListDetailResponse.PhotoView> uploadPhotos(
            Long listId, org.springframework.web.multipart.MultipartFile[] files) {

        DestructionList list = listMapper.selectById(listId);
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }
        if (files == null || files.length == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "未上传任何照片");
        }

        // 读取上传白名单/大小上限（复用 system_configs，与 StagingFileService 同款）
        // String allowedExt = ...; long maxBytes = ...;

        List<DestructionListDetailResponse.PhotoView> views = new ArrayList<>();
        for (org.springframework.web.multipart.MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String ext = extractExt(file.getOriginalFilename());
            String mime = file.getContentType();
            // 1. 格式/大小/MIME 校验（与 StagingFileService 一致），失败抛 UNSUPPORTED_MEDIA_TYPE / PAYLOAD_TOO_LARGE / VALIDATION_FAILED
            // 2. 计算 SHA-256
            //    String sha = DigestUtils.sha256Hex(file.getBytes());  // 实际工具以 StagingFileService 为准
            // 3. ClamAV 扫描：不可用/超时/失败/命中病毒 → 拒绝并仅写 audit（不创建附件），与 StagingFileService 一致
            //    clamavClient.scan(file.getBytes()); // 失败抛 BusinessException(VALIDATION_FAILED, "文件未通过病毒扫描")
            // 4. MinIO 上传
            //    String objectKey = minioService.upload("destruction/" + listId + "/", file);

            BusinessAttachment att = new BusinessAttachment();
            att.setBusinessType("destruction_list");
            att.setBusinessId(listId);
            att.setAttachmentType("destruction_photo");
            att.setOriginalFilename(file.getOriginalFilename());
            att.setFileExt(ext);
            att.setMimeType(mime);
            att.setFileSize(file.getSize());
            // att.setSha256(sha);
            // att.setObjectKey(objectKey);
            // att.setBucketName(minioService.getBucket());
            attachmentMapper.insert(att);

            DestructionListDetailResponse.PhotoView v = new DestructionListDetailResponse.PhotoView();
            v.setId(att.getId());
            v.setOriginalFilename(att.getOriginalFilename());
            v.setMimeType(att.getMimeType());
            v.setFileSize(att.getFileSize());
            v.setSha256(att.getSha256());
            views.add(v);
        }

        auditService.log("M11", "upload_destruction_photo", "destruction_list", listId,
                Map.of("count", views.size()));

        return views;
    }

    private String extractExt(String filename) {
        if (filename == null) {
            return null;
        }
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1).toLowerCase() : null;
    }
```

> 上述注释段（SHA-256 / ClamAV / MinIO）在实现时替换为对 `StagingFileService` 同款协作依赖的真实调用，删除注释，补全 `sha256/objectKey/bucketName` 赋值。不得保留为注释交付。

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=DestructionServiceTest`
Expected: PASS（8 个测试通过）

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/DestructionService.java backend/src/test/java/com/archive/service/DestructionServiceTest.java
git commit -m "feat(appraisal): 实现销毁现场照片上传与附件落库"
```

---

## Task 15: ApprovalService 审批工作台（13.1-13.4）

> 审批生效采用 Approach A：`approve` 单事务内按 `approval_type` 窄变更目标表 + 写 `archive_change_logs` + 审计。审批通过密级/开放调整时，`change_log.approval_request_id` 回填审批单 ID，与发起端（M05）留痕一致。

**Files:**
- Create: `backend/src/main/java/com/archive/service/ApprovalService.java`
- Test: `backend/src/test/java/com/archive/service/ApprovalServiceTest.java`

- [ ] **Step 1: 写失败测试**

`backend/src/test/java/com/archive/service/ApprovalServiceTest.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.ApprovalOpinionRequest;
import com.archive.dto.response.ApprovalDetailResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveChangeLog;
import com.archive.entity.ApprovalRequest;
import com.archive.entity.DestructionList;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.DestructionListMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApprovalServiceTest {

    private ApprovalService service;
    private ApprovalRequestMapper approvalMapper;
    private ArchiveMapper archiveMapper;
    private DestructionListMapper destructionListMapper;
    private ArchiveChangeLogMapper changeLogMapper;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        approvalMapper = mock(ApprovalRequestMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        destructionListMapper = mock(DestructionListMapper.class);
        changeLogMapper = mock(ArchiveChangeLogMapper.class);
        auditService = mock(AuditService.class);
        service = new ApprovalService(approvalMapper, archiveMapper, destructionListMapper,
                changeLogMapper, auditService);
    }

    @Test
    void approve_销毁审批通过置清册待销毁() {
        ApprovalRequest ap = pending(ApprovalType.destruction, "destruction_list", 1L);
        when(approvalMapper.selectById(7L)).thenReturn(ap);
        DestructionList list = new DestructionList();
        list.setId(1L);
        list.setStatus(DestructionListStatus.pending_approval);
        when(destructionListMapper.selectById(1L)).thenReturn(list);

        ApprovalOpinionRequest req = new ApprovalOpinionRequest();
        req.setOpinion("同意");
        service.approve(7L, req);

        verify(approvalMapper).updateById(argThat(a -> a.getStatus() == ApprovalStatus.approved));
        verify(destructionListMapper).updateById(argThat(l -> l.getStatus() == DestructionListStatus.pending_destroy));
        verify(auditService).log(eq("M08"), eq("approve"), eq("approval_request"), eq(7L), any());
    }

    @Test
    void approve_密级调整生效档案并写变更日志() {
        ApprovalRequest ap = pending(ApprovalType.security_adjust, "archive", 10L);
        ap.setOldValue("1");
        ap.setNewValue("2");
        when(approvalMapper.selectById(7L)).thenReturn(ap);
        Archive a = new Archive();
        a.setId(10L);
        a.setSecurityLevel(1);
        a.setLifecycleStatus(LifecycleStatus.normal);
        when(archiveMapper.selectById(10L)).thenReturn(a);

        ApprovalOpinionRequest req = new ApprovalOpinionRequest();
        req.setOpinion("同意调整");
        service.approve(7L, req);

        verify(archiveMapper).updateById(argThat(x -> x.getSecurityLevel() == 2));
        verify(changeLogMapper).insert(argThat(l -> l.getArchiveId() == 10L
                && "security_level".equals(l.getFieldName())
                && l.getApprovalRequestId() == 7L));
    }

    @Test
    void approve_非待审批抛冲突() {
        ApprovalRequest ap = new ApprovalRequest();
        ap.setId(7L);
        ap.setStatus(ApprovalStatus.approved);
        when(approvalMapper.selectById(7L)).thenReturn(ap);

        assertThatThrownBy(() -> service.approve(7L, new ApprovalOpinionRequest()))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void reject_退回销毁清册回草稿() {
        ApprovalRequest ap = pending(ApprovalType.destruction, "destruction_list", 1L);
        when(approvalMapper.selectById(7L)).thenReturn(ap);
        DestructionList list = new DestructionList();
        list.setId(1L);
        list.setStatus(DestructionListStatus.pending_approval);
        when(destructionListMapper.selectById(1L)).thenReturn(list);

        ApprovalOpinionRequest req = new ApprovalOpinionRequest();
        req.setOpinion("依据不足");
        service.reject(7L, req);

        verify(approvalMapper).updateById(argThat(a -> a.getStatus() == ApprovalStatus.rejected
                && "依据不足".equals(a.getApprovalOpinion())));
        verify(destructionListMapper).updateById(argThat(l -> l.getStatus() == DestructionListStatus.draft));
    }

    @Test
    void reject_退回意见为空抛校验失败() {
        ApprovalRequest ap = pending(ApprovalType.security_adjust, "archive", 10L);
        when(approvalMapper.selectById(7L)).thenReturn(ap);

        ApprovalOpinionRequest req = new ApprovalOpinionRequest(); // opinion 空
        assertThatThrownBy(() -> service.reject(7L, req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    private ApprovalRequest pending(ApprovalType type, String targetType, Long targetId) {
        ApprovalRequest ap = new ApprovalRequest();
        ap.setId(7L);
        ap.setApprovalType(type);
        ap.setTargetType(targetType);
        ap.setTargetId(targetId);
        ap.setStatus(ApprovalStatus.pending);
        return ap;
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=ApprovalServiceTest`
Expected: 编译失败（`ApprovalService` 不存在）

- [ ] **Step 3: 写 ApprovalService**

`backend/src/main/java/com/archive/service/ApprovalService.java`：

```java
package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.ApprovalOpinionRequest;
import com.archive.dto.response.ApprovalDetailResponse;
import com.archive.dto.response.ApprovalResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveChangeLog;
import com.archive.entity.ApprovalRequest;
import com.archive.entity.DestructionList;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.DestructionListMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批工作台服务。
 * 统一受理密级调整/开放调整/销毁三类审批，按类型在单事务内生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestMapper approvalMapper;
    private final ArchiveMapper archiveMapper;
    private final DestructionListMapper destructionListMapper;
    private final ArchiveChangeLogMapper changeLogMapper;
    private final AuditService auditService;

    // ==================== 13.1 查询审批单 ====================

    public PageResult<ApprovalResponse> listApprovals(
            String approvalType, String status, String keyword, int pageNo, int pageSize) {
        QueryWrapper<ApprovalRequest> w = new QueryWrapper<>();
        if (approvalType != null && !approvalType.isBlank()) {
            w.eq("approval_type", approvalType);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like("reason", keyword);
        }
        w.orderByDesc("submitted_at");

        Page<ApprovalRequest> page = approvalMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<ApprovalResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private ApprovalResponse toResponse(ApprovalRequest ap) {
        ApprovalResponse r = new ApprovalResponse();
        r.setId(ap.getId());
        r.setApprovalType(ap.getApprovalType() != null ? ap.getApprovalType().name() : null);
        r.setTargetType(ap.getTargetType());
        r.setTargetId(ap.getTargetId());
        r.setStatus(ap.getStatus() != null ? ap.getStatus().name() : null);
        r.setReason(ap.getReason());
        r.setSubmittedBy(ap.getSubmittedBy());
        r.setSubmittedAt(ap.getSubmittedAt());
        r.setApprovalOpinion(ap.getApprovalOpinion());
        r.setTargetSummary(resolveTargetSummary(ap));
        return r;
    }

    // ==================== 13.2 获取审批详情 ====================

    public ApprovalDetailResponse getApprovalDetail(Long approvalId) {
        ApprovalRequest ap = approvalMapper.selectById(approvalId);
        if (ap == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审批单不存在");
        }
        ApprovalDetailResponse r = new ApprovalDetailResponse();
        r.setId(ap.getId());
        r.setApprovalType(ap.getApprovalType() != null ? ap.getApprovalType().name() : null);
        r.setTargetType(ap.getTargetType());
        r.setTargetId(ap.getTargetId());
        r.setEvidenceArchiveId(ap.getEvidenceArchiveId());
        r.setOldValue(ap.getOldValue());
        r.setNewValue(ap.getNewValue());
        r.setReason(ap.getReason());
        r.setStatus(ap.getStatus() != null ? ap.getStatus().name() : null);
        r.setSubmittedBy(ap.getSubmittedBy());
        r.setSubmittedAt(ap.getSubmittedAt());
        r.setApprovedBy(ap.getApprovedBy());
        r.setApprovedAt(ap.getApprovedAt());
        r.setApprovalOpinion(ap.getApprovalOpinion());
        r.setTargetSummary(resolveTargetSummary(ap));
        if (ap.getEvidenceArchiveId() != null) {
            Archive ev = archiveMapper.selectById(ap.getEvidenceArchiveId());
            if (ev != null) {
                r.setEvidenceArchiveNo(ev.getArchiveNo());
            }
        }
        return r;
    }

    private String resolveTargetSummary(ApprovalRequest ap) {
        if (ap.getTargetId() == null) {
            return null;
        }
        if ("archive".equals(ap.getTargetType())) {
            Archive a = archiveMapper.selectById(ap.getTargetId());
            return a != null ? a.getArchiveNo() : null;
        }
        if ("destruction_list".equals(ap.getTargetType())) {
            DestructionList l = destructionListMapper.selectById(ap.getTargetId());
            return l != null ? l.getListNo() : null;
        }
        return null;
    }

    // ==================== 13.3 审批通过 ====================

    @Transactional
    public ApprovalDetailResponse approve(Long approvalId, ApprovalOpinionRequest req) {
        ApprovalRequest ap = approvalMapper.selectById(approvalId);
        if (ap == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审批单不存在");
        }
        if (ap.getStatus() != ApprovalStatus.pending) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "审批单非待审批状态");
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();

        switch (ap.getApprovalType()) {
            case security_adjust -> applySecurityAdjust(ap, uid, now);
            case open_adjust -> applyOpenAdjust(ap, uid, now);
            case destruction -> applyDestructionApprove(ap);
        }

        ap.setStatus(ApprovalStatus.approved);
        ap.setApprovedBy(uid);
        ap.setApprovedAt(now);
        ap.setApprovalOpinion(req.getOpinion());
        approvalMapper.updateById(ap);

        auditService.log("M08", "approve", "approval_request", approvalId,
                Map.of("type", ap.getApprovalType().name()));
        return getApprovalDetail(approvalId);
    }

    private void applySecurityAdjust(ApprovalRequest ap, Long uid, OffsetDateTime now) {
        Archive a = loadEffectableArchive(ap);
        Integer oldLevel = a.getSecurityLevel();
        a.setSecurityLevel(parseIntOrNull(ap.getNewValue()));
        archiveMapper.updateById(a);
        writeChangeLog(a.getId(), "security_level",
                oldLevel != null ? String.valueOf(oldLevel) : null, ap.getNewValue(),
                ap.getReason(), "security_adjust", ap.getId(), uid, now);
    }

    private void applyOpenAdjust(ApprovalRequest ap, Long uid, OffsetDateTime now) {
        Archive a = loadEffectableArchive(ap);
        String oldStatus = a.getOpenStatus();
        a.setOpenStatus(ap.getNewValue());
        archiveMapper.updateById(a);
        writeChangeLog(a.getId(), "open_status", oldStatus, ap.getNewValue(),
                ap.getReason(), "open_adjust", ap.getId(), uid, now);
    }

    private void applyDestructionApprove(ApprovalRequest ap) {
        DestructionList list = destructionListMapper.selectById(ap.getTargetId());
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }
        if (list.getStatus() != DestructionListStatus.pending_approval) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "销毁清册当前状态不允许审批生效");
        }
        list.setStatus(DestructionListStatus.pending_destroy);
        destructionListMapper.updateById(list);
    }

    /** 加载可生效的档案：必须存在且未销毁。 */
    private Archive loadEffectableArchive(ApprovalRequest ap) {
        Archive a = archiveMapper.selectById(ap.getTargetId());
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标档案不存在");
        }
        if (a.getLifecycleStatus() == LifecycleStatus.destroyed) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标档案已销毁，审批不可生效");
        }
        return a;
    }

    // ==================== 13.4 审批退回 ====================

    @Transactional
    public ApprovalDetailResponse reject(Long approvalId, ApprovalOpinionRequest req) {
        if (req.getOpinion() == null || req.getOpinion().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "退回意见必填");
        }
        ApprovalRequest ap = approvalMapper.selectById(approvalId);
        if (ap == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审批单不存在");
        }
        if (ap.getStatus() != ApprovalStatus.pending) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "审批单非待审批状态");
        }

        ap.setStatus(ApprovalStatus.rejected);
        ap.setApprovedBy(safeCurrentUserId());
        ap.setApprovedAt(OffsetDateTime.now());
        ap.setApprovalOpinion(req.getOpinion());
        approvalMapper.updateById(ap);

        // 销毁清册退回可重新编辑
        if (ap.getApprovalType() == ApprovalType.destruction) {
            DestructionList list = destructionListMapper.selectById(ap.getTargetId());
            if (list != null && list.getStatus() == DestructionListStatus.pending_approval) {
                list.setStatus(DestructionListStatus.draft);
                destructionListMapper.updateById(list);
            }
        }

        auditService.log("M08", "reject", "approval_request", approvalId,
                Map.of("opinion", req.getOpinion()));
        return getApprovalDetail(approvalId);
    }

    private void writeChangeLog(Long archiveId, String fieldName, String oldValue, String newValue,
                                String reason, String source, Long approvalRequestId,
                                Long changedBy, OffsetDateTime changedAt) {
        ArchiveChangeLog log = new ArchiveChangeLog();
        log.setArchiveId(archiveId);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setChangeReason(reason);
        log.setChangeSource(source);
        log.setApprovalRequestId(approvalRequestId);
        log.setChangedBy(changedBy);
        log.setChangedAt(changedAt);
        changeLogMapper.insert(log);
    }

    private Long safeCurrentUserId() {
        try {
            return AuthContext.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=ApprovalServiceTest`
Expected: PASS（6 个测试通过）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/service/ApprovalService.java backend/src/test/java/com/archive/service/ApprovalServiceTest.java
git commit -m "feat(appraisal): 实现审批工作台查询与通过退回生效"
```

---

## Task 16: SystemConfigService（22.8-22.10）+ RoleService（22.7）

**Files:**
- Create: `backend/src/main/java/com/archive/service/SystemConfigService.java`
- Create: `backend/src/main/java/com/archive/service/RoleService.java`
- Test: `backend/src/test/java/com/archive/service/SystemConfigServiceTest.java`
- Test: `backend/src/test/java/com/archive/service/RoleServiceTest.java`

- [ ] **Step 1: 写 SystemConfigServiceTest**

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.SystemConfigUpdateRequest;
import com.archive.dto.response.SystemConfigResponse;
import com.archive.entity.SystemConfig;
import com.archive.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemConfigServiceTest {

    private SystemConfigService service;
    private SystemConfigMapper mapper;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        mapper = mock(SystemConfigMapper.class);
        auditService = mock(AuditService.class);
        service = new SystemConfigService(mapper, auditService);
    }

    @Test
    void listConfigs_敏感键值脱敏() {
        SystemConfig normal = cfg("upload.max_file_size_mb", "100", "number", true);
        SystemConfig secret = cfg("ai.secret_key", "sk-xxx", "string", true);
        when(mapper.selectList(any())).thenReturn(List.of(normal, secret));

        List<SystemConfigResponse> r = service.listConfigs();

        assertThat(r).extracting(SystemConfigResponse::getConfigKey)
                .contains("upload.max_file_size_mb", "ai.secret_key");
        assertThat(r.stream().filter(x -> "ai.secret_key".equals(x.getConfigKey()))
                .findFirst().orElseThrow().getConfigValue()).isEqualTo("***");
        assertThat(r.stream().filter(x -> "upload.max_file_size_mb".equals(x.getConfigKey()))
                .findFirst().orElseThrow().getConfigValue()).isEqualTo("100");
    }

    @Test
    void updateConfig_不可编辑抛冲突() {
        SystemConfig cfg = cfg("system.locked", "1", "string", false);
        when(mapper.selectOne(any())).thenReturn(cfg);

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("2");

        assertThatThrownBy(() -> service.updateConfig("system.locked", req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void updateConfig_number类型非数字抛校验失败() {
        SystemConfig cfg = cfg("upload.max_file_size_mb", "100", "number", true);
        when(mapper.selectOne(any())).thenReturn(cfg);

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("abc");

        assertThatThrownBy(() -> service.updateConfig("upload.max_file_size_mb", req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void updateConfig_合法值更新成功并审计() {
        SystemConfig cfg = cfg("upload.max_file_size_mb", "100", "number", true);
        when(mapper.selectOne(any())).thenReturn(cfg);

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("200");

        SystemConfigResponse r = service.updateConfig("upload.max_file_size_mb", req);

        assertThat(r.getConfigValue()).isEqualTo("200");
        verify(mapper).updateById(any());
        verify(auditService).log(eq("M14"), eq("update_config"), eq("system_config"), any(), any());
    }

    private SystemConfig cfg(String key, String value, String type, boolean editable) {
        SystemConfig c = new SystemConfig();
        c.setId(1L);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setValueType(type);
        c.setEditable(editable);
        return c;
    }
}
```

- [ ] **Step 2: 写 SystemConfigService**

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.SystemConfigBatchUpdateRequest;
import com.archive.dto.request.SystemConfigUpdateRequest;
import com.archive.dto.response.SystemConfigResponse;
import com.archive.entity.SystemConfig;
import com.archive.exception.BusinessException;
import com.archive.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务。
 * 敏感密钥脱敏返回；按 valueType 校验值；editable 控制可改性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;
    private final AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 22.8 查询系统配置 ====================

    public List<SystemConfigResponse> listConfigs() {
        List<SystemConfig> configs = configMapper.selectList(null);
        return configs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private SystemConfigResponse toResponse(SystemConfig c) {
        SystemConfigResponse r = new SystemConfigResponse();
        r.setId(c.getId());
        r.setConfigKey(c.getConfigKey());
        r.setConfigValue(isSensitive(c.getConfigKey()) ? "***" : c.getConfigValue());
        r.setValueType(c.getValueType());
        r.setDescription(c.getDescription());
        r.setEditable(c.getEditable());
        return r;
    }

    private boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return lower.contains("key") || lower.contains("secret")
                || lower.contains("password") || lower.contains("token");
    }

    // ==================== 22.9 更新系统配置 ====================

    @Transactional
    public SystemConfigResponse updateConfig(String configKey, SystemConfigUpdateRequest req) {
        SystemConfig cfg = loadByKey(configKey);
        if (!Boolean.TRUE.equals(cfg.getEditable())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "该配置项不可编辑");
        }
        validateValueType(cfg.getValueType(), req.getConfigValue());

        cfg.setConfigValue(req.getConfigValue());
        configMapper.updateById(cfg);

        auditService.log("M14", "update_config", "system_config", cfg.getId(),
                Map.of("configKey", configKey));
        return toResponse(cfg);
    }

    // ==================== 22.10 批量更新系统配置 ====================

    @Transactional
    public List<SystemConfigResponse> batchUpdate(SystemConfigBatchUpdateRequest req) {
        return req.getItems().stream()
                .map(it -> {
                    SystemConfigUpdateRequest single = new SystemConfigUpdateRequest();
                    single.setConfigValue(it.getConfigValue());
                    return updateConfig(it.getConfigKey(), single);
                })
                .collect(Collectors.toList());
    }

    private SystemConfig loadByKey(String configKey) {
        SystemConfig cfg = configMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", configKey));
        if (cfg == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置项不存在: " + configKey);
        }
        return cfg;
    }

    private void validateValueType(String valueType, String value) {
        if (valueType == null || "string".equals(valueType)) {
            return;
        }
        switch (valueType) {
            case "number" -> {
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "配置值不是合法数字");
                }
            }
            case "boolean" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "配置值必须为 true 或 false");
                }
            }
            case "json" -> {
                try {
                    objectMapper.readTree(value);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "配置值不是合法 JSON");
                }
            }
            default -> {
                // 未知类型放行（仅 string 语义）
            }
        }
    }
}
```

- [ ] **Step 3: 写 RoleService + 测试**

```java
package com.archive.service;

import com.archive.dto.response.RoleResponse;
import com.archive.entity.Role;
import com.archive.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** 预设角色查询。本期不做逐按钮自定义授权。 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<RoleResponse> listRoles() {
        List<Role> roles = roleMapper.selectList(null);
        return roles.stream().map(r -> {
            RoleResponse resp = new RoleResponse();
            resp.setId(r.getId());
            resp.setRoleCode(r.getRoleCode());
            // roleName/description：若 Role 实体无对应列，则取 RoleCode 枚举 displayName
            try {
                com.archive.enums.RoleCode rc = com.archive.enums.RoleCode.valueOf(r.getRoleCode());
                resp.setRoleName(rc.getDisplayName());
            } catch (Exception ignored) {
                resp.setRoleName(r.getRoleCode());
            }
            resp.setDescription(null);
            return resp;
        }).collect(Collectors.toList());
    }
}
```

`RoleServiceTest`：

```java
package com.archive.service;

import com.archive.dto.response.RoleResponse;
import com.archive.entity.Role;
import com.archive.mapper.RoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceTest {

    @Test
    void listRoles_返回预设角色及展示名() {
        RoleMapper mapper = mock(RoleMapper.class);
        Role r = new Role();
        r.setId(1L);
        r.setRoleCode("sys_admin");
        when(mapper.selectList(null)).thenReturn(List.of(r));

        List<RoleResponse> result = new RoleService(mapper).listRoles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoleCode()).isEqualTo("sys_admin");
        assertThat(result.get(0).getRoleName()).isNotBlank();
    }
}
```

> `RoleCode` 枚举的 `getDisplayName()` 已存在（其他枚举同款）；确认 `RoleCode` 含该方法。若无，改用 `roleCode` 字符串作为 roleName。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=SystemConfigServiceTest,RoleServiceTest`
Expected: PASS（SystemConfig 5 个 + Role 1 个）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/service/SystemConfigService.java backend/src/main/java/com/archive/service/RoleService.java backend/src/test/java/com/archive/service/SystemConfigServiceTest.java backend/src/test/java/com/archive/service/RoleServiceTest.java
git commit -m "feat(appraisal): 实现系统配置脱敏校验与角色查询"
```

---

## Task 17: UserService 扩展（22.1-22.6）

> 改造现有 `UserService`：构造器追加 `AuditService`、`JdbcTemplate`、`AuditLogMapper`（任选其一查最近操作，本计划用 `JdbcTemplate`）。`listUsers` 增过滤；`createUser/updateUser` 绑定 `roleCodes`；新增 `getUserDetail`、`updateStatus`；`resetPassword` 补审计。`UserInfoResponse` 已在 Task 7 追加 `recentOperations`。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/UserService.java`
- Create: `backend/src/test/java/com/archive/service/UserServiceTest.java`

- [ ] **Step 1: 写 UserServiceTest（覆盖新增/扩展逻辑）**

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserStatusRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.entity.Role;
import com.archive.entity.User;
import com.archive.entity.UserRole;
import com.archive.enums.UserStatus;
import com.archive.mapper.RoleMapper;
import com.archive.mapper.UserMapper;
import com.archive.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserService service;
    private UserMapper userMapper;
    private RoleMapper roleMapper;
    private UserRoleMapper userRoleMapper;
    private AuditService auditService;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        userMapper = mock(UserMapper.class);
        roleMapper = mock(RoleMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        auditService = mock(AuditService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new UserService(userMapper, roleMapper, userRoleMapper, auditService, jdbcTemplate);
    }

    @Test
    void createUser_绑定角色并写user_roles() {
        UserCreateRequest req = newUserCreateReq("chen.front", List.of("front_archivist"));
        when(userMapper.selectOne(any())).thenReturn(null);
        Role r = new Role();
        r.setId((short) 2);
        r.setRoleCode("front_archivist");
        when(roleMapper.selectOne(any())).thenReturn(r);

        UserInfoResponse resp = service.createUser(req);

        verify(userMapper).insert(any(User.class));
        verify(userRoleMapper).insert(any(UserRole.class));
        assertThat(resp.getLoginName()).isEqualTo("chen.front");
    }

    @Test
    void createUser_角色不存在抛校验失败() {
        UserCreateRequest req = newUserCreateReq("bad.user", List.of("ghost_role"));
        when(userMapper.selectOne(any())).thenReturn(null);
        when(roleMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void updateStatus_禁用最后一个sys_admin抛冲突() {
        User u = new User();
        u.setId(1L);
        u.setStatus(UserStatus.active);
        when(userMapper.selectById(1L)).thenReturn(u);
        // 该用户是 sys_admin
        UserRole ur = new UserRole();
        ur.setRoleId((short) 1);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));
        Role sysAdmin = new Role();
        sysAdmin.setId((short) 1);
        sysAdmin.setRoleCode("sys_admin");
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(sysAdmin));
        // 只有一个启用的 sys_admin
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        UserStatusRequest req = new UserStatusRequest();
        req.setStatus("disabled");
        req.setReason("离岗");

        assertThatThrownBy(() -> service.updateStatus(1L, req))
                .isInstanceOf(com.archive.exception.BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void updateStatus_正常禁用并审计() {
        User u = new User();
        u.setId(1L);
        u.setStatus(UserStatus.active);
        when(userMapper.selectById(1L)).thenReturn(u);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);

        UserStatusRequest req = new UserStatusRequest();
        req.setStatus("disabled");
        req.setReason("离岗");

        UserInfoResponse resp = service.updateStatus(1L, req);

        assertThat(resp.getStatus()).isEqualTo("disabled");
        verify(userMapper).updateById(any(User.class));
        verify(auditService).log(eq("M14"), eq("update_user_status"), eq("user"), eq(1L), any());
    }

    private UserCreateRequest newUserCreateReq(String loginName, List<String> roles) {
        UserCreateRequest req = new UserCreateRequest();
        req.setUserType("internal");
        req.setLoginName(loginName);
        req.setRealName("小陈");
        req.setPassword("123456");
        req.setDataScope("all");
        req.setRoleCodes(roles);
        return req;
    }
}
```

> `Role.id` 类型为 `Short`（`user_roles.role_id` 对齐）；若实际为 `Long`/`Integer` 则同步调整测试与 `UserRole.roleId` 类型。

- [ ] **Step 2: 改造 UserService（完整文件）**

`backend/src/main/java/com/archive/service/UserService.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserStatusRequest;
import com.archive.dto.request.UserUpdateRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.entity.Role;
import com.archive.entity.User;
import com.archive.entity.UserRole;
import com.archive.enums.DataScope;
import com.archive.enums.UserStatus;
import com.archive.enums.UserType;
import com.archive.exception.BusinessException;
import com.archive.mapper.RoleMapper;
import com.archive.mapper.UserMapper;
import com.archive.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String ACTIVE_SYS_ADMIN_COUNT_SQL =
            "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id " +
            "JOIN roles r ON r.id = ur.role_id " +
            "WHERE r.role_code = 'sys_admin' AND u.status = 'active'";

    // ==================== 22.1 查询用户 ====================

    public PageResult<UserInfoResponse> listUsers(int pageNo, int pageSize, String keyword, String status,
                                                   String userType, String roleCode, Long organizationId) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("real_name", keyword)
                    .or().like("login_name", keyword)
                    .or().like("phone", keyword)
                    .or().like("employee_no", keyword));
        }
        if (status != null && !status.isBlank()) {
            qw.eq("status", status);
        }
        if (userType != null && !userType.isBlank()) {
            qw.eq("user_type", userType);
        }
        if (organizationId != null) {
            qw.eq("organization_id", organizationId);
        }
        if (roleCode != null && !roleCode.isBlank()) {
            List<UserRole> urs = userRoleMapper.selectList(new QueryWrapper<UserRole>()
                    .inSql("role_id", "SELECT id FROM roles WHERE role_code = '" + roleCode.replace("'", "") + "'"));
            if (urs.isEmpty()) {
                return new PageResult<>(List.of(), pageNo, pageSize, 0L);
            }
            List<Long> userIds = urs.stream().map(UserRole::getUserId).distinct().collect(Collectors.toList());
            qw.in("id", userIds);
        }
        qw.orderByDesc("created_at");

        Page<User> result = userMapper.selectPage(new Page<>(pageNo, pageSize), qw);
        List<UserInfoResponse> voList = result.getRecords().stream()
                .map(this::toUserInfoResponse)
                .collect(Collectors.toList());
        return new PageResult<>(voList, pageNo, pageSize, result.getTotal());
    }

    // ==================== 22.2 创建用户 ====================

    @Transactional
    public UserInfoResponse createUser(UserCreateRequest req) {
        User existing = userMapper.selectOne(new QueryWrapper<User>().eq("login_name", req.getLoginName()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "登录名已存在");
        }

        User user = new User();
        user.setUserType(UserType.valueOf(req.getUserType()));
        user.setLoginName(req.getLoginName());
        user.setEmployeeNo(req.getEmployeeNo());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setOrganizationId(req.getOrganizationId());
        user.setDepartmentName(req.getDepartmentName());
        user.setMaxSecurityLevel(req.getMaxSecurityLevel());
        user.setDataScope(req.getDataScope() != null ? DataScope.valueOf(req.getDataScope()) : DataScope.own_org);
        user.setStatus(UserStatus.active);
        userMapper.insert(user);

        bindRoles(user.getId(), req.getRoleCodes());

        auditService.log("M14", "create_user", "user", user.getId(),
                java.util.Map.of("loginName", user.getLoginName()));
        return toUserInfoResponse(user);
    }

    // ==================== 22.4 更新用户 ====================

    @Transactional
    public UserInfoResponse updateUser(Long id, UserUpdateRequest req) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getOrganizationId() != null) user.setOrganizationId(req.getOrganizationId());
        if (req.getDepartmentName() != null) user.setDepartmentName(req.getDepartmentName());
        if (req.getMaxSecurityLevel() != null) user.setMaxSecurityLevel(req.getMaxSecurityLevel());
        if (req.getDataScope() != null) user.setDataScope(DataScope.valueOf(req.getDataScope()));
        userMapper.updateById(user);

        if (req.getRoleCodes() != null) {
            rebindRoles(id, req.getRoleCodes());
        }
        auditService.log("M14", "update_user", "user", id, java.util.Map.of());
        return toUserInfoResponse(user);
    }

    // ==================== 22.3 用户详情 ====================

    public UserInfoResponse getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        UserInfoResponse vo = toUserInfoResponse(user);
        vo.setRecentOperations(loadRecentOperations(id));
        return vo;
    }

    // ==================== 22.5 启用/禁用 ====================

    @Transactional
    public UserInfoResponse updateStatus(Long id, UserStatusRequest req) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if ("disabled".equals(req.getStatus()) && isLastActiveSysAdmin(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "不能禁用最后一个启用的系统管理员");
        }
        user.setStatus(UserStatus.valueOf(req.getStatus()));
        userMapper.updateById(user);

        auditService.log("M14", "update_user_status", "user", id,
                java.util.Map.of("status", req.getStatus(),
                        "reason", req.getReason() != null ? req.getReason() : ""));
        return toUserInfoResponse(user);
    }

    // ==================== 22.6 重置密码 ====================

    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword != null ? newPassword : "123456"));
        userMapper.updateById(user);
        auditService.log("M14", "reset_password", "user", id, java.util.Map.of());
    }

    // ==================== 角色绑定辅助 ====================

    private void bindRoles(Long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        for (String code : roleCodes) {
            Role role = mustGetRole(code);
            insertUserRole(userId, role.getId());
        }
    }

    private void rebindRoles(Long userId, List<String> roleCodes) {
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", userId));
        bindRoles(userId, roleCodes);
    }

    private void insertUserRole(Long userId, java.lang.Short roleId) {
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);
    }

    private Role mustGetRole(String roleCode) {
        Role role = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_code", roleCode));
        if (role == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色不存在或已禁用: " + roleCode);
        }
        return role;
    }

    /** 判断禁用该用户是否会使启用中的 sys_admin 归零。 */
    private boolean isLastActiveSysAdmin(Long userId) {
        // 该用户是否 sys_admin
        List<UserRole> urs = userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("user_id", userId));
        if (urs.isEmpty()) {
            return false;
        }
        List<Short> roleIds = urs.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        boolean isAdmin = roles.stream().anyMatch(r -> "sys_admin".equals(r.getRoleCode()));
        if (!isAdmin) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(ACTIVE_SYS_ADMIN_COUNT_SQL, Long.class);
        return count != null && count <= 1;
    }

    private List<UserInfoResponse.RecentOperation> loadRecentOperations(Long userId) {
        try {
            return jdbcTemplate.query(
                    "SELECT module_name, operation_type, operated_at FROM audit_logs " +
                            "WHERE actor_user_id = ? ORDER BY operated_at DESC LIMIT 10",
                    (rs, i) -> {
                        UserInfoResponse.RecentOperation op = new UserInfoResponse.RecentOperation();
                        op.setModuleName(rs.getString("module_name"));
                        op.setOperationType(rs.getString("operation_type"));
                        op.setOperatedAt(rs.getTimestamp("operated_at") != null
                                ? rs.getTimestamp("operated_at").toLocalDateTime().atOffset(java.time.ZoneOffset.UTC)
                                : null);
                        return op;
                    }, userId);
        } catch (Exception e) {
            return List.of();
        }
    }

    private UserInfoResponse toUserInfoResponse(User user) {
        UserInfoResponse vo = new UserInfoResponse();
        vo.setId(user.getId());
        vo.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        vo.setLoginName(user.getLoginName());
        vo.setEmployeeNo(user.getEmployeeNo());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setOrganizationId(user.getOrganizationId());
        vo.setDepartmentName(user.getDepartmentName());
        vo.setMaxSecurityLevel(user.getMaxSecurityLevel());
        vo.setDataScope(user.getDataScope() != null ? user.getDataScope().name() : null);
        vo.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        vo.setCreatedAt(user.getCreatedAt());

        List<UserRole> urs = userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("user_id", user.getId()));
        if (!urs.isEmpty()) {
            List<Short> roleIds = urs.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            vo.setRoles(roles.stream().map(Role::getRoleCode).collect(Collectors.toList()));
        } else {
            vo.setRoles(List.of());
        }
        return vo;
    }
}
```

> `UserRole.userId` 字段需确认存在（现有 `toUserInfoResponse` 用 `user_id` 查询，实体应有 `userId`）。`Role.id` 类型以实际为准（`Short`/`Long`）。原 `listUsers(int,int,String,String)` 签名变更，调用方 `UserController` 在 Task 18 同步更新。

- [ ] **Step 3: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=UserServiceTest`
Expected: PASS（5 个测试通过）

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/archive/service/UserService.java backend/src/test/java/com/archive/service/UserServiceTest.java
git commit -m "feat(appraisal): 扩展用户管理含角色绑定状态与详情"
```

---

## Task 18: Controller 层（6 个，含 UserController 迁移）

Controller 仅参数转发，无业务逻辑，以编译 + 服务层测试为验证。鉴权依赖 `SaTokenConfig` 对 `/api/**` 的登录校验，与 `WarehouseController` 一致；角色级校验本期不在拦截器层强制（与既有 admin 控制器一致）。

**Files:**
- Create: `backend/src/main/java/com/archive/controller/AppraisalController.java`
- Create: `backend/src/main/java/com/archive/controller/DestructionController.java`
- Create: `backend/src/main/java/com/archive/controller/ApprovalController.java`
- Create: `backend/src/main/java/com/archive/controller/SystemConfigController.java`
- Create: `backend/src/main/java/com/archive/controller/RoleController.java`
- Modify: `backend/src/main/java/com/archive/controller/UserController.java`

- [ ] **Step 1: AppraisalController**

```java
package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.AppraisalBatchCreateRequest;
import com.archive.dto.request.AppraisalItemSaveRequest;
import com.archive.dto.response.AppraisalBatchDetailResponse;
import com.archive.dto.response.AppraisalBatchResponse;
import com.archive.service.AppraisalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/appraisal-batches")
@RequiredArgsConstructor
@Tag(name = "档案鉴定", description = "鉴定批次与明细")
public class AppraisalController {

    private final AppraisalService appraisalService;

    @GetMapping
    @Operation(summary = "查询鉴定批次")
    public R<PageResult<AppraisalBatchResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer formedYearStart,
            @RequestParam(required = false) Integer formedYearEnd,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(appraisalService.listBatches(status, categoryId, formedYearStart, formedYearEnd, pageNo, pageSize));
    }

    @PostMapping
    @Operation(summary = "创建鉴定批次")
    public R<AppraisalBatchDetailResponse> create(@RequestBody @Valid AppraisalBatchCreateRequest req) {
        return R.ok(appraisalService.createBatch(req));
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "获取鉴定批次详情")
    public R<AppraisalBatchDetailResponse> detail(@PathVariable Long batchId) {
        return R.ok(appraisalService.getBatchDetail(batchId));
    }

    @PutMapping("/{batchId}/items")
    @Operation(summary = "保存鉴定明细")
    public R<AppraisalBatchDetailResponse> saveItems(
            @PathVariable Long batchId, @RequestBody @Valid AppraisalItemSaveRequest req) {
        return R.ok(appraisalService.saveItems(batchId, req));
    }

    @PostMapping("/{batchId}/complete")
    @Operation(summary = "完成鉴定")
    public R<AppraisalBatchDetailResponse> complete(@PathVariable Long batchId) {
        return R.ok(appraisalService.completeBatch(batchId));
    }
}
```

- [ ] **Step 2: DestructionController**

```java
package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.DestructionDestroyRequest;
import com.archive.dto.request.DestructionSubmitRequest;
import com.archive.dto.response.DestructionListDetailResponse;
import com.archive.dto.response.DestructionListResponse;
import com.archive.service.DestructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/destruction-lists")
@RequiredArgsConstructor
@Tag(name = "档案销毁", description = "销毁清册与确认")
public class DestructionController {

    private final DestructionService destructionService;

    @GetMapping
    @Operation(summary = "查询销毁清册")
    public R<PageResult<DestructionListResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(destructionService.listLists(status, keyword, pageNo, pageSize));
    }

    @GetMapping("/{listId}")
    @Operation(summary = "获取销毁清册详情")
    public R<DestructionListDetailResponse> detail(@PathVariable Long listId) {
        return R.ok(destructionService.getListDetail(listId));
    }

    @PostMapping("/{listId}/submit-approval")
    @Operation(summary = "提交销毁审批")
    public R<DestructionListDetailResponse> submitApproval(
            @PathVariable Long listId, @RequestBody @Valid DestructionSubmitRequest req) {
        return R.ok(destructionService.submitApproval(listId, req));
    }

    @PostMapping("/{listId}/photos")
    @Operation(summary = "上传销毁现场照片")
    public R<List<DestructionListDetailResponse.PhotoView>> uploadPhotos(
            @PathVariable Long listId,
            @RequestParam("files") MultipartFile[] files) {
        return R.ok(destructionService.uploadPhotos(listId, files));
    }

    @PostMapping("/{listId}/destroy")
    @Operation(summary = "确认销毁")
    public R<DestructionListDetailResponse> destroy(
            @PathVariable Long listId, @RequestBody @Valid DestructionDestroyRequest req) {
        return R.ok(destructionService.confirmDestroy(listId, req));
    }
}
```

- [ ] **Step 3: ApprovalController**

```java
package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.ApprovalOpinionRequest;
import com.archive.dto.response.ApprovalDetailResponse;
import com.archive.dto.response.ApprovalResponse;
import com.archive.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
@Tag(name = "审批工作台", description = "密级/开放/销毁审批")
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    @Operation(summary = "查询审批单")
    public R<PageResult<ApprovalResponse>> list(
            @RequestParam(required = false) String approvalType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(approvalService.listApprovals(approvalType, status, keyword, pageNo, pageSize));
    }

    @GetMapping("/{approvalId}")
    @Operation(summary = "获取审批详情")
    public R<ApprovalDetailResponse> detail(@PathVariable Long approvalId) {
        return R.ok(approvalService.getApprovalDetail(approvalId));
    }

    @PostMapping("/{approvalId}/approve")
    @Operation(summary = "审批通过")
    public R<ApprovalDetailResponse> approve(
            @PathVariable Long approvalId, @RequestBody ApprovalOpinionRequest req) {
        return R.ok(approvalService.approve(approvalId, req));
    }

    @PostMapping("/{approvalId}/reject")
    @Operation(summary = "审批退回")
    public R<ApprovalDetailResponse> reject(
            @PathVariable Long approvalId, @RequestBody ApprovalOpinionRequest req) {
        return R.ok(approvalService.reject(approvalId, req));
    }
}
```

- [ ] **Step 4: SystemConfigController**

```java
package com.archive.controller;

import com.archive.common.R;
import com.archive.dto.request.SystemConfigBatchUpdateRequest;
import com.archive.dto.request.SystemConfigUpdateRequest;
import com.archive.dto.response.SystemConfigResponse;
import com.archive.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system-configs")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "配置项查询与更新")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @Operation(summary = "查询系统配置")
    public R<List<SystemConfigResponse>> list() {
        return R.ok(systemConfigService.listConfigs());
    }

    @PutMapping("/{configKey}")
    @Operation(summary = "更新单项配置")
    public R<SystemConfigResponse> update(
            @PathVariable String configKey, @RequestBody @Valid SystemConfigUpdateRequest req) {
        return R.ok(systemConfigService.updateConfig(configKey, req));
    }

    @PutMapping
    @Operation(summary = "批量更新配置")
    public R<List<SystemConfigResponse>> batchUpdate(@RequestBody @Valid SystemConfigBatchUpdateRequest req) {
        return R.ok(systemConfigService.batchUpdate(req));
    }
}
```

- [ ] **Step 5: RoleController**

```java
package com.archive.controller;

import com.archive.common.R;
import com.archive.dto.response.RoleResponse;
import com.archive.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Tag(name = "角色", description = "预设角色查询")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "查询角色列表")
    public R<List<RoleResponse>> list() {
        return R.ok(roleService.listRoles());
    }
}
```

- [ ] **Step 6: UserController 迁移与扩展（完整文件）**

`backend/src/main/java/com/archive/controller/UserController.java`：

```java
package com.archive.controller;

import com.archive.common.PageRequest;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserStatusRequest;
import com.archive.dto.request.UserUpdateRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户、角色、状态（系统管理员）")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "查询用户列表")
    public R<PageResult<UserInfoResponse>> list(
            @Valid PageRequest page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Long organizationId) {
        return R.ok(userService.listUsers(page.getPageNo(), page.getPageSize(),
                keyword, status, userType, roleCode, organizationId));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public R<UserInfoResponse> create(@Valid @RequestBody UserCreateRequest req) {
        return R.ok(userService.createUser(req));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详情")
    public R<UserInfoResponse> detail(@PathVariable Long userId) {
        return R.ok(userService.getUserDetail(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "更新用户")
    public R<UserInfoResponse> update(@PathVariable Long userId,
                                      @Valid @RequestBody UserUpdateRequest req) {
        return R.ok(userService.updateUser(userId, req));
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "启用或禁用用户")
    public R<UserInfoResponse> updateStatus(@PathVariable Long userId,
                                            @RequestBody @Valid UserStatusRequest req) {
        return R.ok(userService.updateStatus(userId, req));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "重置内部用户密码")
    public R<Void> resetPassword(@PathVariable Long userId,
                                 @RequestBody(required = false) java.util.Map<String, String> body) {
        String newPassword = body != null ? body.get("newPassword") : null;
        userService.resetPassword(userId, newPassword);
        return R.ok();
    }
}
```

> 路径由 `/api/users` 迁移至 `/api/admin/users`。实现后全局搜索 `/api/users` 引用（`SaTokenConfig`、测试、文档），同步更新；无前端调用方故无破坏。

- [ ] **Step 7: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/archive/controller/
git commit -m "feat(appraisal): 新增鉴定销毁审批配置控制器并迁移用户接口路径"
```

---

## Task 19: 全量构建、测试、推送与 PR

- [ ] **Step 1: 全量测试**

Run: `cd backend && mvn -q test`
Expected: 全部测试通过（AppraisalNoUtil 2 + DestructionNoUtil 1 + AppraisalService 10 + DestructionService 8 + ApprovalService 6 + SystemConfig 5 + Role 1 + User 5 + 既有测试无回归）

- [ ] **Step 2: 全量打包验证**

Run: `cd backend && mvn -q package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: （可选）Runtime 冒烟**

若本机 Docker 已起 postgres/minio/clamav：启动应用，触发「创建鉴定批次→保存明细（destroy）→完成鉴定→查看销毁清册→提交审批→审批通过→确认销毁」闭环，确认状态流转与审计落库；记录到 `backend/docs/superpowers/runtime/2026-06-16-appraisal-zhou-runtime.md`。若环境不可用，标注「未 runtime 验证，仅单测 + 编译」。

- [ ] **Step 4: 确认提交身份为周扬**

```bash
git config user.name && git config user.email
```
Expected: 周扬 / rougishiki 邮箱（由 includeIf 自动切换）。若不符，按 `~/gitconfig` 的 `includeIf` 与 `../.envrc` 校正。

- [ ] **Step 5: 推送分支（用周扬 PAT）**

读取上一级 `.envrc`（`/home/guagua/projects/cupk-3s/zhouyang/.envrc`）获取 `GH_CONFIG_DIR`，导出后推送：

```bash
export GH_CONFIG_DIR=$(grep -oP 'GH_CONFIG_DIR=\K.*' /home/guagua/projects/cupk-3s/zhouyang/.envrc)
git push -u origin feat/appraisal-zhou
```

> 推送前确认 `GH_CONFIG_DIR` 指向 `~/.config/gh-zhouyang`（周扬 PAT），避免误用默认账号。

- [ ] **Step 6: 发起 PR（周扬账号）**

```bash
gh pr create --base develop --head feat/appraisal-zhou \
  --title "feat(appraisal): 鉴定·销毁·审批·用户配置模块（M08/M10/M11/M14）" \
  --body "实现接口文档第 13/14/15/22 章共 24 端点 + seq_appraisal_batch_no 序列，打通鉴定→销毁清册→馆领导审批→确认销毁闭环。
设计：backend/docs/superpowers/specs/2026-06-16-appraisal-zhou-design.md
计划：backend/docs/superpowers/plans/2026-06-16-appraisal-zhou.md
验证：mvn test 全绿（单测覆盖状态机与边界）。"
```

---

## 计划自审（writing-plans Self-Review）

**1. Spec 覆盖**：
- §13 审批 13.1-13.4 → Task 15 ✓
- §14 鉴定 14.1-14.5 → Task 8/9/10/11 ✓
- §15 销毁 15.1-15.5 → Task 12/13/14 ✓
- §22 用户 22.1-22.6 → Task 17；22.7 角色 → Task 16 RoleService；22.8-22.10 系统配置 → Task 16 SystemConfigService ✓
- V17 序列 → Task 1 ✓
- 闭环状态机（鉴定完成→清册→提交→审批→销毁）跨 Task 11/12/15/13 衔接 ✓

**2. 占位符扫描**：
- Task 14（15.4 照片上传）的 ClamAV 扫描 / MinIO 上传调用以注释标注「复用 StagingFileService 同款」——这是**待实现时对齐的既有外部依赖**（确切方法签名需读 `StagingFileService` 确认），非懒散占位。已显式标注「不得保留为注释交付」。其余任务均给出完整可编译代码。

**3. 类型一致性**：
- `UserService.listUsers` 签名在 Task 17 扩展（增 userType/roleCode/organizationId），`UserController`（Task 18）已同步新签名 ✓
- `DestructionService` 构造器在 Task 12/13/14 三次扩展（→10/13 依赖），各 Task 已注明测试 `@BeforeEach` 同步 ✓
- `Role.id` 类型（Short/Long）、`RetentionPeriod`/`FileStatus` 枚举常量名、`UserRole.userId` 字段——均标注「以实际为准，实现前确认」。

**4. 实现前需确认的既有代码点**（低风险读取后对齐，不阻塞）：
- `StagingFileService`（15.4 上传扫描链路）、`Role`/`UserRole` 实体字段类型、`RetentionPeriod`/`FileStatus`/`ConditionStatus` 枚举常量名、`SaTokenConfig` 是否需为 `/api/admin/**` 配置、`UserCreateRequest`/`UserUpdateRequest` 现有字段（仅追加 roleCodes）。
