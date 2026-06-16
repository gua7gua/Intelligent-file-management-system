package com.archive.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/** 研判任务类型。name() 与 analysis_tasks.task_type CHECK 约束一一对应。 */
@Getter
public enum AnalysisTaskType {
    missing_fields("缺字段"),
    category_conflict("门类冲突"),
    mixed("综合");

    private final String displayName;

    AnalysisTaskType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 宽松反序列化：除枚举名外，兼容前端原型用的 {@code rule}/{@code ai}：
     * <ul>
     *   <li>{@code rule} → {@link #mixed}（规则扫描：缺字段 + 门类冲突，由 Service 内部按 mixed 触发）</li>
     *   <li>{@code ai} → {@link #mixed}（规则 + AI 建议；AI 由 rule.includeAiSuggestion 控制）</li>
     *   <li>其它直接走 name() 匹配，匹配不到返回 null（由 @NotNull 校验拦截）</li>
     * </ul>
     */
    @JsonCreator
    public static AnalysisTaskType fromString(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        if ("rule".equalsIgnoreCase(s) || "ai".equalsIgnoreCase(s)) {
            return mixed;
        }
        for (AnalysisTaskType t : values()) {
            if (t.name().equalsIgnoreCase(s)) {
                return t;
            }
        }
        return null;
    }
}
