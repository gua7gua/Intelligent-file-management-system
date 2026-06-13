# 移交/征集清单模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现移交/征集清单的纯元数据 CRUD、前台验收回退、回执导出接口，供前端按接口文档联调。

**Architecture:** 遵循现有分层模式（Controller → Service → Mapper → Entity），枚举用 `@EnumValue` 映射，DTO 独立于 Entity，批次状态重算集中在 Service 一个方法中。暂不涉及文件上传和 AI 补全。

**Tech Stack:** Spring Boot 3.5.3 + MyBatis-Plus 3.5.7 + Sa-Token 1.44.0 + springdoc-openapi 2.8.6 + PostgreSQL 17 + Maven

**Branch:** `feat/transfer-zhou`（已从 `origin/develop` 创建）

**TDD Note:** 项目当前无测试基础设施（`ArchiveApplicationTests` 为空），且工期紧（06-11 至 06-13），本期以编译通过 + Swagger 可访问为验证手段，不强制 TDD 流程。

---

## File Structure

```
backend/src/main/java/com/archive/
├── enums/
│   ├── SourceType.java            ✅ 已创建
│   ├── BatchStatus.java           ✅ 已创建
│   ├── ItemStatus.java            ✅ 已创建
│   ├── CarrierStatus.java         ✅ 已创建
│   ├── RetentionPeriod.java       ✅ 已创建
│   └── FileMatchStatus.java       ✅ 已创建
├── entity/
│   ├── IntakeBatch.java           ← Task 2
│   └── IntakeItem.java            ← Task 2
├── mapper/
│   ├── IntakeBatchMapper.java     ← Task 3
│   └── IntakeItemMapper.java      ← Task 3
├── dto/
│   ├── request/
│   │   ├── BatchPageQuery.java            ← Task 4
│   │   ├── TransferBatchCreateRequest.java ← Task 4
│   │   ├── TransferItemRequest.java        ← Task 4
│   │   ├── CollectionBatchCreateRequest.java ← Task 4
│   │   ├── CollectionSubmitRequest.java    ← Task 4
│   │   ├── ItemAcceptanceRequest.java      ← Task 4
│   │   ├── BatchCompleteRequest.java       ← Task 4
│   │   ├── CollectionScheduleRequest.java  ← Task 4
│   │   └── CollectionRejectRequest.java    ← Task 4
│   └── response/
│       ├── IntakeBatchResponse.java    ← Task 5
│       ├── IntakeItemResponse.java     ← Task 5
│       └── TransferDashboardResponse.java ← Task 5
├── service/
│   └── IntakeBatchService.java     ← Task 6
└── controller/
    ├── TransferController.java           ← Task 7
    ├── PublicCollectionController.java   ← Task 8
    ├── ReceptionController.java          ← Task 9
    └── CollectionManageController.java   ← Task 9
```

---

### Task 1: 提交已创建的枚举类

**Files:** 6 个枚举文件已存在于 `backend/src/main/java/com/archive/enums/`

- [ ] **Step 1: 编译验证枚举类**

Run: `cd backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 2: 提交**

```bash
cd backend
git add src/main/java/com/archive/enums/SourceType.java \
        src/main/java/com/archive/enums/BatchStatus.java \
        src/main/java/com/archive/enums/ItemStatus.java \
        src/main/java/com/archive/enums/CarrierStatus.java \
        src/main/java/com/archive/enums/RetentionPeriod.java \
        src/main/java/com/archive/enums/FileMatchStatus.java
git commit -m "feat(intake): 添加清单相关枚举类型"
```

---

### Task 2: 创建 Entity 类

**Files:**
- Create: `backend/src/main/java/com/archive/entity/IntakeBatch.java`
- Create: `backend/src/main/java/com/archive/entity/IntakeItem.java`

- [ ] **Step 1: 创建 IntakeBatch.java**

```java
package com.archive.entity;

import com.archive.enums.BatchStatus;
import com.archive.enums.SourceType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 移交/征集清单批次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("intake_batches")
public class IntakeBatch extends BaseEntity {

    private String batchNo;

    @EnumValue
    private SourceType sourceType;

    private String title;

    @EnumValue
    private BatchStatus status;

    private Long organizationId;

