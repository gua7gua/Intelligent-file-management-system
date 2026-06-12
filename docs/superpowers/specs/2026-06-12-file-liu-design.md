# feat/file-liu 设计规格

> 刘星 06-11 至 06-13 任务：电子文件暂存、哈希、ClamAV、MinIO 匹配
> 分支：`feat/file-liu`，从 `origin/develop` 拉出

---

## 1. 范围

本模块实现前台接收阶段的电子文件暂存功能，覆盖接口文档 7.3（上传暂存文件）、7.4（手工匹配）以及回退时的暂存文件清理。

数据库表 `staging_files` 已由周扬在 V5 迁移中建好，本模块只负责 Java 后端实现。

### 1.1 交付接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 上传暂存电子文件 | POST | `/api/admin/reception/batches/{batchId}/staging-files` | multipart 上传，格式校验、SHA-256、ClamAV、自动匹配 |
| 手工匹配暂存文件 | PUT | `/api/admin/reception/staging-files/{fileId}/match` | 前台人工关联未匹配文件到条目 |
| 回退删除暂存文件 | — | 由 `IntakeBatchService.acceptItem(rejected)` 内部调用 | 物理删除 MinIO 对象，标记 deleted |

### 1.2 不在本期范围

- 暂存转正式（`archive_files` 复制）—— 属于 06-13 至 06-15 入库阶段
- `business_attachments` 上传 —— 独立模块，复用 `MinioService` 和 `ClamAvScanner`
- 四性检查（`file_check_records`）—— 后续阶段

---

## 2. 架构

### 2.1 组件依赖

```
StagingFileController  →  StagingFileService  →  MinioService
                                                      ClamAvScanner
                           ↑
    IntakeBatchService ────┘  (回退时注入调用)
```

### 2.2 新增类清单

| 类 | 包 | 职责 |
|---|---|---|
| `StagingFileController` | `controller` | 上传和手工匹配的 HTTP 入口 |
| `StagingFileService` | `service` | 暂存文件业务：上传、匹配、回退删除 |
| `MinioService` | `service` | MinIO 操作封装：putObject、deleteObject、copyObject、ensureBucket |
| `ClamAvScanner` | `service` | ClamAV clamd TCP INSTREAM 扫描 |
| `FileProperties` | `config` | `file:` 前缀配置属性绑定 |
| `StagingFile` | `entity` | 对应 `staging_files` 表 |
| `StagingFileMapper` | `mapper` | MyBatis-Plus Mapper |
| `MatchStatus` | `enums` | unmatched / matched / duplicate / failed_check / archived / deleted |
| `ScanResult` | `enums` | pending / safe / infected / failed |
| `StagingFileUploadResponse` | `dto.response` | 上传接口返回 |
| `StagingFileResponse` | `dto.response` | 单个暂存文件详情 |
| `MatchSummary` | `dto.response` | 匹配摘要 |
| `StagingFileMatchRequest` | `dto.request` | 手工匹配请求体 |

### 2.3 修改的已有类

| 类 | 修改内容 |
|---|---|
| `IntakeBatchService` | `acceptItem()` 回退分支注入 `StagingFileService`，调用清理逻辑 |
| `application.yml` | 新增 `file:` 和 `minio:` 配置节 |

---

## 3. 核心数据流

### 3.1 上传暂存电子文件

1. **Controller 层**：校验角色 `front_archivist`，校验批次存在且状态为 `pending_transfer` / `pending_receive`
2. **格式校验**：扩展名在白名单内、MIME 与扩展名一致、大小 ≤ 配置上限（默认 500MB）
3. **SHA-256 计算**：流式计算哈希
4. **重复检查**：同批次同 SHA-256 已存在 → 创建记录 `match_status=duplicate`，跳过 MinIO 上传
5. **ClamAV 扫描**：
   - `file.scan.enabled=true`：TCP INSTREAM 扫描，infected/failed → 拒绝上传，写 audit_logs
   - `file.scan.enabled=false`：跳过，标记 safe
6. **先插 DB**：insert `staging_files`（拿 ID 用于构造 MinIO 路径）
7. **MinIO 上传**：`archive-files/_staging/{sourceType}/{batchId}/{stagingFileId}/{fileName}`
8. **MinIO 失败补偿**：事务回滚，DB 记录不残留
9. **自动文件名匹配**：按 `original_filename` 在同批次 `intake_items.expected_filename` 中查找
   - 唯一匹配 → `item_id` 关联，`match_status=matched`，更新 `intake_item.file_match_status=matched`
   - 无匹配或多匹配 → `match_status=unmatched`
