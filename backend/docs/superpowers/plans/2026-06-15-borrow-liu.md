# feat/borrow-liu 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 M09 借阅管理后端——纸质档案借阅申请、审批、凭证、出库、归还完整闭环（9 个 REST 接口 + 补齐内部工作台借阅部分）。

**Architecture:** Spring Boot + MyBatis-Plus（PG）。`BorrowController`（9 端点，方法级全路径）→ `BorrowService`（状态机 + 角色强制 + 审计）→ `BorrowEligibilityChecker`（6 条件可借性 + 盘点拦截 SQL，申请/审批复校/出库复校三处复用）。状态机内联于 Service，借阅凭证 PDF 扩展现有 `PdfGenerator`，逾期动态计算不落库。

**Tech Stack:** Java 17 / Spring Boot 3.5 / MyBatis-Plus / PostgreSQL 17 / OpenPDF（lowagie）/ Sa-Token / JUnit 5 + AssertJ + Mockito 5（默认支持 `mockStatic`）。

**Spec:** [backend/docs/superpowers/specs/2026-06-15-borrow-liu-design.md](../specs/2026-06-15-borrow-liu-design.md)

**约定：** 所有命令在 `backend/` 目录执行。单测 `./mvnw test -Dtest=<类名>`，全量 `./mvnw test`。测试方法名用中文（与 `WarehouseServiceTest` 一致）。提交信息遵循 `docs/commit-convention.md`（`feat(borrow): ...` / `test(borrow): ...`）。

---

## 文件结构

### 新增（15）

| 文件 | 职责 |
|------|------|
| `enums/BorrowStatus.java` | 借阅申请 7 状态枚举 |
| `enums/ReturnCheckResult.java` | 归还检查 4 结果枚举 |
| `entity/BorrowRequest.java` | borrow_requests 实体（extends BaseEntity） |
| `mapper/BorrowRequestMapper.java` | BaseMapper<BorrowRequest> |
| `util/BorrowNoUtil.java` | BRW-/VCH- 编号生成 |
| `dto/request/BorrowApplyRequest.java` | 申请入参 + 校验 |
| `dto/request/BorrowApproveRequest.java` | 审批入参 |
| `dto/request/BorrowCheckoutRequest.java` | 出库入参 |
| `dto/request/BorrowReturnRequest.java` | 归还入参 |
| `dto/request/BorrowRequestQuery.java` | 列表查询（extends PageRequest） |
| `dto/response/BorrowRequestResponse.java` | 列表/详情响应（含 archive/borrower/location 摘要） |
| `dto/response/BorrowSummaryItem.java` | 工作台借阅摘要 |
| `service/BorrowEligibilityChecker.java` | 可借性校验组件 |
| `service/BorrowService.java` | 借阅主服务 |
| `controller/BorrowController.java` | 9 端点 |

### 改动（3）

| 文件 | 改动 |
|------|------|
| `dto/response/InternalDashboardResponse.java` | `List<Object>` ×3 → `List<BorrowSummaryItem>` ×3 |
| `service/SearchService.java` | `getInternalDashboard` 注入并调用 `BorrowService` 三个 dashboard 方法 |
| `util/PdfGenerator.java` | 新增 `generateBorrowVoucherPdf` 方法 |

### 测试（4）

| 文件 |
|------|
| `test/.../util/BorrowNoUtilTest.java` |
| `test/.../service/BorrowEligibilityCheckerTest.java` |
| `test/.../service/BorrowServiceTest.java` |
| `test/.../util/PdfGeneratorBorrowVoucherTest.java` |

---

## Task 1: 借阅状态枚举

**Files:**
- Create: `src/main/java/com/archive/enums/BorrowStatus.java`
- Create: `src/main/java/com/archive/enums/ReturnCheckResult.java`
- Create: `src/test/java/com/archive/enums/BorrowStatusTest.java`

- [ ] **Step 1: 写失败测试（枚举名对齐 V7 CHECK 约束）**

`src/test/java/com/archive/enums/BorrowStatusTest.java`：

```java
package com.archive.enums;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BorrowStatusTest {

    @Test
    void borrowStatus名称集合对齐borrow_requests表的CHECK约束() {
        Set<String> names = java.util.Arrays.stream(BorrowStatus.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder(
                "applied", "rejected", "approved",
                "voucher_issued", "checked_out", "returned", "abnormal_return");
    }

    @Test
    void returnCheckResult名称集合对齐CHECK约束() {
        Set<String> names = java.util.Arrays.stream(ReturnCheckResult.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder(
                "normal", "damaged", "missing_page", "other");
    }

    @Test
    void 每个借阅状态都有中文展示名() {
        for (BorrowStatus s : BorrowStatus.values()) {
            assertThat(s.getDisplayName()).isNotBlank();
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowStatusTest`
Expected: 编译失败（`BorrowStatus` / `ReturnCheckResult` 不存在）。

- [ ] **Step 3: 实现 BorrowStatus**

`src/main/java/com/archive/enums/BorrowStatus.java`：

```java
package com.archive.enums;

import lombok.Getter;

/**
 * 借阅申请状态枚举。
 * name() 与 borrow_requests.status 的 CHECK 约束一一对应，由全局 MybatisEnumTypeHandler 持久化。
 */
@Getter
public enum BorrowStatus {

    applied("已申请"),
    rejected("已拒绝"),
    approved("已审批通过"),
    voucher_issued("凭证已发出"),
    checked_out("已出库"),
    returned("已归还"),
    abnormal_return("异常归还");

    private final String displayName;

    BorrowStatus(String displayName) {
        this.displayName = displayName;
    }
}
```

- [ ] **Step 4: 实现 ReturnCheckResult**

`src/main/java/com/archive/enums/ReturnCheckResult.java`：

```java
package com.archive.enums;

import lombok.Getter;

/**
 * 借阅归还检查结果枚举。
 * name() 与 borrow_requests.return_check_result 的 CHECK 约束一一对应。
 */
@Getter
public enum ReturnCheckResult {

    normal("正常"),
    damaged("破损"),
    missing_page("缺页"),
    other("其他");

    private final String displayName;

    ReturnCheckResult(String displayName) {
        this.displayName = displayName;
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowStatusTest`
Expected: PASS（3 个测试）。

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/archive/enums/BorrowStatus.java \
        src/main/java/com/archive/enums/ReturnCheckResult.java \
        src/test/java/com/archive/enums/BorrowStatusTest.java
git commit -m "feat(borrow): 新增借阅状态与归还结果枚举"
```

---

## Task 2: 借阅编号工具 BorrowNoUtil

**Files:**
- Create: `src/main/java/com/archive/util/BorrowNoUtil.java`
- Create: `src/test/java/com/archive/util/BorrowNoUtilTest.java`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/archive/util/BorrowNoUtilTest.java`：

```java
package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowNoUtilTest {

    @Test
    void 申请号格式为BRW加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_borrow_request_no')"), eq(Long.class)))
                .thenReturn(1L);

        assertThat(new BorrowNoUtil(jdbc).nextRequestNo()).isEqualTo("BRW-000001");
    }

    @Test
    void 申请号大序号也能补零() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_borrow_request_no')"), eq(Long.class)))
                .thenReturn(123456L);

        assertThat(new BorrowNoUtil(jdbc).nextRequestNo()).isEqualTo("BRW-123456");
    }

    @Test
    void 凭证号格式为VCH加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_borrow_voucher_no')"), eq(Long.class)))
                .thenReturn(7L);

        assertThat(new BorrowNoUtil(jdbc).nextVoucherNo()).isEqualTo("VCH-000007");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowNoUtilTest`
Expected: 编译失败（`BorrowNoUtil` 不存在）。

- [ ] **Step 3: 实现 BorrowNoUtil**

`src/main/java/com/archive/util/BorrowNoUtil.java`：

```java
package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 借阅编号生成工具。
 * 申请号 BRW-{6位序号} 使用 seq_borrow_request_no；
 * 凭证号 VCH-{6位序号} 使用 seq_borrow_voucher_no（首次导出凭证时生成）。
 */
@Component
@RequiredArgsConstructor
public class BorrowNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String nextRequestNo() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_borrow_request_no')", Long.class);
        return String.format("BRW-%06d", seq);
    }

    public String nextVoucherNo() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_borrow_voucher_no')", Long.class);
        return String.format("VCH-%06d", seq);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowNoUtilTest`
Expected: PASS（3 个测试）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/util/BorrowNoUtil.java \
        src/test/java/com/archive/util/BorrowNoUtilTest.java
git commit -m "feat(borrow): 新增借阅编号与凭证号生成工具"
```

## Task 3: BorrowRequest 实体与 Mapper

> 说明：实体/Mapper 是 ORM 映射层，无独立行为，其正确性由 Task 4 起的 Service/Checker 测试覆盖。本任务以「编译通过」为验证点。
> **软删除约定**：项目未启用 `@TableLogic`/全局逻辑删除，`deleted_at` 为普通字段（仿 `User`），所有借阅查询需手动 `.isNull("deleted_at")`。

**Files:**
- Create: `src/main/java/com/archive/entity/BorrowRequest.java`
- Create: `src/main/java/com/archive/mapper/BorrowRequestMapper.java`

- [ ] **Step 1: 实现 BorrowRequest 实体**

`src/main/java/com/archive/entity/BorrowRequest.java`：

```java
package com.archive.entity;

import com.archive.enums.BorrowStatus;
import com.archive.enums.ReturnCheckResult;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 借阅申请实体（borrow_requests）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("borrow_requests")
public class BorrowRequest extends BaseEntity {

    private String requestNo;

    private Long archiveId;

    private Long borrowerId;

    private String reason;

    private Integer expectedDays;

    private OffsetDateTime expectedVisitAt;

    private String contactPhone;

    @EnumValue
    private BorrowStatus status;

    private Long approvedBy;

    private OffsetDateTime approvedAt;

    private String rejectReason;

    private String voucherNo;

    private OffsetDateTime voucherIssuedAt;

    private Long checkedOutBy;

    private OffsetDateTime checkedOutAt;

    private OffsetDateTime dueAt;

    private Long returnedBy;

    private OffsetDateTime returnedAt;

    @EnumValue
    private ReturnCheckResult returnCheckResult;

    private String returnNote;

    /** 软删除标记，项目未启用 @TableLogic，查询时手动 isNull("deleted_at")。 */
    private OffsetDateTime deletedAt;
}
```

- [ ] **Step 2: 实现 BorrowRequestMapper**

`src/main/java/com/archive/mapper/BorrowRequestMapper.java`：

```java
package com.archive.mapper;

import com.archive.entity.BorrowRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 借阅申请 Mapper。
 */
public interface BorrowRequestMapper extends BaseMapper<BorrowRequest> {
}
```

- [ ] **Step 3: 验证编译通过**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS（实体注解、枚举 @EnumValue、BaseMapper 绑定均正确）。

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/archive/entity/BorrowRequest.java \
        src/main/java/com/archive/mapper/BorrowRequestMapper.java
git commit -m "feat(borrow): 新增借阅申请实体与 Mapper"
```

---

