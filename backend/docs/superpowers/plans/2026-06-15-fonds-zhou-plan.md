# 全宗管理 + 组织维护 实施计划（feat/fonds-zhou）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 §17.1–17.6 后端：全宗 CRUD（查询/新增/更新，停用替代删除）+ 组织查询/新增，含唯一校验、归档数量、业务关联保护、审计日志。

**Architecture:** 套用现有 `UserService`/`UserController` 分层：具体 Service 类（无接口）+ `R<T>` 统一返回 + `BusinessException` + `PageRequest`/`PageResult` 偏移分页。复用已就绪的 `Fonds`/`Organization` 实体与 Mapper。归档数量与业务关联检查用 `JdbcTemplate` 查 `archives`/`archive_boxes`（均含 `fonds_id`），不污染 Archive 模块边界。

**Tech Stack:** Spring Boot 3.5.3 / Java 17 / MyBatis-Plus 3.5.7 / Sa-Token 1.44 / PostgreSQL / JUnit5 + Mockito + AssertJ。

**分支：** 从最新 `develop` 切 `feat/fonds-zhou`，提交身份周扬 `zhouyang <2686148374@qq.com>`。

**依据设计：** [2026-06-15-supplement-backend-design.md §1](../specs/2026-06-15-supplement-backend-design.md)

---

## 文件结构

| 文件 | 责任 |
|------|------|
| `dto/request/FondsCreateRequest.java` | 新增全宗入参（fondsNo/fondsName/organizationId/description），jakarta.validation 校验 |
| `dto/request/FondsUpdateRequest.java` | 更新全宗入参（fondsName/organizationId/description/status，**不含 fondsNo**） |
| `dto/request/FondsQuery.java` | 查询入参（status/organizationId/keyword），继承 PageRequest |
| `dto/request/OrganizationCreateRequest.java` | 新增组织入参（orgName/orgType/contactName/contactPhone） |
| `dto/request/OrganizationQuery.java` | 查询入参（orgType/status/keyword），继承 PageRequest |
| `dto/response/FondsResponse.java` | 全宗列表项（含 archiveCount 归档数量） |
| `dto/response/FondsDetailResponse.java` | 全宗详情（含 archiveCount、boxCount） |
| `dto/response/OrganizationResponse.java` | 组织响应 |
| `service/FondsService.java` | 全宗业务：唯一校验/新增/查询/更新/停用保护/审计/归档数量 |
| `service/OrganizationService.java` | 组织业务：唯一校验/新增/查询/审计 |
| `controller/FondsController.java` | `/api/admin/fonds` GET/POST/PUT |
| `controller/OrganizationController.java` | `/api/admin/organizations` GET/POST |
| `service/FondsServiceTest.java` | FondsService 单测 |
| `service/OrganizationServiceTest.java` | OrganizationService 单测 |

**复用（不新建）：** `common/R`、`common/PageRequest`、`common/PageResult`、`common/ErrorCode`、`common/AuthContext`、`exception/BusinessException`、`enums/RoleCode`、`enums/OrgType`、`entity/Fonds`、`entity/Organization`、`mapper/FondsMapper`、`mapper/OrganizationMapper`、`service/AuditService`。

**关键约束（来自设计）：**
- 唯一预检**不过滤** `deleted_at`（对齐 `UserService` + V13 非部分唯一索引），冲突抛 `BUSINESS_CONFLICT`；
- 列表/详情查询**过滤** `isNull("deleted_at")`；
- `fondsNo` 创建后不可改（UpdateRequest 无该字段）；
- 有 archives/archive_boxes 关联时，PUT 仅应用 `status` 变更（业务字段忽略），审计 detail 记录该事实；
- 权限：全宗 back_archivist 或 sys_admin；组织新增仅 sys_admin（用 `AuthContext.hasRole(RoleCode.sys_admin)`，**不要用 `isAdmin()`**——后者含前台）。

## Task 1: DTO（请求 + 响应）

**Files:**
- Create: `src/main/java/com/archive/dto/request/FondsCreateRequest.java`
- Create: `src/main/java/com/archive/dto/request/FondsUpdateRequest.java`
- Create: `src/main/java/com/archive/dto/request/FondsQuery.java`
- Create: `src/main/java/com/archive/dto/request/OrganizationCreateRequest.java`
- Create: `src/main/java/com/archive/dto/request/OrganizationQuery.java`
- Create: `src/main/java/com/archive/dto/response/FondsResponse.java`
- Create: `src/main/java/com/archive/dto/response/FondsDetailResponse.java`
- Create: `src/main/java/com/archive/dto/response/OrganizationResponse.java`

