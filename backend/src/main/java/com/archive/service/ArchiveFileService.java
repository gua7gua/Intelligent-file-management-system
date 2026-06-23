package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.config.FileProperties;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveFile;
import com.archive.entity.StagingFile;
import com.archive.enums.FileRole;
import com.archive.enums.FileStatus;
import com.archive.enums.MatchStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.StagingFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 正式档案文件服务。
 * 处理暂存转正式复制、文件预览/下载/作废。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveFileService {

    private final ArchiveFileMapper archiveFileMapper;
    private final StagingFileMapper stagingFileMapper;
    private final MinioService minioService;
    private final AuditService auditService;
    private final FileProperties fileProperties;

    /**
     * 将暂存文件复制为正式文件（入库事务内调用）。
     * <p>
     * 事务内直接复制，MinIO 失败时整个入库事务回滚。
     *
     * @param archive      正式档案（已有 archiveNo）
     * @param stagingFiles 已匹配的暂存文件列表
     */
    public void stagingToFormal(Archive archive, List<StagingFile> stagingFiles) {
        String bucket = fileProperties.getBucket();

        for (StagingFile sf : stagingFiles) {
            if (sf.getMatchStatus() != MatchStatus.matched) {
                continue;
            }

            // 创建正式文件记录
            ArchiveFile af = new ArchiveFile();
            af.setArchiveId(archive.getId());
            af.setFileRole(FileRole.original);
            af.setBucketName(bucket);
            // 正式路径：archive-files/{archiveNo}/{archiveFileId}/{fileName}
            // archiveFileId 在 insert 后由数据库生成，所以先 insert 再拼路径再更新
            af.setObjectKey(""); // 临时占位
            af.setOriginalFilename(sf.getOriginalFilename());
            af.setFileExt(sf.getFileExt());
            af.setMimeType(sf.getMimeType());
            af.setFileSize(sf.getFileSize());
            af.setSha256(sf.getSha256());
            af.setScanResult(sf.getScanResult());
            af.setFileStatus(FileStatus.normal);
            archiveFileMapper.insert(af);

            // 拼正式路径并更新
            String formalKey = String.format("archive-files/%s/%d/%s",
                    archive.getArchiveNo(), af.getId(), sf.getOriginalFilename());
            af.setObjectKey(formalKey);
            archiveFileMapper.updateById(af);

            // 复制 MinIO 对象
            minioService.copyObject(sf.getBucketName(), sf.getObjectKey(), bucket, formalKey);

            // 更新暂存文件状态
            sf.setArchivedFileId(af.getId());
            sf.setMatchStatus(MatchStatus.archived);
            stagingFileMapper.updateById(sf);
        }
    }

    /**
     * 根据 ID 查询文件，不存在抛 404。
     */
    public ArchiveFile findById(Long fileId) {
        ArchiveFile file = archiveFileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案文件不存在");
        }
        return file;
    }

    /**
     * 取得可预览的档案文件（校验状态 + 写访问日志），供 controller 代理流返回。
     */
    public ArchiveFile getFileForPreview(Long fileId) {
        ArchiveFile file = findById(fileId);
        if (file.getFileStatus() == FileStatus.deleted) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件已作废，无法预览");
        }
        // 写入访问日志
        auditService.log("M05", "preview", "archive_file", fileId,
                Map.of("archiveId", file.getArchiveId(), "fileName", file.getOriginalFilename()));
        return file;
    }

    /**
     * 取得可下载的档案文件（校验状态 + 写访问日志），供 controller 代理流返回。
     */
    public ArchiveFile getFileForDownload(Long fileId) {
        ArchiveFile file = findById(fileId);
        if (file.getFileStatus() == FileStatus.deleted) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件已作废，无法下载");
        }
        auditService.log("M05", "download", "archive_file", fileId,
                Map.of("archiveId", file.getArchiveId(), "fileName", file.getOriginalFilename()));
        return file;
    }

    /**
     * 作废档案文件（逻辑删除）。
     */
    public void deleteFile(Long fileId, String reason) {
        ArchiveFile file = findById(fileId);
        if (file.getFileStatus() == FileStatus.deleted) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件已作废");
        }
        file.setFileStatus(FileStatus.deleted);
        file.setDeletedAt(OffsetDateTime.now());
        file.setDeletedBy(AuthContext.getCurrentUserId());
        archiveFileMapper.updateById(file);

        auditService.log("M05", "delete_file", "archive_file", fileId,
                reason != null
                        ? Map.of("archiveId", file.getArchiveId(), "reason", reason)
                        : Map.of("archiveId", file.getArchiveId()));
    }
}
