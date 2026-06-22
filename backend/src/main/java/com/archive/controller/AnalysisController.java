package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.AnalysisItemHandleRequest;
import com.archive.dto.request.AnalysisTaskCreateRequest;
import com.archive.dto.request.AnalysisTaskQuery;
import com.archive.dto.response.AnalysisItemResponse;
import com.archive.dto.response.AnalysisTaskDetailResponse;
import com.archive.dto.response.AnalysisTaskResponse;
import com.archive.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据研判接口（M14）。跨 /api/admin/analysis-tasks 与 /api/admin/analysis-items。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "数据研判", description = "规则扫描与 AI 标签建议")
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/api/admin/analysis-tasks")
    @Operation(summary = "20.4 查询研判任务")
    public R<PageResult<AnalysisTaskResponse>> list(@ModelAttribute AnalysisTaskQuery query) {
        return R.ok(analysisService.list(query));
    }

    @PostMapping("/api/admin/analysis-tasks")
    @Operation(summary = "20.5 创建研判任务")
    public R<AnalysisTaskResponse> create(@RequestBody @Valid AnalysisTaskCreateRequest req) {
        return R.ok(analysisService.create(req));
    }

    @GetMapping("/api/admin/analysis-tasks/{taskId}")
    @Operation(summary = "20.6 获取研判任务详情")
    public R<AnalysisTaskDetailResponse> detail(@PathVariable Long taskId) {
        return R.ok(analysisService.getDetail(taskId));
    }

    @PostMapping("/api/admin/analysis-items/{itemId}/handle")
    @Operation(summary = "20.7 处理研判项")
    public R<AnalysisItemResponse> handle(@PathVariable Long itemId,
                                          @RequestBody @Valid AnalysisItemHandleRequest req) {
        return R.ok(analysisService.handleItem(itemId, req));
    }

    @DeleteMapping("/api/admin/analysis-items/{itemId}")
    @Operation(summary = "20.9 删除研判异常项（软删，保留数据）")
    public R<Void> deleteItem(@PathVariable Long itemId) {
        analysisService.deleteItem(itemId);
        return R.ok();
    }

    @DeleteMapping("/api/admin/analysis-tasks/{taskId}")
    @Operation(summary = "20.8 删除研判任务")
    public R<Void> delete(@PathVariable Long taskId) {
        analysisService.delete(taskId);
        return R.ok();
    }
}