- [ ] **Step 1: 新建 8 个 DTO 文件**

`FondsCreateRequest.java`：
```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FondsCreateRequest {
    @NotBlank(message = "全宗号不能为空")
    @Size(max = 64, message = "全宗号长度不能超过 64")
    private String fondsNo;

    @NotBlank(message = "全宗名称不能为空")
    @Size(max = 200, message = "全宗名称长度不能超过 200")
    private String fondsName;

    @NotNull(message = "所属组织不能为空")
    private Long organizationId;

    @Size(max = 1000, message = "备注长度不能超过 1000")
    private String description;
}
```

`FondsUpdateRequest.java`（无 fondsNo，创建后不可改）：
```java
package com.archive.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FondsUpdateRequest {
    @Size(max = 200, message = "全宗名称长度不能超过 200")
    private String fondsName;
    private Long organizationId;
    @Size(max = 1000, message = "备注长度不能超过 1000")
    private String description;
    /** active / disabled；为空则不修改 */
    private String status;
}
```

`FondsQuery.java`：
```java
package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FondsQuery extends PageRequest {
    private String status;
    private Long organizationId;
    private String keyword;
}
```

`OrganizationCreateRequest.java`：
```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationCreateRequest {
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 200, message = "组织名称长度不能超过 200")
    private String orgName;

    @NotBlank(message = "组织类型不能为空")
    private String orgType;

    @Size(max = 64, message = "联系人长度不能超过 64")
    private String contactName;

    @Size(max = 32, message = "联系电话长度不能超过 32")
    private String contactPhone;
}
```

`OrganizationQuery.java`：
```java
package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizationQuery extends PageRequest {
    private String orgType;
    private String status;
    private String keyword;
}
```

`FondsResponse.java`（列表项，含归档数量）：
```java
package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class FondsResponse {
    private Long id;
    private String fondsNo;
    private String fondsName;
    private Long organizationId;
    private String organizationName;
    private String description;
    private String status;
    private Long archiveCount;
    private OffsetDateTime createdAt;
}
```