    private String departmentName;

    private Long publicUserId;

    private String contactName;

    private String contactPhone;

    private Integer archiveYear;

    private LocalDate expectedTransferDate;

    private OffsetDateTime scheduledReceiveAt;

    private OffsetDateTime submittedAt;

    private Long acceptedBy;

    private OffsetDateTime acceptedAt;

    private OffsetDateTime archivedAt;

    private OffsetDateTime shelvedAt;

    private Long latestAiTaskId;

    private String rejectReason;

    private OffsetDateTime agreementAcceptedAt;

    /** 软删除时间。 */
    private OffsetDateTime deletedAt;
}
```

- [ ] **Step 2: 创建 IntakeItem.java**

```java
package com.archive.entity;

import com.archive.enums.CarrierStatus;
import com.archive.enums.FileMatchStatus;
import com.archive.enums.ItemStatus;
import com.archive.enums.RetentionPeriod;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 移交/征集清单条目。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "intake_items", autoResultMap = true)
public class IntakeItem extends BaseEntity {

    private Long batchId;

    private Integer itemNo;

    @EnumValue
    private ItemStatus status;

    private String inputTitle;

    private Integer pageCount;

    @EnumValue
    private RetentionPeriod retentionPeriod;

    @EnumValue
    private CarrierStatus carrierStatus;

    private Integer securityLevel;

    private String openStatus;

    private Boolean allowDigitization;

    private String electronicFormat;

    private String expectedFilename;

    private LocalDate formedDate;

    private String acceptanceNote;

    private String rejectReason;

    @EnumValue
    private FileMatchStatus fileMatchStatus;

    /** AI 补全建议 JSON。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String aiSuggestion;

    private String confirmedTitle;

    private String confirmedResponsibleText;

    private LocalDate confirmedFormedDate;

    private Integer confirmedCategoryId;

    /** 后台确认后的标签名数组。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> confirmedTags;

    private Long generatedArchiveId;

    /** 软删除时间。 */
    private OffsetDateTime deletedAt;
}
```

- [ ] **Step 3: 编译验证**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
cd backend
git add src/main/java/com/archive/entity/IntakeBatch.java \
        src/main/java/com/archive/entity/IntakeItem.java
git commit -m "feat(intake): 添加 IntakeBatch 和 IntakeItem 实体"
```

---

### Task 3: 创建 Mapper 接口

**Files:**
- Create: `backend/src/main/java/com/archive/mapper/IntakeBatchMapper.java`
- Create: `backend/src/main/java/com/archive/mapper/IntakeItemMapper.java`

- [ ] **Step 1: 创建 IntakeBatchMapper.java**

```java
package com.archive.mapper;

import com.archive.entity.IntakeBatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IntakeBatchMapper extends BaseMapper<IntakeBatch> {
}
```

- [ ] **Step 2: 创建 IntakeItemMapper.java**

```java
package com.archive.mapper;

import com.archive.entity.IntakeItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IntakeItemMapper extends BaseMapper<IntakeItem> {

    @Select("SELECT COALESCE(MAX(item_no), 0) FROM intake_items WHERE batch_id = #{batchId}")
    int maxItemNo(@Param("batchId") Long batchId);
}
```

- [ ] **Step 3: 提交**

```bash
cd backend
git add src/main/java/com/archive/mapper/IntakeBatchMapper.java \
        src/main/java/com/archive/mapper/IntakeItemMapper.java
git commit -m "feat(intake): 添加 IntakeBatch 和 IntakeItem Mapper"
```

---

### Task 4: 创建 DTO 请求类

**Files:**
- Create: `backend/src/main/java/com/archive/dto/request/BatchPageQuery.java`
- Create: `backend/src/main/java/com/archive/dto/request/TransferBatchCreateRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/TransferItemRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/CollectionBatchCreateRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/CollectionSubmitRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/ItemAcceptanceRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/BatchCompleteRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/CollectionScheduleRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/CollectionRejectRequest.java`

- [ ] **Step 1: 创建 BatchPageQuery.java**

```java
package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 清单分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchPageQuery extends PageRequest {

    private String status;

    private String keyword;

    private Integer archiveYear;
}
```

- [ ] **Step 2: 创建 TransferBatchCreateRequest.java**

