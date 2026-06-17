package com.archive.dto.request;

import com.archive.enums.AnalysisScanMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnalysisTaskCreateRequest {

    /**
     * 扫描方式（rule 规则扫描 / ai AI研判 / mixed 综合）。
     * <p>与历史 task_type 分离：前端「任务类型」选择器语义即扫描方式。
     * 规则扫描统一覆盖缺字段 + 门类冲突两种检查，结果类型固定 mixed。</p>
     */
    @NotNull(message = "扫描方式不能为空")
    private AnalysisScanMethod scanMethod;

    @Valid
    private AnalysisRule rule;
}
