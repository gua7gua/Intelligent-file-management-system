# feat/storage-zhou 设计规格

- 分支：`feat/storage-zhou`（从最新 `develop` 拉出）
- 对应模块：M07 库房管理
- 接口规范：[doc/接口文档.md](../../../doc/接口文档.md) 第 16 章
- 数据库：[doc/数据库设计.md](../../../doc/数据库设计.md) 第 7 章（库房、架位与纸质档案）
- 业务场景：[doc/业务场景.md](../../../doc/业务场景.md) 场景六

## 1 实现范围

### 1.1 本次实现（9 个接口 + 1 个概览辅助方法）

| # | 方法 | 路径 | 角色 | 说明 |
|---|------|------|------|------|
| 16.1 | GET | `/api/admin/warehouse/rooms` | back_archivist | 查询库房列表，含占用盒位数、占用率、告警状态 |
| 16.2 | POST | `/api/admin/warehouse/rooms` | back_archivist | 新增库房，按结构批量生成架位 |
| 16.3 | PUT | `/api/admin/warehouse/rooms/{roomId}` | back_archivist | 更新库房（仅名称/阈值/状态） |
| 16.4 | GET | `/api/admin/warehouse/locations` | back_archivist | 分页查询架位，含当前档案盒与盒内件数 |
| 16.5 | PUT | `/api/admin/warehouse/locations/{locationId}/status` | back_archivist | 停用/启用架位，已占用架位不得停用 |
| 16.6 | GET | `/api/admin/warehouse/boxes` | back_archivist | 分页查询档案盒 |
| 16.7 | GET | `/api/admin/warehouse/boxes/{boxId}` | back_archivist | 档案盒详情，含架位与盒内条目 |
| 16.8 | POST | `/api/admin/warehouse/boxes` | back_archivist | 新增档案盒，绑定空闲启用架位 |
| 16.9 | POST | `/api/admin/warehouse/boxes/{boxId}/move` | back_archivist | 移动档案盒到目标空闲架位 |
| 辅助 | - | `WarehouseService.listWarningRooms()` | - | 供接口 6.1 管理概览 `warehouseWarnings` 字段调用 |

### 1.2 本次不实现

- **上架生命周期转换**（`pending_shelf → normal`）：完全归 M05 `PendingArchiveService.shelveBatch`（接口 9.8），M07 不重复实现。
- **M05 装盒预占逻辑的重构**：M05 `confirmArchive` 现有直接操作 `archive_boxes`/`archive_box_items` 的代码保持不变，M07 独立实现库房/盒管理，不抽取共享方法。理由：M05 入库流程已验证通过，避免跨模块重构引入回归风险。
- **软删除**：库房/架位/档案盒统一用 `status=disabled` 表达停用；`deleted_at` 字段本期不写入（项目未全局开启 MyBatis-Plus 逻辑删除）。
- **库房内部复杂迁移、密集架控制、路径规划**：文档已排除。
- **实物盘点**（M12）、**借阅**（M09）相关接口：属其他模块。

### 1.3 与 M05 的衔接契约（已存在，M07 仅提供创建盒+绑定架位能力）

```
M07「新增档案盒」(16.8): 创建 box 并写入 box.location_id → 架位被占用
M05「确认入库」(9.7):   传入 boxId，向已存在的 box 追加 archive_box_items、累加 used_count
M05「批次上架」(9.8):   pending_shelf → normal（与 M07 无关）
```

M05 入库时假设 box 已绑定 location_id；M07 的 16.8 正是设置该绑定的唯一入口，契约一致。

## 2 文件结构

### 2.1 复用已有 Entity（不新增）

`WarehouseRoom`、`StorageLocation`、`ArchiveBox`、`ArchiveBoxItem` 均已存在，字段与数据库设计第 7 章一致，直接复用。

### 2.2 复用已有 Mapper（不新增）

`WarehouseRoomMapper`、`StorageLocationMapper`、`ArchiveBoxMapper`、`ArchiveBoxItemMapper` 已存在（继承 `BaseMapper`），直接复用。本期不引入 XML Mapper，聚合查询用 `JdbcTemplate`（与 `ArchiveNoUtil` 同款用法）。

### 2.3 新增 Util

| 文件 | 说明 |
|------|------|
| `util/BoxNoUtil.java` | 仿 `ArchiveNoUtil`，用 `seq_archive_box_no` 序列生成 `BOX-{6位序号}` |

### 2.4 新增 Service

| 文件 | 说明 |
|------|------|
| `service/WarehouseService.java` | 全部库房管理业务逻辑 |

### 2.5 新增 Controller

| 文件 | 说明 |
|------|------|
| `controller/WarehouseController.java` | 9 个端点，`@RequestMapping("/api/admin/warehouse")` |

### 2.6 新增 DTO

