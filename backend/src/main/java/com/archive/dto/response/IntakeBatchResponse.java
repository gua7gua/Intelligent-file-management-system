package com.archive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 清单批次响应。
 */
@Data
public class IntakeBatchResponse {

    private Long id;
    private String batchNo;
    private String sourceType;
    private String title;
    private String status;
    private String statusText;
    private Long organizationId;
    private String departmentName;
    private Long publicUserId;
    private String contactName;
    private String contactPhone;
    private Integer archiveYear;
    private LocalDate expectedTransferDate;
    private OffsetDateTime scheduledReceiveAt;
    private OffsetDateTime submittedAt;
    private Long acceptedBy;
    private OffsetDateTime acceptedAt;
    private OffsetDateTime archivedAt;
    private OffsetDateTime shelvedAt;
    private String rejectReason;
    private OffsetDateTime agreementAcceptedAt;
    private Integer itemCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** 批次详情时填充条目列表。 */
    private List<IntakeItemResponse> items;

    /** 批次详情时填充该批次已上传的暂存电子文件（含 ClamAV 扫描结果与匹配状态）。 */
    private List<StagingFileResponse> stagingFiles;
}