`FondsDetailResponse.java`（详情，含归档数 + 档案盒数）：
```java
package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class FondsDetailResponse {
    private Long id;
    private String fondsNo;
    private String fondsName;
    private Long organizationId;
    private String organizationName;
    private String description;
    private String status;
    private Long archiveCount;
    private Long boxCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

`OrganizationResponse.java`：
```java
package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class OrganizationResponse {
    private Long id;
    private String orgName;
    private String orgType;
    private String contactName;
    private String contactPhone;
    private String status;
    private OffsetDateTime createdAt;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS（DTO 无依赖问题）

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/archive/dto/request/Fonds*.java src/main/java/com/archive/dto/request/Organization*.java src/main/java/com/archive/dto/response/Fonds*.java src/main/java/com/archive/dto/response/OrganizationResponse.java
git commit -m "feat(fonds): 新增全宗与组织的请求响应DTO"
```

## Task 2: FondsService（TDD）

**Files:**
- Create: `src/main/java/com/archive/service/FondsService.java`
- Test: `src/test/java/com/archive/service/FondsServiceTest.java`

> 权限在 Controller 层守卫（见 Task 4），Service 保持纯业务、可单测，不调用 `AuthContext`。

- [ ] **Step 1: 写失败测试 `FondsServiceTest`**

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.FondsCreateRequest;
import com.archive.dto.request.FondsQuery;
import com.archive.dto.request.FondsUpdateRequest;
import com.archive.dto.response.FondsDetailResponse;
import com.archive.dto.response.FondsResponse;
import com.archive.entity.Fonds;
import com.archive.entity.Organization;
import com.archive.exception.BusinessException;
import com.archive.mapper.FondsMapper;
import com.archive.mapper.OrganizationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

class FondsServiceTest {

    private FondsService service;
    private FondsMapper fondsMapper;
    private OrganizationMapper organizationMapper;
    private AuditService auditService;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        fondsMapper = mock(FondsMapper.class);
        organizationMapper = mock(OrganizationMapper.class);
        auditService = mock(AuditService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new FondsService(fondsMapper, organizationMapper, auditService, jdbcTemplate);
    }

    @Test
    void createFonds_成功并写审计() {
        when(fondsMapper.selectOne(any())).thenReturn(null);
        Organization org = new Organization();
        org.setId(2L);
        org.setOrgName("苏州市财政局");
        when(organizationMapper.selectById(2L)).thenReturn(org);
        when(fondsMapper.insert(any(Fonds.class))).thenAnswer(inv -> {
            ((Fonds) inv.getArgument(0)).setId(10L);
            return 1;
        });

        FondsCreateRequest req = new FondsCreateRequest();
        req.setFondsNo("F001");
        req.setFondsName("财政局全宗");
        req.setOrganizationId(2L);
        FondsResponse resp = service.createFonds(req);

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getOrganizationName()).isEqualTo("苏州市财政局");
        verify(fondsMapper).insert(any(Fonds.class));
        verify(auditService).log(eq("M06"), eq("create_fonds"), eq("fonds"), eq(10L), any());
    }

    @Test
    void createFonds_全宗号重复抛冲突() {
        Fonds exist = new Fonds();
        exist.setId(1L);
        exist.setFondsNo("F001");
        when(fondsMapper.selectOne(any())).thenReturn(exist);

        FondsCreateRequest req = new FondsCreateRequest();
        req.setFondsNo("F001");
        req.setFondsName("x");
        req.setOrganizationId(2L);

        assertThatThrownBy(() -> service.createFonds(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("全宗号已存在");
        verify(fondsMapper, never()).insert(any());
    }

    @Test
    void createFonds_组织不存在抛冲突() {
        when(fondsMapper.selectOne(any())).thenReturn(null);
        when(organizationMapper.selectById(2L)).thenReturn(null);

        FondsCreateRequest req = new FondsCreateRequest();
        req.setFondsNo("F002");
        req.setFondsName("x");
        req.setOrganizationId(2L);

        assertThatThrownBy(() -> service.createFonds(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("所属组织不存在");
    }

    @Test
    void updateFonds_有业务关联时仅更新状态() {
        Fonds f = newFonds(10L, "F001", "旧名", 2L);
        when(fondsMapper.selectById(10L)).thenReturn(f);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(10L))).thenReturn(5L); // 归档关联 5 条

        FondsUpdateRequest req = new FondsUpdateRequest();
        req.setFondsName("新名");
        req.setStatus("disabled");

        service.updateFonds(10L, req);

        // fondsName 不应被应用，仅 status
        assertThat(f.getFondsName()).isEqualTo("旧名");
        assertThat(f.getStatus()).isEqualTo("disabled");
        verify(fondsMapper).updateById(any(Fonds.class));
        verify(auditService).log(eq("M06"), eq("update_fonds"), eq("fonds"), eq(10L), any());
    }

    @Test
    void updateFonds_无业务关联时全字段更新() {
        Fonds f = newFonds(10L, "F001", "旧名", 2L);
        when(fondsMapper.selectById(10L)).thenReturn(f);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(10L))).thenReturn(0L);

        FondsUpdateRequest req = new FondsUpdateRequest();
        req.setFondsName("新名");
        req.setDescription("新备注");

        service.updateFonds(10L, req);

        assertThat(f.getFondsName()).isEqualTo("新名");
        assertThat(f.getDescription()).isEqualTo("新备注");
    }

    @Test
    void updateFonds_不存在抛404() {
        when(fondsMapper.selectById(99L)).thenReturn(null);
        FondsUpdateRequest req = new FondsUpdateRequest();
        req.setFondsName("x");
        assertThatThrownBy(() -> service.updateFonds(99L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void listFonds_分页并补充归档数量与组织名() {
        Fonds f = newFonds(10L, "F001", "财政局全宗", 2L);
        Page<Fonds> page = new Page<>(1, 20);
        page.setRecords(List.of(f));
        page.setTotal(1L);
        when(fondsMapper.selectPage(any(Page.class), any())).thenReturn(page);
        Organization org = new Organization();
        org.setId(2L);
        org.setOrgName("苏州市财政局");
        when(organizationMapper.selectById(2L)).thenReturn(org);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(
                List.of(Map.of("fid", 10L, "cnt", 3L)));

        FondsQuery q = new FondsQuery();
        q.setPageNo(1);
        q.setPageSize(20);
        var result = service.listFonds(q);

        assertThat(result.getRecords()).hasSize(1);
        FondsResponse r = result.getRecords().get(0);
        assertThat(r.getArchiveCount()).isEqualTo(3L);
        assertThat(r.getOrganizationName()).isEqualTo("苏州市财政局");
    }

    @Test
    void getDetail_带归档与档案盒数量() {
        Fonds f = newFonds(10L, "F001", "财政局全宗", 2L);
        when(fondsMapper.selectById(10L)).thenReturn(f);
        when(organizationMapper.selectById(2L)).thenReturn(org(2L, "苏州市财政局"));
        // 第一次查 archives，第二次查 archive_boxes
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(10L)))
                .thenReturn(7L)   // archives
                .thenReturn(2L);  // archive_boxes

        FondsDetailResponse d = service.getFondsDetail(10L);
        assertThat(d.getArchiveCount()).isEqualTo(7L);
        assertThat(d.getBoxCount()).isEqualTo(2L);
    }

    private Fonds newFonds(Long id, String no, String name, Long orgId) {
        Fonds f = new Fonds();
        f.setId(id);
        f.setFondsNo(no);
        f.setFondsName(name);
        f.setOrganizationId(orgId);
        f.setStatus("active");
        return f;
    }

    private Organization org(Long id, String name) {
        Organization o = new Organization();
        o.setId(id);
        o.setOrgName(name);
        return o;
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -Dtest=FondsServiceTest test`
Expected: 编译失败（FondsService 不存在）

- [ ] **Step 3: 实现 `FondsService`**

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.FondsCreateRequest;
import com.archive.dto.request.FondsQuery;
import com.archive.dto.request.FondsUpdateRequest;
import com.archive.dto.response.FondsDetailResponse;
import com.archive.dto.response.FondsResponse;
import com.archive.entity.Fonds;
import com.archive.entity.Organization;
import com.archive.exception.BusinessException;
import com.archive.mapper.FondsMapper;
import com.archive.mapper.OrganizationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FondsService {

    private final FondsMapper fondsMapper;
    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    /** 17.1 查询全宗 */
    public PageResult<FondsResponse> listFonds(FondsQuery query) {
        QueryWrapper<Fonds> qw = new QueryWrapper<>();
        qw.isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            qw.eq("status", query.getStatus());
        }
        if (query.getOrganizationId() != null) {
            qw.eq("organization_id", query.getOrganizationId());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(w -> w.like("fonds_no", query.getKeyword())
                    .or().like("fonds_name", query.getKeyword()));
        }
        qw.orderByDesc("created_at");

        Page<Fonds> result = fondsMapper.selectPage(
                new Page<>(query.getPageNo(), query.getPageSize()), qw);

        List<Fonds> rows = result.getRecords();
        Map<Long, Long> archiveCounts = countByFonds("archives",
                rows.stream().map(Fonds::getId).toList());
        Map<Long, Organization> orgMap = loadOrganizations(
                rows.stream().map(Fonds::getOrganizationId).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<FondsResponse> voList = rows.stream().map(f -> {
            FondsResponse vo = toResponse(f);
            vo.setArchiveCount(archiveCounts.getOrDefault(f.getId(), 0L));
            Organization o = orgMap.get(f.getOrganizationId());
            vo.setOrganizationName(o != null ? o.getOrgName() : null);
            return vo;
        }).toList();
        return new PageResult<>(voList, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    /** 17.2 新增全宗 */
    @Transactional
    public FondsResponse createFonds(FondsCreateRequest req) {
        Fonds existing = fondsMapper.selectOne(new QueryWrapper<Fonds>().eq("fonds_no", req.getFondsNo()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "全宗号已存在");
        }
        Organization org = organizationMapper.selectById(req.getOrganizationId());
        if (org == null || org.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "所属组织不存在");
        }

        Fonds fonds = new Fonds();
        fonds.setFondsNo(req.getFondsNo());
        fonds.setFondsName(req.getFondsName());
        fonds.setOrganizationId(req.getOrganizationId());
        fonds.setDescription(req.getDescription());
        fonds.setStatus("active");
        fondsMapper.insert(fonds);

        auditService.log("M06", "create_fonds", "fonds", fonds.getId(),
                Map.of("fondsNo", fonds.getFondsNo(), "fondsName", fonds.getFondsName()));
        return toResponse(fonds);
    }

    /** 17.3 更新全宗（fondsNo 不可改；有业务关联时仅更新状态） */
    @Transactional
    public void updateFonds(Long id, FondsUpdateRequest req) {
        Fonds fonds = fondsMapper.selectById(id);
        if (fonds == null || fonds.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全宗不存在");
        }

        long assoc = countAssoc(id);
        boolean onlyStatus = assoc > 0L;

        if (onlyStatus) {
            // 有档案/档案盒关联：仅允许停用，忽略其余业务字段
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                fonds.setStatus(req.getStatus());
            }
            fondsMapper.updateById(fonds);
            auditService.log("M06", "update_fonds", "fonds", id,
                    Map.of("onlyStatus", true, "assoc", assoc,
                            "status", req.getStatus() != null ? req.getStatus() : fonds.getStatus()));
            return;
        }

        if (req.getFondsName() != null) fonds.setFondsName(req.getFondsName());
        if (req.getOrganizationId() != null) {
            Organization org = organizationMapper.selectById(req.getOrganizationId());
            if (org == null || org.getDeletedAt() != null) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "所属组织不存在");
            }
            fonds.setOrganizationId(req.getOrganizationId());
        }
        if (req.getDescription() != null) fonds.setDescription(req.getDescription());
        if (req.getStatus() != null && !req.getStatus().isBlank()) fonds.setStatus(req.getStatus());
        fondsMapper.updateById(fonds);
        auditService.log("M06", "update_fonds", "fonds", id, Map.of());
    }

    /** 详情 */
    public FondsDetailResponse getFondsDetail(Long id) {
        Fonds fonds = fondsMapper.selectById(id);
        if (fonds == null || fonds.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全宗不存在");
        }
        FondsDetailResponse vo = new FondsDetailResponse();
        vo.setId(fonds.getId());
        vo.setFondsNo(fonds.getFondsNo());
        vo.setFondsName(fonds.getFondsName());
        vo.setOrganizationId(fonds.getOrganizationId());
        vo.setDescription(fonds.getDescription());
        vo.setStatus(fonds.getStatus());
        vo.setCreatedAt(fonds.getCreatedAt());
        vo.setUpdatedAt(fonds.getUpdatedAt());
        if (fonds.getOrganizationId() != null) {
            Organization o = organizationMapper.selectById(fonds.getOrganizationId());
            vo.setOrganizationName(o != null ? o.getOrgName() : null);
        }
        vo.setArchiveCount(countWhere("archives", id));
        vo.setBoxCount(countWhere("archive_boxes", id));
        return vo;
    }

    // ===== 内部辅助 =====

    private long countAssoc(Long fondsId) {
        long a = countWhere("archives", fondsId);
        long b = countWhere("archive_boxes", fondsId);
        return a + b;
    }

    private long countWhere(String table, Long fondsId) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE fonds_id = ?", Long.class, fondsId);
        return cnt != null ? cnt : 0L;
    }

    private Map<Long, Long> countByFonds(String table, Collection<Long> fondsIds) {
        if (fondsIds.isEmpty()) return Map.of();
        String in = fondsIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT fonds_id AS fid, COUNT(*) AS cnt FROM " + table
                        + " WHERE fonds_id IN (" + in + ") GROUP BY fonds_id");
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(((Number) row.get("fid")).longValue(), ((Number) row.get("cnt")).longValue());
        }
        return result;
    }

    private Map<Long, Organization> loadOrganizations(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        List<Organization> list = organizationMapper.selectBatchIds(ids);
        Map<Long, Organization> map = new HashMap<>();
        for (Organization o : list) map.put(o.getId(), o);
        return map;
    }

    private FondsResponse toResponse(Fonds f) {
        FondsResponse vo = new FondsResponse();
        vo.setId(f.getId());
        vo.setFondsNo(f.getFondsNo());
        vo.setFondsName(f.getFondsName());
        vo.setOrganizationId(f.getOrganizationId());
        vo.setDescription(f.getDescription());
        vo.setStatus(f.getStatus());
        vo.setCreatedAt(f.getCreatedAt());
        return vo;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -Dtest=FondsServiceTest test`
Expected: BUILD SUCCESS，全部测试通过

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/FondsService.java src/test/java/com/archive/service/FondsServiceTest.java
git commit -m "feat(fonds): 实现全宗CRUD与业务关联保护"
```

## Task 3: OrganizationService（TDD）

**Files:**
- Create: `src/main/java/com/archive/service/OrganizationService.java`
- Test: `src/test/java/com/archive/service/OrganizationServiceTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.archive.service;

import com.archive.dto.request.OrganizationCreateRequest;
import com.archive.dto.request.OrganizationQuery;
import com.archive.dto.response.OrganizationResponse;
import com.archive.entity.Organization;
import com.archive.enums.OrgType;
import com.archive.exception.BusinessException;
import com.archive.mapper.OrganizationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrganizationServiceTest {

    private OrganizationService service;
    private OrganizationMapper organizationMapper;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        organizationMapper = mock(OrganizationMapper.class);
        auditService = mock(AuditService.class);
        service = new OrganizationService(organizationMapper, auditService);
    }

    @Test
    void createOrganization_成功并写审计() {
        when(organizationMapper.selectOne(any())).thenReturn(null);
        when(organizationMapper.insert(any(Organization.class))).thenAnswer(inv -> {
            ((Organization) inv.getArgument(0)).setId(20L);
            return 1;
        });

        OrganizationCreateRequest req = new OrganizationCreateRequest();
        req.setOrgName("苏州市财政局");
        req.setOrgType("government");
        req.setContactName("张伟");
        OrganizationResponse resp = service.createOrganization(req);

        assertThat(resp.getId()).isEqualTo(20L);
        assertThat(resp.getOrgType()).isEqualTo("government");
        verify(organizationMapper).insert(any(Organization.class));
        verify(auditService).log(eq("M06"), eq("create_organization"), eq("organization"), eq(20L), any());
    }

    @Test
    void createOrganization_名称重复抛冲突() {
        Organization exist = new Organization();
        exist.setId(1L);
        exist.setOrgName("苏州市财政局");
        when(organizationMapper.selectOne(any())).thenReturn(exist);

        OrganizationCreateRequest req = new OrganizationCreateRequest();
        req.setOrgName("苏州市财政局");
        req.setOrgType("government");
        assertThatThrownBy(() -> service.createOrganization(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("组织名称已存在");
        verify(organizationMapper, never()).insert(any());
    }

    @Test
    void createOrganization_非法orgType抛校验失败() {
        when(organizationMapper.selectOne(any())).thenReturn(null);
        OrganizationCreateRequest req = new OrganizationCreateRequest();
        req.setOrgName("x");
        req.setOrgType("not_a_type");
        assertThatThrownBy(() -> service.createOrganization(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listOrganizations_分页过滤() {
        Organization o = new Organization();
        o.setId(1L);
        o.setOrgName("苏州市财政局");
        o.setOrgType(OrgType.government);
        o.setStatus("active");
        Page<Organization> page = new Page<>(1, 20);
        page.setRecords(List.of(o));
        page.setTotal(1L);
        when(organizationMapper.selectPage(any(), any())).thenReturn(page);

        OrganizationQuery q = new OrganizationQuery();
        q.setPageNo(1);
        q.setPageSize(20);
        var result = service.listOrganizations(q);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getOrgName()).isEqualTo("苏州市财政局");
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -q -Dtest=OrganizationServiceTest test`
Expected: 编译失败（OrganizationService 不存在）

- [ ] **Step 3: 实现 `OrganizationService`**

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.OrganizationCreateRequest;
import com.archive.dto.request.OrganizationQuery;
import com.archive.dto.response.OrganizationResponse;
import com.archive.entity.Organization;
import com.archive.enums.OrgType;
import com.archive.exception.BusinessException;
import com.archive.mapper.OrganizationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;

    /** 17.4 查询组织 */
    public PageResult<OrganizationResponse> listOrganizations(OrganizationQuery query) {
        QueryWrapper<Organization> qw = new QueryWrapper<>();
        qw.isNull("deleted_at");
        if (query.getOrgType() != null && !query.getOrgType().isBlank()) {
            qw.eq("org_type", query.getOrgType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            qw.eq("status", query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.like("org_name", query.getKeyword());
        }
        qw.orderByDesc("created_at");

        Page<Organization> result = organizationMapper.selectPage(
                new Page<>(query.getPageNo(), query.getPageSize()), qw);
        List<OrganizationResponse> voList = result.getRecords().stream()
                .map(this::toResponse).toList();
        return new PageResult<>(voList, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    /** 17.5 新增组织（名称唯一 §17.6） */
    @Transactional
    public OrganizationResponse createOrganization(OrganizationCreateRequest req) {
        Organization existing = organizationMapper.selectOne(
                new QueryWrapper<Organization>().eq("org_name", req.getOrgName()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "组织名称已存在");
        }

        Organization org = new Organization();
        org.setOrgName(req.getOrgName());
        org.setOrgType(OrgType.valueOf(req.getOrgType())); // 非法值抛 IllegalArgumentException → 全局 400
        org.setContactName(req.getContactName());
        org.setContactPhone(req.getContactPhone());
        org.setStatus("active");
        organizationMapper.insert(org);

        auditService.log("M06", "create_organization", "organization", org.getId(),
                Map.of("orgName", org.getOrgName(), "orgType", org.getOrgType().name()));
        return toResponse(org);
    }

    private OrganizationResponse toResponse(Organization o) {
        OrganizationResponse vo = new OrganizationResponse();
        vo.setId(o.getId());
        vo.setOrgName(o.getOrgName());
        vo.setOrgType(o.getOrgType() != null ? o.getOrgType().name() : null);
        vo.setContactName(o.getContactName());
        vo.setContactPhone(o.getContactPhone());
        vo.setStatus(o.getStatus());
        vo.setCreatedAt(o.getCreatedAt());
        return vo;
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `mvn -q -Dtest=OrganizationServiceTest test`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/archive/service/OrganizationService.java src/test/java/com/archive/service/OrganizationServiceTest.java
git commit -m "feat(fonds): 实现组织查询与新增服务"
```

## Task 4: Controller（含角色守卫）

**Files:**
- Create: `src/main/java/com/archive/controller/FondsController.java`
- Create: `src/main/java/com/archive/controller/OrganizationController.java`

> 角色守卫放 Controller（Service 保持可单测）。`isAdmin()` 含前台，不能用；显式判断 `back_archivist`/`sys_admin`。

- [ ] **Step 1: 实现 `FondsController`**

```java
package com.archive.controller;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.FondsCreateRequest;
import com.archive.dto.request.FondsQuery;
import com.archive.dto.request.FondsUpdateRequest;
import com.archive.dto.response.FondsDetailResponse;
import com.archive.dto.response.FondsResponse;
import com.archive.enums.RoleCode;
import com.archive.common.AuthContext;
import com.archive.exception.BusinessException;
import com.archive.service.FondsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/fonds")
@RequiredArgsConstructor
@Tag(name = "全宗管理", description = "全宗 CRUD（后台档案管理员、系统管理员）")
public class FondsController {

    private final FondsService fondsService;

    @GetMapping
    @Operation(summary = "查询全宗列表")
    public R<PageResult<FondsResponse>> list(@Valid FondsQuery query) {
        requireBackOrAdmin();
        return R.ok(fondsService.listFonds(query));
    }

    @PostMapping
    @Operation(summary = "新增全宗")
    public R<FondsResponse> create(@Valid @RequestBody FondsCreateRequest req) {
        requireBackOrAdmin();
        return R.ok(fondsService.createFonds(req));
    }

    @GetMapping("/{fondsId}")
    @Operation(summary = "全宗详情")
    public R<FondsDetailResponse> detail(@PathVariable Long fondsId) {
        requireBackOrAdmin();
        return R.ok(fondsService.getFondsDetail(fondsId));
    }

    @PutMapping("/{fondsId}")
    @Operation(summary = "更新全宗（有业务关联时仅停用）")
    public R<Void> update(@PathVariable Long fondsId, @Valid @RequestBody FondsUpdateRequest req) {
        requireBackOrAdmin();
        fondsService.updateFonds(fondsId, req);
        return R.ok();
    }

    private void requireBackOrAdmin() {
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.sys_admin))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }
}
```

- [ ] **Step 2: 实现 `OrganizationController`**

```java
package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.OrganizationCreateRequest;
import com.archive.dto.request.OrganizationQuery;
import com.archive.dto.response.OrganizationResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
@Tag(name = "组织维护", description = "组织查询/新增（并入用户管理页）")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "查询组织列表")
    public R<PageResult<OrganizationResponse>> list(@Valid OrganizationQuery query) {
        // 查询供下拉，后台档案管理员与系统管理员均可
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.sys_admin))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足");
        }
        return R.ok(organizationService.listOrganizations(query));
    }

    @PostMapping
    @Operation(summary = "新增组织（仅系统管理员）")
    public R<OrganizationResponse> create(@Valid @RequestBody OrganizationCreateRequest req) {
        if (!AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅系统管理员可新增组织");
        }
        return R.ok(organizationService.createOrganization(req));
    }
}
```

- [ ] **Step 3: 编译 + 全量单测**

Run: `mvn -q test`
Expected: BUILD SUCCESS（全部已有测试 + 新增测试通过）

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/archive/controller/FondsController.java src/main/java/com/archive/controller/OrganizationController.java
git commit -m "feat(fonds): 新增全宗与组织接口控制器"
```

