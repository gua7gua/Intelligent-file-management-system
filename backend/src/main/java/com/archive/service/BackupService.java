package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.BackupTaskCreateRequest;
import com.archive.dto.request.BackupTaskQuery;
import com.archive.dto.response.BackupTaskResponse;
import com.archive.entity.BackupTask;
import com.archive.enums.BackupScope;
import com.archive.enums.BackupStatus;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.BackupTaskMapper;
import com.archive.util.BackupTaskNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 备份服务（M14 保存）。
 * 可移植折中：database 用 JDBC 导出核心业务表为 SQL，files 复制 MinIO 对象到备份 bucket，产物计算 sha256。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupTaskMapper backupTaskMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MinioService minioService;
    private final BackupTaskNoUtil noUtil;
    private final AuditService auditService;

    @Value("${archive.backup.dir:./data/backups}")
    private String backupDir;

    @Value("${file.bucket:archive-files}")
    private String fileBucket;

    /** 备份白名单：覆盖演示与 runtime 所需业务数据，排除 users/roles 等敏感表。 */
    private static final List<String> TABLES = List.of(
            "organizations", "fonds", "categories", "tags",
            "archives", "archive_files", "archive_tags", "archive_boxes", "archive_box_items",
            "storage_locations", "warehouse_rooms",
            "intake_batches", "intake_items", "staging_files", "business_attachments",
            "borrow_requests", "appraisal_batches", "appraisal_items",
            "destruction_lists", "destruction_items", "approval_requests", "archive_change_logs",
            "inventory_tasks", "inventory_items", "compilations", "compilation_materials",
            "analysis_tasks", "analysis_items", "backup_tasks", "file_check_records",
            "system_configs");

    private void requireRole() {
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.sys_admin))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }

    /** 21.1 查询备份任务 */
    public PageResult<BackupTaskResponse> list(BackupTaskQuery q) {
        requireRole();
        QueryWrapper<BackupTask> w = new QueryWrapper<>();
        w.isNull("deleted_at");
        if (q.getStatus() != null) w.eq("status", q.getStatus());
        if (q.getBackupScope() != null) w.eq("backup_scope", q.getBackupScope());
        w.orderByDesc("created_at");
        Page<BackupTask> p = backupTaskMapper.selectPage(new Page<>(q.getPageNo(), q.getPageSize()), w);
        return new PageResult<>(p.getRecords().stream().map(this::toResponse).toList(),
                q.getPageNo(), q.getPageSize(), p.getTotal());
    }

    /** 21.2 创建并立即执行备份 */
    public BackupTaskResponse create(BackupTaskCreateRequest req) {
        requireRole();
        Long running = backupTaskMapper.selectCount(new QueryWrapper<BackupTask>()
                .eq("status", BackupStatus.running.name()).isNull("deleted_at"));
        if (running != null && running > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "已有运行中的备份任务");
        }

        BackupTask t = new BackupTask();
        t.setTaskNo(noUtil.generate());
        t.setBackupScope(req.getBackupScope());
        t.setStatus(BackupStatus.running);
        t.setStartedAt(OffsetDateTime.now());
        backupTaskMapper.insert(t);

        int copiedFiles = 0;
        String primaryArtifact = null;
        try {
            Files.createDirectories(Paths.get(backupDir));
            if (req.getBackupScope() == BackupScope.database || req.getBackupScope() == BackupScope.both) {
                primaryArtifact = exportDatabase(t.getTaskNo());
            }
            if (req.getBackupScope() == BackupScope.files || req.getBackupScope() == BackupScope.both) {
                copiedFiles = copyFiles(t.getTaskNo());
                Path manifest = Paths.get(backupDir, t.getTaskNo() + "-files.manifest");
                Files.writeString(manifest, "copied=" + copiedFiles);
                if (primaryArtifact == null) primaryArtifact = manifest.toString();
            }
            if (primaryArtifact != null) {
                t.setBackupPath(primaryArtifact);
                t.setFileSize(Files.size(Paths.get(primaryArtifact)));
                t.setSha256(sha256(primaryArtifact));
            }
            t.setStatus(BackupStatus.success);
            t.setMessage((req.getBackupScope() == BackupScope.files ? "" : "数据库导出完成; ")
                    + (copiedFiles > 0 ? "文件复制 " + copiedFiles + " 个" : ""));
        } catch (Exception e) {
            log.error("备份任务 {} 失败", t.getTaskNo(), e);
            t.setStatus(BackupStatus.failed);
            t.setMessage("备份失败: " + e.getMessage());
        }
        t.setFinishedAt(OffsetDateTime.now());
        backupTaskMapper.updateById(t);
        auditService.log("M14", "create_backup", "backup_task", t.getId(),
                Map.of("scope", req.getBackupScope().name(), "result", t.getStatus().name()));
        return toResponse(t);
    }

    /** 用 JDBC 把白名单表导出为 INSERT 语句，写入 {dir}/{taskNo}-db.sql。不依赖 pg_dump。 */
    private String exportDatabase(String taskNo) throws Exception {
        Path file = Paths.get(backupDir, taskNo + "-db.sql");
        try (OutputStream os = Files.newOutputStream(file)) {
            os.write(("-- archive-db backup %s%n".formatted(taskNo)).getBytes());
            for (String table : TABLES) {
                os.write(("%n-- TABLE %s%n".formatted(table)).getBytes());
                List<Map<String, Object>> rows;
                try {
                    rows = jdbcTemplate.queryForList("SELECT * FROM " + table);
                } catch (Exception e) {
                    os.write(("-- skip %s: %s%n".formatted(table, e.getMessage())).getBytes());
                    continue;
                }
                for (Map<String, Object> row : rows) {
                    StringBuilder sb = new StringBuilder("INSERT INTO ").append(table).append(" (");
                    StringBuilder vh = new StringBuilder(") VALUES (");
                    boolean first = true;
                    for (Map.Entry<String, Object> e : row.entrySet()) {
                        if (!first) { sb.append(","); vh.append(","); }
                        sb.append(e.getKey());
                        vh.append(sqlLiteral(e.getValue()));
                        first = false;
                    }
                    sb.append(vh).append(");\n");
                    os.write(sb.toString().getBytes());
                }
            }
        }
        return file.toString();
    }

    /** 枚举业务 bucket 对象并复制到备份 bucket（archive-backups）的 {taskNo}/ 前缀下。 */
    private int copyFiles(String taskNo) {
        String backupBucket = "archive-backups";
        minioService.ensureBucket(backupBucket);
        List<String> keys = minioService.listObjects(fileBucket, "");
        int n = 0;
        for (String key : keys) {
            try {
                minioService.copyObject(fileBucket, key, backupBucket, taskNo + "/" + key);
                n++;
            } catch (Exception e) {
                log.warn("复制对象失败 {}: {}", key, e.getMessage());
            }
        }
        return n;
    }

    private String sqlLiteral(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return "'" + v.toString().replace("'", "''") + "'";
    }

    private String sha256(String path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(Files.readAllBytes(Paths.get(path)));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private BackupTaskResponse toResponse(BackupTask t) {
        BackupTaskResponse r = new BackupTaskResponse();
        r.setId(t.getId());
        r.setTaskNo(t.getTaskNo());
        r.setBackupScope(t.getBackupScope() != null ? t.getBackupScope().name() : null);
        r.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        r.setBackupPath(t.getBackupPath());
        r.setFileSize(t.getFileSize());
        r.setSha256(t.getSha256());
        r.setStartedAt(t.getStartedAt());
        r.setFinishedAt(t.getFinishedAt());
        r.setMessage(t.getMessage());
        return r;
    }
}