## Task 4: BorrowEligibilityChecker 可借性校验

**Files:**
- Create: `src/main/java/com/archive/service/BorrowEligibilityChecker.java`
- Create: `src/test/java/com/archive/service/BorrowEligibilityCheckerTest.java`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/archive/service/BorrowEligibilityCheckerTest.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.entity.Archive;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.archive.enums.CarrierStatus;
import com.archive.enums.ConditionStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.LoanStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowEligibilityCheckerTest {

    private BorrowEligibilityChecker checker;
    private ArchiveMapper archiveMapper;
    private BorrowRequestMapper borrowRequestMapper;
    private JdbcTemplate jdbcTemplate;

    private static final Long ARCHIVE_ID = 1L;
    private static final Long ROOM_ID = 5L;
    private static final Integer CATEGORY_ID = 3;

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        borrowRequestMapper = mock(BorrowRequestMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        checker = new BorrowEligibilityChecker(archiveMapper, borrowRequestMapper, jdbcTemplate);

        // 基线：room 解析成功、无 running 盘点、无未结束申请
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARCHIVE_ID)))
                .thenReturn(ROOM_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(ROOM_ID), eq(CATEGORY_ID)))
                .thenReturn(0);
        when(borrowRequestMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
    }

    private Archive borrowableArchive() {
        Archive a = new Archive();
        a.setId(ARCHIVE_ID);
        a.setCarrierStatus(CarrierStatus.paper);
        a.setLifecycleStatus(LifecycleStatus.normal);
        a.setLoanStatus(LoanStatus.available);
        a.setConditionStatus(ConditionStatus.normal);
        a.setCategoryId(CATEGORY_ID);
        return a;
    }

    private void assertBiz(Runnable action, ErrorCode code, String msgFragment) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(code))
                .hasMessageContaining(msgFragment);
    }

    @Test
    void 档案不存在抛NOT_FOUND() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(null);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID), ErrorCode.NOT_FOUND, "档案不存在");
    }

    @Test
    void 纯电子档案拒绝() {
        Archive a = borrowableArchive();
        a.setCarrierStatus(CarrierStatus.electronic);
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(a);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.VALIDATION_FAILED, "纯电子档案不可借阅");
    }

    @Test
    void 生命周期非正常拒绝() {
        Archive a = borrowableArchive();
        a.setLifecycleStatus(LifecycleStatus.pending_shelf);
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(a);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "生命周期状态异常");
    }

    @Test
    void 已借出拒绝() {
        Archive a = borrowableArchive();
        a.setLoanStatus(LoanStatus.on_loan);
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(a);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "已借出中");
    }

    @Test
    void 实体损坏拒绝() {
        Archive a = borrowableArchive();
        a.setConditionStatus(ConditionStatus.damaged);
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(a);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "实体状态异常");
    }

    @Test
    void 命中运行盘点拒绝() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(ROOM_ID), eq(CATEGORY_ID)))
                .thenReturn(1);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "正在盘点");
    }

    @Test
    void 存在未结束申请拒绝() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        when(borrowRequestMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "未结束的借阅申请");
    }

    @Test
    void 全部条件满足通过() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        assertThatCode(() -> checker.checkBorrowable(ARCHIVE_ID)).doesNotThrowAnyException();
    }

    @Test
    void 档案无盒位时跳过盘点仍通过() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARCHIVE_ID)))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertThatCode(() -> checker.checkBorrowable(ARCHIVE_ID)).doesNotThrowAnyException();
    }
}
```

> 说明：`assertBiz` 复用断言 errorCode + 消息片段，顶部已静态导入 `assertThat`/`assertThatCode`/`assertThatThrownBy`。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowEligibilityCheckerTest`
Expected: 编译失败（`BorrowEligibilityChecker` 不存在）。

- [ ] **Step 3: 实现 BorrowEligibilityChecker**

`src/main/java/com/archive/service/BorrowEligibilityChecker.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.enums.BorrowStatus;
import com.archive.enums.CarrierStatus;
import com.archive.enums.ConditionStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.LoanStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 借阅可借性校验组件。
 * 申请、审批复校（12.3）、出库复校（12.4）三处复用，保证状态变更瞬间档案仍可借。
 */
@Component
@RequiredArgsConstructor
public class BorrowEligibilityChecker {

    private final ArchiveMapper archiveMapper;
    private final BorrowRequestMapper borrowRequestMapper;
    private final JdbcTemplate jdbcTemplate;

    /** 解析档案所在库房 room_id（纸质档案应恰有一个盒位）。 */
    private static final String ROOM_OF_ARCHIVE_SQL =
            "SELECT sl.room_id FROM archive_box_items abi " +
            "JOIN archive_boxes ab ON ab.id = abi.box_id " +
            "JOIN storage_locations sl ON sl.id = ab.location_id " +
            "WHERE abi.archive_id = ? AND abi.deleted_at IS NULL " +
            "LIMIT 1";

    /** 命中运行中盘点任务计数。 */
    private static final String RUNNING_INVENTORY_SQL =
            "SELECT COUNT(*) FROM inventory_tasks " +
            "WHERE status = 'running' AND room_id = ? AND category_id = ?";

    /**
     * 校验档案可借。任一条件不满足抛 BusinessException（附具体原因）。
     */
    public void checkBorrowable(Long archiveId) {
        // 1. 档案存在
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在");
        }

        // 2. 载体：纸质或纸质+电子
        CarrierStatus carrier = archive.getCarrierStatus();
        if (carrier != CarrierStatus.paper && carrier != CarrierStatus.paper_electronic) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "纯电子档案不可借阅");
        }

        // 3. 生命周期正常
        if (archive.getLifecycleStatus() != LifecycleStatus.normal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案当前不可借阅（生命周期状态异常）");
        }

        // 4. 未借出
        if (archive.getLoanStatus() != LoanStatus.available) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案已借出中");
        }

        // 5. 实体状态正常
        if (archive.getConditionStatus() != ConditionStatus.normal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案实体状态异常，暂停借阅");
        }

        // 6. 未命中运行中盘点（按 room + category 匹配）
        Long roomId = resolveRoomId(archiveId);
        if (roomId != null) {
            Integer running = jdbcTemplate.queryForObject(
                    RUNNING_INVENTORY_SQL, Integer.class, roomId, archive.getCategoryId());
            if (running != null && running > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                        "档案所在架位/门类正在盘点，暂停借阅");
            }
        }

        // 7. 无未结束借阅申请
        QueryWrapper<BorrowRequest> qw = new QueryWrapper<>();
        qw.eq("archive_id", archiveId)
          .isNull("deleted_at")
          .in("status",
                  BorrowStatus.applied.name(),
                  BorrowStatus.approved.name(),
                  BorrowStatus.voucher_issued.name(),
                  BorrowStatus.checked_out.name());
        Long openCount = borrowRequestMapper.selectCount(qw);
        if (openCount != null && openCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "该档案存在未结束的借阅申请");
        }
    }

    private Long resolveRoomId(Long archiveId) {
        try {
            return jdbcTemplate.queryForObject(ROOM_OF_ARCHIVE_SQL, Long.class, archiveId);
        } catch (EmptyResultDataAccessException e) {
            // 档案暂无盒位（未上架），lifecycle 校验已先行拦截，此处跳过盘点
            return null;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowEligibilityCheckerTest`
Expected: PASS（8 个测试）。若 `assertBiz` 中 `assertThat` 未识别，确认顶部已 `import static org.assertj.core.api.Assertions.assertThat;`。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/BorrowEligibilityChecker.java \
        src/test/java/com/archive/service/BorrowEligibilityCheckerTest.java