```java
package com.archive.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建/更新移交清单请求。
 */
@Data
public class TransferBatchCreateRequest {

    @NotBlank(message = "清单标题不能为空")
    private String title;

    private String departmentName;

    private String contactPhone;

    private Integer archiveYear;

    private LocalDate expectedTransferDate;

    @NotEmpty(message = "至少包含一条清单条目")
    @Valid
    private List<TransferItemRequest> items;
}
```

- [ ] **Step 3: 创建 TransferItemRequest.java**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 移交清单条目请求（新增/编辑共用）。
 */
@Data
public class TransferItemRequest {

    @NotBlank(message = "档案标题不能为空")
    private String inputTitle;

    private Integer pageCount;

    @NotBlank(message = "保管期限不能为空")
    private String retentionPeriod;

    @NotBlank(message = "载体状态不能为空")
    private String carrierStatus;

    @NotNull(message = "密级不能为空")
    private Integer securityLevel;

    @NotBlank(message = "公开状态不能为空")
    private String openStatus;

    private Boolean allowDigitization;

    private String electronicFormat;

    private String expectedFilename;

    private LocalDate formedDate;
}
```

- [ ] **Step 4: 创建 CollectionBatchCreateRequest.java**

```java
package com.archive.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建/更新征集清单请求。
 */
@Data
public class CollectionBatchCreateRequest {

    @NotBlank(message = "清单标题不能为空")
    private String title;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    private String contactPhone;

    private Integer archiveYear;

    @NotEmpty(message = "至少包含一条清单条目")
    @Valid
    private List<CollectionItemRequest> items;

    /**
     * 征集清单条目（字段比移交少，使用独立内部类）。
     */
    @Data
    public static class CollectionItemRequest {

        @NotBlank(message = "档案标题不能为空")
        private String inputTitle;

        @NotBlank(message = "载体状态不能为空")
        private String carrierStatus;

        private String electronicFormat;

        private String expectedFilename;

        private LocalDate formedDate;
    }
}
```

- [ ] **Step 5: 创建 CollectionSubmitRequest.java**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * 提交征集清单请求。
 */
@Data
public class CollectionSubmitRequest {

    @AssertTrue(message = "必须同意捐赠协议")
    private Boolean agreementAccepted;
}
```

- [ ] **Step 6: 创建 ItemAcceptanceRequest.java**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 条目验收结果请求。
 */
@Data
public class ItemAcceptanceRequest {

    /** accepted 或 rejected。 */
    @NotBlank(message = "验收结果不能为空")
    private String result;

    private String acceptanceNote;

    /** 回退时必填。 */
    private String rejectReason;
}
```

- [ ] **Step 7: 创建 BatchCompleteRequest.java**

```java
package com.archive.dto.request;

import lombok.Data;

/**
 * 完成批次验收请求。
 */
@Data
public class BatchCompleteRequest {

    private String acceptanceNote;
}
```

- [ ] **Step 8: 创建 CollectionScheduleRequest.java**

```java
package com.archive.dto.request;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 征集约定到馆时间请求。
 */
@Data
public class CollectionScheduleRequest {

    private OffsetDateTime scheduledReceiveAt;

    private String contactNote;
}
```

- [ ] **Step 9: 创建 CollectionRejectRequest.java**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 征集拒绝请求。
 */
@Data
public class CollectionRejectRequest {

    @NotBlank(message = "拒绝原因不能为空")
    private String rejectReason;
}
```

- [ ] **Step 10: 编译验证**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 11: 提交**

```bash
cd backend
git add src/main/java/com/archive/dto/request/
git commit -m "feat(intake): 添加清单相关请求 DTO"
```

---

### Task 5: 创建 DTO 响应类

**Files:**
- Create: `backend/src/main/java/com/archive/dto/response/IntakeBatchResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/IntakeItemResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/TransferDashboardResponse.java`

- [ ] **Step 1: 创建 IntakeItemResponse.java**

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 清单条目响应。
 */
@Data
public class IntakeItemResponse {

