# M07 库房管理模块（feat/storage-zhou）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 M07 库房管理的 9 个 REST 端点（接口文档第 16 章）+ 管理概览告警查询方法，覆盖库房/架位/档案盒的增删改查与移动、占用率告警。

**Architecture:** 复用已存在的 4 个实体与 Mapper（WarehouseRoom/StorageLocation/ArchiveBox/ArchiveBoxItem），新建 WarehouseService + WarehouseController + BoxNoUtil + 10 个 DTO。占用率通过 JdbcTemplate 聚合查询实时计算；架位占用以 `archive_boxes.location_id` 为单一事实来源。不重构 M05、不软删、不做上架生命周期转换（归 M05）。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、PostgreSQL、SaToken；测试 JUnit5 + Mockito + AssertJ。

**分支：** `feat/storage-zhou`（已创建，spec 已提交）。任务顺序执行，每个 Service 方法先写测试（红）再实现（绿）。

**设计依据：** [2026-06-14-storage-zhou-design.md](../specs/2026-06-14-storage-zhou-design.md)

---

## 文件结构

| 文件 | 职责 | 动作 |
|------|------|------|
| `util/BoxNoUtil.java` | 用 `seq_archive_box_no` 生成 `BOX-{6位序号}` | 新建 |
| `service/WarehouseService.java` | 全部库房管理业务逻辑（9 端点 + listWarningRooms） | 新建 |
| `controller/WarehouseController.java` | 9 个 REST 端点 | 新建 |
| `dto/request/WarehouseRoomCreateRequest.java` | 新增库房入参 | 新建 |
| `dto/request/WarehouseRoomUpdateRequest.java` | 更新库房入参 | 新建 |
| `dto/request/LocationStatusRequest.java` | 架位停用/启用入参 | 新建 |
| `dto/request/ArchiveBoxCreateRequest.java` | 新增档案盒入参 | 新建 |
| `dto/request/ArchiveBoxMoveRequest.java` | 移动档案盒入参 | 新建 |
| `dto/response/WarehouseRoomResponse.java` | 库房列表项（含占用率/告警） | 新建 |
| `dto/response/StorageLocationResponse.java` | 架位列表项（含当前盒/盒内件数） | 新建 |
| `dto/response/ArchiveBoxResponse.java` | 档案盒列表项 | 新建 |
| `dto/response/ArchiveBoxDetailResponse.java` | 档案盒详情（含盒内条目） | 新建 |
| `dto/response/WarehouseWarningResponse.java` | 概览告警库房 | 新建 |
| `test/util/BoxNoUtilTest.java` | BoxNoUtil 单测 | 新建 |
| `test/service/WarehouseServiceTest.java` | WarehouseService 单测 | 新建 |

测试目录前缀：`backend/src/test/java/com/archive/`
主代码目录前缀：`backend/src/main/java/com/archive/`

---

## Task 1: BoxNoUtil

**Files:**
- Create: `backend/src/main/java/com/archive/util/BoxNoUtil.java`
- Test: `backend/src/test/java/com/archive/util/BoxNoUtilTest.java`

- [ ] **Step 1: 写失败测试**

`backend/src/test/java/com/archive/util/BoxNoUtilTest.java`：

```java
package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoxNoUtilTest {

    @Test
    void generate_使用序列并格式化为BOX加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                eq("SELECT nextval('seq_archive_box_no')"), eq(Long.class)))
                .thenReturn(1L);

        BoxNoUtil util = new BoxNoUtil(jdbc);

        assertThat(util.generate()).isEqualTo("BOX-000001");
    }

    @Test
    void generate_大序号也能正确补零() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                eq("SELECT nextval('seq_archive_box_no')"), eq(Long.class)))
                .thenReturn(12345L);

        assertThat(new BoxNoUtil(jdbc).generate()).isEqualTo("BOX-012345");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=BoxNoUtilTest`
Expected: 编译失败（`BoxNoUtil` 类不存在）

- [ ] **Step 3: 写最小实现**

`backend/src/main/java/com/archive/util/BoxNoUtil.java`：

```java
package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 档案盒号生成工具。
 * 使用 seq_archive_box_no 序列生成 BOX-{6位序号} 格式盒号。
 * 唯一生成点：新增档案盒时。
 */
@Component
@RequiredArgsConstructor
public class BoxNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_archive_box_no')", Long.class);
        return String.format("BOX-%06d", seq);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=BoxNoUtilTest`
Expected: PASS（2 个测试通过）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/util/BoxNoUtil.java backend/src/test/java/com/archive/util/BoxNoUtilTest.java
git commit -m "feat(storage): 新增档案盒号生成工具 BoxNoUtil"
```

---

## Task 2: 请求 DTO（5 个）

DTO 为纯数据载体，无逻辑，以编译通过为验证标准。

**Files:**
- Create: `backend/src/main/java/com/archive/dto/request/WarehouseRoomCreateRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/WarehouseRoomUpdateRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/LocationStatusRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/ArchiveBoxCreateRequest.java`
- Create: `backend/src/main/java/com/archive/dto/request/ArchiveBoxMoveRequest.java`

- [ ] **Step 1: 创建 WarehouseRoomCreateRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增库房请求。
 */
@Data
public class WarehouseRoomCreateRequest {

    @NotBlank(message = "库房号不能为空")
    private String roomNo;

    @NotBlank(message = "库房名称不能为空")
    private String roomName;

    @NotNull(message = "机架数量不能为空")
    @Min(value = 1, message = "机架数量必须大于 0")
    private Integer rackCount;

    @NotNull(message = "单机架层数不能为空")
    @Min(value = 1, message = "单机架层数必须大于 0")
    private Integer layersPerRack;

    @NotNull(message = "每层盒数不能为空")
    @Min(value = 1, message = "每层盒数必须大于 0")
    private Integer boxesPerLayer;

    /** 占用告警阈值，空时服务层取默认 0.85。 */
    private BigDecimal warningThreshold;
}
```

- [ ] **Step 2: 创建 WarehouseRoomUpdateRequest**

```java
package com.archive.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新库房请求（仅名称/阈值/状态可改）。
 */
@Data
public class WarehouseRoomUpdateRequest {

    private String roomName;

    private BigDecimal warningThreshold;

    /** active / disabled。 */
    private String status;
}
```

- [ ] **Step 3: 创建 LocationStatusRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 架位停用/启用请求。
 */
@Data
public class LocationStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    private String reason;
}
```

- [ ] **Step 4: 创建 ArchiveBoxCreateRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增档案盒请求。
 */
@Data
public class ArchiveBoxCreateRequest {

    @NotNull(message = "架位不能为空")
    private Long locationId;

    private Integer categoryId;

    private Long fondsId;

    private String yearLabel;

    private String spineText;

    private Integer capacity;
}
```

- [ ] **Step 5: 创建 ArchiveBoxMoveRequest**

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移动档案盒请求。
 */
@Data
public class ArchiveBoxMoveRequest {

    @NotNull(message = "目标架位不能为空")
    private Long targetLocationId;

    private String reason;
}
```

- [ ] **Step 6: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/archive/dto/request/
git commit -m "feat(storage): 新增库房管理请求 DTO"
```

---

## Task 3: 响应 DTO（5 个）

**Files:**
- Create: `backend/src/main/java/com/archive/dto/response/WarehouseRoomResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/StorageLocationResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/ArchiveBoxResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/ArchiveBoxDetailResponse.java`
- Create: `backend/src/main/java/com/archive/dto/response/WarehouseWarningResponse.java`