git commit -m "feat(borrow): 实现借阅可借性校验与盘点拦截"
```

## Task 5: 借阅请求 DTO

**Files:**
- Create: `src/main/java/com/archive/dto/request/BorrowApplyRequest.java`
- Create: `src/main/java/com/archive/dto/request/BorrowApproveRequest.java`
- Create: `src/main/java/com/archive/dto/request/BorrowCheckoutRequest.java`
- Create: `src/main/java/com/archive/dto/request/BorrowReturnRequest.java`
- Create: `src/main/java/com/archive/dto/request/BorrowRequestQuery.java`
- Create: `src/test/java/com/archive/dto/request/BorrowApplyRequestValidationTest.java`

- [ ] **Step 1: 写失败测试（BorrowApplyRequest 校验约束）**

`src/test/java/com/archive/dto/request/BorrowApplyRequestValidationTest.java`：

```java
package com.archive.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BorrowApplyRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private BorrowApplyRequest valid() {
        BorrowApplyRequest r = new BorrowApplyRequest();
        r.setArchiveId(1L);
        r.setReason("财政预算核查需要查阅原件");
        r.setExpectedDays(7);
        return r;
    }

    @Test
    void 合法请求无违反() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    void expectedDays小于1违反约束() {
        BorrowApplyRequest r = valid();
        r.setExpectedDays(0);
        Set<ConstraintViolation<BorrowApplyRequest>> v = validator.validate(r);
        assertThat(v).anyMatch(c -> c.getPropertyPath().toString().equals("expectedDays"));
    }

    @Test
    void 理由为空违反约束() {
        BorrowApplyRequest r = valid();
        r.setReason("");
        Set<ConstraintViolation<BorrowApplyRequest>> v = validator.validate(r);
        assertThat(v).anyMatch(c -> c.getPropertyPath().toString().equals("reason"));
    }

    @Test
    void 手机号格式错误违反约束() {
        BorrowApplyRequest r = valid();
        r.setContactPhone("12345");
        assertThat(validator.validate(r))
                .anyMatch(c -> c.getPropertyPath().toString().equals("contactPhone"));
    }

    @Test
    void 手机号留空合法() {
        BorrowApplyRequest r = valid();
        r.setContactPhone(null);
        assertThat(validator.validate(r)).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowApplyRequestValidationTest`
Expected: 编译失败（`BorrowApplyRequest` 不存在）。

- [ ] **Step 3: 实现 BorrowApplyRequest**

`src/main/java/com/archive/dto/request/BorrowApplyRequest.java`：

```java
package com.archive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 提交借阅申请入参（11.7）。
 */
@Data
public class BorrowApplyRequest {

    @NotNull(message = "档案 ID 不能为空")
    private Long archiveId;

    @NotBlank(message = "借阅理由不能为空")
    @Size(max = 500, message = "借阅理由不超过 500 字")
    private String reason;

    @NotNull(message = "预计借阅天数不能为空")
    @Min(value = 1, message = "预计借阅天数至少 1 天")
    @Max(value = 365, message = "预计借阅天数不超过 365 天")
    private Integer expectedDays;

    /** 预计到馆取件时间，可空。 */
    private OffsetDateTime expectedVisitAt;

    /** 联系电话，可空；非空时须为 11 位手机号。 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;
}
```

- [ ] **Step 4: 实现 BorrowApproveRequest**

`src/main/java/com/archive/dto/request/BorrowApproveRequest.java`：

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批借阅申请入参（12.3）。
 * 拒绝时 opinion 必填，由 Service 层校验（Bean Validation 无法表达条件必填）。
 */
@Data
public class BorrowApproveRequest {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Size(max = 500)
    private String opinion;
}
```

- [ ] **Step 5: 实现 BorrowCheckoutRequest**

`src/main/java/com/archive/dto/request/BorrowCheckoutRequest.java`：

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 核验凭证并确认出库入参（12.4）。dueAt 须晚于当前时间，由 Service 层校验。
 */
@Data
public class BorrowCheckoutRequest {

    @NotBlank(message = "凭证号不能为空")
    private String voucherNo;

    @NotNull(message = "应还时间不能为空")
    private OffsetDateTime dueAt;

    @Size(max = 500)
    private String note;
}
```

- [ ] **Step 6: 实现 BorrowReturnRequest**

`src/main/java/com/archive/dto/request/BorrowReturnRequest.java`：

```java
package com.archive.dto.request;

import com.archive.enums.ReturnCheckResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 确认归还入参（12.5）。
 */
@Data
public class BorrowReturnRequest {

    @NotNull(message = "归还检查结果不能为空")
    private ReturnCheckResult returnCheckResult;

    @Size(max = 500)
    private String returnNote;
}
```

- [ ] **Step 7: 实现 BorrowRequestQuery**

`src/main/java/com/archive/dto/request/BorrowRequestQuery.java`：

```java
package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 借阅申请列表查询参数。
 * 内部查阅者用 keyword（我的列表）；管理端用 borrowerKeyword/archiveKeyword。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowRequestQuery extends PageRequest {

    private String status;
    private String borrowerKeyword;
    private String archiveKeyword;
    private Boolean overdue;
    private String keyword;
}
```

- [ ] **Step 8: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowApplyRequestValidationTest`
Expected: PASS（5 个测试）。

- [ ] **Step 9: 提交**

```bash
git add src/main/java/com/archive/dto/request/Borrow*.java \
        src/test/java/com/archive/dto/request/BorrowApplyRequestValidationTest.java
git commit -m "feat(borrow): 新增借阅请求入参与校验"
```

---

## Task 6: 借阅响应 DTO

> 纯数据载体，无独立行为，由 BorrowServiceTest 的响应断言覆盖。

**Files:**
- Create: `src/main/java/com/archive/dto/response/BorrowRequestResponse.java`
- Create: `src/main/java/com/archive/dto/response/BorrowSummaryItem.java`

- [ ] **Step 1: 实现 BorrowRequestResponse**

`src/main/java/com/archive/dto/response/BorrowRequestResponse.java`：

```java
package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 借阅申请列表/详情响应（11.8/11.9/12.1/12.2）。
 * archive 总是填充；borrower/location 仅管理端填充，内部端为 null。
 */
@Data
@AllArgsConstructor
public class BorrowRequestResponse {

    private Long id;
    private String requestNo;
    private String status;
    private String reason;
    private Integer expectedDays;
    private OffsetDateTime expectedVisitAt;
    private String contactPhone;
    private OffsetDateTime dueAt;
    /** 动态计算：status=checked_out 且 dueAt<now。 */
    private Boolean overdue;
    private String rejectReason;
    private String voucherNo;
    private OffsetDateTime voucherIssuedAt;
    private OffsetDateTime approvedAt;
    private Long approvedBy;
    private OffsetDateTime checkedOutAt;
    private OffsetDateTime returnedAt;
    private String returnCheckResult;
    private String returnNote;
    private OffsetDateTime createdAt;

    private ArchiveSummary archive;
    private BorrowerSummary borrower;
    private LocationSummary location;

    @Data
    @AllArgsConstructor
    public static class ArchiveSummary {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private String carrierStatus;
    }

    @Data
    @AllArgsConstructor
    public static class BorrowerSummary {
        private Long borrowerId;
        private String realName;
        private String employeeNo;
        private String departmentName;
        private String organizationName;
    }

    @Data
    @AllArgsConstructor
    public static class LocationSummary {
        private String boxNo;
        private String locationCode;
    }
}
```

- [ ] **Step 2: 实现 BorrowSummaryItem**

`src/main/java/com/archive/dto/response/BorrowSummaryItem.java`：

```java
package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 内部工作台借阅摘要（11.1 的我的申请/当前借阅/逾期提示）。
 */
@Data
@AllArgsConstructor
public class BorrowSummaryItem {

    private String requestNo;
    private Long archiveId;
    private String archiveNo;
    private String title;
    private String status;
    private OffsetDateTime dueAt;
    private OffsetDateTime appliedAt;
}
```

- [ ] **Step 3: 验证编译通过**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS。

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/archive/dto/response/BorrowRequestResponse.java \
        src/main/java/com/archive/dto/response/BorrowSummaryItem.java
git commit -m "feat(borrow): 新增借阅响应与工作台摘要 DTO"
```

## Task 7: BorrowService 申请 / 我的列表 / 我的详情

> 角色强制在 Service 内读 `AuthContext`（与 `SearchService.internalSearch` 约定一致），测试用 Mockito 5 `mockStatic(AuthContext.class)`。

**Files:**
- Create: `src/main/java/com/archive/service/BorrowService.java`
- Create: `src/test/java/com/archive/service/BorrowServiceTest.java`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/archive/service/BorrowServiceTest.java`：

```java
package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.PageResult;
import com.archive.dto.request.BorrowApplyRequest;
import com.archive.dto.request.BorrowRequestQuery;
import com.archive.dto.response.BorrowRequestResponse;
import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.enums.BorrowStatus;
import com.archive.enums.CarrierStatus;
import com.archive.enums.ConditionStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.LoanStatus;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.archive.mapper.OrganizationMapper;
import com.archive.mapper.UserMapper;
import com.archive.util.BorrowNoUtil;
import com.archive.util.PdfGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    private BorrowService service;
    private BorrowRequestMapper borrowRequestMapper;
    private ArchiveMapper archiveMapper;
    private UserMapper userMapper;
    private OrganizationMapper organizationMapper;
    private JdbcTemplate jdbcTemplate;
    private BorrowNoUtil borrowNoUtil;
    private BorrowEligibilityChecker eligibilityChecker;
    private PdfGenerator pdfGenerator;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        borrowRequestMapper = mock(BorrowRequestMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        userMapper = mock(UserMapper.class);
        organizationMapper = mock(OrganizationMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        borrowNoUtil = mock(BorrowNoUtil.class);
        eligibilityChecker = mock(BorrowEligibilityChecker.class);
        pdfGenerator = mock(PdfGenerator.class);
        auditService = mock(AuditService.class);

        service = new BorrowService(borrowRequestMapper, archiveMapper, userMapper,
                organizationMapper, jdbcTemplate, borrowNoUtil,
                eligibilityChecker, pdfGenerator, auditService);
    }

    // ---- 角色辅助：包内打开 MockedStatic<AuthContext> ----

    private void asRole(RoleCode role, long userId, Runnable body) {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            if (role != null) {
                a.when(() -> AuthContext.hasRole(role)).thenReturn(true);
            }
            a.when(AuthContext::getCurrentUserId).thenReturn(userId);
            body.run();
        }
    }

    private Archive borrowableArchive(Long id) {
        Archive a = new Archive();
        a.setId(id);
        a.setArchiveNo("ARC-000002");
        a.setTitle("测试档案");
        a.setCarrierStatus(CarrierStatus.paper);
        a.setLifecycleStatus(LifecycleStatus.normal);
        a.setLoanStatus(LoanStatus.available);
        a.setConditionStatus(ConditionStatus.normal);
        a.setCategoryId(3);
        return a;
    }

    private BorrowRequest sampleRequest(Long id, String requestNo, Long borrowerId, BorrowStatus status) {
        BorrowRequest b = new BorrowRequest();
        b.setId(id);
        b.setRequestNo(requestNo);
        b.setBorrowerId(borrowerId);
        b.setArchiveId(2L);
        b.setStatus(status);
        b.setReason("核查");
        b.setExpectedDays(7);
        return b;
    }

    // ---- 11.7 apply ----

    @Test
    void apply_内部查阅者提交申请成功() {
        when(borrowNoUtil.nextRequestNo()).thenReturn("BRW-000001");
        when(borrowRequestMapper.insert(any(BorrowRequest.class))).thenAnswer(inv -> {
            ((BorrowRequest) inv.getArgument(0)).setId(100L);
            return 1;
        });
        when(archiveMapper.selectById(2L)).thenReturn(borrowableArchive(2L));

        BorrowApplyRequest req = new BorrowApplyRequest();
        req.setArchiveId(2L);
        req.setReason("财政核查");
        req.setExpectedDays(7);

        asRole(RoleCode.internal_reader, 4L, () -> {
            BorrowRequestResponse resp = service.apply(req);
            assertThat(resp.getId()).isEqualTo(100L);
            assertThat(resp.getRequestNo()).isEqualTo("BRW-000001");
            assertThat(resp.getStatus()).isEqualTo("applied");
            assertThat(resp.getBorrower()).isNull(); // 内部端不带 borrower
        });

        verify(borrowRequestMapper).insert(any(BorrowRequest.class));
        verify(eligibilityChecker).checkBorrowable(2L);
        verify(auditService).log(eq("M09"), eq("apply"), eq("borrow_request"), eq(100L), any());
    }

    @Test
    void apply_非内部查阅者抛FORBIDDEN() {
        BorrowApplyRequest req = new BorrowApplyRequest();
        req.setArchiveId(2L);
        req.setReason("财政核查");
        req.setExpectedDays(7);

        asRole(null, 4L, () ->
                assertThatThrownBy(() -> service.apply(req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }

    // ---- 11.8 listMine ----

    @Test
    void listMine_按当前用户过滤分页且内部端不带borrower() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        Page<BorrowRequest> page = new Page<>(1, 20);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(borrowRequestMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        BorrowRequestQuery q = new BorrowRequestQuery();
        asRole(RoleCode.internal_reader, 4L, () -> {
            PageResult<BorrowRequestResponse> result = service.listMine(q);
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getRequestNo()).isEqualTo("BRW-000001");
            assertThat(result.getRecords().get(0).getBorrower()).isNull();
        });
    }

    // ---- 11.9 getMine ----

    @Test
    void getMine_本人申请可见() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThat(service.getMine(1L).getRequestNo()).isEqualTo("BRW-000001"));
    }

    @Test
    void getMine_非本人申请抛FORBIDDEN() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 9L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.getMine(1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无权查看"));
    }

    @Test
    void getMine_申请不存在抛NOT_FOUND() {
        when(borrowRequestMapper.selectById(1L)).thenReturn(null);
        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.getMine(1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("不存在"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: 编译失败（`BorrowService` 不存在）。

- [ ] **Step 3: 实现 BorrowService（apply / listMine / getMine + 公共辅助）**

`src/main/java/com/archive/service/BorrowService.java`：

```java
package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.BorrowApplyRequest;
import com.archive.dto.request.BorrowRequestQuery;
import com.archive.dto.response.BorrowRequestResponse;
import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.entity.Organization;
import com.archive.entity.User;
import com.archive.enums.BorrowStatus;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.archive.mapper.OrganizationMapper;
import com.archive.mapper.UserMapper;
import com.archive.util.BorrowNoUtil;
import com.archive.util.PdfGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 借阅管理主服务（M09）。
 * 状态机内联：applied→approved/rejected→voucher_issued→checked_out→returned/abnormal_return。
 * 角色强制与审计写入在此层完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRequestMapper borrowRequestMapper;
    private final ArchiveMapper archiveMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BorrowNoUtil borrowNoUtil;
    private final BorrowEligibilityChecker eligibilityChecker;
    private final PdfGenerator pdfGenerator;
    private final AuditService auditService;

    private static final String LOCATION_OF_ARCHIVE_SQL =
            "SELECT ab.box_no, sl.location_code FROM archive_box_items abi " +
            "JOIN archive_boxes ab ON ab.id = abi.box_id " +
            "JOIN storage_locations sl ON sl.id = ab.location_id " +
            "WHERE abi.archive_id = ? AND abi.deleted_at IS NULL LIMIT 1";

    // ==================== 11.7 提交借阅申请 ====================

    @Transactional
    public BorrowRequestResponse apply(BorrowApplyRequest req) {
        requireRole(RoleCode.internal_reader);
        long userId = AuthContext.getCurrentUserId();

        eligibilityChecker.checkBorrowable(req.getArchiveId());

        BorrowRequest b = new BorrowRequest();
        b.setRequestNo(borrowNoUtil.nextRequestNo());
        b.setArchiveId(req.getArchiveId());
        b.setBorrowerId(userId);
        b.setReason(req.getReason());
        b.setExpectedDays(req.getExpectedDays());
        b.setExpectedVisitAt(req.getExpectedVisitAt());
        b.setContactPhone(req.getContactPhone());
        b.setStatus(BorrowStatus.applied);
        borrowRequestMapper.insert(b);

        auditService.log("M09", "apply", "borrow_request", b.getId(),
                Map.of("archiveId", req.getArchiveId(), "requestNo", b.getRequestNo()));

        return toResponse(b, false, false);
    }

    // ==================== 11.8 查询我的借阅申请 ====================

    public PageResult<BorrowRequestResponse> listMine(BorrowRequestQuery query) {
        requireRole(RoleCode.internal_reader);
        long userId = AuthContext.getCurrentUserId();

        Page<BorrowRequest> page = new Page<>(query.getPageNo(), query.getPageSize());
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.eq("borrower_id", userId).isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            w.eq("status", query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            w.like("request_no", query.getKeyword());
        }
        if (Boolean.TRUE.equals(query.getOverdue())) {
            w.eq("status", BorrowStatus.checked_out.name())
             .isNotNull("due_at").lt("due_at", OffsetDateTime.now());
        }
        w.orderByDesc("created_at");

        Page<BorrowRequest> result = borrowRequestMapper.selectPage(page, w);
        List<BorrowRequestResponse> items = result.getRecords().stream()
                .map(b -> toResponse(b, false, false))
                .collect(Collectors.toList());
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    // ==================== 11.9 借阅申请详情（本人） ====================

    public BorrowRequestResponse getMine(Long requestId) {
        requireRole(RoleCode.internal_reader);
        BorrowRequest b = mustGet(requestId);
        if (!b.getBorrowerId().equals(AuthContext.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该借阅申请");
        }
        return toResponse(b, false, false);
    }

    // ==================== 公共辅助 ====================

    private BorrowRequest mustGet(Long requestId) {
        BorrowRequest b = borrowRequestMapper.selectById(requestId);
        if (b == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "借阅申请不存在");
        }
        return b;
    }

    private void requireRole(RoleCode... allowed) {
        for (RoleCode r : allowed) {
            if (AuthContext.hasRole(r)) return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
    }

    private BorrowRequestResponse toResponse(BorrowRequest b, boolean withBorrower, boolean withLocation) {
        Archive archive = b.getArchiveId() == null ? null : archiveMapper.selectById(b.getArchiveId());
        BorrowRequestResponse.ArchiveSummary archiveSummary = null;
        if (archive != null) {
            archiveSummary = new BorrowRequestResponse.ArchiveSummary(
                    archive.getId(), archive.getArchiveNo(), archive.getTitle(),
                    archive.getCarrierStatus() != null ? archive.getCarrierStatus().name() : null);
        }

        BorrowRequestResponse.BorrowerSummary borrowerSummary = null;
        if (withBorrower && b.getBorrowerId() != null) {
            User u = userMapper.selectById(b.getBorrowerId());
            if (u != null) {
                String orgName = null;
                if (u.getOrganizationId() != null) {
                    Organization org = organizationMapper.selectById(u.getOrganizationId());
                    orgName = org != null ? org.getOrgName() : null;
                }
                borrowerSummary = new BorrowRequestResponse.BorrowerSummary(
                        u.getId(), u.getRealName(), u.getEmployeeNo(),
                        u.getDepartmentName(), orgName);
            }
        }

        BorrowRequestResponse.LocationSummary locationSummary =
                withLocation ? resolveLocation(b.getArchiveId()) : null;

        boolean overdue = b.getStatus() == BorrowStatus.checked_out
                && b.getDueAt() != null && b.getDueAt().isBefore(OffsetDateTime.now());

        return new BorrowRequestResponse(
                b.getId(), b.getRequestNo(), b.getStatus().name(), b.getReason(),
                b.getExpectedDays(), b.getExpectedVisitAt(), b.getContactPhone(),
                b.getDueAt(), overdue, b.getRejectReason(), b.getVoucherNo(),
                b.getVoucherIssuedAt(), b.getApprovedAt(), b.getApprovedBy(),
                b.getCheckedOutAt(), b.getReturnedAt(),
                b.getReturnCheckResult() != null ? b.getReturnCheckResult().name() : null,
                b.getReturnNote(), b.getCreatedAt(),
                archiveSummary, borrowerSummary, locationSummary);
    }

    private BorrowRequestResponse.LocationSummary resolveLocation(Long archiveId) {
        if (archiveId == null) return null;
        try {
            return jdbcTemplate.queryForObject(LOCATION_OF_ARCHIVE_SQL,
                    (rs, rowNum) -> new BorrowRequestResponse.LocationSummary(
                            rs.getString("box_no"), rs.getString("location_code")),
                    archiveId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: PASS（6 个测试）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/BorrowService.java \
        src/test/java/com/archive/service/BorrowServiceTest.java
git commit -m "feat(borrow): 实现借阅申请提交与查阅者查询"
```

---

## Task 8: BorrowService 审批

**Files:**
- Modify: `src/main/java/com/archive/service/BorrowService.java`（新增 `approve` 方法 + import `BorrowApproveRequest`）
- Modify: `src/test/java/com/archive/service/BorrowServiceTest.java`（追加审批测试）

- [ ] **Step 1: 追加失败测试**

在 `BorrowServiceTest` 末尾大括号前追加：

```java
    // ---- 12.3 approve ----

    @Test
    void approve_通过则状态变approved并记录处理人() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(true);
        req.setOpinion("同意借阅7天");

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.approve(1L, req);
            assertThat(resp.getStatus()).isEqualTo("approved");
            assertThat(resp.getApprovedBy()).isEqualTo(2L);
            assertThat(resp.getRejectReason()).isNull();
        });
    }

    @Test
    void approve_拒绝则状态变rejected并写rejectReason() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(false);
        req.setOpinion("档案盘点中");

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.approve(1L, req);
            assertThat(resp.getStatus()).isEqualTo("rejected");
            assertThat(resp.getRejectReason()).isEqualTo("档案盘点中");
        });
    }

    @Test
    void approve_拒绝未填意见抛VALIDATION_FAILED() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(false);

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.approve(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("意见"));
    }

    @Test
    void approve_非applied状态抛BUSINESS_CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.checked_out);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(true);

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.approve(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("当前状态"));
    }

    @Test
    void approve_非后台管理员抛FORBIDDEN() {
        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(true);

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.approve(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }
```

并在测试文件顶部 import 区追加：

```java
import com.archive.dto.request.BorrowApproveRequest;
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: 编译失败（`service.approve(...)` 不存在）。

- [ ] **Step 3: 实现 approve 方法**

在 `BorrowService` import 区追加：

```java
import com.archive.dto.request.BorrowApproveRequest;
```

在 `getMine` 方法之后、「公共辅助」注释之前，插入 `approve`：

```java
    // ==================== 12.3 审批借阅申请 ====================

    @Transactional
    public BorrowRequestResponse approve(Long requestId, BorrowApproveRequest req) {
        requireRole(RoleCode.back_archivist);
        long reviewer = AuthContext.getCurrentUserId();

        BorrowRequest b = mustGet(requestId);
        if (b.getStatus() != BorrowStatus.applied) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "当前状态不允许审批");
        }

        // 审批前重新校验可借状态与盘点范围
        eligibilityChecker.checkBorrowable(b.getArchiveId());

        OffsetDateTime now = OffsetDateTime.now();
        b.setApprovedBy(reviewer);
        b.setApprovedAt(now);

        if (Boolean.TRUE.equals(req.getApproved())) {
            b.setStatus(BorrowStatus.approved);
            b.setRejectReason(null);
            borrowRequestMapper.updateById(b);
            auditService.log("M09", "approve", "borrow_request", b.getId(),
                    Map.of("opinion", req.getOpinion() != null ? req.getOpinion() : ""));
        } else {
            String reason = req.getOpinion();
            if (reason == null || reason.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "拒绝时必须填写意见");
            }
            b.setStatus(BorrowStatus.rejected);
            b.setRejectReason(reason);
            borrowRequestMapper.updateById(b);
            auditService.log("M09", "reject", "borrow_request", b.getId(),
                    Map.of("rejectReason", reason));
        }
        return toResponse(b, true, false);
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: PASS（11 个测试）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/BorrowService.java \
        src/test/java/com/archive/service/BorrowServiceTest.java
git commit -m "feat(borrow): 实现借阅审批通过与拒绝"
```

## Task 9: 借阅凭证 PDF 生成

> 扩展现有 `PdfGenerator`（复用 `getCnFont`/`makeCell`/`addInfoRow` 与 OpenPDF 基础设施，与移交清单/回执同源）。`PdfGenerator` 无构造依赖，可直接 `new`。

**Files:**
- Modify: `src/main/java/com/archive/util/PdfGenerator.java`（新增方法 + import）
- Create: `src/test/java/com/archive/util/PdfGeneratorBorrowVoucherTest.java`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/archive/util/PdfGeneratorBorrowVoucherTest.java`：

```java
package com.archive.util;

import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.entity.Organization;
import com.archive.entity.User;
import com.archive.enums.BorrowStatus;
import com.archive.enums.CarrierStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PdfGeneratorBorrowVoucherTest {

    @Test
    void 生成非空PDF且以PDF头开头() {
        BorrowRequest b = new BorrowRequest();
        b.setRequestNo("BRW-000001");
        b.setVoucherNo("VCH-000001");
        b.setStatus(BorrowStatus.voucher_issued);
        b.setReason("财政预算核查需要查阅原件");
        b.setExpectedDays(7);
        b.setExpectedVisitAt(OffsetDateTime.now().plusDays(1));
        b.setApprovedAt(OffsetDateTime.now());

        Archive a = new Archive();
        a.setArchiveNo("ARC-000001");
        a.setTitle("2025 年第一季度会计凭证");
        a.setCarrierStatus(CarrierStatus.paper);

        User u = new User();
        u.setRealName("小李");
        u.setEmployeeNo("E001");
        u.setPhone("13800000004");
        u.setDepartmentName("财务部");

        Organization org = new Organization();
        org.setOrgName("克拉玛依市某单位");

        byte[] pdf = new PdfGenerator().generateBorrowVoucherPdf(b, a, u, org);

        assertThat(pdf).isNotEmpty();
        assertThat(pdf.length).isGreaterThan(100);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void 借阅人或单位为空时不抛异常() {
        BorrowRequest b = new BorrowRequest();
        b.setRequestNo("BRW-000001");
        b.setVoucherNo("VCH-000001");
        b.setStatus(BorrowStatus.voucher_issued);
        b.setExpectedDays(7);

        Archive a = new Archive();
        a.setArchiveNo("ARC-000001");
        a.setTitle("测试档案");
        a.setCarrierStatus(CarrierStatus.paper);

        byte[] pdf = new PdfGenerator().generateBorrowVoucherPdf(b, a, null, null);
        assertThat(pdf).isNotEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=PdfGeneratorBorrowVoucherTest`
Expected: 编译失败（`generateBorrowVoucherPdf` 方法不存在）。

- [ ] **Step 3: 实现凭证 PDF 方法**

在 `PdfGenerator.java` import 区追加：

```java
import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.entity.Organization;
import com.archive.entity.User;
```

在类内（`generateReceiptPdf` 之后、`// ==================== 工具方法 ====================` 之前）插入：

```java
    /**
     * 生成借阅凭证 PDF（业务场景八）。
     * 内容：凭证信息、借阅人信息、档案信息、单位意见盖章区、已归还盖章区。
     */
    public byte[] generateBorrowVoucherPdf(BorrowRequest borrow, Archive archive,
                                           User borrower, Organization org) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = getCnFont(18, Font.BOLD);
            Font headerFont = getCnFont(10, Font.BOLD);
            Font normalFont = getCnFont(9, Font.NORMAL);

            // 标题
            Paragraph title = new Paragraph("档案借阅凭证", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            doc.add(title);

            // 凭证信息
            PdfPTable info = new PdfPTable(4);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{1.5f, 3f, 1.5f, 3f});
            addInfoRow(info, "凭证号", nullSafe(borrow.getVoucherNo()),
                    "申请号", nullSafe(borrow.getRequestNo()), normalFont);
            addInfoRow(info, "审批时间", formatOdt(borrow.getApprovedAt()),
                    "状态", borrow.getStatus() != null ? borrow.getStatus().getDisplayName() : "", normalFont);
            doc.add(info);

            doc.add(Chunk.NEWLINE);

            // 借阅人信息
            PdfPTable borrowerTab = new PdfPTable(4);
            borrowerTab.setWidthPercentage(100);
            borrowerTab.setWidths(new float[]{1.5f, 3f, 1.5f, 3f});
            addInfoRow(borrowerTab, "姓名", borrower != null ? nullSafe(borrower.getRealName()) : "",
                    "工号", borrower != null ? nullSafe(borrower.getEmployeeNo()) : "", normalFont);
            addInfoRow(borrowerTab, "联系电话", borrower != null ? nullSafe(borrower.getPhone()) : "",
                    "部门", borrower != null ? nullSafe(borrower.getDepartmentName()) : "", normalFont);
            addInfoRow(borrowerTab, "所在单位", org != null ? nullSafe(org.getOrgName()) : "",
                    "", "", normalFont);
            doc.add(borrowerTab);

            doc.add(Chunk.NEWLINE);

            // 档案信息
            PdfPTable archTab = new PdfPTable(4);
            archTab.setWidthPercentage(100);
            archTab.setWidths(new float[]{1.5f, 3f, 1.5f, 3f});
            addInfoRow(archTab, "档号", archive != null ? nullSafe(archive.getArchiveNo()) : "",
                    "题名", archive != null ? nullSafe(archive.getTitle()) : "", normalFont);
            addInfoRow(archTab, "载体状态",
                    archive != null && archive.getCarrierStatus() != null
                            ? archive.getCarrierStatus().getDisplayName() : "",
                    "借阅天数", nullSafe(borrow.getExpectedDays()), normalFont);
            addInfoRow(archTab, "预计到馆", formatOdt(borrow.getExpectedVisitAt()),
                    "借阅理由", nullSafe(borrow.getReason()), normalFont);
            doc.add(archTab);

            doc.add(Chunk.NEWLINE);

            // 单位意见盖章区
            doc.add(new Paragraph("单位意见（盖章）：", headerFont));
            doc.add(new Paragraph("\n\n\n（借阅人所在单位意见与盖章位置）\n\n\n", normalFont));

            // 已归还盖章区
            doc.add(new Paragraph("档案馆归还确认（盖章）：", headerFont));
            doc.add(new Paragraph("\n\n\n（归还确认与盖章位置）\n\n\n", normalFont));

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("本凭证由系统自动生成。首次导出时生成凭证号，重复导出复用原号。", normalFont));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("借阅凭证 PDF 生成失败: " + e.getMessage(), e);
        }
    }
```

> 说明：`getCnFont`/`makeCell`/`addInfoRow`/`nullSafe`/`formatOdt` 均为 `PdfGenerator` 已有私有方法，新方法在同类内直接复用，无需新增。

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=PdfGeneratorBorrowVoucherTest`
Expected: PASS（2 个测试）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/util/PdfGenerator.java \
        src/test/java/com/archive/util/PdfGeneratorBorrowVoucherTest.java
git commit -m "feat(borrow): 实现借阅凭证 PDF 生成"
```

## Task 10: BorrowService 导出借阅凭证

**Files:**
- Modify: `src/main/java/com/archive/service/BorrowService.java`（新增 `exportVoucher`）
- Modify: `src/test/java/com/archive/service/BorrowServiceTest.java`（追加导出测试 + import）

- [ ] **Step 1: 追加失败测试**

测试文件 import 区追加：

```java
import org.mockito.ArgumentCaptor;
```

在测试类末尾大括号前追加：

```java
    // ---- 11.10 exportVoucher ----

    @Test
    void exportVoucher_首次导出生成凭证号并置voucher_issued() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.approved);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(borrowNoUtil.nextVoucherNo()).thenReturn("VCH-000001");
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        when(userMapper.selectById(any())).thenReturn(new com.archive.entity.User());
        when(pdfGenerator.generateBorrowVoucherPdf(any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        asRole(RoleCode.internal_reader, 4L, () -> {
            byte[] pdf = service.exportVoucher(1L);
            assertThat(pdf).isNotEmpty();
        });

        ArgumentCaptor<BorrowRequest> cap = ArgumentCaptor.forClass(BorrowRequest.class);
        verify(borrowRequestMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(BorrowStatus.voucher_issued);
        assertThat(cap.getValue().getVoucherNo()).isEqualTo("VCH-000001");
    }

    @Test
    void exportVoucher_重复导出复用凭证号不更新() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.voucher_issued);
        b.setVoucherNo("VCH-000001");
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        when(userMapper.selectById(any())).thenReturn(new com.archive.entity.User());
        when(pdfGenerator.generateBorrowVoucherPdf(any(), any(), any(), any()))
                .thenReturn(new byte[]{1});

        asRole(RoleCode.internal_reader, 4L, () -> service.exportVoucher(1L));

        verify(borrowNoUtil, never()).nextVoucherNo();
        verify(borrowRequestMapper, never()).updateById(any());
    }

    @Test
    void exportVoucher_未审批通过抛BUSINESS_CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.exportVoucher(1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("审批通过"));
    }

    @Test
    void exportVoucher_非本人抛FORBIDDEN() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 9L, BorrowStatus.approved);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.exportVoucher(1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无权"));
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: 编译失败（`exportVoucher` 不存在）。

- [ ] **Step 3: 实现 exportVoucher**

在 `BorrowService` 的 `approve` 方法之后插入：

```java
    // ==================== 11.10 导出借阅凭证 ====================

    @Transactional
    public byte[] exportVoucher(Long requestId) {
        requireRole(RoleCode.internal_reader);
        BorrowRequest b = mustGet(requestId);
        if (!b.getBorrowerId().equals(AuthContext.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权导出该借阅凭证");
        }

        BorrowStatus st = b.getStatus();
        if (st != BorrowStatus.approved && st != BorrowStatus.voucher_issued
                && st != BorrowStatus.checked_out) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "申请尚未审批通过，无法导出凭证");
        }

        boolean firstIssue = b.getVoucherNo() == null;
        if (firstIssue) {
            b.setVoucherNo(borrowNoUtil.nextVoucherNo());
            b.setVoucherIssuedAt(OffsetDateTime.now());
            b.setStatus(BorrowStatus.voucher_issued);
            borrowRequestMapper.updateById(b);
            auditService.log("M09", "issue_voucher", "borrow_request", b.getId(),
                    Map.of("voucherNo", b.getVoucherNo(), "firstIssue", true));
        } else {
            auditService.log("M09", "issue_voucher", "borrow_request", b.getId(),
                    Map.of("voucherNo", b.getVoucherNo(), "firstIssue", false));
        }

        Archive archive = archiveMapper.selectById(b.getArchiveId());
        User borrower = userMapper.selectById(b.getBorrowerId());
        Organization org = (borrower != null && borrower.getOrganizationId() != null)
                ? organizationMapper.selectById(borrower.getOrganizationId()) : null;
        return pdfGenerator.generateBorrowVoucherPdf(b, archive, borrower, org);
    }
```

> 说明：`User`、`Organization`、`Archive`、`OffsetDateTime`、`Map` 在 Task 7 已 import；无新增 import。

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: PASS（15 个测试）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/BorrowService.java \
        src/test/java/com/archive/service/BorrowServiceTest.java
git commit -m "feat(borrow): 实现借阅凭证导出与首次/重复状态机"
```

---

## Task 11: BorrowService 出库

**Files:**
- Modify: `src/main/java/com/archive/service/BorrowService.java`（新增 `checkout` + import）
- Modify: `src/test/java/com/archive/service/BorrowServiceTest.java`（追加出库测试 + import）

- [ ] **Step 1: 追加失败测试**

测试文件 import 区追加：

```java
import com.archive.dto.request.BorrowCheckoutRequest;
import java.time.OffsetDateTime;
```

在测试类末尾大括号前追加：

```java
    // ---- 12.4 checkout ----

    @Test
    void checkout_凭证匹配则出库成功档案置on_loan() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.voucher_issued);
        b.setVoucherNo("VCH-000001");
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        when(archiveMapper.update(any(com.archive.entity.Archive.class), any())).thenReturn(1);

        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-000001");
        req.setDueAt(OffsetDateTime.now().plusDays(7));

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.checkout(1L, req);
            assertThat(resp.getStatus()).isEqualTo("checked_out");
        });
        verify(archiveMapper).update(any(com.archive.entity.Archive.class), any());
    }

    @Test
    void checkout_凭证号不匹配抛CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.voucher_issued);
        b.setVoucherNo("VCH-000001");
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-999999");
        req.setDueAt(OffsetDateTime.now().plusDays(7));

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.checkout(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("凭证号不匹配"));
    }

    @Test
    void checkout_应还时间不晚于当前抛VALIDATION_FAILED() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.voucher_issued);
        b.setVoucherNo("VCH-000001");
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-000001");
        req.setDueAt(OffsetDateTime.now().minusDays(1));

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.checkout(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("应还时间"));
    }

    @Test
    void checkout_非可出库状态抛CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-000001");
        req.setDueAt(OffsetDateTime.now().plusDays(7));

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.checkout(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("当前状态"));
    }

    @Test
    void checkout_无权限角色抛FORBIDDEN() {
        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-000001");
        req.setDueAt(OffsetDateTime.now().plusDays(7));

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.checkout(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: 编译失败（`checkout` 不存在）。

- [ ] **Step 3: 实现 checkout**

`BorrowService` import 区追加：

```java
import com.archive.dto.request.BorrowCheckoutRequest;
import com.archive.enums.LoanStatus;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
```

在 `exportVoucher` 之后插入：

```java
    // ==================== 12.4 核验凭证并确认出库 ====================

    @Transactional
    public BorrowRequestResponse checkout(Long requestId, BorrowCheckoutRequest req) {
        requireRole(RoleCode.front_archivist, RoleCode.back_archivist);
        long operator = AuthContext.getCurrentUserId();

        BorrowRequest b = mustGet(requestId);
        BorrowStatus st = b.getStatus();
        if (st != BorrowStatus.approved && st != BorrowStatus.voucher_issued) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "当前状态不允许出库");
        }
        if (b.getVoucherNo() == null || !b.getVoucherNo().equals(req.getVoucherNo())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "凭证号不匹配");
        }
        if (req.getDueAt() == null || !req.getDueAt().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "应还时间必须晚于当前时间");
        }

        // 出库复校可借状态与盘点范围
        eligibilityChecker.checkBorrowable(b.getArchiveId());

        OffsetDateTime now = OffsetDateTime.now();
        b.setCheckedOutBy(operator);
        b.setCheckedOutAt(now);
        b.setDueAt(req.getDueAt());
        b.setStatus(BorrowStatus.checked_out);
        borrowRequestMapper.updateById(b);

        // 条件更新档案为借出中（充当乐观锁，并发时 updated=0 即冲突）
        Archive patch = new Archive();
        patch.setLoanStatus(LoanStatus.on_loan);
        UpdateWrapper<Archive> uw = new UpdateWrapper<>();
        uw.eq("id", b.getArchiveId()).eq("loan_status", LoanStatus.available.name());
        int updated = archiveMapper.update(patch, uw);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案状态已变更，出库失败");
        }

        auditService.log("M09", "checkout", "borrow_request", b.getId(),
                Map.of("voucherNo", req.getVoucherNo(), "dueAt", String.valueOf(req.getDueAt())));

        return toResponse(b, true, true);
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: PASS（20 个测试）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/BorrowService.java \
        src/test/java/com/archive/service/BorrowServiceTest.java
