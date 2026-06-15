package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.CompilationArchiveRequest;
import com.archive.dto.request.CompilationQuery;
import com.archive.dto.request.CompilationSaveRequest;
import com.archive.dto.response.CompilationDetailResponse;
import com.archive.dto.response.CompilationResponse;
import com.archive.service.CompilationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 档案编研接口（M13）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "档案编研", description = "编研正文生成与成果入库")
public class CompilationController {

    private final CompilationService compilationService;

    @GetMapping("/api/admin/compilations")
    @Operation(summary = "19.1 查询编研成果")
    public R<PageResult<CompilationResponse>> list(@ModelAttribute CompilationQuery query) {
        return R.ok(compilationService.list(query));
    }

    @PostMapping("/api/admin/compilations")
    @Operation(summary = "19.2 创建编研草稿")
    public R<CompilationDetailResponse> create(@RequestBody @Valid CompilationSaveRequest req) {
        return R.ok(compilationService.save(null, req));
    }

    @GetMapping("/api/admin/compilations/{id}")
    @Operation(summary = "19.3 获取编研详情")
    public R<CompilationDetailResponse> detail(@PathVariable Long id) {
        return R.ok(compilationService.getDetail(id));
    }

    @PutMapping("/api/admin/compilations/{id}")
    @Operation(summary = "19.4 更新编研草稿")
    public R<CompilationDetailResponse> update(@PathVariable Long id, @RequestBody @Valid CompilationSaveRequest req) {
        return R.ok(compilationService.save(id, req));
    }

    @PostMapping("/api/admin/compilations/{id}/generate")
    @Operation(summary = "19.5 生成正文附件")
    public R<CompilationDetailResponse> generate(@PathVariable Long id) {
        return R.ok(compilationService.generate(id));
    }

    @PostMapping("/api/admin/compilations/{id}/archive")
    @Operation(summary = "19.6 编研成果入库")
    public R<Map<String, Object>> archive(@PathVariable Long id, @RequestBody @Valid CompilationArchiveRequest req) {
        return R.ok(Map.of("archiveId", compilationService.archive(id, req)));
    }
}
