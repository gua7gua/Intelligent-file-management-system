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

    private String fileMatchStatus;

    /** AI 补全建议 JSON。 */
    private String aiSuggestion;

    private String confirmedTitle;

    private String confirmedResponsibleText;

    private LocalDate confirmedFormedDate;

    private Integer confirmedCategoryId;

    private List<String> confirmedTags;

    /** 已匹配暂存文件摘要。 */
    private List<StagingFileSummary> stagingFiles;

    private Long generatedArchiveId;

    @Data
    public static class StagingFileSummary {
        private Long id;
        private String originalFilename;
        private Long fileSize;
        private String fileExt;
        private String matchStatus;
    }
}
