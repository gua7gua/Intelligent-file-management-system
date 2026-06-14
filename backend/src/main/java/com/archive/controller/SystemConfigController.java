package com.archive.controller;

import com.archive.common.R;
import com.archive.dto.request.SystemConfigBatchUpdateRequest;
import com.archive.dto.request.SystemConfigUpdateRequest;
import com.archive.dto.response.SystemConfigResponse;
import com.archive.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system-configs")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "配置项查询与更新")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @Operation(summary = "查询系统配置")
    public R<List<SystemConfigResponse>> list() {
        return R.ok(systemConfigService.listConfigs());
    }

    @PutMapping("/{configKey}")
    @Operation(summary = "更新单项配置")
    public R<SystemConfigResponse> update(
            @PathVariable String configKey, @RequestBody @Valid SystemConfigUpdateRequest req) {
        return R.ok(systemConfigService.updateConfig(configKey, req));
    }

    @PutMapping
    @Operation(summary = "批量更新配置")
    public R<List<SystemConfigResponse>> batchUpdate(@RequestBody @Valid SystemConfigBatchUpdateRequest req) {
        return R.ok(systemConfigService.batchUpdate(req));
    }
}