请求（`dto/request/`）：
- `WarehouseRoomCreateRequest`：roomNo、roomName、rackCount、layersPerRack、boxesPerLayer、warningThreshold
- `WarehouseRoomUpdateRequest`：roomName、warningThreshold、status
- `LocationStatusRequest`：status、reason
- `ArchiveBoxCreateRequest`：locationId、categoryId、fondsId、yearLabel、spineText、capacity
- `ArchiveBoxMoveRequest`：targetLocationId、reason

响应（`dto/response/`）：
- `WarehouseRoomResponse`：库房基础信息 + occupiedSlots + occupancyRate + warning（boolean）
- `StorageLocationResponse`：架位基础信息 + currentBoxId + currentBoxNo + boxItemCount + occupied（boolean）
- `ArchiveBoxResponse`：盒基础信息 + locationId + locationCode + roomNo
- `ArchiveBoxDetailResponse`：盒信息 + 架位 + 盒内条目列表（archiveNo、title、sortNo、pageCount、physicalStatus）
- `WarehouseWarningResponse`：roomId、roomNo、roomName、occupiedSlots、capacity、occupancyRate、warningThreshold（供概览）

## 3 核心业务逻辑

### 3.1 新增库房（16.2）— 批量生成架位

1. 校验 `roomNo` 唯一（查 `warehouse_rooms`），重复 → `BUSINESS_CONFLICT`。
2. 校验 `rackCount`、`layersPerRack`、`boxesPerLayer` 均 > 0，否则 → `VALIDATION_FAILED`。
3. `warningThreshold` 为空时取默认 `0.85`；校验 `0 < threshold <= 1`。
4. 计算 `capacity = rackCount * layersPerRack * boxesPerLayer`。
5. `@Transactional`：insert `warehouse_rooms`；三重循环（rack 1..rackCount、layer 1..layersPerRack、slot 1..boxesPerLayer）构造 `storage_locations` 列表，`location_code = {roomNo}-{rackNo:02d}-{layerNo:02d}-{boxSlotNo:02d}`，批量 insert。
6. 写审计 `auditService.log("M07", "create_room", "warehouse_room", roomId, ...)`。

location_code 编码示例：库房号 `401`、机架 3、层 2、盒位 5 → `401-03-02-05`（rack/layer/slot 两位补零）。

### 3.2 占用率计算与告警（16.1、listWarningRooms）

- **占用盒位数**（某库房）：`COUNT(archive_boxes WHERE location_id IN (SELECT id FROM storage_locations WHERE room_id = ?) AND status IN ('normal','full'))`，用 `JdbcTemplate` 聚合查询。
- **占用率** = 占用盒位数 / `room.capacity`（capacity 为 0 时记为 0，避免除零；建表约束已保证 capacity > 0）。
- **告警** = 占用率 >= `room.warning_threshold`。
- 16.1 查询库房列表后，对每条库房计算上述三项拼装 `WarehouseRoomResponse`。
- `listWarningRooms()`：查询全部 `active` 库房，过滤出告警为 true 的，返回 `WarehouseWarningResponse` 列表。

### 3.3 查询架位（16.4）

- 查询参数：roomId、rackNo、status、occupied、pageNo、pageSize。
- `QueryWrapper` 拼 `storage_locations` 条件（roomId、rackNo、status）。
- `occupied` 过滤：true 仅返回已占用架位，false 仅返回空闲架位。占用判定见 3.4。
- 对分页结果中每条架位，批量查 `archive_boxes`（location_id IN 当前页架位 id 且 status IN normal,full）得到当前盒，再批量查盒内件数（`archive_box_items` group by box_id）。组装 `StorageLocationResponse`。
- 不产生 N+1：先取本页架位 id 集合，再两次批量查询回填。

### 3.4 空闲/占用判定（单一事实来源）

架位 X 被占用 ⟺ 存在 `archive_boxes.location_id = X AND status IN ('normal','full')`。

数据库已有 partial unique index `uk_archive_boxes_location_active` 保证一个活动架位最多绑一个活动盒，并发安全由该约束兜底。

### 3.5 停用/启用架位（16.5）

- 启用（status=active）：直接更新，写审计。
- 停用（status=disabled）：先按 3.4 判定该架位是否被占用；已占用 → `BUSINESS_CONFLICT`（"已占用架位不得直接停用"）；空闲则更新并写审计。

### 3.6 新增档案盒（16.8）

1. 校验 `locationId` 对应架位存在且 `status=active`，否则 → `BUSINESS_CONFLICT`/`NOT_FOUND`。
2. 按 3.4 校验目标架位空闲（无活动盒），已占用 → `BUSINESS_CONFLICT`。
3. `BoxNoUtil.generate()` 生成 `box_no`。
4. insert `archive_boxes`：locationId、categoryId、fondsId、yearLabel、spineText、capacity、usedCount=0、status=normal。
5. 写审计 `auditService.log("M07", "create_box", "archive_box", boxId, ...)`。