- [ ] **Step 1: 创建 WarehouseRoomResponse**

```java
package com.archive.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库房列表项，含占用率与告警状态。
 */
@Data
public class WarehouseRoomResponse {

    private Long id;
    private String roomNo;
    private String roomName;
    private Integer rackCount;
    private Integer layersPerRack;
    private Integer boxesPerLayer;
    private Integer capacity;
    private BigDecimal warningThreshold;
    private String status;

    /** 已占用盒位数。 */
    private Integer occupiedSlots;
    /** 占用率，0~1。 */
    private BigDecimal occupancyRate;
    /** 是否达到告警阈值。 */
    private Boolean warning;
}
```

- [ ] **Step 2: 创建 StorageLocationResponse**

```java
package com.archive.dto.response;

import lombok.Data;

/**
 * 架位列表项，含当前档案盒与盒内件数。
 */
@Data
public class StorageLocationResponse {

    private Long id;
    private Long roomId;
    private Integer rackNo;
    private Integer layerNo;
    private Integer boxSlotNo;
    private String locationCode;
    private String status;

    /** 是否被档案盒占用。 */
    private Boolean occupied;
    private Long currentBoxId;
    private String currentBoxNo;
    /** 当前盒内件数。 */
    private Integer boxItemCount;
}
```

- [ ] **Step 3: 创建 ArchiveBoxResponse**

```java
package com.archive.dto.response;

import lombok.Data;

/**
 * 档案盒列表项。
 */
@Data
public class ArchiveBoxResponse {

    private Long id;
    private String boxNo;
    private Long locationId;
    private String locationCode;
    private String roomNo;
    private Integer categoryId;
    private Long fondsId;
    private String yearLabel;
    private String spineText;
    private Integer capacity;
    private Integer usedCount;
    private String status;
}
```

- [ ] **Step 4: 创建 ArchiveBoxDetailResponse（含盒内条目嵌套类）**

```java
package com.archive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 档案盒详情，含架位与盒内档案条目。
 */
@Data
public class ArchiveBoxDetailResponse {

    private Long id;
    private String boxNo;
    private Long locationId;
    private String locationCode;
    private String roomNo;
    private Integer categoryId;
    private Long fondsId;
    private String yearLabel;
    private String spineText;
    private Integer capacity;
    private Integer usedCount;
    private String status;

    private List<BoxItemView> items;

    /** 盒内档案条目视图。 */
    @Data
    public static class BoxItemView {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private Integer sortNo;
        private Integer pageCount;
        private String physicalStatus;
    }
}
```

- [ ] **Step 5: 创建 WarehouseWarningResponse**

```java
package com.archive.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 概览用库房告警项。
 */
@Data
public class WarehouseWarningResponse {

    private Long roomId;
    private String roomNo;
    private String roomName;
    private Integer occupiedSlots;
    private Integer capacity;
    private BigDecimal occupancyRate;
    private BigDecimal warningThreshold;
}
```

- [ ] **Step 6: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/archive/dto/response/
git commit -m "feat(storage): 新增库房管理响应 DTO"
```

---

## Task 4: WarehouseService 骨架 + 新增库房（16.2）

**Files:**
- Create: `backend/src/main/java/com/archive/service/WarehouseService.java`
- Test: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`

测试类一次性 mock 全部依赖（后续任务复用同一 `@BeforeEach`）。

- [ ] **Step 1: 写失败测试**

