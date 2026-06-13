package com.archive.controller;

import com.archive.common.ErrorCode;
import com.archive.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * AI 任务接口（空壳，待刘星 feat/search-liu 实现）。
 */
@RestController
@RequestMapping("/api/admin/ai-tasks")
@Tag(name = "AI 任务", description = "AI 补全任务查询与重试")
public class AiTaskController {

    @GetMapping("/{taskId}")
    @Operation(summary = "查询 AI 补全任务")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public R<Void> getAiTask(@PathVariable Long taskId) {
        return R.fail(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 补全功能待实现");
    }

    @PostMapping("/{taskId}/retry-failed")
    @Operation(summary = "重试失败 AI 批次")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public R<Void> retryAiTask(@PathVariable Long taskId) {
        return R.fail(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 补全功能待实现");
    }
}