> `categoryId`、`fondsId` 在创建盒时确定，用于「同盒分类/全宗一致」约束。该约束的实际校验在 M05 `confirmArchive` 入库时执行（已实现），M07 创建盒只记录盒的主分类/全宗，不校验盒内档案（此时盒为空）。

### 3.7 移动档案盒（16.9）

1. 校验 `boxId` 存在且 `status != 'destroyed'`，否则 → `NOT_FOUND`/`BUSINESS_CONFLICT`。
2. 校验 `targetLocationId` 架位存在且 active、空闲（按 3.4），否则 → `BUSINESS_CONFLICT`。
3. `@Transactional`：更新 `archive_boxes.location_id = targetLocationId`。
4. 写审计（含原架位、目标架位、reason）。

### 3.8 盒号生成（BoxNoUtil）

```java
@Component
@RequiredArgsConstructor
public class BoxNoUtil {
    private final JdbcTemplate jdbcTemplate;
    public String generate() {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('seq_archive_box_no')", Long.class);
        return String.format("BOX-%06d", seq);
    }
}
```

序列 `seq_archive_box_no` 已由 Flyway `V14__create-sequences.sql` 创建。

## 4 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 上架确认归属 | M07 只做库房侧管理，生命周期转换归 M05 | 接口 9.8 已由 M05 实现；职责清晰，避免重复 |
| 盒预占代码归属 | M07 独立实现，不重构 M05 | M05 已验证通过；surgical 改动，避免跨模块回归 |
| 删除策略 | 不软删，用 status=disabled | 项目未开启逻辑删除；停用语义与状态字段一致 |
| 聚合查询方式 | JdbcTemplate | 与 ArchiveNoUtil 同款用法；不引入 XML Mapper，保持脚手架简单 |
| 占用率事实来源 | archive_boxes.location_id 实时计算 | 数据库设计第 7 章明确不在 storage_locations 冗余占用数 |
| 概览告警 | 提供 listWarningRooms() | 接口 6.1 需要 warehouseWarnings，占用率逻辑已在 M07 |

## 5 依赖关系

### 5.1 依赖已有（develop 已具备）

- 实体与 Mapper：`WarehouseRoom`/`StorageLocation`/`ArchiveBox`/`ArchiveBoxItem` 及对应 Mapper（base-zhou / archive-zhou 已建）。
- 序列：`seq_archive_box_no`（V14）。
- 通用：`R`、`PageResult`、`PageRequest`、`ErrorCode`、`BusinessException`、`AuthContext`、`AuditService`、`BaseEntity`。
- 鉴权：SaToken，路由鉴权由 `SaTokenConfig` 统一配置（与 `PendingArchiveController` 一致）。

### 5.2 不依赖未完成模块

- 不依赖 M12 盘点、M09 借阅、M06 全宗管理（盒的 fondsId/categoryId 仅作存储，创建时不强制校验存在性）。

## 6 错误处理

| 场景 | 异常 | ErrorCode |
|------|------|-----------|
| 库房/架位/档案盒不存在 | BusinessException | NOT_FOUND |
| roomNo 重复 | BusinessException | BUSINESS_CONFLICT |
| 容量参数 <= 0、warningThreshold 越界 | BusinessException | VALIDATION_FAILED |
| 停用已占用架位 | BusinessException | BUSINESS_CONFLICT |
| 新增盒到已占用/停用架位 | BusinessException | BUSINESS_CONFLICT |
| 移盒到非空/停用架位 | BusinessException | BUSINESS_CONFLICT |
| 移动已销毁的盒 | BusinessException | BUSINESS_CONFLICT |

所有写操作在同事务内完成；批量生成架位失败整体回滚。

## 7 测试方案（严格 TDD）

测试风格仿 `SearchServiceSearchTest`：JUnit5 + Mockito + AssertJ，mock 全部 Mapper + JdbcTemplate，纯单测。

`WarehouseServiceTest` 覆盖：
- 新建库房生成正确数量与 location_code 格式的架位；roomNo 重复抛 `BUSINESS_CONFLICT`；容量参数 <= 0 抛 `VALIDATION_FAILED`。
- 占用率计算与告警判定（mock JdbcTemplate 返回占用数）。
- 停用已占用架位抛 `BUSINESS_CONFLICT`，空闲架位成功更新。
- 新增盒到已占用/停用架位抛错；box_no 正确生成（mock BoxNoUtil 或 JdbcTemplate）。
- 移盒到非空架位抛 `BUSINESS_CONFLICT`，到空架位成功更新 location_id。
- `listWarningRooms()` 只返回占用率 >= 阈值的库房。

`BoxNoUtilTest` 覆盖：
- mock JdbcTemplate 返回序号，断言生成 `BOX-000001` 等格式。

每个端点的核心分支先写测试（红）→ 实现（绿）→ 必要重构。
