package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.FileCheckRecordQuery;
import com.archive.dto.request.FileCheckTriggerRequest;
import com.archive.dto.response.FileCheckRecordResponse;
import com.archive.entity.ArchiveFile;
import com.archive.entity.FileCheckRecord;
import com.archive.enums.FileCheckResult;
import com.archive.enums.FileCheckTargetType;
import com.archive.enums.FileCheckType;
import com.archive.enums.RoleCode;
import com.archive.enums.ScanResult;
import com.archive.exception.BusinessException;
import com.archive.mapper.FileCheckRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 四性检测服务（M14 保存）。
 * 完整性=哈希比对，可用性=对象可读，真实性=未接入签名体系(not_configured)，安全性=ClamAV 不降级放行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileCheckService {

    private final ArchiveFileService archiveFileService;
    private final MinioService minioService;
    private final ClamAvScanner clamAvScanner;
    private final FileCheckRecordMapper recordMapper;
    private final AuditService auditService;

    private void requireRole() {
        if (!AuthContext.hasRole(RoleCode.back_archivist)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }

    /** 21.3 查询四性检测记录 */
    public PageResult<FileCheckRecordResponse> list(FileCheckRecordQuery q) {
        requireRole();
        QueryWrapper<FileCheckRecord> w = new QueryWrapper<>();
        w.isNull("deleted_at");
        if (q.getTargetType() != null) w.eq("target_type", q.getTargetType());
        if (q.getTargetId() != null) w.eq("target_id", q.getTargetId());
        if (q.getCheckType() != null) w.eq("check_type", q.getCheckType());
        if (q.getCheckResult() != null) w.eq("check_result", q.getCheckResult());
        w.orderByDesc("checked_at");
        Page<FileCheckRecord> p = recordMapper.selectPage(new Page<>(q.getPageNo(), q.getPageSize()), w);
        return new PageResult<>(p.getRecords().stream().map(this::toResponse).toList(),
                q.getPageNo(), q.getPageSize(), p.getTotal());
    }

    /** 21.4 触发正式文件检测 */
    public List<FileCheckRecordResponse> trigger(Long fileId, FileCheckTriggerRequest req) {
        requireRole();
        ArchiveFile f = archiveFileService.findById(fileId);
        List<FileCheckRecord> out = new ArrayList<>();
        for (FileCheckType ct : req.getCheckTypes()) {
            out.add(switch (ct) {
                case integrity -> checkIntegrity(f);
                case usability -> checkUsability(f);
                case authenticity -> checkAuthenticity(f);
                case security -> checkSecurity(f);
            });
        }
        auditService.log("M14", "trigger_file_check", "archive_file", fileId,
                Map.of("types", req.getCheckTypes().stream().map(Enum::name).toList()));
        return out.stream().map(this::toResponse).toList();
    }

    private FileCheckRecord checkIntegrity(ArchiveFile f) {
        FileCheckRecord r = base(f, FileCheckType.integrity);
        r.setExpectedHash(f.getSha256());
        try (InputStream in = minioService.getObjectStream(f.getBucketName(), f.getObjectKey())) {
            String actual = sha256(in);
            r.setActualHash(actual);
            boolean ok = f.getSha256() != null && f.getSha256().equalsIgnoreCase(actual);
            r.setCheckResult(ok ? FileCheckResult.passed : FileCheckResult.failed);
            r.setMessage(ok ? "哈希一致" : "哈希不一致");
        } catch (Exception e) {
            r.setCheckResult(FileCheckResult.failed);
            r.setMessage("读取对象失败: " + e.getMessage());
        }
        recordMapper.insert(r);
        return r;
    }

    private FileCheckRecord checkUsability(ArchiveFile f) {
        FileCheckRecord r = base(f, FileCheckType.usability);
        try (InputStream in = minioService.getObjectStream(f.getBucketName(), f.getObjectKey())) {
            boolean readable = in.read() >= 0;
            r.setCheckResult(readable ? FileCheckResult.passed : FileCheckResult.failed);
            r.setMessage(readable ? "对象可读" : "对象不可读");
        } catch (Exception e) {
            r.setCheckResult(FileCheckResult.failed);
            r.setMessage("对象读取失败: " + e.getMessage());
        }
        recordMapper.insert(r);
        return r;
    }

    private FileCheckRecord checkAuthenticity(ArchiveFile f) {
        FileCheckRecord r = base(f, FileCheckType.authenticity);
        r.setCheckResult(FileCheckResult.not_configured);
        r.setSignatureResult("not_configured");
        r.setMessage("未接入外部数字签名体系");
        recordMapper.insert(r);
        return r;
    }

    /** 安全性：ClamAV 扫描，不降级放行（异常→failed）。 */
    private FileCheckRecord checkSecurity(ArchiveFile f) {
        FileCheckRecord r = base(f, FileCheckType.security);
        try (InputStream in = minioService.getObjectStream(f.getBucketName(), f.getObjectKey())) {
            ScanResult s = clamAvScanner.scan(in);
            boolean safe = s == ScanResult.safe;
            r.setCheckResult(safe ? FileCheckResult.passed : FileCheckResult.failed);
            r.setMessage(safe ? "病毒扫描通过" : "病毒扫描未通过: " + s);
        } catch (Exception e) {
            r.setCheckResult(FileCheckResult.failed);
            r.setMessage("病毒扫描异常(不放行): " + e.getMessage());
        }
        recordMapper.insert(r);
        return r;
    }

    private FileCheckRecord base(ArchiveFile f, FileCheckType ct) {
        FileCheckRecord r = new FileCheckRecord();
        r.setTargetType(FileCheckTargetType.archive_file);
        r.setTargetId(f.getId());
        r.setCheckType(ct);
        r.setCheckedAt(OffsetDateTime.now());
        return r;
    }

    private String sha256(InputStream in) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private FileCheckRecordResponse toResponse(FileCheckRecord r) {
        FileCheckRecordResponse resp = new FileCheckRecordResponse();
        resp.setId(r.getId());
        resp.setTargetType(r.getTargetType() != null ? r.getTargetType().name() : null);
        resp.setTargetId(r.getTargetId());
        resp.setCheckType(r.getCheckType() != null ? r.getCheckType().name() : null);
        resp.setCheckResult(r.getCheckResult() != null ? r.getCheckResult().name() : null);
        resp.setExpectedHash(r.getExpectedHash());
        resp.setActualHash(r.getActualHash());
        resp.setSignatureResult(r.getSignatureResult());
        resp.setMessage(r.getMessage());
        resp.setCheckedAt(r.getCheckedAt());
        return resp;
    }
}