    private Long id;
    private Long batchId;
    private Integer itemNo;
    private String status;
    private String statusText;
    private String inputTitle;
    private Integer pageCount;
    private String retentionPeriod;
    private String carrierStatus;
    private Integer securityLevel;
    private String openStatus;
    private Boolean allowDigitization;
    private String electronicFormat;
    private String expectedFilename;
    private LocalDate formedDate;
    private String acceptanceNote;
    private String rejectReason;
    private String fileMatchStatus;
    private Object aiSuggestion;
    private String confirmedTitle;
    private String confirmedResponsibleText;
    private LocalDate confirmedFormedDate;
    private Integer confirmedCategoryId;
    private List<String> confirmedTags;
    private Long generatedArchiveId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 IntakeBatchResponse.java**

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 清单批次响应。
 */
@Data
public class IntakeBatchResponse {

    private Long id;
    private String batchNo;
    private String sourceType;
    private String title;
    private String status;
    private String statusText;
    private Long organizationId;
    private String departmentName;
    private Long publicUserId;
    private String contactName;
    private String contactPhone;
    private Integer archiveYear;
    private LocalDate expectedTransferDate;
    private OffsetDateTime scheduledReceiveAt;
    private OffsetDateTime submittedAt;
    private Long acceptedBy;
    private OffsetDateTime acceptedAt;
    private OffsetDateTime archivedAt;
    private OffsetDateTime shelvedAt;
    private String rejectReason;
    private OffsetDateTime agreementAcceptedAt;
    private Integer itemCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** 批次详情时填充条目列表。 */
    private List<IntakeItemResponse> items;
}
```

- [ ] **Step 3: 创建 TransferDashboardResponse.java**

```java
package com.archive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 移交工作台统计响应。
 */
@Data
public class TransferDashboardResponse {

    private Summary summary;
    private List<IntakeBatchResponse> recentBatches;

