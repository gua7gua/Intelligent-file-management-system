package com.archive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 待入库条目详情。
 */
@Data
public class PendingItemResponse {

    private Long id;

    private Integer itemNo;

    private String inputTitle;

    private String status;

    private String carrierStatus;

    private Integer pageCount;

    private Integer securityLevel;

    private String retentionPeriod;

    /** 开放状态：open/closed。征集条目按目标属性默认 open（公众捐赠固定公开）。 */
    private String openStatus;

    /** 是否允许数字化。 */
    private Boolean allowDigitization;

    private String fileMatchStatus;

    /** AI 补全建议 JSON。 */
    private String aiSuggestion;

    /** AI 建议的结构化字段，便于前端直接回填表单。 */
    private String suggestedTitle;
    private String suggestedResponsible;
    private LocalDate suggestedFormedDate;
    private Integer suggestedCategoryId;
    private List<String> suggestedTags;

    private String confirmedTitle;

    private String confirmedResponsibleText;

    private LocalDate confirmedFormedDate;

    private Integer confirmedCategoryId;

    private List<String> confirmedTags;

    /** 已匹配暂存文件摘要。 */
    private List<StagingFileSummary> stagingFiles;

    private Long generatedArchiveId;

    /** 已入库档案的生命周期状态（pending_shelf/normal 等），未入库为 null。 */
    private String lifecycleStatus;

    @Data
    public static class StagingFileSummary {
        private Long id;
        private String originalFilename;
        private Long fileSize;
        private String fileExt;
        private String matchStatus;
    }
}
