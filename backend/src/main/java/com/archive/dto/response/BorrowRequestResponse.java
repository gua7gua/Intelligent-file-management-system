package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 借阅申请列表/详情响应（11.8/11.9/12.1/12.2）。
 * archive 总是填充；borrower/location 仅管理端填充，内部端为 null。
 */
@Data
@AllArgsConstructor
public class BorrowRequestResponse {

    private Long id;
    private String requestNo;
    private String status;
    private String reason;
    private Integer expectedDays;
    private OffsetDateTime expectedVisitAt;
    private String contactPhone;
    private OffsetDateTime dueAt;
    /** 动态计算：status=checked_out 且 dueAt<now。 */
    private Boolean overdue;
    private String rejectReason;
    private String voucherNo;
    private OffsetDateTime voucherIssuedAt;
    private OffsetDateTime approvedAt;
    private Long approvedBy;
    private OffsetDateTime checkedOutAt;
    private OffsetDateTime returnedAt;
    private String returnCheckResult;
    private String returnNote;
    private OffsetDateTime createdAt;

    /** 可借检查结果（管理端 §12.2 审批辅助信息，内部端不返回） */
    private String checkCarrier;
    private String checkLifecycle;
    private String checkLoan;
    private String checkInventory;

    private ArchiveSummary archive;
    private BorrowerSummary borrower;
    private LocationSummary location;

    @Data
    @AllArgsConstructor
    public static class ArchiveSummary {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private String carrierStatus;
        private String lifecycleStatus;
        private String loanStatus;
        private String conditionStatus;
    }

    @Data
    @AllArgsConstructor
    public static class BorrowerSummary {
        private Long borrowerId;
        private String realName;
        private String employeeNo;
        private String departmentName;
        private String organizationName;
    }

    @Data
    @AllArgsConstructor
    public static class LocationSummary {
        private String boxNo;
        private String locationCode;
    }
}
