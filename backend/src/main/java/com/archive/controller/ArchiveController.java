package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.ArchivePlacementRequest;
import com.archive.dto.request.ArchiveUpdateRequest;
import com.archive.dto.request.OpenAdjustRequest;
import com.archive.dto.request.SecurityAdjustRequest;
import com.archive.dto.response.ArchiveResponse;
import com.archive.entity.ApprovalRequest;
import com.archive.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 档案管理接口。
 */
@RestController
@RequestMapping("/api/admin/archives")
@RequiredArgsConstructor
@Tag(name = "档案管理", description = "档案列表、详情、元数据编辑、密级/开放调整")
public class ArchiveController {

    private final ArchiveService archiveService;

    @GetMapping
    @Operation(summary = "管理端查询档案")
    public R<PageResult<ArchiveResponse>> listArchives(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String archiveNo,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer formedYearStart,
            @RequestParam(required = false) Integer formedYearEnd,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long fondsId,
            @RequestParam(required = false) Integer securityLevel,
            @RequestParam(required = false) String openStatus,
            @RequestParam(required = false) String carrierStatus,
            @RequestParam(required = false) String lifecycleStatus,
            @RequestParam(required = false) String loanStatus,
            @RequestParam(required = false) String conditionStatus,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Boolean hasElectronicFile,
            @RequestParam(required = false) String retentionPeriod,
            @RequestParam(required = false) Integer securityLevelMax,
            @RequestParam(required = false) String fondsName,
            @RequestParam(required = false) String organizationName,
            @RequestParam(required = false) String fileExt,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String tagKeyword,
            @RequestParam(defaultValue = "false") boolean includeDestroyed,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(archiveService.listArchives(keyword, archiveNo, categoryId,
                formedYearStart, formedYearEnd, organizationId, fondsId, securityLevel,
                openStatus, carrierStatus, lifecycleStatus, loanStatus, conditionStatus,
                sourceType, hasElectronicFile,
                retentionPeriod, securityLevelMax, fondsName, organizationName, fileExt, sortBy,
                tagKeyword, includeDestroyed, pageNo, pageSize));
    }

    @GetMapping("/{archiveId}")
    @Operation(summary = "获取档案详情")
    public R<ArchiveResponse> getDetail(@PathVariable Long archiveId) {
        return R.ok(archiveService.getArchiveDetail(archiveId));
    }

    @PutMapping("/{archiveId}")
    @Operation(summary = "编辑非受保护元数据")
    public R<ArchiveResponse> updateArchive(
            @PathVariable Long archiveId,
            @RequestBody @Valid ArchiveUpdateRequest req) {
        return R.ok(archiveService.updateArchive(archiveId, req));
    }

    @PutMapping("/{archiveId}/placement")
    @Operation(summary = "档案换盒（改所在档案盒）")
    public R<Void> placeArchive(@PathVariable Long archiveId,
                                @RequestBody @Valid ArchivePlacementRequest req) {
        archiveService.placeArchive(archiveId, req);
        return R.ok();
    }

    @PostMapping("/{archiveId}/security-adjustments")
    @Operation(summary = "发起密级调整审批")
    public R<ApprovalRequest> securityAdjust(
            @PathVariable Long archiveId,
            @RequestBody @Valid SecurityAdjustRequest req) {
        return R.ok(archiveService.createSecurityAdjustment(archiveId, req));
    }

    @PostMapping("/{archiveId}/open-adjustments")
    @Operation(summary = "发起开放调整审批")
    public R<ApprovalRequest> openAdjust(
            @PathVariable Long archiveId,
            @RequestBody @Valid OpenAdjustRequest req) {
        return R.ok(archiveService.createOpenAdjustment(archiveId, req));
    }
}
