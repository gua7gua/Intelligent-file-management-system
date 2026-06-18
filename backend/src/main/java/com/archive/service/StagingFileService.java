package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.config.FileProperties;
import com.archive.dto.request.StagingFileMatchRequest;
import com.archive.dto.response.MatchSummary;
import com.archive.dto.response.StagingFileResponse;
import com.archive.dto.response.StagingFileUploadResponse;
import com.archive.entity.IntakeBatch;
import com.archive.entity.IntakeItem;
import com.archive.entity.StagingFile;
import com.archive.enums.*;
import com.archive.exception.BusinessException;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.IntakeItemMapper;
import com.archive.mapper.StagingFileMapper;
import com.archive.util.FileTypeUtil;
import com.archive.util.HashUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 暂存电子文件核心业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StagingFileService {

    private final StagingFileMapper stagingFileMapper;
    private final IntakeBatchMapper batchMapper;
    private final IntakeItemMapper itemMapper;
    private final MinioService minioService;
    private final ClamAvScanner clamAvScanner;
    private final FileProperties fileProperties;
    private final AuditService auditService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // ==================== 上传 ====================

    /**
     * 上传暂存电子文件。
     *
     * @param batchId       清单批次 ID
     * @param files         上传的文件数组
     * @param uploadBatchNo 前端传入的上传批次号，为空则后端生成
     * @return 上传结果摘要
     */
    @Transactional
    public StagingFileUploadResponse uploadFiles(Long batchId, MultipartFile[] files,
                                                  String uploadBatchNo) {
        // 校验批次存在且状态允许上传
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单不存在");
        }
        Set<String> allowedStatuses = Set.of(
                BatchStatus.pending_transfer.name(),
                BatchStatus.pending_receive.name());
        if (!allowedStatuses.contains(batch.getStatus().name())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "当前批次状态不允许上传文件");
        }

        // 生成 uploadBatchNo
        if (uploadBatchNo == null || uploadBatchNo.isBlank()) {
            uploadBatchNo = "UP-" + System.currentTimeMillis();
        }

        String sourceType = batch.getSourceType().name();
        List<StagingFileResponse> fileResponses = new ArrayList<>();
        int matched = 0, unmatched = 0, duplicate = 0, failed = 0;

        for (MultipartFile file : files) {
            try {
                StagingFileResponse resp = processSingleFile(batch, file, uploadBatchNo, sourceType);
                fileResponses.add(resp);
                switch (resp.getMatchStatus()) {
                    case "matched" -> matched++;
                    case "duplicate" -> duplicate++;
                    case "failed_check" -> failed++;
                    default -> unmatched++;
                }
            } catch (BusinessException e) {
                failed++;
                StagingFileResponse errResp = new StagingFileResponse();
                errResp.setOriginalFilename(file.getOriginalFilename());
                errResp.setScanResult("failed");
                errResp.setMatchStatus("failed_check");
                errResp.setScanMessage(e.getMessage());
                fileResponses.add(errResp);
            }
        }

        List<Long> missingItems = findMissingItems(batchId);
        MatchSummary summary = new MatchSummary(matched, unmatched, duplicate, failed, missingItems);
        return new StagingFileUploadResponse(uploadBatchNo, fileResponses, summary);
    }

    /**
     * 处理单个上传文件：校验 → 哈希 → 扫描 → 存储 → 匹配。
     */
    private StagingFileResponse processSingleFile(IntakeBatch batch, MultipartFile file,
                                                   String uploadBatchNo, String sourceType) {
        String originalFilename = file.getOriginalFilename();
        String ext = FileTypeUtil.getExtension(originalFilename);

        // 格式校验
        if (!FileTypeUtil.isAllowedExtension(originalFilename, fileProperties.getAllowedExtensions())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    "不支持的文件格式: " + ext);
        }

        // 大小校验
        if (file.getSize() > fileProperties.getMaxSize()) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE,
                    "文件超过大小限制: " + originalFilename);
        }

        // 读取文件字节
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "读取文件失败: " + originalFilename);
        }

        String contentType = file.getContentType();

        // SHA-256 哈希
        String sha256 = HashUtil.sha256(fileBytes);

        // 同批次重复检查
        Long dupCount = stagingFileMapper.selectCount(
                new QueryWrapper<StagingFile>()
                        .eq("batch_id", batch.getId())
                        .eq("sha256", sha256)
                        .ne("match_status", MatchStatus.deleted.name()));
        if (dupCount > 0) {
            StagingFileResponse resp = new StagingFileResponse();
            resp.setOriginalFilename(originalFilename);
            resp.setSha256(sha256);
            resp.setFileSize(file.getSize());
            resp.setMatchStatus("duplicate");
            resp.setScanResult("safe");
            return resp;
        }

        // ClamAV 扫描
        ScanResult scanResult = clamAvScanner.scan(new ByteArrayInputStream(fileBytes));
        if (scanResult == ScanResult.infected) {
            log.warn("文件被 ClamAV 检测为感染病毒: {}", originalFilename);
            auditService.log("M03", "scan_reject", "staging_file", null,
                    "filename", originalFilename);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "文件检测到病毒，上传被拒绝: " + originalFilename);
        }
        if (scanResult == ScanResult.failed && fileProperties.getScan().isEnabled()) {
            auditService.log("M03", "scan_failed", "staging_file", null,
                    "filename", originalFilename);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "病毒扫描服务异常，上传被拒绝: " + originalFilename);
        }

        // 先从序列获取下一个 ID，用于构造 MinIO 暂存路径
        Long nextId = jdbcTemplate.queryForObject(
                "SELECT nextval('staging_files_id_seq')", Long.class);
        String objectKey = String.format("archive-files/_staging/%s/%d/%d/%s",
                sourceType, batch.getId(), nextId, originalFilename);

        // 一次性插入完整记录（object_key 为 NOT NULL，不能先插再更新）
        StagingFile sf = new StagingFile();
        sf.setId(nextId);
        sf.setBatchId(batch.getId());
        sf.setUploadBatchNo(uploadBatchNo);
        sf.setOriginalFilename(originalFilename);
        sf.setFileExt(ext);
        sf.setMimeType(contentType);
        sf.setFileSize(file.getSize());
        sf.setSha256(sha256);
        sf.setBucketName(fileProperties.getBucket());
        sf.setObjectKey(objectKey);
        sf.setMatchStatus(MatchStatus.unmatched);
        sf.setScanResult(scanResult == ScanResult.failed ? ScanResult.pending : scanResult);
        sf.setUploadedBy(AuthContext.getCurrentUserId());
        sf.setUploadedAt(OffsetDateTime.now());
        stagingFileMapper.insert(sf);

        // 上传到 MinIO
        minioService.ensureBucket(fileProperties.getBucket());
        minioService.putObject(fileProperties.getBucket(), objectKey,
                new ByteArrayInputStream(fileBytes), file.getSize(), contentType);

        // 记录审计日志：文件上传成功
        auditService.log("M03", "upload", "staging_file", sf.getId(),
                "filename", originalFilename);

        // 自动文件名匹配
        Long matchedItemId = tryAutoMatch(batch.getId(), originalFilename);

        StagingFileResponse resp = new StagingFileResponse();
        resp.setFileId(sf.getId());
        resp.setOriginalFilename(originalFilename);
        resp.setFileSize(file.getSize());
        resp.setSha256(sha256);
        resp.setScanResult(sf.getScanResult().name());
        // tryAutoMatch 内部会更新 staging_files.match_status；响应字段须与 matchedItemId 一致，
        // 避免出现 matchStatus=unmatched 但 matchedItemId 非空的矛盾
        resp.setMatchStatus(matchedItemId != null ? MatchStatus.matched.name() : sf.getMatchStatus().name());
        resp.setMatchedItemId(matchedItemId);
        return resp;
    }

    // ==================== 手工匹配 ====================

    /**
     * 手工匹配暂存文件到清单条目。
     */
    @Transactional
    public StagingFileResponse matchFile(Long fileId, StagingFileMatchRequest req) {
        StagingFile sf = stagingFileMapper.selectById(fileId);
        if (sf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂存文件不存在");
        }
        if (sf.getMatchStatus() != MatchStatus.unmatched) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "只有未匹配的文件才能手工匹配");
        }
        if (sf.getScanResult() != ScanResult.safe) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "只有扫描通过的文件才能匹配");
        }

        IntakeItem item = itemMapper.selectById(req.getItemId());
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单条目不存在");
        }
        if (!item.getBatchId().equals(sf.getBatchId())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "文件与条目不属于同一批次");
        }
        if (item.getStatus() == ItemStatus.rejected) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "已回退的条目不能匹配文件");
        }

        sf.setItemId(item.getId());
        sf.setMatchStatus(MatchStatus.matched);
        stagingFileMapper.updateById(sf);

        item.setFileMatchStatus(FileMatchStatus.matched);
        itemMapper.updateById(item);

        return toResponse(sf);
    }

    // ==================== 回退删除 ====================

    /**
     * 回退时删除条目关联的暂存文件（由 IntakeBatchService 调用）。
     * 物理删除 MinIO 暂存对象，将 staging_files.match_status 置为 deleted。
     *
     * @param itemId 被回退的清单条目 ID
     */
    @Transactional
    public void deleteStagingFilesForItem(Long itemId) {
        List<StagingFile> files = stagingFileMapper.selectList(
                new QueryWrapper<StagingFile>()
                        .eq("item_id", itemId)
                        .in("match_status", List.of(MatchStatus.matched, MatchStatus.unmatched)));

        for (StagingFile sf : files) {
            try {
                minioService.deleteObject(sf.getBucketName(), sf.getObjectKey());
            } catch (Exception e) {
                log.error("删除 MinIO 暂存对象失败: bucket={}, key={}",
                        sf.getBucketName(), sf.getObjectKey(), e);
            }
            sf.setMatchStatus(MatchStatus.deleted);
            stagingFileMapper.updateById(sf);
            auditService.log("M03", "delete", "staging_file", sf.getId(),
                    "reason", "item_rejected");
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 自动按文件名匹配清单条目。
     * 在同批次 intake_items 中查找 expected_filename 与上传文件名完全一致的条目，
     * 唯一匹配时自动关联。
     *
     * @return 匹配到的条目 ID，未匹配或多匹配返回 null
     */
    private Long tryAutoMatch(Long batchId, String filename) {
        List<IntakeItem> candidates = itemMapper.selectList(
                new QueryWrapper<IntakeItem>()
                        .eq("batch_id", batchId)
                        .eq("expected_filename", filename)
                        .ne("status", ItemStatus.rejected.name()));

        if (candidates.size() == 1) {
            IntakeItem item = candidates.get(0);

            // 找到当前批次中刚插入的、文件名一致、状态为 unmatched 的暂存文件
            StagingFile sf = stagingFileMapper.selectOne(
                    new QueryWrapper<StagingFile>()
                            .eq("batch_id", batchId)
                            .eq("original_filename", filename)
                            .eq("match_status", MatchStatus.unmatched.name())
                            .orderByDesc("id")
                            .last("LIMIT 1"));
            if (sf != null) {
                sf.setItemId(item.getId());
                sf.setMatchStatus(MatchStatus.matched);
                stagingFileMapper.updateById(sf);
            }

            item.setFileMatchStatus(FileMatchStatus.matched);
            itemMapper.updateById(item);
            return item.getId();
        }
        return null;
    }

    /**
     * 查找仍缺少电子文件的条目（需要电子文件但 fileMatchStatus=none 的条目）。
     */
    private List<Long> findMissingItems(Long batchId) {
        List<IntakeItem> items = itemMapper.selectList(
                new QueryWrapper<IntakeItem>()
                        .eq("batch_id", batchId)
                        .ne("status", ItemStatus.rejected.name())
                        .in("carrier_status", List.of(
                                CarrierStatus.electronic.name(),
                                CarrierStatus.paper_electronic.name()))
                        .eq("file_match_status", FileMatchStatus.none.name()));
        return items.stream().map(IntakeItem::getId).collect(Collectors.toList());
    }

    /**
     * 列出指定批次下未删除的暂存电子文件（按上传时间倒序）。
     * 供验收详情接口回填 stagingFiles 字段，让前台核对 U 盘文件扫描与匹配结果。
     */
    public List<StagingFileResponse> listByBatch(Long batchId) {
        List<StagingFile> files = stagingFileMapper.selectList(
                new QueryWrapper<StagingFile>()
                        .eq("batch_id", batchId)
                        .ne("match_status", com.archive.enums.MatchStatus.deleted.name())
                        .orderByDesc("id"));
        return files.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private StagingFileResponse toResponse(StagingFile sf) {
        StagingFileResponse resp = new StagingFileResponse();
        resp.setFileId(sf.getId());
        resp.setOriginalFilename(sf.getOriginalFilename());
        resp.setFileSize(sf.getFileSize());
        resp.setSha256(sf.getSha256());
        resp.setScanResult(sf.getScanResult().name());
        resp.setScanMessage(sf.getScanMessage());
        resp.setMatchStatus(sf.getMatchStatus().name());
        resp.setMatchedItemId(sf.getItemId());
        return resp;
    }
}