10. **返回** `StagingFileUploadResponse { uploadBatchNo, files[], matchSummary }`

### 3.2 手工匹配

1. 查 `staging_file`，校验存在且 `match_status=unmatched`、`scan_result=safe`
2. 查 `intake_item`，校验同批次、未回退
3. 更新 `staging_file.item_id`、`match_status=matched`
4. 更新 `intake_item.file_match_status=matched`
5. 返回 `StagingFileResponse` + 条目匹配汇总

### 3.3 回退删除暂存文件

由 `IntakeBatchService.acceptItem()` rejected 分支调用：

1. 查该条目关联的所有 `staging_files`（`match_status IN ('matched','unmatched')`）
2. 调 `MinioService.deleteObject()` 物理删除每个 MinIO 对象
3. 更新 `staging_files.match_status = deleted`
4. 写入 `audit_logs`

---

## 4. 配置属性

```yaml
file:
  bucket: archive-files
  max-size: 524288000            # 500MB
  allowed-extensions: pdf,doc,docx,xls,xlsx,jpg,jpeg,png,tiff,tif,mp4,avi,ofd
  scan:
    enabled: false               # 开发期关闭，生产改为 true
    host: ${CLAMAV_HOST:localhost}
    port: ${CLAMAV_PORT:3310}
    timeout: 60                  # 秒
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9001}
  access-key: ${MINIO_ROOT_USER:minioadmin}
  secret-key: ${MINIO_ROOT_PASSWORD:minioadmin123}
```

---

## 5. 错误处理

| 场景 | 处理方式 | 错误码 |
|------|----------|--------|
| 格式不在白名单 | 拒绝上传，返回不支持的扩展名 | `UNSUPPORTED_MEDIA_TYPE` (415) |
| 文件超限 | 拒绝上传 | `PAYLOAD_TOO_LARGE` (413) |
| SHA-256 重复 | 创建记录 `duplicate`，不上传 MinIO | 正常返回 |
| ClamAV infected | 拒绝上传，不创建 staging_files，写 audit_logs | `EXTERNAL_SERVICE_ERROR` (502) |
| ClamAV 不可用/超时 (enabled=true) | 拒绝上传，写 audit_logs | `EXTERNAL_SERVICE_ERROR` (502) |
| MinIO 上传失败 | 事务回滚 | `EXTERNAL_SERVICE_ERROR` (502) |
| 批次状态不允许上传 | 拒绝 | `BUSINESS_CONFLICT` (409) |
| 匹配目标条目已回退 | 拒绝 | `BUSINESS_CONFLICT` (409) |
| 非 safe 文件尝试匹配 | 拒绝 | `BUSINESS_CONFLICT` (409) |

---

## 6. MinioService 接口

```java
@Service
public class MinioService {
    void ensureBucket(String bucket);
    String putObject(String bucket, String objectKey, InputStream data,
                     long size, String contentType);
    void deleteObject(String bucket, String objectKey);
    void copyObject(String srcBucket, String srcKey,
                    String dstBucket, String dstKey);
}
```

`copyObject` 本期预留，暂存转正式阶段使用。

---

## 7. ClamAvScanner 接口

```java
@Service
public class ClamAvScanner {
    /**
     * 扫描输入流。
     * file.scan.enabled=false 时直接返回 safe。
     * 通过 Java Socket 连接 clamd TCP 端口，发送 zINSTREAM 命令，
     * 按 4 字节大端长度前缀分块传输，读取响应判断结果。
     */
    ScanResult scan(InputStream data);
}
```

---

## 8. Maven 依赖新增

| groupId | artifactId | 用途 |
|---------|-----------|------|
| `io.minio` | `minio` | MinIO Java SDK |
| `commons-io` | `commons-io` | 文件工具方法 |

---

## 9. 关键设计决策

1. **先插 DB 再上传 MinIO**：因为暂存路径需要 `stagingFileId`，必须先拿到 ID。MinIO 失败时事务回滚保证一致性。
2. **ClamAV 配置开关**：`file.scan.enabled` 控制扫描，开发期关闭直接标记 safe，生产环境强制开启。
3. **重复文件不占 MinIO 空间**：SHA-256 重复时创建 `duplicate` 记录但不上传 MinIO，前端展示重复提示。
4. **MinioService / ClamAvScanner 独立封装**：后续 `archive_files` 和 `business_attachments` 模块可直接复用。
5. **回退清理由 StagingFileService 提供，IntakeBatchService 调用**：保持周扬代码的 Service 边界不变，只注入新增依赖。