    @Data
    public static class Summary {
        private long draft;
        private long pendingTransfer;
        private long partiallyReceived;
        private long received;
        private long archived;
        private long shelved;
        private long rejected;
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
cd backend
git add src/main/java/com/archive/dto/response/
git commit -m "feat(intake): 添加清单相关响应 DTO"
```

---

### Task 6: 创建 IntakeBatchService

**Files:**
- Create: `backend/src/main/java/com/archive/service/IntakeBatchService.java`

这是核心任务，包含所有业务逻辑。代码较长，分为以下子步骤。

- [ ] **Step 1: 创建 IntakeBatchService.java — 序列号生成 + 移交清单 CRUD**

完整代码见执行阶段。关键方法清单：
- `generateBatchNo()` — `SELECT nextval('seq_batch_no')`，格式 `BAT-{000001}`
- `createTransferBatch()` — 创建移交清单草稿 + 条目
- `getTransferBatch()` — 获取移交清单详情
- `updateTransferBatch()` — 更新草稿
- `deleteTransferBatch()` — 删除草稿
- `listTransferBatches()` — 分页查询移交清单
- `addTransferItem()` / `updateTransferItem()` / `deleteTransferItem()` — 条目 CRUD

- [ ] **Step 2: 补充征集清单方法**

- `createCollection()` — 创建征集清单（默认 permanent、securityLevel=0、openStatus=open）
- `getCollection()` — 获取征集清单详情
- `updateCollection()` — 更新草稿
- `submitCollection()` — 提交征集（draft → pending_contact，校验 agreementAccepted）

- [ ] **Step 3: 补充验收回退方法**

- `listPendingReception()` — 查询待验收批次
- `getReceptionDetail()` — 获取验收详情
- `acceptItem()` / `rejectItem()` — 条目验收/回退
- `completeAcceptance()` — 完成批次验收（触发批次状态重算）
- `recalculateBatchStatus()` — 批次状态重算核心方法

- [ ] **Step 4: 补充征集管理方法**

- `listCollections()` — 查询征集待办
- `scheduleReceive()` — 约定到馆时间
- `rejectCollection()` — 拒绝征集意向

- [ ] **Step 5: 补充回执导出和工具方法**

- `exportReceipt()` — 回执导出骨架（暂返回空 byte[]）
- `toBatchResponse()` — Entity → Response 转换
- `toItemResponse()` — Entity → Response 转换
- `resolveStatusText()` — 根据来源类型映射中文状态文本

- [ ] **Step 6: 编译验证**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
cd backend
git add src/main/java/com/archive/service/IntakeBatchService.java
git commit -m "feat(intake): 实现清单 Service 完整业务逻辑"
```

---

### Task 7: 创建 TransferController

**Files:**
- Create: `backend/src/main/java/com/archive/controller/TransferController.java`

- [ ] **Step 1: 创建 TransferController.java**

接口清单（对应接口文档 Section 4）：
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/transfer/dashboard` | 移交工作台统计 |
| GET | `/api/transfer/batches` | 查询移交清单列表 |
| POST | `/api/transfer/batches` | 创建移交清单草稿 |
| GET | `/api/transfer/batches/{batchId}` | 获取移交清单详情 |
| PUT | `/api/transfer/batches/{batchId}` | 更新移交清单草稿 |
| DELETE | `/api/transfer/batches/{batchId}` | 删除移交清单草稿 |
| POST | `/api/transfer/batches/{batchId}/items` | 新增条目 |
| PUT | `/api/transfer/batches/{batchId}/items/{itemId}` | 更新条目 |
| DELETE | `/api/transfer/batches/{batchId}/items/{itemId}` | 删除条目 |
| POST | `/api/transfer/batches/{batchId}/submit` | 提交移交清单 |
| GET | `/api/transfer/batches/{batchId}/export` | 导出清单 PDF 骨架 |

- [ ] **Step 2: 编译验证**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
cd backend
git add src/main/java/com/archive/controller/TransferController.java
git commit -m "feat(intake): 实现移交门户 Controller"
```

---

### Task 8: 创建 PublicCollectionController

**Files:**
- Create: `backend/src/main/java/com/archive/controller/PublicCollectionController.java`

- [ ] **Step 1: 创建 PublicCollectionController.java**

接口清单（对应接口文档 Section 5.7-5.11）：
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/public/collections` | 查询本人征集清单 |
| POST | `/api/public/collections` | 创建征集清单草稿 |
| GET | `/api/public/collections/{batchId}` | 获取征集清单详情 |
| PUT | `/api/public/collections/{batchId}` | 更新征集草稿 |
| POST | `/api/public/collections/{batchId}/submit` | 提交征集清单 |

- [ ] **Step 2: 编译验证**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
cd backend
git add src/main/java/com/archive/controller/PublicCollectionController.java
git commit -m "feat(intake): 实现公众征集 Controller"
```

---

### Task 9: 创建 ReceptionController + CollectionManageController

**Files:**
- Create: `backend/src/main/java/com/archive/controller/ReceptionController.java`
- Create: `backend/src/main/java/com/archive/controller/CollectionManageController.java`

- [ ] **Step 1: 创建 ReceptionController.java**

接口清单（对应接口文档 Section 7，不含文件上传）：
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/reception/batches` | 查询待验收批次 |
| GET | `/api/admin/reception/batches/{batchId}` | 获取验收详情 |
| PUT | `/api/admin/reception/items/{itemId}/acceptance` | 条目验收/回退 |
| POST | `/api/admin/reception/batches/{batchId}/complete` | 完成批次验收 |
| GET | `/api/admin/reception/batches/{batchId}/receipt` | 导出接收回执骨架 |

- [ ] **Step 2: 创建 CollectionManageController.java**

接口清单（对应接口文档 Section 8）：
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/collections` | 查询征集待办 |
| POST | `/api/admin/collections/{batchId}/schedule` | 约定到馆时间 |
| POST | `/api/admin/collections/{batchId}/reject` | 拒绝征集意向 |

- [ ] **Step 3: 编译验证**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
cd backend
git add src/main/java/com/archive/controller/ReceptionController.java \
        src/main/java/com/archive/controller/CollectionManageController.java
git commit -m "feat(intake): 实现前台验收和征集管理 Controller"
```

---

### Task 10: 整体编译验证 + 推送

- [ ] **Step 1: 全量编译**

Run: `cd backend && ./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 推送到远端**

```bash
git push origin feat/transfer-zhou
```

- [ ] **Step 3: 确认 Swagger 可访问（可选，需启动服务）**

如需启动服务验证，需先确认 PostgreSQL 和环境配置可用。Swagger 地址：`http://localhost:8080/swagger-ui.html`
