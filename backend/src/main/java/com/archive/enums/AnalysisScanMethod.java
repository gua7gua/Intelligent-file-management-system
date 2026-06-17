package com.archive.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * 数据研判扫描方式（analysis_tasks.scan_method）。
 *
 * <p>与 {@link AnalysisTaskType}（结果类型）分离：用户在前端「任务类型」选择器里选的是
 * 扫描方式（rule/ai/mixed），后端落库到独立列 scan_method；task_type 仍按结果类型语义
 * 固定为 mixed（规则扫描覆盖缺字段 + 门类冲突两种检查）。</p>
 */
@Getter
public enum AnalysisScanMethod {
    rule("规则扫描"),
    ai("AI研判"),
    mixed("综合");

    private final String displayName;

    AnalysisScanMethod(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 宽松反序列化：大小写不敏感匹配 rule/ai/mixed；未知值返回 null（由 @NotNull 校验拦截）。
     */
    @JsonCreator
    public static AnalysisScanMethod fromString(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        for (AnalysisScanMethod m : values()) {
            if (m.name().equalsIgnoreCase(s)) {
                return m;
            }
        }
        return null;
    }
}
