package com.archive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 清单条目响应。
 */
@Data
public class IntakeItemResponse {

    private Long id;
    private Long batchId;
    private Integer itemNo;
    private String status;
    private String statusText;
    private String inputTitle;
    private Integer pageCount;
    private String retentionPeriod;
    private String carrierStatus;
    private Integer securityLevel;
    private String openStatus;
    private Boolean allowDigitization;
    private String electronicFormat;
    private String expectedFilename;
    private LocalDate formedDate;
    private String acceptanceNote;
    private String rejectReason;
    private String fileMatchStatus;
    private Object aiSuggestion;
    private String confirmedTitle;
    private String confirmedResponsibleText;
    private LocalDate confirmedFormedDate;
    private Integer confirmedCategoryId;
    private List<String> confirmedTags;
    private Long generatedArchiveId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