## Task 5: 运行时验证

- [ ] **Step 1: 启动依赖与后端**

Run（项目根）: `docker compose up -d postgres`（确保 5432 可用）
Run（backend）: `mvn spring-boot:run` （dev profile，8080）

Expected: 启动日志无异常，Swagger 可访问 `http://localhost:8080/swagger-ui.html`

- [ ] **Step 2: 用 sys_admin 登录拿 token**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginName":"fang.admin","password":"123456"}' | jq .
```
> 种子管理员账号名以 V16 为准（实现时确认；若为 `zhou.admin` 等则替换）。取返回 token 存入 `$T`。

- [ ] **Step 3: 验证全宗 CRUD 闭环**

```bash
T=<上一步 token>
# 新增
curl -s -X POST http://localhost:8080/api/admin/fonds -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"fondsNo":"F999","fondsName":"验证全宗","organizationId":1,"description":"运行时验证"}' | jq .
# 重复全宗号 → 409
curl -s -X POST http://localhost:8080/api/admin/fonds -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"fondsNo":"F999","fondsName":"重复","organizationId":1}' | jq .
# 列表筛选 keyword=F999
curl -s "http://localhost:8080/api/admin/fonds?keyword=F999" -H "Authorization: Bearer $T" | jq .
# 详情（带 archiveCount/boxCount，新全宗应为 0）
curl -s "http://localhost:8080/api/admin/fonds/<id>" -H "Authorization: Bearer $T" | jq .
# 更新（停用）
curl -s -X PUT http://localhost:8080/api/admin/fonds/<id> -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"status":"disabled"}' | jq .
```

- [ ] **Step 4: 验证组织查询/新增 + 重名**

```bash
curl -s "http://localhost:8080/api/admin/organizations" -H "Authorization: Bearer $T" | jq '.data.records[0:3]'
curl -s -X POST http://localhost:8080/api/admin/organizations -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"orgName":"验证组织","orgType":"government","contactName":"测试","contactPhone":"13800000000"}' | jq .
# 重名 → 409
curl -s -X POST http://localhost:8080/api/admin/organizations -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"orgName":"验证组织","orgType":"government"}' | jq .
```

- [ ] **Step 5: 验证审计写入**

Run: `psql` 或 `docker exec` 查 `SELECT module_name, operation_type, business_type, business_id FROM audit_logs WHERE module_name='M06' ORDER BY operated_at DESC LIMIT 5;`
Expected: 出现 create_fonds / update_fonds / create_organization 记录。

- [ ] **Step 6: 验证权限（非 sys_admin/back_archivist 访问 → 403）**

用公众账号 token（或无 token）访问 `GET /api/admin/fonds` → 403/401。

- [ ] **Step 7: 记录验证报告并提交**

将命令与响应摘要写入 `backend/docs/runtime-verification-fonds-zhou-06-15.md`（请求/响应关键字段 + DB 核对）。
```bash
git add backend/docs/runtime-verification-fonds-zhou-06-15.md
git commit -m "docs(fonds): 记录全宗与组织运行时验证结果"
```

> 运行时验证全绿后，模块一视为基本完成。**push / PR 等用户查看后再操作。**