`backend/src/test/java/com/archive/service/WarehouseServiceTest.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.WarehouseRoomCreateRequest;
import com.archive.dto.response.WarehouseRoomResponse;
import com.archive.entity.StorageLocation;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveBoxMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.StorageLocationMapper;
import com.archive.mapper.WarehouseRoomMapper;
import com.archive.util.BoxNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WarehouseServiceTest {

    private WarehouseService service;
    private WarehouseRoomMapper warehouseRoomMapper;
    private StorageLocationMapper storageLocationMapper;
    private ArchiveBoxMapper archiveBoxMapper;
    private ArchiveBoxItemMapper archiveBoxItemMapper;
    private ArchiveMapper archiveMapper;
    private JdbcTemplate jdbcTemplate;
    private BoxNoUtil boxNoUtil;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        warehouseRoomMapper = mock(WarehouseRoomMapper.class);
        storageLocationMapper = mock(StorageLocationMapper.class);
        archiveBoxMapper = mock(ArchiveBoxMapper.class);
        archiveBoxItemMapper = mock(ArchiveBoxItemMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        boxNoUtil = mock(BoxNoUtil.class);
        auditService = mock(AuditService.class);

        service = new WarehouseService(warehouseRoomMapper, storageLocationMapper,
                archiveBoxMapper, archiveBoxItemMapper, archiveMapper,
                jdbcTemplate, boxNoUtil, auditService);
    }

    @Test
    void createRoom_库房号重复抛冲突() {
        WarehouseRoomCreateRequest req = new WarehouseRoomCreateRequest();
        req.setRoomNo("401");
        req.setRoomName("A");
        req.setRackCount(1);
        req.setLayersPerRack(1);
        req.setBoxesPerLayer(1);
        when(warehouseRoomMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createRoom(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库房号已存在");
    }

    @Test
    void createRoom_生成正确数量与编码的架位() {
        WarehouseRoomCreateRequest req = new WarehouseRoomCreateRequest();
        req.setRoomNo("401");
        req.setRoomName("综合库房");
        req.setRackCount(2);
        req.setLayersPerRack(3);
        req.setBoxesPerLayer(4);
        when(warehouseRoomMapper.selectCount(any())).thenReturn(0L);

        ArgumentCaptor<StorageLocation> captor = ArgumentCaptor.forClass(StorageLocation.class);

        WarehouseRoomResponse resp = service.createRoom(req);

        // 2*3*4 = 24 个架位
        verify(storageLocationMapper, times(24)).insert(captor.capture());
        List<StorageLocation> inserted = captor.getAllValues();
        assertThat(inserted).extracting(StorageLocation::getLocationCode)
                .contains("401-01-01-01", "401-02-03-04");
        assertThat(resp.getCapacity()).isEqualTo(24);
        assertThat(resp.getOccupancyRate()).isEqualByComparTo(BigDecimal.ZERO);
        verify(auditService).log(eq("M07"), eq("create_room"), eq("warehouse_room"), any(), any());
    }

    @Test
    void createRoom_阈值越界抛校验失败() {
        WarehouseRoomCreateRequest req = new WarehouseRoomCreateRequest();
        req.setRoomNo("402");
        req.setRoomName("B");
        req.setRackCount(1);
        req.setLayersPerRack(1);
        req.setBoxesPerLayer(1);
        req.setWarningThreshold(new BigDecimal("1.5"));
        when(warehouseRoomMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.createRoom(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: 编译失败（`WarehouseService` 类不存在）

- [ ] **Step 3: 写 WarehouseService 骨架与 createRoom**

`backend/src/main/java/com/archive/service/WarehouseService.java`：

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.WarehouseRoomCreateRequest;
import com.archive.dto.response.WarehouseRoomResponse;
import com.archive.entity.StorageLocation;
import com.archive.entity.WarehouseRoom;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveBoxMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.StorageLocationMapper;
import com.archive.mapper.WarehouseRoomMapper;
import com.archive.util.BoxNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 库房管理服务。
 * 处理库房、架位、档案盒的增删改查、移动与占用率告警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRoomMapper warehouseRoomMapper;
    private final StorageLocationMapper storageLocationMapper;
    private final ArchiveBoxMapper archiveBoxMapper;
    private final ArchiveBoxItemMapper archiveBoxItemMapper;
    private final ArchiveMapper archiveMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BoxNoUtil boxNoUtil;
    private final AuditService auditService;

    private static final BigDecimal DEFAULT_WARNING_THRESHOLD = new BigDecimal("0.85");

    // ==================== 16.2 新增库房 ====================

    @Transactional
    public WarehouseRoomResponse createRoom(WarehouseRoomCreateRequest req) {
        // 1. roomNo 唯一
        QueryWrapper<WarehouseRoom> rw = new QueryWrapper<>();
        rw.eq("room_no", req.getRoomNo());
        if (warehouseRoomMapper.selectCount(rw) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "库房号已存在");
        }

        // 2. 阈值默认与范围校验
        BigDecimal threshold = req.getWarningThreshold() != null
                ? req.getWarningThreshold() : DEFAULT_WARNING_THRESHOLD;
        if (threshold.compareTo(BigDecimal.ZERO) <= 0
                || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "告警阈值必须在 (0, 1] 范围内");
        }

        // 3. 容量
        int capacity = req.getRackCount() * req.getLayersPerRack() * req.getBoxesPerLayer();

        WarehouseRoom room = new WarehouseRoom();
        room.setRoomNo(req.getRoomNo());
        room.setRoomName(req.getRoomName());
        room.setRackCount(req.getRackCount());
        room.setLayersPerRack(req.getLayersPerRack());
        room.setBoxesPerLayer(req.getBoxesPerLayer());
        room.setCapacity(capacity);
        room.setWarningThreshold(threshold);
        room.setStatus("active");
        warehouseRoomMapper.insert(room);

        // 4. 批量生成架位
        generateLocations(room);

        auditService.log("M07", "create_room", "warehouse_room", room.getId(),
                Map.of("roomNo", req.getRoomNo(), "capacity", capacity));

        return toRoomResponse(room, 0);
    }

    /** 按库房结构生成固定架位，编码 {roomNo}-{rack:02d}-{layer:02d}-{slot:02d}。 */
    private void generateLocations(WarehouseRoom room) {
        List<StorageLocation> locations = new ArrayList<>();
        for (int rack = 1; rack <= room.getRackCount(); rack++) {
            for (int layer = 1; layer <= room.getLayersPerRack(); layer++) {
                for (int slot = 1; slot <= room.getBoxesPerLayer(); slot++) {
                    StorageLocation loc = new StorageLocation();
                    loc.setRoomId(room.getId());
                    loc.setRackNo(rack);
                    loc.setLayerNo(layer);
                    loc.setBoxSlotNo(slot);
                    loc.setLocationCode(String.format("%s-%02d-%02d-%02d",
                            room.getRoomNo(), rack, layer, slot));
                    loc.setStatus("active");
                    locations.add(loc);
                }
            }
        }
        for (StorageLocation loc : locations) {
            storageLocationMapper.insert(loc);
        }
    }

    /** 组装库房响应，occupiedSlots 由调用方提供（listRooms 时实时计算）。 */
    private WarehouseRoomResponse toRoomResponse(WarehouseRoom room, int occupiedSlots) {
        WarehouseRoomResponse resp = new WarehouseRoomResponse();
        resp.setId(room.getId());
        resp.setRoomNo(room.getRoomNo());
        resp.setRoomName(room.getRoomName());
        resp.setRackCount(room.getRackCount());
        resp.setLayersPerRack(room.getLayersPerRack());
        resp.setBoxesPerLayer(room.getBoxesPerLayer());
        resp.setCapacity(room.getCapacity());
        resp.setWarningThreshold(room.getWarningThreshold());
        resp.setStatus(room.getStatus());
        resp.setOccupiedSlots(occupiedSlots);
        int capacity = room.getCapacity() != null ? room.getCapacity() : 0;
        BigDecimal rate = capacity > 0
                ? BigDecimal.valueOf(occupiedSlots)
                        .divide(BigDecimal.valueOf(capacity), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        resp.setOccupancyRate(rate);
        BigDecimal threshold = room.getWarningThreshold() != null
                ? room.getWarningThreshold() : DEFAULT_WARNING_THRESHOLD;
        resp.setWarning(rate.compareTo(threshold) >= 0);
        return resp;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（3 个测试通过）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现新增库房与架位批量生成"
```

---

## Task 5: 查询库房列表 + 占用率（16.1）

**Files:**
- Modify: `backend/src/main/java/com/archive/service/WarehouseService.java`（新增 `listRooms` + 占用率 SQL 常量）
- Modify: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`（新增测试）

- [ ] **Step 1: 在 WarehouseServiceTest 新增测试**

在测试类内追加：

```java
    @Test
    void listRooms_计算占用率与告警状态() {
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setRoomNo("401");
        room.setRoomName("综合库房");
        room.setCapacity(100);
        room.setWarningThreshold(new BigDecimal("0.85"));
        room.setStatus("active");
        when(warehouseRoomMapper.selectList(any())).thenReturn(List.of(room));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(90L);

        List<WarehouseRoomResponse> result = service.listRooms(null, null);

        assertThat(result).hasSize(1);
        WarehouseRoomResponse r = result.get(0);
        assertThat(r.getOccupiedSlots()).isEqualTo(90);
        assertThat(r.getOccupancyRate()).isEqualByComparTo(new BigDecimal("0.9000"));
        assertThat(r.getWarning()).isTrue();
    }

    @Test
    void listRooms_未达阈值不告警() {
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(2L);
        room.setRoomNo("402");
        room.setCapacity(100);
        room.setWarningThreshold(new BigDecimal("0.85"));
        room.setStatus("active");
        when(warehouseRoomMapper.selectList(any())).thenReturn(List.of(room));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(2L))).thenReturn(50L);

        List<WarehouseRoomResponse> result = service.listRooms(null, null);

        assertThat(result.get(0).getWarning()).isFalse();
        assertThat(result.get(0).getOccupiedSlots()).isEqualTo(50);
    }
```

在测试类 import 区追加：`import static org.mockito.ArgumentMatchers.anyString;`、`import static org.mockito.ArgumentMatchers.eq;`

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: 编译失败（`listRooms` 方法不存在）

- [ ] **Step 3: 在 WarehouseService 实现 listRooms**

在 `DEFAULT_WARNING_THRESHOLD` 常量下方新增占用率 SQL 常量：

```java
    /** 按库房统计已占用盒位数（架位上有活动档案盒）。 */
    private static final String COUNT_OCCUPIED_BY_ROOM_SQL =
            "SELECT COUNT(*) FROM archive_boxes " +
            "WHERE location_id IN (SELECT id FROM storage_locations WHERE room_id = ?) " +
            "AND status IN ('normal','full')";
