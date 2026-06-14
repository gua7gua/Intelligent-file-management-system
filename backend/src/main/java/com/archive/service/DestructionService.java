package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.config.FileProperties;
import com.archive.dto.request.DestructionDestroyRequest;
import com.archive.dto.request.DestructionSubmitRequest;
import com.archive.dto.response.DestructionListDetailResponse;
import com.archive.dto.response.DestructionListResponse;
import com.archive.entity.ApprovalRequest;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveBox;
import com.archive.entity.ArchiveBoxItem;
import com.archive.entity.ArchiveFile;
import com.archive.entity.BusinessAttachment;
import com.archive.entity.DestructionItem;
import com.archive.entity.DestructionList;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.archive.enums.ConditionStatus;
import com.archive.enums.DestroyMethod;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.FileStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.ScanResult;
import com.archive.exception.BusinessException;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveBoxMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BusinessAttachmentMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.util.FileTypeUtil;
import com.archive.util.HashUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 档案销毁服务。
 * 负责销毁清册查询/详情/提交审批/上传现场照片/确认销毁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestructionService {

    private final DestructionListMapper listMapper;
    private final DestructionItemMapper itemMapper;
    private final ArchiveMapper archiveMapper;
    private final ApprovalRequestMapper approvalMapper;
    private final BusinessAttachmentMapper attachmentMapper;
    private final ArchiveFileMapper archiveFileMapper;
    private final ArchiveBoxItemMapper archiveBoxItemMapper;
    private final ArchiveBoxMapper archiveBoxMapper;
    private final MinioService minioService;
    private final ClamAvScanner clamAvScanner;
    private final FileProperties fileProperties;
    private final AuditService auditService;

    // ==================== 15.1 查询销毁清册 ====================

    public PageResult<DestructionListResponse> listLists(
            String status, String keyword, int pageNo, int pageSize) {
        QueryWrapper<DestructionList> w = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like("list_no", keyword).or().like("list_name", keyword));
        }
        w.orderByDesc("id");

        Page<DestructionList> page = listMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<DestructionListResponse> records = page.getRecords().stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private DestructionListResponse toListResponse(DestructionList l) {
        DestructionListResponse r = new DestructionListResponse();
        r.setId(l.getId());
        r.setListNo(l.getListNo());
        r.setListName(l.getListName());
        r.setAppraisalBatchId(l.getAppraisalBatchId());
        r.setStatus(l.getStatus() != null ? l.getStatus().name() : null);
        r.setDestroyedAt(l.getDestroyedAt());
        QueryWrapper<DestructionItem> iw = new QueryWrapper<>();
        iw.eq("destruction_list_id", l.getId());
        Long c = itemMapper.selectCount(iw);
        r.setItemCount(c != null ? c.intValue() : 0);
        return r;
    }

    // ==================== 15.2 获取销毁清册详情 ====================

    public DestructionListDetailResponse getListDetail(Long listId) {
        DestructionList list = listMapper.selectById(listId);
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }

        QueryWrapper<DestructionItem> iw = new QueryWrapper<>();
        iw.eq("destruction_list_id", listId).orderByAsc("id");
        List<DestructionItem> items = itemMapper.selectList(iw);

        String approvalStatus = null;
        if (list.getApprovalRequestId() != null) {
            ApprovalRequest ap = approvalMapper.selectById(list.getApprovalRequestId());
            if (ap != null) {
                approvalStatus = ap.getStatus() != null ? ap.getStatus().name() : null;
            }
        }

        QueryWrapper<BusinessAttachment> pw = new QueryWrapper<>();
        pw.eq("business_type", "destruction_list")
                .eq("business_id", listId)
                .eq("attachment_type", "destruction_photo");
        List<BusinessAttachment> photos = attachmentMapper.selectList(pw);

        return toDetail(list, items, approvalStatus, photos);
    }

    private DestructionListDetailResponse toDetail(DestructionList list, List<DestructionItem> items,
                                                   String approvalStatus, List<BusinessAttachment> photos) {
        DestructionListDetailResponse resp = new DestructionListDetailResponse();
        resp.setId(list.getId());
        resp.setListNo(list.getListNo());
        resp.setListName(list.getListName());
        resp.setAppraisalBatchId(list.getAppraisalBatchId());
        resp.setStatus(list.getStatus() != null ? list.getStatus().name() : null);
        resp.setApprovalRequestId(list.getApprovalRequestId());
        resp.setApprovalStatus(approvalStatus);
        resp.setDestroyMethod(list.getDestroyMethod() != null ? list.getDestroyMethod().name() : null);
        resp.setSupervisorName1(list.getSupervisorName1());
        resp.setSupervisorName2(list.getSupervisorName2());
        resp.setDestroyNote(list.getDestroyNote());
        resp.setDestroyedAt(list.getDestroyedAt());

        resp.setItems(items.stream().map(it -> {
            DestructionListDetailResponse.ItemView v = new DestructionListDetailResponse.ItemView();
            v.setArchiveId(it.getArchiveId());
            v.setArchiveNoSnapshot(it.getArchiveNoSnapshot());
            v.setTitleSnapshot(it.getTitleSnapshot());
            v.setCategorySnapshot(it.getCategorySnapshot());
            v.setPageCountSnapshot(it.getPageCountSnapshot());
            v.setRetentionSnapshot(it.getRetentionSnapshot());
            v.setSecurityLevelSnapshot(it.getSecurityLevelSnapshot());
            v.setAppraisalOpinionSnapshot(it.getAppraisalOpinionSnapshot());
            v.setFileDeleteStatus(it.getFileDeleteStatus());
            return v;
        }).collect(Collectors.toList()));

        resp.setPhotos(photos.stream().map(p -> {
            DestructionListDetailResponse.PhotoView pv = new DestructionListDetailResponse.PhotoView();
            pv.setId(p.getId());
            pv.setOriginalFilename(p.getOriginalFilename());
            pv.setMimeType(p.getMimeType());
            pv.setFileSize(p.getFileSize());
            pv.setSha256(p.getSha256());
            return pv;
        }).collect(Collectors.toList()));
        return resp;
    }

    // ==================== 15.3 提交销毁审批 ====================

    @Transactional
    public DestructionListDetailResponse submitApproval(Long listId, DestructionSubmitRequest req) {
        DestructionList list = listMapper.selectById(listId);
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }
        if (list.getStatus() != DestructionListStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "非草稿清册不可提交审批");
        }

        QueryWrapper<DestructionItem> iw = new QueryWrapper<>();
        iw.eq("destruction_list_id", listId);
        List<DestructionItem> items = itemMapper.selectList(iw);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "销毁清册无明细");
        }
        List<Long> archiveIds = items.stream().map(DestructionItem::getArchiveId).collect(Collectors.toList());
        for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
            if (a.getLifecycleStatus() != LifecycleStatus.pending_destruction) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "销毁明细存在非待销毁档案");
            }
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();

        ApprovalRequest ap = new ApprovalRequest();
        ap.setApprovalType(ApprovalType.destruction);
        ap.setTargetType("destruction_list");
        ap.setTargetId(listId);
        ap.setReason(req.getReason());
        ap.setStatus(ApprovalStatus.pending);
        ap.setSubmittedBy(uid);
        ap.setSubmittedAt(now);
        approvalMapper.insert(ap);

        list.setApprovalRequestId(ap.getId());
        list.setStatus(DestructionListStatus.pending_approval);
        listMapper.updateById(list);

        auditService.log("M11", "submit_destruction_approval", "destruction_list", listId,
                Map.of("approvalRequestId", ap.getId(), "reason", req.getReason()));

        return getListDetail(listId);
    }

    // ==================== 15.4 上传销毁现场照片 ====================

    @Transactional
    public List<DestructionListDetailResponse.PhotoView> uploadPhotos(
            Long listId, MultipartFile[] files) {

        DestructionList list = listMapper.selectById(listId);
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }
        if (files == null || files.length == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "未上传任何照片");
        }

        String bucket = fileProperties.getBucket();
        minioService.ensureBucket(bucket);
        Long uid = safeCurrentUserId();
        List<DestructionListDetailResponse.PhotoView> views = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalFilename = file.getOriginalFilename();
            String ext = FileTypeUtil.getExtension(originalFilename);

            // 格式校验
            if (!FileTypeUtil.isAllowedExtension(originalFilename, fileProperties.getAllowedExtensions())) {
                throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "不支持的文件格式: " + ext);
            }
            // 大小校验
            if (file.getSize() > fileProperties.getMaxSize()) {
                throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "文件超过大小限制: " + originalFilename);
            }

            byte[] fileBytes;
            try {
                fileBytes = file.getBytes();
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败: " + originalFilename);
            }
            String sha256 = HashUtil.sha256(fileBytes);
            String mime = file.getContentType();

            // ClamAV 扫描
            ScanResult scanResult = clamAvScanner.scan(new ByteArrayInputStream(fileBytes));
            if (scanResult == ScanResult.infected) {
                auditService.log("M11", "scan_reject", "destruction_list", listId, "filename", originalFilename);
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "文件检测到病毒，上传被拒绝: " + originalFilename);
            }
            if (scanResult == ScanResult.failed && fileProperties.getScan().isEnabled()) {
                auditService.log("M11", "scan_failed", "destruction_list", listId, "filename", originalFilename);
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "病毒扫描服务异常，上传被拒绝: " + originalFilename);
            }

            String objectKey = String.format("destruction/%d/%d/%s",
                    listId, System.currentTimeMillis(), originalFilename);
            minioService.putObject(bucket, objectKey,
                    new ByteArrayInputStream(fileBytes), file.getSize(), mime);

            BusinessAttachment att = new BusinessAttachment();
            att.setBusinessType("destruction_list");
            att.setBusinessId(listId);
            att.setAttachmentType("destruction_photo");
            att.setBucketName(bucket);
            att.setObjectKey(objectKey);
            att.setOriginalFilename(originalFilename);
            att.setFileExt(ext);
            att.setMimeType(mime);
            att.setFileSize(file.getSize());
            att.setSha256(sha256);
            attachmentMapper.insert(att);

            DestructionListDetailResponse.PhotoView v = new DestructionListDetailResponse.PhotoView();
            v.setId(att.getId());
            v.setOriginalFilename(originalFilename);
            v.setMimeType(mime);
            v.setFileSize(file.getSize());
            v.setSha256(sha256);
            views.add(v);
        }

        auditService.log("M11", "upload_destruction_photo", "destruction_list", listId,
                Map.of("count", views.size(), "operator", uid != null ? uid : 0));
        return views;
    }

    // ==================== 15.5 确认销毁 ====================

    @Transactional
    public DestructionListDetailResponse confirmDestroy(Long listId, DestructionDestroyRequest req) {
        DestructionList list = listMapper.selectById(listId);
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }
        if (list.getStatus() != DestructionListStatus.pending_destroy) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "非待销毁清册不可确认销毁");
        }
        if (list.getApprovalRequestId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "销毁清册未关联审批单");
        }
        ApprovalRequest ap = approvalMapper.selectById(list.getApprovalRequestId());
        if (ap == null || ap.getStatus() != ApprovalStatus.approved) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "销毁审批未通过，不可确认销毁");
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();

        QueryWrapper<DestructionItem> iw = new QueryWrapper<>();
        iw.eq("destruction_list_id", listId);
        List<DestructionItem> items = itemMapper.selectList(iw);
        List<Long> archiveIds = items.stream().map(DestructionItem::getArchiveId)
                .collect(Collectors.toList());

        Map<Long, Archive> archMap = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
                archMap.put(a.getId(), a);
            }
        }

        // 1. 档案置 destroyed
        for (DestructionItem it : items) {
            Archive a = archMap.get(it.getArchiveId());
            if (a != null) {
                a.setLifecycleStatus(LifecycleStatus.destroyed);
                a.setConditionStatus(ConditionStatus.destroyed);
                archiveMapper.updateById(a);
            }
        }

        // 2. 电子文件标记删除
        if (!archiveIds.isEmpty()) {
            QueryWrapper<ArchiveFile> fw = new QueryWrapper<>();
            fw.in("archive_id", archiveIds).ne("file_status", "deleted");
            List<ArchiveFile> files = archiveFileMapper.selectList(fw);
            for (ArchiveFile f : files) {
                f.setFileStatus(FileStatus.deleted);
                f.setDeletedAt(now);
                f.setDeletedBy(uid);
                archiveFileMapper.updateById(f);
            }
            for (DestructionItem it : items) {
                it.setFileDeleteStatus("deleted");
                it.setFileDeletedAt(now);
                itemMapper.updateById(it);
            }
        }

        // 3. 纸质：解除盒内关系，盒内数量递减
        if (!archiveIds.isEmpty()) {
            QueryWrapper<ArchiveBoxItem> bw = new QueryWrapper<>();
            bw.in("archive_id", archiveIds);
            List<ArchiveBoxItem> boxItems = archiveBoxItemMapper.selectList(bw);
            Map<Long, Integer> boxDecr = new HashMap<>();
            for (ArchiveBoxItem bi : boxItems) {
                boxDecr.merge(bi.getBoxId(), 1, Integer::sum);
            }
            archiveBoxItemMapper.delete(bw);
            for (Map.Entry<Long, Integer> e : boxDecr.entrySet()) {
                ArchiveBox box = archiveBoxMapper.selectById(e.getKey());
                if (box != null) {
                    int used = box.getUsedCount() != null ? box.getUsedCount() : 0;
                    box.setUsedCount(Math.max(0, used - e.getValue()));
                    archiveBoxMapper.updateById(box);
                }
            }
        }

        // 4. 清册置 destroyed
        list.setStatus(DestructionListStatus.destroyed);
        list.setDestroyedAt(now);
        list.setDestroyMethod(DestroyMethod.valueOf(req.getDestroyMethod()));
        list.setSupervisorName1(req.getSupervisorName1());
        list.setSupervisorName2(req.getSupervisorName2());
        list.setDestroyNote(req.getDestroyNote());
        listMapper.updateById(list);

        auditService.log("M11", "destroy", "destruction_list", listId,
                Map.of("destroyMethod", req.getDestroyMethod(), "itemCount", items.size()));

        return getListDetail(listId);
    }

    private Long safeCurrentUserId() {
        try {
            return AuthContext.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
