package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.FondsCreateRequest;
import com.archive.dto.request.FondsQuery;
import com.archive.dto.request.FondsUpdateRequest;
import com.archive.dto.response.FondsDetailResponse;
import com.archive.dto.response.FondsResponse;
import com.archive.enums.RoleCode;
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

    @DeleteMapping("/{fondsId}")
    @Operation(summary = "删除全宗（物理删除；关联档案/档案盒的 fonds_id 置空）")
    public R<Void> delete(@PathVariable Long fondsId) {
        requireBackOrAdmin();
        fondsService.deleteFonds(fondsId);
        return R.ok();
    }

    private void requireBackOrAdmin() {
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.sys_admin))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }
}