```

在 `createRoom` 方法之后新增：

```java
    // ==================== 16.1 查询库房列表 ====================

    public List<WarehouseRoomResponse> listRooms(String status, String keyword) {
        QueryWrapper<WarehouseRoom> w = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like("room_no", keyword).or().like("room_name", keyword));
        }
        w.orderByAsc("room_no");

        List<WarehouseRoom> rooms = warehouseRoomMapper.selectList(w);
        return rooms.stream()
                .map(room -> toRoomResponse(room, countOccupiedSlots(room.getId())))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 统计某库房当前被活动档案盒占用的盒位数。 */
    private int countOccupiedSlots(Long roomId) {
        Long count = jdbcTemplate.queryForObject(
                COUNT_OCCUPIED_BY_ROOM_SQL, Long.class, roomId);
        return count != null ? count.intValue() : 0;
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（5 个测试通过）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现库房列表查询与占用率告警"
```

---

## Task 6: 更新库房（16.3）

仅允许修改 roomName / warningThreshold / status。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/WarehouseService.java`（新增 `updateRoom`）
- Modify: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`

- [ ] **Step 1: 新增测试**

```java
    @Test
    void updateRoom_仅修改名称阈值状态() {
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setRoomNo("401");
        room.setCapacity(100);
        room.setWarningThreshold(new BigDecimal("0.85"));
        room.setStatus("active");
        when(warehouseRoomMapper.selectById(1L)).thenReturn(room);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(10L);

        com.archive.dto.request.WarehouseRoomUpdateRequest req =
                new com.archive.dto.request.WarehouseRoomUpdateRequest();
        req.setRoomName("新名称");
        req.setWarningThreshold(new BigDecimal("0.90"));
        req.setStatus("disabled");

        WarehouseRoomResponse resp = service.updateRoom(1L, req);

        assertThat(resp.getRoomName()).isEqualTo("新名称");
        assertThat(resp.getWarningThreshold()).isEqualByComparTo(new BigDecimal("0.90"));
        assertThat(resp.getStatus()).isEqualTo("disabled");
        verify(warehouseRoomMapper).updateById(any());
    }

    @Test
    void updateRoom_库房不存在抛404() {
        when(warehouseRoomMapper.selectById(99L)).thenReturn(null);
        com.archive.dto.request.WarehouseRoomUpdateRequest req =
                new com.archive.dto.request.WarehouseRoomUpdateRequest();
        req.setRoomName("x");

        assertThatThrownBy(() -> service.updateRoom(99L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void updateRoom_阈值越界抛校验失败() {
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setCapacity(100);
        room.setWarningThreshold(new BigDecimal("0.85"));
        when(warehouseRoomMapper.selectById(1L)).thenReturn(room);
        com.archive.dto.request.WarehouseRoomUpdateRequest req =
                new com.archive.dto.request.WarehouseRoomUpdateRequest();
        req.setWarningThreshold(new BigDecimal("2"));

        assertThatThrownBy(() -> service.updateRoom(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: 编译失败（`updateRoom` 不存在）

- [ ] **Step 3: 实现 updateRoom**

在 `listRooms` 之后新增：

```java
    // ==================== 16.3 更新库房 ====================

    public WarehouseRoomResponse updateRoom(Long roomId, WarehouseRoomUpdateRequest req) {
        WarehouseRoom room = warehouseRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库房不存在");
        }
        if (req.getRoomName() != null) {
            room.setRoomName(req.getRoomName());
        }
        if (req.getWarningThreshold() != null) {
            if (req.getWarningThreshold().compareTo(BigDecimal.ZERO) <= 0
                    || req.getWarningThreshold().compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "告警阈值必须在 (0, 1] 范围内");
            }
            room.setWarningThreshold(req.getWarningThreshold());
        }
        if (req.getStatus() != null) {
            room.setStatus(req.getStatus());
        }
        warehouseRoomMapper.updateById(room);
        return toRoomResponse(room, countOccupiedSlots(roomId));
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（8 个测试通过）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现更新库房信息"
```

---

## Task 7: 查询架位列表（16.4）

分页查询，支持 occupied 过滤（用 `inSql`/`notInSql` 下推到 SQL，保持分页正确），并回填当前档案盒与盒内件数。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/WarehouseService.java`（新增 `listLocations`）
- Modify: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`

- [ ] **Step 1: 在 WarehouseService import 区补充**

```java
import com.archive.common.PageResult;
import com.archive.dto.response.StorageLocationResponse;
import com.archive.entity.ArchiveBox;
import com.archive.entity.ArchiveBoxItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void listLocations_回填当前盒与盒内件数() {
        com.archive.entity.StorageLocation loc1 = new com.archive.entity.StorageLocation();
        loc1.setId(10L);
        loc1.setRoomId(1L);
        loc1.setLocationCode("401-01-01-01");
        loc1.setStatus("active");
        com.archive.entity.StorageLocation loc2 = new com.archive.entity.StorageLocation();
        loc2.setId(11L);
        loc2.setRoomId(1L);
        loc2.setLocationCode("401-01-01-02");
        loc2.setStatus("active");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.archive.entity.StorageLocation> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(loc1, loc2));
        page.setTotal(2L);
        when(storageLocationMapper.selectPage(any(), any())).thenReturn(page);

        com.archive.entity.ArchiveBox box = new com.archive.entity.ArchiveBox();
        box.setId(5L);
        box.setLocationId(10L);
        box.setBoxNo("BOX-000001");
        box.setStatus("normal");
        when(archiveBoxMapper.selectList(any())).thenReturn(List.of(box));
        when(archiveBoxItemMapper.selectCount(any())).thenReturn(3L);

        com.archive.common.PageResult<com.archive.dto.response.StorageLocationResponse> result =
                service.listLocations(1L, null, null, null, 1, 20);

        assertThat(result.getRecords()).hasSize(2);
        com.archive.dto.response.StorageLocationResponse r1 = result.getRecords().get(0);
        assertThat(r1.getOccupied()).isTrue();
        assertThat(r1.getCurrentBoxNo()).isEqualTo("BOX-000001");
        assertThat(r1.getBoxItemCount()).isEqualTo(3);
        com.archive.dto.response.StorageLocationResponse r2 = result.getRecords().get(1);
        assertThat(r2.getOccupied()).isFalse();
        assertThat(r2.getBoxItemCount()).isEqualTo(0);
    }
```

- [ ] **Step 3: 实现 listLocations**

在 `updateRoom` 之后新增：

```java
    // ==================== 16.4 查询架位列表 ====================

    public PageResult<StorageLocationResponse> listLocations(
            Long roomId, Integer rackNo, String status, Boolean occupied,
            int pageNo, int pageSize) {

        QueryWrapper<StorageLocation> w = new QueryWrapper<>();
        if (roomId != null) {
            w.eq("room_id", roomId);
        }
        if (rackNo != null) {
            w.eq("rack_no", rackNo);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (occupied != null) {
            String sub = "SELECT location_id FROM archive_boxes WHERE status IN ('normal','full')";
            if (occupied) {
                w.inSql("id", sub);
            } else {
                w.notInSql("id", sub);
            }
        }
        w.orderByAsc("location_code");

        Page<StorageLocation> page = storageLocationMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<StorageLocation> locs = page.getRecords();
        if (locs.isEmpty()) {
            return new PageResult<>(java.util.Collections.emptyList(), pageNo, pageSize, page.getTotal());
        }

        // 批量取本页架位上的活动档案盒
        List<Long> locIds = locs.stream().map(StorageLocation::getId).collect(Collectors.toList());
        QueryWrapper<ArchiveBox> bw = new QueryWrapper<>();
        bw.in("location_id", locIds);
        bw.in("status", java.util.List.of("normal", "full"));
        List<ArchiveBox> boxes = archiveBoxMapper.selectList(bw);
        Map<Long, ArchiveBox> boxByLoc = new HashMap<>();
        for (ArchiveBox box : boxes) {
            boxByLoc.put(box.getLocationId(), box);
        }

        List<StorageLocationResponse> records = locs.stream()
                .map(loc -> toLocationResponse(loc, boxByLoc))
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private StorageLocationResponse toLocationResponse(StorageLocation loc, Map<Long, ArchiveBox> boxByLoc) {
        StorageLocationResponse resp = new StorageLocationResponse();
        resp.setId(loc.getId());
        resp.setRoomId(loc.getRoomId());
        resp.setRackNo(loc.getRackNo());
        resp.setLayerNo(loc.getLayerNo());
        resp.setBoxSlotNo(loc.getBoxSlotNo());
        resp.setLocationCode(loc.getLocationCode());
        resp.setStatus(loc.getStatus());

        ArchiveBox box = boxByLoc.get(loc.getId());
        resp.setOccupied(box != null);
        if (box != null) {
            resp.setCurrentBoxId(box.getId());
            resp.setCurrentBoxNo(box.getBoxNo());
            QueryWrapper<ArchiveBoxItem> iw = new QueryWrapper<>();
            iw.eq("box_id", box.getId());
            Long c = archiveBoxItemMapper.selectCount(iw);
            resp.setBoxItemCount(c != null ? c.intValue() : 0);
        } else {
            resp.setBoxItemCount(0);
        }
        return resp;
    }
```

> **实现说明（与 spec 3.3 的偏差）**：spec 描述「两次批量查询回填」盒内件数；本计划对盒内件数采用「每个占用架位一次 `selectCount`」（N≤pageSize 次），而非 `GROUP BY box_id` 批量聚合。理由：管理端分页 pageSize 默认 20、架位占用率低，N 很小；逐盒 `selectCount` 便于 Mockito 单测（mock `selectCount` 即可），批量聚合需 mock `JdbcTemplate.query(RowMapper)` 更脆弱。如后续库房规模增大，可重构为 `countBoxItemsBatch` 聚合查询。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（9 个测试通过）

- [ ] **Step 5: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现架位分页查询与占用回填"
```

---

## Task 8: 停用/启用架位（16.5）

**Files:**
- Modify: `backend/src/main/java/com/archive/service/WarehouseService.java`
- Modify: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`

- [ ] **Step 1: 在 import 区补充**

```java
import com.archive.dto.request.LocationStatusRequest;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void updateLocationStatus_停用已占用架位抛冲突() {
        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setStatus("active");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);
        when(archiveBoxMapper.selectCount(any())).thenReturn(1L); // 已占用

        LocationStatusRequest req = new LocationStatusRequest();
        req.setStatus("disabled");

        assertThatThrownBy(() -> service.updateLocationStatus(10L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void updateLocationStatus_停用空闲架位成功() {
        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setRoomId(1L);
        loc.setLocationCode("401-01-01-01");
        loc.setStatus("active");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);
        when(archiveBoxMapper.selectCount(any())).thenReturn(0L);
        when(archiveBoxMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());

        LocationStatusRequest req = new LocationStatusRequest();
        req.setStatus("disabled");
        req.setReason("架位维修");

        com.archive.dto.response.StorageLocationResponse resp =
                service.updateLocationStatus(10L, req);

        assertThat(resp.getStatus()).isEqualTo("disabled");
        assertThat(resp.getOccupied()).isFalse();
        verify(storageLocationMapper).updateById(any());
        verify(auditService).log(eq("M07"), eq("update_location_status"),
                eq("storage_location"), eq(10L), any());
    }
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: 编译失败（`updateLocationStatus` 不存在）

- [ ] **Step 4: 实现 updateLocationStatus 与占用判定辅助方法**

在 `listLocations` 之后新增：

```java
    // ==================== 16.5 停用/启用架位 ====================

    public StorageLocationResponse updateLocationStatus(Long locationId, LocationStatusRequest req) {
        StorageLocation loc = storageLocationMapper.selectById(locationId);
        if (loc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "架位不存在");
        }
        if ("disabled".equals(req.getStatus()) && isLocationOccupied(locationId)) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "已占用架位不得直接停用");
        }
        loc.setStatus(req.getStatus());
        storageLocationMapper.updateById(loc);
        auditService.log("M07", "update_location_status", "storage_location", locationId,
                Map.of("status", req.getStatus(),
                        "reason", req.getReason() != null ? req.getReason() : ""));

        ArchiveBox box = findActiveBoxAt(locationId);
        Map<Long, ArchiveBox> m = new HashMap<>();
        if (box != null) {
            m.put(locationId, box);
        }
        return toLocationResponse(loc, m);
    }

    /** 架位是否被活动档案盒占用。 */
    private boolean isLocationOccupied(Long locationId) {
        QueryWrapper<ArchiveBox> w = new QueryWrapper<>();
        w.eq("location_id", locationId);
        w.in("status", java.util.List.of("normal", "full"));
        return archiveBoxMapper.selectCount(w) > 0;
    }

    /** 取某架位上当前的活动档案盒（至多一个）。 */
    private ArchiveBox findActiveBoxAt(Long locationId) {
        QueryWrapper<ArchiveBox> w = new QueryWrapper<>();
        w.eq("location_id", locationId);
        w.in("status", java.util.List.of("normal", "full"));
        w.last("LIMIT 1");
        return archiveBoxMapper.selectOne(w);
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（11 个测试通过）

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现架位停用启用与占用校验"
```

---

## Task 9: 新增档案盒（16.8）

**Files:**
- Modify: `backend/src/main/java/com/archive/service/WarehouseService.java`
- Modify: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`

- [ ] **Step 1: 在 import 区补充**

```java
import com.archive.dto.request.ArchiveBoxCreateRequest;
import com.archive.dto.response.ArchiveBoxResponse;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void createBox_架位已占用抛冲突() {
        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setStatus("active");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);
        when(archiveBoxMapper.selectCount(any())).thenReturn(1L);

        ArchiveBoxCreateRequest req = new ArchiveBoxCreateRequest();
        req.setLocationId(10L);

        assertThatThrownBy(() -> service.createBox(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void createBox_停用架位抛冲突() {
        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setStatus("disabled");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);

        ArchiveBoxCreateRequest req = new ArchiveBoxCreateRequest();
        req.setLocationId(10L);

        assertThatThrownBy(() -> service.createBox(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void createBox_空闲启用架位成功并生成盒号() {
        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setRoomId(1L);
        loc.setLocationCode("401-01-01-01");
        loc.setStatus("active");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);
        when(archiveBoxMapper.selectCount(any())).thenReturn(0L);
        when(boxNoUtil.generate()).thenReturn("BOX-000001");
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setRoomNo("401");
        when(warehouseRoomMapper.selectById(1L)).thenReturn(room);

        ArchiveBoxCreateRequest req = new ArchiveBoxCreateRequest();
        req.setLocationId(10L);
        req.setCategoryId(3);
        req.setYearLabel("2025");
        req.setCapacity(30);

        ArchiveBoxResponse resp = service.createBox(req);

        assertThat(resp.getBoxNo()).isEqualTo("BOX-000001");
        assertThat(resp.getLocationCode()).isEqualTo("401-01-01-01");
        assertThat(resp.getRoomNo()).isEqualTo("401");
        assertThat(resp.getUsedCount()).isEqualTo(0);
        assertThat(resp.getStatus()).isEqualTo("normal");
        verify(archiveBoxMapper).insert(any());
        verify(auditService).log(eq("M07"), eq("create_box"), eq("archive_box"), any(), any());
    }
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: 编译失败（`createBox` 不存在）

- [ ] **Step 4: 实现 createBox 与 toBoxResponse**

在 `updateLocationStatus` 之后新增：

```java
    // ==================== 16.8 新增档案盒 ====================

    public ArchiveBoxResponse createBox(ArchiveBoxCreateRequest req) {
        StorageLocation loc = storageLocationMapper.selectById(req.getLocationId());
        if (loc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "架位不存在");
        }
        if (!"active".equals(loc.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标架位已停用");
        }
        if (isLocationOccupied(req.getLocationId())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标架位已被占用");
        }

        ArchiveBox box = new ArchiveBox();
        box.setBoxNo(boxNoUtil.generate());
        box.setLocationId(req.getLocationId());
        box.setCategoryId(req.getCategoryId());
        box.setFondsId(req.getFondsId());
        box.setYearLabel(req.getYearLabel());
        box.setSpineText(req.getSpineText());
        box.setCapacity(req.getCapacity());
        box.setUsedCount(0);
        box.setStatus("normal");
        archiveBoxMapper.insert(box);

        auditService.log("M07", "create_box", "archive_box", box.getId(),
                Map.of("boxNo", box.getBoxNo(), "locationId", req.getLocationId()));

        return toBoxResponse(box, loc);
    }

    private ArchiveBoxResponse toBoxResponse(ArchiveBox box, StorageLocation loc) {
        ArchiveBoxResponse resp = new ArchiveBoxResponse();
        resp.setId(box.getId());
        resp.setBoxNo(box.getBoxNo());
        resp.setLocationId(box.getLocationId());
        resp.setCategoryId(box.getCategoryId());
        resp.setFondsId(box.getFondsId());
        resp.setYearLabel(box.getYearLabel());
        resp.setSpineText(box.getSpineText());
        resp.setCapacity(box.getCapacity());
        resp.setUsedCount(box.getUsedCount());
        resp.setStatus(box.getStatus());
        if (loc != null) {
            resp.setLocationCode(loc.getLocationCode());
            WarehouseRoom room = warehouseRoomMapper.selectById(loc.getRoomId());
            if (room != null) {
                resp.setRoomNo(room.getRoomNo());
            }
        }
        return resp;
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（14 个测试通过）

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现新增档案盒与架位占用绑定"
```

---

## Task 10: 移动档案盒（16.9）

**Files:**
- Modify: `backend/src/main/java/com/archive/service/WarehouseService.java`
- Modify: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`

- [ ] **Step 1: 在 import 区补充**

```java
import com.archive.dto.request.ArchiveBoxMoveRequest;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void moveBox_已销毁盒不可移动() {
        com.archive.entity.ArchiveBox box = new com.archive.entity.ArchiveBox();
        box.setId(5L);
        box.setStatus("destroyed");
        when(archiveBoxMapper.selectById(5L)).thenReturn(box);

        ArchiveBoxMoveRequest req = new ArchiveBoxMoveRequest();
        req.setTargetLocationId(10L);

        assertThatThrownBy(() -> service.moveBox(5L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void moveBox_目标架位被占用抛冲突() {
        com.archive.entity.ArchiveBox box = new com.archive.entity.ArchiveBox();
        box.setId(5L);
        box.setStatus("normal");
        com.archive.entity.StorageLocation target = new com.archive.entity.StorageLocation();
        target.setId(20L);
        target.setStatus("active");
        when(archiveBoxMapper.selectById(5L)).thenReturn(box);
        when(storageLocationMapper.selectById(20L)).thenReturn(target);
        when(archiveBoxMapper.selectCount(any())).thenReturn(1L); // 目标已占用

        ArchiveBoxMoveRequest req = new ArchiveBoxMoveRequest();
        req.setTargetLocationId(20L);

        assertThatThrownBy(() -> service.moveBox(5L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void moveBox_移动到空闲架位成功() {
        com.archive.entity.ArchiveBox box = new com.archive.entity.ArchiveBox();
        box.setId(5L);
        box.setBoxNo("BOX-000001");
        box.setLocationId(10L);
        box.setStatus("normal");
        com.archive.entity.StorageLocation target = new com.archive.entity.StorageLocation();
        target.setId(20L);
        target.setRoomId(1L);
        target.setLocationCode("401-02-01-01");
        target.setStatus("active");
        when(archiveBoxMapper.selectById(5L)).thenReturn(box);
        when(storageLocationMapper.selectById(20L)).thenReturn(target);
        when(archiveBoxMapper.selectCount(any())).thenReturn(0L);
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setRoomNo("401");
        when(warehouseRoomMapper.selectById(1L)).thenReturn(room);

        ArchiveBoxMoveRequest req = new ArchiveBoxMoveRequest();
        req.setTargetLocationId(20L);
        req.setReason("库房整理");

        ArchiveBoxResponse resp = service.moveBox(5L, req);

        assertThat(resp.getLocationId()).isEqualTo(20L);
        assertThat(resp.getLocationCode()).isEqualTo("401-02-01-01");
        verify(archiveBoxMapper).updateById(any());
    }
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: 编译失败（`moveBox` 不存在）

- [ ] **Step 4: 实现 moveBox**

在 `createBox` 之后新增：

```java
    // ==================== 16.9 移动档案盒 ====================

    @Transactional
    public ArchiveBoxResponse moveBox(Long boxId, ArchiveBoxMoveRequest req) {
        ArchiveBox box = archiveBoxMapper.selectById(boxId);
        if (box == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案盒不存在");
        }
        if ("destroyed".equals(box.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案盒已销毁，不可移动");
        }
        StorageLocation target = storageLocationMapper.selectById(req.getTargetLocationId());
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标架位不存在");
        }
        if (!"active".equals(target.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标架位已停用");
        }
        if (isLocationOccupied(req.getTargetLocationId())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标架位已被占用");
        }

        box.setLocationId(req.getTargetLocationId());
        archiveBoxMapper.updateById(box);

        auditService.log("M07", "move_box", "archive_box", boxId,
                Map.of("targetLocationId", req.getTargetLocationId(),
                        "reason", req.getReason() != null ? req.getReason() : ""));

        return toBoxResponse(box, target);
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（17 个测试通过）

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现移动档案盒与目标架位校验"
```

---

## Task 11: 查询档案盒列表（16.6）+ 盒详情（16.7）

`roomId` 过滤需经 `storage_locations` 中转：先查出该库房全部架位 id，再用 `IN` 过滤 `archive_boxes.location_id`。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/WarehouseService.java`
- Modify: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`

- [ ] **Step 1: 在 import 区补充**

```java
import com.archive.dto.response.ArchiveBoxDetailResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveBoxItem;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void listBoxes_回填架位编码与库房号() {
        com.archive.entity.ArchiveBox box = new com.archive.entity.ArchiveBox();
        box.setId(5L);
        box.setBoxNo("BOX-000001");
        box.setLocationId(10L);
        box.setStatus("normal");
        box.setUsedCount(2);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.archive.entity.ArchiveBox> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(box));
        page.setTotal(1L);
        when(archiveBoxMapper.selectPage(any(), any())).thenReturn(page);

        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setRoomId(1L);
        loc.setLocationCode("401-01-01-01");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setRoomNo("401");
        when(warehouseRoomMapper.selectById(1L)).thenReturn(room);

        com.archive.common.PageResult<ArchiveBoxResponse> result =
                service.listBoxes(null, null, null, null, null, 1, 20);

        assertThat(result.getRecords()).hasSize(1);
        ArchiveBoxResponse r = result.getRecords().get(0);
        assertThat(r.getBoxNo()).isEqualTo("BOX-000001");
        assertThat(r.getLocationCode()).isEqualTo("401-01-01-01");
        assertThat(r.getRoomNo()).isEqualTo("401");
    }

    @Test
    void getBoxDetail_返回盒内条目与档案信息() {
        com.archive.entity.ArchiveBox box = new com.archive.entity.ArchiveBox();
        box.setId(5L);
        box.setBoxNo("BOX-000001");
        box.setLocationId(10L);
        box.setStatus("normal");
        box.setUsedCount(1);
        when(archiveBoxMapper.selectById(5L)).thenReturn(box);

        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setRoomId(1L);
        loc.setLocationCode("401-01-01-01");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setRoomNo("401");
        when(warehouseRoomMapper.selectById(1L)).thenReturn(room);

        com.archive.entity.ArchiveBoxItem item = new com.archive.entity.ArchiveBoxItem();
        item.setBoxId(5L);
        item.setArchiveId(100L);
        item.setSortNo(1);
        item.setPageCount(30);
        item.setPhysicalStatus("normal");
        when(archiveBoxItemMapper.selectList(any())).thenReturn(List.of(item));

        com.archive.entity.Archive archive = new com.archive.entity.Archive();
        archive.setId(100L);
        archive.setArchiveNo("ARC-000100");
        archive.setTitle("2025 年度会计凭证");
        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(archive));

        ArchiveBoxDetailResponse resp = service.getBoxDetail(5L);

        assertThat(resp.getBoxNo()).isEqualTo("BOX-000001");
        assertThat(resp.getLocationCode()).isEqualTo("401-01-01-01");
        assertThat(resp.getRoomNo()).isEqualTo("401");
        assertThat(resp.getItems()).hasSize(1);
        ArchiveBoxDetailResponse.BoxItemView v = resp.getItems().get(0);
        assertThat(v.getArchiveNo()).isEqualTo("ARC-000100");
        assertThat(v.getTitle()).isEqualTo("2025 年度会计凭证");
        assertThat(v.getSortNo()).isEqualTo(1);
    }
```

> 注：`archiveMapper.selectBatchIds(any())` 的 mock 使用 `any()` 匹配 Collection 入参，Mockito 默认支持。

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: 编译失败（`listBoxes` / `getBoxDetail` 不存在）

- [ ] **Step 4: 实现 listBoxes 与 getBoxDetail**

在 `moveBox` 之后新增：

```java
    // ==================== 16.6 查询档案盒列表 ====================

    public PageResult<ArchiveBoxResponse> listBoxes(
            String boxNo, Long roomId, Integer categoryId, Long fondsId, String status,
            int pageNo, int pageSize) {

        QueryWrapper<ArchiveBox> w = new QueryWrapper<>();
        if (boxNo != null && !boxNo.isBlank()) {
            w.like("box_no", boxNo);
        }
        if (categoryId != null) {
            w.eq("category_id", categoryId);
        }
        if (fondsId != null) {
            w.eq("fonds_id", fondsId);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (roomId != null) {
            QueryWrapper<StorageLocation> lw = new QueryWrapper<>();
            lw.eq("room_id", roomId);
            lw.select("id");
            List<StorageLocation> roomLocs = storageLocationMapper.selectList(lw);
            List<Long> locIds = roomLocs.stream()
                    .map(StorageLocation::getId).collect(Collectors.toList());
            if (locIds.isEmpty()) {
                return new PageResult<>(java.util.Collections.emptyList(), pageNo, pageSize, 0L);
            }
            w.in("location_id", locIds);
        }
        w.orderByDesc("id");

        Page<ArchiveBox> page = archiveBoxMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<ArchiveBoxResponse> records = page.getRecords().stream()
                .map(box -> {
                    StorageLocation loc = box.getLocationId() != null
                            ? storageLocationMapper.selectById(box.getLocationId()) : null;
                    return toBoxResponse(box, loc);
                })
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    // ==================== 16.7 档案盒详情 ====================

    public ArchiveBoxDetailResponse getBoxDetail(Long boxId) {
        ArchiveBox box = archiveBoxMapper.selectById(boxId);
        if (box == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案盒不存在");
        }
        StorageLocation loc = box.getLocationId() != null
                ? storageLocationMapper.selectById(box.getLocationId()) : null;
        WarehouseRoom room = loc != null ? warehouseRoomMapper.selectById(loc.getRoomId()) : null;

        ArchiveBoxDetailResponse resp = new ArchiveBoxDetailResponse();
        resp.setId(box.getId());
        resp.setBoxNo(box.getBoxNo());
        resp.setLocationId(box.getLocationId());
        resp.setCategoryId(box.getCategoryId());
        resp.setFondsId(box.getFondsId());
        resp.setYearLabel(box.getYearLabel());
        resp.setSpineText(box.getSpineText());
        resp.setCapacity(box.getCapacity());
        resp.setUsedCount(box.getUsedCount());
        resp.setStatus(box.getStatus());
        if (loc != null) {
            resp.setLocationCode(loc.getLocationCode());
        }
        if (room != null) {
            resp.setRoomNo(room.getRoomNo());
        }

        // 盒内条目 + 档案信息
        QueryWrapper<ArchiveBoxItem> iw = new QueryWrapper<>();
        iw.eq("box_id", boxId);
        iw.orderByAsc("sort_no");
        List<ArchiveBoxItem> items = archiveBoxItemMapper.selectList(iw);

        List<Long> archiveIds = items.stream()
                .map(ArchiveBoxItem::getArchiveId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Archive> archiveMap = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            List<Archive> archives = archiveMapper.selectBatchIds(archiveIds);
            for (Archive a : archives) {
                archiveMap.put(a.getId(), a);
            }
        }

        List<ArchiveBoxDetailResponse.BoxItemView> views = items.stream()
                .map(it -> {
                    ArchiveBoxDetailResponse.BoxItemView v = new ArchiveBoxDetailResponse.BoxItemView();
                    v.setArchiveId(it.getArchiveId());
                    v.setSortNo(it.getSortNo());
                    v.setPageCount(it.getPageCount());
                    v.setPhysicalStatus(it.getPhysicalStatus());
                    Archive a = archiveMap.get(it.getArchiveId());
                    if (a != null) {
                        v.setArchiveNo(a.getArchiveNo());
                        v.setTitle(a.getTitle());
                    }
                    return v;
                })
                .collect(Collectors.toList());
        resp.setItems(views);
        return resp;
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（19 个测试通过）

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现档案盒列表与详情查询"
```

---

## Task 12: 概览告警查询（listWarningRooms）

供接口 6.1 管理概览 `warehouseWarnings` 字段调用。

**Files:**
- Modify: `backend/src/main/java/com/archive/service/WarehouseService.java`
- Modify: `backend/src/test/java/com/archive/service/WarehouseServiceTest.java`

- [ ] **Step 1: 在 import 区补充**

```java
import com.archive.dto.response.WarehouseWarningResponse;
```

- [ ] **Step 2: 新增测试**

```java
    @Test
    void listWarningRooms_仅返回超阈值库房() {
        com.archive.entity.WarehouseRoom over = new com.archive.entity.WarehouseRoom();
        over.setId(1L);
        over.setRoomNo("401");
        over.setRoomName("A");
        over.setCapacity(100);
        over.setWarningThreshold(new BigDecimal("0.85"));
        over.setStatus("active");
        com.archive.entity.WarehouseRoom under = new com.archive.entity.WarehouseRoom();
        under.setId(2L);
        under.setRoomNo("402");
        under.setRoomName("B");
        under.setCapacity(100);
        under.setWarningThreshold(new BigDecimal("0.85"));
        under.setStatus("active");
        when(warehouseRoomMapper.selectList(any())).thenReturn(List.of(over, under));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(90L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(2L))).thenReturn(50L);

        List<com.archive.dto.response.WarehouseWarningResponse> result =
                service.listWarningRooms();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomId()).isEqualTo(1L);
        assertThat(result.get(0).getOccupiedSlots()).isEqualTo(90);
        assertThat(result.get(0).getOccupancyRate()).isEqualByComparTo(new BigDecimal("0.9000"));
    }
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: 编译失败（`listWarningRooms` 不存在）

- [ ] **Step 4: 实现 listWarningRooms**

在 `getBoxDetail` 之后新增：

```java
    // ==================== 概览：库房告警查询 ====================

    public List<WarehouseWarningResponse> listWarningRooms() {
        QueryWrapper<WarehouseRoom> w = new QueryWrapper<>();
        w.eq("status", "active");
        List<WarehouseRoom> rooms = warehouseRoomMapper.selectList(w);

        List<WarehouseWarningResponse> result = new ArrayList<>();
        for (WarehouseRoom room : rooms) {
            int occupied = countOccupiedSlots(room.getId());
            int capacity = room.getCapacity() != null ? room.getCapacity() : 0;
            BigDecimal rate = capacity > 0
                    ? BigDecimal.valueOf(occupied)
                            .divide(BigDecimal.valueOf(capacity), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal threshold = room.getWarningThreshold() != null
                    ? room.getWarningThreshold() : DEFAULT_WARNING_THRESHOLD;
            if (rate.compareTo(threshold) >= 0) {
                WarehouseWarningResponse r = new WarehouseWarningResponse();
                r.setRoomId(room.getId());
                r.setRoomNo(room.getRoomNo());
                r.setRoomName(room.getRoomName());
                r.setOccupiedSlots(occupied);
                r.setCapacity(capacity);
                r.setOccupancyRate(rate);
                r.setWarningThreshold(threshold);
                result.add(r);
            }
        }
        return result;
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `cd backend && mvn -q test -Dtest=WarehouseServiceTest`
Expected: PASS（20 个测试通过）

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/archive/service/WarehouseService.java backend/src/test/java/com/archive/service/WarehouseServiceTest.java
git commit -m "feat(storage): 实现库房占用告警查询供概览调用"
```

---

## Task 13: WarehouseController（9 个端点）

Controller 仅做参数转发，无业务逻辑，以编译 + 服务层测试为验证。鉴权依赖 `SaTokenConfig` 对 `/api/**` 的登录校验；项目当前未在拦截器层做角色级（`back_archivist`）校验，与 `PendingArchiveController` 一致，本期不新增角色注解。

**Files:**
- Create: `backend/src/main/java/com/archive/controller/WarehouseController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.ArchiveBoxCreateRequest;
import com.archive.dto.request.ArchiveBoxMoveRequest;
import com.archive.dto.request.LocationStatusRequest;
import com.archive.dto.request.WarehouseRoomCreateRequest;
import com.archive.dto.request.WarehouseRoomUpdateRequest;
import com.archive.dto.response.ArchiveBoxDetailResponse;
import com.archive.dto.response.ArchiveBoxResponse;
import com.archive.dto.response.StorageLocationResponse;
import com.archive.dto.response.WarehouseRoomResponse;
import com.archive.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库房管理接口。
 */
@RestController
@RequestMapping("/api/admin/warehouse")
@RequiredArgsConstructor
@Tag(name = "库房管理", description = "库房、架位、档案盒管理")
public class WarehouseController {

    private final WarehouseService warehouseService;

    // ==================== 16.1 查询库房列表 ====================

    @GetMapping("/rooms")
    @Operation(summary = "查询库房列表")
    public R<List<WarehouseRoomResponse>> listRooms(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(warehouseService.listRooms(status, keyword));
    }

    // ==================== 16.2 新增库房 ====================

    @PostMapping("/rooms")
    @Operation(summary = "新增库房")
    public R<WarehouseRoomResponse> createRoom(@RequestBody @Valid WarehouseRoomCreateRequest req) {
        return R.ok(warehouseService.createRoom(req));
    }

    // ==================== 16.3 更新库房 ====================

    @PutMapping("/rooms/{roomId}")
    @Operation(summary = "更新库房")
    public R<WarehouseRoomResponse> updateRoom(
            @PathVariable Long roomId,
            @RequestBody WarehouseRoomUpdateRequest req) {
        return R.ok(warehouseService.updateRoom(roomId, req));
    }

    // ==================== 16.4 查询架位 ====================

    @GetMapping("/locations")
    @Operation(summary = "查询架位列表")
    public R<PageResult<StorageLocationResponse>> listLocations(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer rackNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean occupied,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(warehouseService.listLocations(roomId, rackNo, status, occupied, pageNo, pageSize));
    }

    // ==================== 16.5 停用/启用架位 ====================

    @PutMapping("/locations/{locationId}/status")
    @Operation(summary = "停用或启用架位")
    public R<StorageLocationResponse> updateLocationStatus(
            @PathVariable Long locationId,
            @RequestBody @Valid LocationStatusRequest req) {
        return R.ok(warehouseService.updateLocationStatus(locationId, req));
    }

    // ==================== 16.6 查询档案盒 ====================

    @GetMapping("/boxes")
    @Operation(summary = "查询档案盒列表")
    public R<PageResult<ArchiveBoxResponse>> listBoxes(
            @RequestParam(required = false) String boxNo,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Long fondsId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(warehouseService.listBoxes(boxNo, roomId, categoryId, fondsId, status, pageNo, pageSize));
    }

    // ==================== 16.7 档案盒详情 ====================

    @GetMapping("/boxes/{boxId}")
    @Operation(summary = "获取档案盒详情")
    public R<ArchiveBoxDetailResponse> getBoxDetail(@PathVariable Long boxId) {
        return R.ok(warehouseService.getBoxDetail(boxId));
    }

    // ==================== 16.8 新增档案盒 ====================

    @PostMapping("/boxes")
    @Operation(summary = "新增档案盒")
    public R<ArchiveBoxResponse> createBox(@RequestBody @Valid ArchiveBoxCreateRequest req) {
        return R.ok(warehouseService.createBox(req));
    }

    // ==================== 16.9 移动档案盒 ====================

    @PostMapping("/boxes/{boxId}/move")
    @Operation(summary = "移动档案盒")
    public R<ArchiveBoxResponse> moveBox(
            @PathVariable Long boxId,
            @RequestBody @Valid ArchiveBoxMoveRequest req) {
        return R.ok(warehouseService.moveBox(boxId, req));
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/archive/controller/WarehouseController.java
git commit -m "feat(storage): 新增库房管理 Controller 9 个端点"
```

---

## Task 14: 全量构建、测试与推送

- [ ] **Step 1: 全量测试**

Run: `cd backend && mvn -q test`
Expected: 全部测试通过（含 BoxNoUtilTest 2 个 + WarehouseServiceTest 20 个 + 既有测试无回归）

- [ ] **Step 2: 全量打包验证**

Run: `cd backend && mvn -q package -DskipTests`
Expected: BUILD SUCCESS（生成可执行 jar，确认无打包问题）

- [ ] **Step 3: 推送分支**

```bash
git push -u origin feat/storage-zhou
```

- [ ] **Step 4: 创建 PR（标题遵循项目规范）**

```bash
gh pr create --base develop --head feat/storage-zhou \
  --title "feat(storage): 库房管理模块（库房/架位/档案盒/占用告警）" \
  --body "M07 库房管理：9 个端点 + 概览告警查询。详见 backend/docs/superpowers/specs/2026-06-14-storage-zhou-design.md"
```

> 角色鉴权：端点经 SaTokenConfig 登录校验；角色级 back_archivist 校验项目尚未在拦截器层实现，与现有 PendingArchiveController 一致，本期不新增。
