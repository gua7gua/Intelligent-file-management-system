package com.archive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class AppraisalBatchDetailResponse {

    private Long id;
    private String batchNo;
    private String batchName;
    private Integer categoryId;
    private String categoryName;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    /** 到期窗口天数（D4）。 */
    private Integer dueDays;
    private String status;
    private Integer hitCount;
    private Integer destroyCount;
    private Integer extendCount;
    private OffsetDateTime completedAt;
    private Long generatedListId;
    private String generatedListNo;

    private List<ItemView> items;

    @Data
    public static class ItemView {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private Integer formedYear;
        private String retentionPeriod;
        private LocalDate retentionUntil;
        /** extend / destroy / null（未鉴定）。 */
        private String appraisalResult;
        private String newRetentionPeriod;
        private LocalDate newRetentionUntil;
        private String opinion;
    }
}