git commit -m "feat(borrow): 实现凭证核验出库与档案借出联动"
```

## Task 12: BorrowService 归还

**Files:**
- Modify: `src/main/java/com/archive/service/BorrowService.java`（新增 `returnBorrow` + import）
- Modify: `src/test/java/com/archive/service/BorrowServiceTest.java`（追加归还测试 + import）

- [ ] **Step 1: 追加失败测试**

测试文件 import 区追加：

```java
import com.archive.dto.request.BorrowReturnRequest;
import com.archive.enums.ConditionStatus;
import com.archive.enums.ReturnCheckResult;
import com.archive.entity.Archive;
```

在测试类末尾大括号前追加：

```java
    // ---- 12.5 returnBorrow ----

    @Test
    void returnBorrow_正常归还则状态returned且档案恢复可借() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.checked_out);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.update(any(Archive.class), any())).thenReturn(1);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        BorrowReturnRequest req = new BorrowReturnRequest();
        req.setReturnCheckResult(ReturnCheckResult.normal);

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.returnBorrow(1L, req);
            assertThat(resp.getStatus()).isEqualTo("returned");
        });

        ArgumentCaptor<Archive> cap = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper).update(cap.capture(), any());
        assertThat(cap.getValue().getLoanStatus()).isEqualTo(LoanStatus.available);
        assertThat(cap.getValue().getConditionStatus()).isNull(); // 正常不改实体状态
    }

    @Test
    void returnBorrow_异常归还则状态abnormal_return且档案置damaged() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.checked_out);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.update(any(Archive.class), any())).thenReturn(1);

        BorrowReturnRequest req = new BorrowReturnRequest();
        req.setReturnCheckResult(ReturnCheckResult.damaged);
        req.setReturnNote("缺页2页");

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.returnBorrow(1L, req);
            assertThat(resp.getStatus()).isEqualTo("abnormal_return");
            assertThat(resp.getReturnCheckResult()).isEqualTo("damaged");
        });

        ArgumentCaptor<Archive> cap = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper).update(cap.capture(), any());
        assertThat(cap.getValue().getConditionStatus()).isEqualTo(ConditionStatus.damaged);
    }

    @Test
    void returnBorrow_非checked_out状态抛CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowReturnRequest req = new BorrowReturnRequest();
        req.setReturnCheckResult(ReturnCheckResult.normal);

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.returnBorrow(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("已出库"));
    }

    @Test
    void returnBorrow_无权限角色抛FORBIDDEN() {
        BorrowReturnRequest req = new BorrowReturnRequest();
        req.setReturnCheckResult(ReturnCheckResult.normal);

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.returnBorrow(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: 编译失败（`returnBorrow` 不存在）。

- [ ] **Step 3: 实现 returnBorrow**

`BorrowService` import 区追加：

```java
import com.archive.dto.request.BorrowReturnRequest;
import com.archive.enums.ConditionStatus;
import com.archive.enums.ReturnCheckResult;
```

在 `checkout` 之后插入：

```java
    // ==================== 12.5 确认归还 ====================

    @Transactional
    public BorrowRequestResponse returnBorrow(Long requestId, BorrowReturnRequest req) {
        requireRole(RoleCode.front_archivist, RoleCode.back_archivist);
        long operator = AuthContext.getCurrentUserId();

        BorrowRequest b = mustGet(requestId);
        if (b.getStatus() != BorrowStatus.checked_out) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有已出库申请可归还");
        }

        OffsetDateTime now = OffsetDateTime.now();
        b.setReturnedBy(operator);
        b.setReturnedAt(now);
        b.setReturnCheckResult(req.getReturnCheckResult());
        b.setReturnNote(req.getReturnNote());

        boolean abnormal = req.getReturnCheckResult() != ReturnCheckResult.normal;
        b.setStatus(abnormal ? BorrowStatus.abnormal_return : BorrowStatus.returned);
        borrowRequestMapper.updateById(b);

        // 档案恢复可借；异常归还时实体状态置 damaged（条件更新充当乐观锁）
        Archive patch = new Archive();
        patch.setLoanStatus(LoanStatus.available);
        if (abnormal) {
            patch.setConditionStatus(ConditionStatus.damaged);
        }
        UpdateWrapper<Archive> uw = new UpdateWrapper<>();
        uw.eq("id", b.getArchiveId()).eq("loan_status", LoanStatus.on_loan.name());
        int updated = archiveMapper.update(patch, uw);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案状态已变更，归还失败");
        }

        auditService.log("M09", "return", "borrow_request", b.getId(),
                Map.of("returnCheckResult", req.getReturnCheckResult().name(), "abnormal", abnormal));

        return toResponse(b, true, true);
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: PASS（24 个测试）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/BorrowService.java \
        src/test/java/com/archive/service/BorrowServiceTest.java
git commit -m "feat(borrow): 实现归还确认与异常归还实体状态联动"
```

## Task 13: BorrowService 管理端列表/详情 + 工作台摘要

**Files:**
- Modify: `src/main/java/com/archive/service/BorrowService.java`（新增 `adminList`/`adminGet`/`dashboardMine`/`dashboardCurrent`/`dashboardOverdue`/`toSummary` + import）
- Modify: `src/test/java/com/archive/service/BorrowServiceTest.java`（追加测试 + import）

- [ ] **Step 1: 追加失败测试**

测试文件 import 区追加：

```java
import com.archive.dto.response.BorrowSummaryItem;
import org.springframework.jdbc.core.RowMapper;
```

在测试类末尾大括号前追加：

```java
    // ---- 12.1 adminList ----

    @Test
    void adminList_管理端响应带borrower摘要() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        Page<BorrowRequest> page = new Page<>(1, 20);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(borrowRequestMapper.selectPage(any(), any())).thenReturn(page);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        com.archive.entity.User u = new com.archive.entity.User();
        u.setId(4L);
        u.setRealName("小李");
        when(userMapper.selectById(4L)).thenReturn(u);

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestQuery q = new BorrowRequestQuery();
            PageResult<BorrowRequestResponse> r = service.adminList(q);
            assertThat(r.getRecords()).hasSize(1);
            assertThat(r.getRecords().get(0).getBorrower()).isNotNull();
            assertThat(r.getRecords().get(0).getBorrower().getRealName()).isEqualTo("小李");
        });
    }

    @Test
    void adminList_非管理端角色抛FORBIDDEN() {
        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.adminList(new BorrowRequestQuery()))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }

    // ---- 12.2 adminGet ----

    @Test
    void adminGet_带盒位架位location() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        when(userMapper.selectById(any())).thenReturn(new com.archive.entity.User());
        BorrowRequestResponse.LocationSummary loc =
                new BorrowRequestResponse.LocationSummary("BOX-000001", "401-01-02-03");
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(loc);

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.adminGet(1L);
            assertThat(resp.getLocation()).isNotNull();
            assertThat(resp.getLocation().getBoxNo()).isEqualTo("BOX-000001");
        });
    }

    // ---- 工作台 dashboard 方法（不需角色校验，由 SearchService 已认证用户调用） ----

    @Test
    void dashboardMine_返回用户最近申请摘要() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        Page<BorrowRequest> page = new Page<>(1, 5);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(borrowRequestMapper.selectPage(any(), any())).thenReturn(page);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        List<BorrowSummaryItem> items = service.dashboardMine(4L, 5);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getRequestNo()).isEqualTo("BRW-000001");
        assertThat(items.get(0).getTitle()).isEqualTo("测试档案");
    }

    @Test
    void dashboardOverdue_只返回逾期借阅() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.checked_out);
        b.setDueAt(java.time.OffsetDateTime.now().minusDays(2));
        Page<BorrowRequest> page = new Page<>(1, 5);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(borrowRequestMapper.selectPage(any(), any())).thenReturn(page);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        List<BorrowSummaryItem> items = service.dashboardOverdue(4L, 5);
        assertThat(items).hasSize(1);
    }
```

> 说明：`anyString()` 已随 Task 4 同包静态导入？——本测试文件尚未导入。在 import 区追加 `import static org.mockito.ArgumentMatchers.anyString;`。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: 编译失败（`adminList`/`adminGet`/`dashboardMine` 等不存在）。

- [ ] **Step 3: 实现管理端与工作台方法**

`BorrowService` import 区追加：

```java
import com.archive.dto.response.BorrowSummaryItem;
```

在 `returnBorrow` 之后、「公共辅助」之前插入：

```java
    // ==================== 12.1 管理端查询借阅申请 ====================

    public PageResult<BorrowRequestResponse> adminList(BorrowRequestQuery query) {
        requireRole(RoleCode.front_archivist, RoleCode.back_archivist);

        Page<BorrowRequest> page = new Page<>(query.getPageNo(), query.getPageSize());
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            w.eq("status", query.getStatus());
        }
        if (query.getBorrowerKeyword() != null && !query.getBorrowerKeyword().isBlank()) {
            String kw = "%" + query.getBorrowerKeyword() + "%";
            w.apply("borrower_id IN (SELECT id FROM users WHERE deleted_at IS NULL AND "
                    + "(real_name LIKE {0} OR login_name LIKE {0} OR employee_no LIKE {0}))", kw);
        }
        if (query.getArchiveKeyword() != null && !query.getArchiveKeyword().isBlank()) {
            String kw = "%" + query.getArchiveKeyword() + "%";
            w.apply("archive_id IN (SELECT id FROM archives WHERE deleted_at IS NULL AND "
                    + "(title LIKE {0} OR archive_no LIKE {0}))", kw);
        }
        if (Boolean.TRUE.equals(query.getOverdue())) {
            w.eq("status", BorrowStatus.checked_out.name())
             .isNotNull("due_at").lt("due_at", OffsetDateTime.now());
        }
        w.orderByDesc("created_at");

        Page<BorrowRequest> result = borrowRequestMapper.selectPage(page, w);
        List<BorrowRequestResponse> items = result.getRecords().stream()
                .map(b -> toResponse(b, true, false))
                .collect(Collectors.toList());
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    // ==================== 12.2 管理端申请详情 ====================

    public BorrowRequestResponse adminGet(Long requestId) {
        requireRole(RoleCode.front_archivist, RoleCode.back_archivist);
        BorrowRequest b = mustGet(requestId);
        return toResponse(b, true, true);
    }

    // ==================== 11.1 内部工作台借阅摘要 ====================

    public List<BorrowSummaryItem> dashboardMine(long userId, int limit) {
        Page<BorrowRequest> page = new Page<>(1, limit);
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.eq("borrower_id", userId).isNull("deleted_at").orderByDesc("created_at");
        return borrowRequestMapper.selectPage(page, w).getRecords().stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public List<BorrowSummaryItem> dashboardCurrent(long userId, int limit) {
        Page<BorrowRequest> page = new Page<>(1, limit);
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.eq("borrower_id", userId).eq("status", BorrowStatus.checked_out.name())
         .isNull("deleted_at").orderByAsc("due_at");
        return borrowRequestMapper.selectPage(page, w).getRecords().stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public List<BorrowSummaryItem> dashboardOverdue(long userId, int limit) {
        Page<BorrowRequest> page = new Page<>(1, limit);
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.eq("borrower_id", userId).eq("status", BorrowStatus.checked_out.name())
         .isNull("deleted_at").isNotNull("due_at").lt("due_at", OffsetDateTime.now())
         .orderByAsc("due_at");
        return borrowRequestMapper.selectPage(page, w).getRecords().stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    private BorrowSummaryItem toSummary(BorrowRequest b) {
        String archiveNo = null;
        String title = null;
        if (b.getArchiveId() != null) {
            Archive a = archiveMapper.selectById(b.getArchiveId());
            if (a != null) {
                archiveNo = a.getArchiveNo();
                title = a.getTitle();
            }
        }
        return new BorrowSummaryItem(b.getRequestNo(), b.getArchiveId(), archiveNo, title,
                b.getStatus().name(), b.getDueAt(), b.getCreatedAt());
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=BorrowServiceTest`
Expected: PASS（29 个测试）。若 `adminGet_带盒位架位location` 因 `queryForObject` 重载匹配失败报错，将桩改为 `lenient().when(...)` 或显式 `org.springframework.jdbc.core.RowMapper` 类型，确认 `resolveLocation` 命中。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/BorrowService.java \
        src/test/java/com/archive/service/BorrowServiceTest.java
git commit -m "feat(borrow): 实现管理端列表/详情与内部工作台借阅摘要"
```

## Task 14: 内部工作台整合（SearchService + InternalDashboardResponse）

> `BorrowService` 与 `SearchService` 同包（`com.archive.service`），无需 import。本任务改动会影响 3 个既有 SearchService 测试的构造器调用（新增最后参数），需同步更新。

**Files:**
- Modify: `src/main/java/com/archive/dto/response/InternalDashboardResponse.java`
- Modify: `src/main/java/com/archive/service/SearchService.java`
- Modify: `src/test/java/com/archive/service/SearchServiceSearchTest.java`
- Modify: `src/test/java/com/archive/service/SearchServiceAccessTest.java`
- Modify: `src/test/java/com/archive/service/SearchServiceAiTest.java`
- Create: `src/test/java/com/archive/service/SearchServiceDashboardTest.java`

- [ ] **Step 1: 收紧 InternalDashboardResponse 类型**

`InternalDashboardResponse.java` 中三个 `List<Object>` 改为 `List<BorrowSummaryItem>`：

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
    /** 我的借阅申请 */
    private List<BorrowSummaryItem> myBorrowRequests;
    /** 当前借阅 */
    private List<BorrowSummaryItem> currentBorrows;
    /** 逾期提示 */
    private List<BorrowSummaryItem> overdueReminders;

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

- [ ] **Step 2: SearchService 注入 BorrowService 并填充 dashboard**

在 `SearchService` 字段区（`private final AiClient aiClient;` 之后）追加：

```java
    private final BorrowService borrowService;
```

将 `getInternalDashboard` 的 return 改为：

```java
        return new InternalDashboardResponse(
                recentViews,
                borrowService.dashboardMine(userId, 5),
                borrowService.dashboardCurrent(userId, 5),
                borrowService.dashboardOverdue(userId, 5));
```

（删除原「借阅部分本次返回空占位」注释与 `List.of(), List.of(), List.of()`。）

- [ ] **Step 3: 更新 3 个既有 SearchService 测试的构造器调用**

三个文件中 `new SearchService(...)` 调用末尾各追加一个 `borrowService` 参数。以 `SearchServiceSearchTest` 为例：

修改前（第 50-51 行）：
```java
        service = new SearchService(archiveMapper, archiveFileMapper, accessLogMapper,
                categoryMapper, tagMapper, jdbcTemplate, null, null);
```

修改后：
```java
        service = new SearchService(archiveMapper, archiveFileMapper, accessLogMapper,
                categoryMapper, tagMapper, jdbcTemplate, null, null, mock(BorrowService.class));
```

对 `SearchServiceAccessTest.java`（第 48 行附近）与 `SearchServiceAiTest.java`（第 44 行附近）做同样处理：构造调用末尾追加 `mock(BorrowService.class)`。

> 若 `SearchServiceAiTest` 中有用例直接断言 `getInternalDashboard` 的借阅列表为空，将其改为：在 setup 中声明 `BorrowService borrowService = mock(BorrowService.class);` 并 `when(borrowService.dashboardMine(any(), anyInt())).thenReturn(List.of())`（其余两个方法同），构造调用末尾传 `borrowService`。

- [ ] **Step 4: 写工作台整合测试**

`src/test/java/com/archive/service/SearchServiceDashboardTest.java`：

```java
package com.archive.service;

import com.archive.dto.response.BorrowSummaryItem;
import com.archive.dto.response.InternalDashboardResponse;
import com.archive.mapper.ArchiveAccessLogMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceDashboardTest {

    @Test
    void getInternalDashboard_接入BorrowService填充三列表() {
        ArchiveAccessLogMapper accessLogMapper = mock(ArchiveAccessLogMapper.class);
        when(accessLogMapper.findRecentViews(4L)).thenReturn(List.of());

        BorrowService borrowService = mock(BorrowService.class);
        BorrowSummaryItem mine = new BorrowSummaryItem(
                "BRW-1", 2L, "ARC-1", "测试档案", "applied", null, null);
        when(borrowService.dashboardMine(4L, 5)).thenReturn(List.of(mine));
        when(borrowService.dashboardCurrent(4L, 5)).thenReturn(List.of());
        when(borrowService.dashboardOverdue(4L, 5)).thenReturn(List.of());

        SearchService service = new SearchService(null, null, accessLogMapper,
                null, null, null, null, null, borrowService);

        InternalDashboardResponse resp = service.getInternalDashboard(4L);
        assertThat(resp.getMyBorrowRequests()).hasSize(1);
        assertThat(resp.getMyBorrowRequests().get(0).getRequestNo()).isEqualTo("BRW-1");
        assertThat(resp.getCurrentBorrows()).isEmpty();
        assertThat(resp.getOverdueReminders()).isEmpty();
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./mvnw test -Dtest="SearchService*Test,BorrowServiceTest"`
Expected: 全部 PASS（含既有 SearchService 测试 + 新 dashboard 测试 + BorrowService 全部用例）。

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/archive/dto/response/InternalDashboardResponse.java \
        src/main/java/com/archive/service/SearchService.java \
        src/test/java/com/archive/service/SearchService*.java
git commit -m "feat(borrow): 内部工作台接入借阅摘要并收紧响应类型"
```

## Task 15: BorrowController 接口层

> 控制器是薄封装层。项目无既有控制器单元测试（无 MockMvc + Sa-Token 测试基建），与 `WarehouseController`/`SearchController` 一致：编译 + runtime 验证覆盖。

**Files:**
- Create: `src/main/java/com/archive/controller/BorrowController.java`

- [ ] **Step 1: 实现 BorrowController**

`src/main/java/com/archive/controller/BorrowController.java`：

```java
package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.BorrowApplyRequest;
import com.archive.dto.request.BorrowApproveRequest;
import com.archive.dto.request.BorrowCheckoutRequest;
import com.archive.dto.request.BorrowRequestQuery;
import com.archive.dto.request.BorrowReturnRequest;
import com.archive.dto.response.BorrowRequestResponse;
import com.archive.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 借阅管理接口（M09）。方法级全路径，跨 /api/internal 与 /api/admin 两个 base path。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "借阅管理", description = "借阅申请、审批、出库、归还")
public class BorrowController {

    private final BorrowService borrowService;

    // ==================== 11.7 提交借阅申请 ====================

    @PostMapping("/api/internal/borrow-requests")
    @Operation(summary = "提交借阅申请")
    public R<BorrowRequestResponse> apply(@RequestBody @Valid BorrowApplyRequest req) {
        return R.ok(borrowService.apply(req));
    }

    // ==================== 11.8 查询我的借阅申请 ====================

    @GetMapping("/api/internal/borrow-requests")
    @Operation(summary = "查询我的借阅申请")
    public R<PageResult<BorrowRequestResponse>> listMine(@ModelAttribute BorrowRequestQuery query) {
        return R.ok(borrowService.listMine(query));
    }

    // ==================== 11.9 借阅申请详情（本人） ====================

    @GetMapping("/api/internal/borrow-requests/{requestId}")
    @Operation(summary = "我的借阅申请详情")
    public R<BorrowRequestResponse> getMine(@PathVariable Long requestId) {
        return R.ok(borrowService.getMine(requestId));
    }

    // ==================== 11.10 导出借阅凭证 ====================

    @GetMapping("/api/internal/borrow-requests/{requestId}/voucher")
    @Operation(summary = "导出借阅凭证 PDF")
    public ResponseEntity<byte[]> exportVoucher(@PathVariable Long requestId) {
        byte[] pdf = borrowService.exportVoucher(requestId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = URLEncoder.encode("借阅凭证.pdf", StandardCharsets.UTF_8);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + filename);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // ==================== 12.1 管理端查询借阅申请 ====================

    @GetMapping("/api/admin/borrow-requests")
    @Operation(summary = "管理端查询借阅申请")
    public R<PageResult<BorrowRequestResponse>> adminList(@ModelAttribute BorrowRequestQuery query) {
        return R.ok(borrowService.adminList(query));
    }

    // ==================== 12.2 管理端申请详情 ====================

    @GetMapping("/api/admin/borrow-requests/{requestId}")
    @Operation(summary = "管理端借阅申请详情")
    public R<BorrowRequestResponse> adminGet(@PathVariable Long requestId) {
        return R.ok(borrowService.adminGet(requestId));
    }

    // ==================== 12.3 审批借阅申请 ====================

    @PostMapping("/api/admin/borrow-requests/{requestId}/approve")
    @Operation(summary = "审批借阅申请")
    public R<BorrowRequestResponse> approve(@PathVariable Long requestId,
                                            @RequestBody @Valid BorrowApproveRequest req) {
        return R.ok(borrowService.approve(requestId, req));
    }

    // ==================== 12.4 核验凭证并确认出库 ====================

    @PostMapping("/api/admin/borrow-requests/{requestId}/checkout")
    @Operation(summary = "核验凭证并确认出库")
    public R<BorrowRequestResponse> checkout(@PathVariable Long requestId,
                                             @RequestBody @Valid BorrowCheckoutRequest req) {
        return R.ok(borrowService.checkout(requestId, req));
    }

    // ==================== 12.5 确认归还 ====================

    @PostMapping("/api/admin/borrow-requests/{requestId}/return")
    @Operation(summary = "确认归还")
    public R<BorrowRequestResponse> returnBorrow(@PathVariable Long requestId,
                                                 @RequestBody @Valid BorrowReturnRequest req) {
        return R.ok(borrowService.returnBorrow(requestId, req));
    }
}
```

- [ ] **Step 2: 全量构建验证**

Run: `./mvnw -q compile && ./mvnw test`
Expected: BUILD SUCCESS；全部既有测试 + 借阅新测试通过（控制器编译无误，路由注册成功）。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/archive/controller/BorrowController.java
git commit -m "feat(borrow): 新增借阅管理 9 个 REST 接口"
```

---

## Task 16: Runtime 全链路验证

> 按 runtime 验证完整性：真实起服务，登录态 + 完整链路，不靠单元测试替代。据实记录结果。

- [ ] **Step 1: 起依赖与后端**

```bash
# 在仓库根目录起 Postgres（MinIO/ClamAV 按既有 docker-compose 可选，借阅链路不依赖）
docker compose up -d postgres
# 起后端（后台运行，记录端口，默认 8080）
./mvnw spring-boot:run
```

确认 `http://localhost:8080/swagger-ui` 可访问，`借阅管理` 分组下 9 个接口出现。

- [ ] **Step 2: 登录获取 token（按角色）**

种子用户：用户4=internal_reader、用户2=back_archivist、用户1=front_archivist（登录名/密码见 V16 种子数据或向加明确认）。

```bash
# internal_reader
curl -s -c cookies_reader.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginName":"<reader登录名>","password":"<密码>","userType":"internal"}'
# back_archivist
curl -s -c cookies_admin.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginName":"<admin登录名>","password":"<密码>","userType":"admin"}'
```

> Sa-Token 默认 cookie `satoken`；cookie jar（`-b cookies_*.txt`）携带即可。若用 header 方式，从登录响应取 token 后传 `-H "satoken: <token>"`。

- [ ] **Step 3: 执行闭环场景并核对**

```bash
BASE=http://localhost:8080

# 1) 申请（用 paper 档案，如 archive id=2；先确认其 loanStatus=available、lifecycle=normal）
curl -s -b cookies_reader.txt -X POST $BASE/api/internal/borrow-requests \
  -H 'Content-Type: application/json' \
  -d '{"archiveId":2,"reason":"财政预算核查","expectedDays":7}' | jq .
# 期望：code=OK，data.status=applied，data.requestNo=BRW-xxxxxx；记下 requestId

# 2) 审批通过（back_archivist）
curl -s -b cookies_admin.txt -X POST $BASE/api/admin/borrow-requests/<requestId>/approve \
  -H 'Content-Type: application/json' -d '{"approved":true,"opinion":"同意7天"}' | jq .
# 期望：status=approved，approvedBy 非 null

# 3) 导出凭证（首次）
curl -s -b cookies_reader.txt -o voucher.pdf -D - \
  $BASE/api/internal/borrow-requests/<requestId>/voucher
# 期望：Content-Type: application/pdf，voucher.pdf 非空且以 %PDF 开头；DB status=voucher_issued，voucherNo=VCH-xxxxxx
# 重复导出：复用同 voucherNo，状态不变

# 4) 出库（back_archivist 或 front_archivist）
curl -s -b cookies_admin.txt -X POST $BASE/api/admin/borrow-requests/<requestId>/checkout \
  -H 'Content-Type: application/json' \
  -d "{\"voucherNo\":\"<上一步 voucherNo>\",\"dueAt\":\"$(date -u -d '+7 days' +%Y-%m-%dT%H:%M:%S+08:00)\"}" | jq .
# 期望：status=checked_out；DB archives.loan_status=on_loan

# 5) 归还（正常）
curl -s -b cookies_admin.txt -X POST $BASE/api/admin/borrow-requests/<requestId>/return \
  -H 'Content-Type: application/json' -d '{"returnCheckResult":"normal"}' | jq .
# 期望：status=returned；archives.loan_status=available
```

- [ ] **Step 4: 边界与拒绝场景**

```bash
# 非法状态流转（approved 再 approve / checked_out 再 checkout）→ 409 BUSINESS_CONFLICT
# 凭证号不匹配 checkout → 409
# 应还时间早于当前 checkout → 422
# internal_reader 调 /api/admin/borrow-requests → 403 FORBIDDEN
# 纯电子档案申请 → 422/409（按 checker 抛出的码）
# 命中 running 盘点的档案申请 → 409（构造 inventory_tasks status=running 匹配 room+category）
```

- [ ] **Step 5: 异常归还分支（独立构造一条 checked_out 记录后）**

```bash
curl -s -b cookies_admin.txt -X POST $BASE/api/admin/borrow-requests/<requestId>/return \
  -H 'Content-Type: application/json' -d '{"returnCheckResult":"damaged","returnNote":"缺页"}' | jq .
# 期望：status=abnormal_return；archives.condition_status=damaged；后续对该档案申请被实体状态拦截
```

- [ ] **Step 6: 工作台（11.1）借阅部分**

```bash
curl -s -b cookies_reader.txt $BASE/api/internal/dashboard | jq '.data.myBorrowRequests, .data.currentBorrows, .data.overdueReminders'
# 期望：三列表非空结构（BorrowSummaryItem 字段），逾期动态计算
```

- [ ] **Step 7: 记录结果并清理**

逐条记录实际 HTTP 码与 DB 状态（`SELECT status, voucher_no, ... FROM borrow_requests`；`SELECT loan_status, condition_status FROM archives WHERE id=2`）。验证不通过的条目据实标记，不模拟通过。完成后停后端与容器：

```bash
# 停后端（找端口 8080 进程）
docker compose down
```

- [ ] **Step 8: 提交验证记录**

将 runtime 结果（通过的条目清单 + 不通过条目的复现）记入 `backend/docs/superpowers/runtime/2026-06-15-borrow-liu-runtime.md`（无该目录则创建），提交：

```bash
git add backend/docs/superpowers/runtime/2026-06-15-borrow-liu-runtime.md
git commit -m "docs(borrow): 记录借阅模块 runtime 验证结果"
```

---

## 自审记录（writing-plans Self-Review）

**1. Spec 覆盖：**
- 9 接口：11.7-11.9→Task7、11.10→Task10、12.1-12.2→Task13、12.3→Task8、12.4→Task11、12.5→Task12 ✓
- 工作台 11.1 借阅部分 → Task14 ✓
- 可借性 6 条件 + 盘点拦截 → Task4 ✓
- 状态机 7 态 → Task7/8/10/11/12 ✓
- Service 角色强制 → 各 service 方法 ✓
- 凭证 PDF（含双盖章区）→ Task9 ✓
- 逾期动态计算 → toResponse + dashboardOverdue + 列表 overdue 过滤 ✓
- 审计写入 → 每个流转方法 ✓
- 事务 + 乐观锁（条件更新）→ checkout/return ✓
- 枚举/实体/Mapper/Util/DTO → Task1/2/3/5/6 ✓

**2. 占位符扫描：** 无 TBD/TODO；curl 中 `$BASE`/`<requestId>`/`<密码>` 为运行期变量，非占位。✓

**3. 类型/命名一致性：**
- BorrowService 构造器参数序（Task7 setup 与定义一致）：borrowRequestMapper, archiveMapper, userMapper, organizationMapper, jdbcTemplate, borrowNoUtil, eligibilityChecker, pdfGenerator, auditService ✓
- 方法名跨任务一致：apply/listMine/getMine/approve/exportVoucher/checkout/returnBorrow/adminList/adminGet/dashboardMine/dashboardCurrent/dashboardOverdue/checkBorrowable/nextRequestNo/nextVoucherNo ✓
- 状态名与 V7 CHECK、种子数据一致；request_no=BRW-、voucher_no=VCH- ✓

**4. 已知风险（执行时注意）：**
- Task13 `adminGet` 的 `jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(Object[].class))` 桩在个别 Mockito 版本可能因 varargs 重载匹配失败 → 已在 Task13 Step4 备注 lenient/显式类型回退。
- Task14 改 SearchService 构造器破坏 3 个既有测试 → 已在 Step3 给出精确改法。
- Task16 依赖种子用户登录名/密码与一份 paper 且 available 的档案 → 执行前确认 V16 种子（archive id=2 的 loanStatus；若无合适档案，先在库里置一条 normal/available/paper 档案）。

---

## 执行交接

Plan complete and saved to `backend/docs/superpowers/plans/2026-06-15-borrow-liu.md`。

