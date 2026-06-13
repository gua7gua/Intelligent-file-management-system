package com.archive.controller;

import com.archive.common.R;
import com.archive.dto.response.AiTaskResponse;
import com.archive.service.AiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 任务接口（9.4 查询、9.5 重试失败批次）。
 */
@RestController
@RequestMapping("/api/admin/ai-tasks")
@RequiredArgsConstructor
@Tag(name = "AI 任务", description = "AI 补全任务查询与重试")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    @GetMapping("/{taskId}")
    @Operation(summary = "查询 AI 补全任务")
    public R<AiTaskResponse> getAiTask(@PathVariable Long taskId) {
        return R.ok(aiTaskService.getTask(taskId));
    }

    @PostMapping("/{taskId}/retry-failed")
    @Operation(summary = "重试失败 AI 批次")
    public R<AiTaskResponse> retryAiTask(@PathVariable Long taskId) {
        return R.ok(aiTaskService.retryFailed(taskId));
    }
}
