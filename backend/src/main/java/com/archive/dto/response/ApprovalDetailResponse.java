package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ApprovalDetailResponse {

    private Long id;
    private String approvalType;
    private String targetType;
    private Long targetId;
    private Long evidenceArchiveId;
    private String evidenceArchiveNo;
    private String oldValue;
    private String newValue;
    private String reason;
    private String status;
    private Long submittedBy;
    private String submittedByName;
    private OffsetDateTime submittedAt;
    private Long approvedBy;
    private String approvedByName;
    private OffsetDateTime approvedAt;
    private String approvalOpinion;
    private String targetSummary;
    /** 目标档案信息（archive 类型审批使用）。 */
    private String targetArchiveNo;
    private String targetArchiveTitle;
    /** 目标销毁清册信息（destruction 类型审批使用）。 */
    private String targetListNo;
    private String targetListName;
    /** 凭证档案是否与目标档案匹配（密级/开放调整审批）。 */
    private Boolean evidenceMatched;
    /** 目标档案摘要（archive 类型审批）。 */
    private ArchiveSummary targetArchive;
    /** 凭证档案摘要（密级/开放调整审批）。 */
    private ArchiveSummary evidenceArchive;
    /** 销毁清册摘要（destruction 类型审批）。 */
    private DestructionListSummary destructionList;

    @Data
    public static class ArchiveSummary {
        private Long id;
        private String archiveNo;
        private String title;
        private String categoryName;
        private String organizationName;
        private String fondsName;
        private Integer securityLevel;
        private String openStatus;
        private String lifecycleStatus;
    }

    @Data
    public static class DestructionListSummary {
        private Long id;
        private String listNo;
        private String listName;
        private Integer itemCount;
        private String appraisalBatchNo;
        private List<DestructionListItem> items;
    }

    @Data
    public static class DestructionListItem {
        private Long archiveId;
        private String archiveNoSnapshot;
        private String titleSnapshot;
        private String categorySnapshot;
        private String retentionSnapshot;
        private Integer securityLevelSnapshot;
        private String appraisalOpinionSnapshot;
    }
}
