package com.archive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 档案列表/详情（10.1/10.2）。
 */
@Data
public class ArchiveResponse {

    private Long id;

    private String archiveNo;

    private String title;

    private String responsibleText;

    private LocalDate formedDate;

    private Integer formedYear;

    private Integer categoryId;

    private String categoryName;

    private String sourceType;

    private Long organizationId;

    private String organizationName;

    private Long fondsId;

    private String fondsName;

    private String carrierStatus;

    private String retentionPeriod;

    private LocalDate retentionUntil;

    private Integer securityLevel;

    private String openStatus;

    private Boolean allowDigitization;

    private String lifecycleStatus;

    private String loanStatus;

    private String conditionStatus;

    private OffsetDateTime archivedAt;

    private OffsetDateTime shelvedAt;

    private List<String> tagNames;

    private String boxNo;

    private String locationCode;

    /** 详情时包含文件列表。 */
    private List<ArchiveFileResponse> files;

    /** 详情时包含变更日志。 */
    private List<ArchiveChangeLogEntry> changeLogs;

    @Data
    public static class ArchiveChangeLogEntry {
        private Long id;
        private String fieldName;
        private String oldValue;
        private String newValue;
        private String changeSource;
        private String changedByName;
        private OffsetDateTime changedAt;
    }
}
