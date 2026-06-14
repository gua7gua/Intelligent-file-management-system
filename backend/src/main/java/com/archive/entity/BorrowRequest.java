package com.archive.entity;

import com.archive.enums.BorrowStatus;
import com.archive.enums.ReturnCheckResult;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 借阅申请实体（borrow_requests）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("borrow_requests")
public class BorrowRequest extends BaseEntity {

    private String requestNo;

    private Long archiveId;

    private Long borrowerId;

    private String reason;

    private Integer expectedDays;

    private OffsetDateTime expectedVisitAt;

    private String contactPhone;

    @EnumValue
    private BorrowStatus status;

    private Long approvedBy;

    private OffsetDateTime approvedAt;

    private String rejectReason;

    private String voucherNo;

    private OffsetDateTime voucherIssuedAt;

    private Long checkedOutBy;

    private OffsetDateTime checkedOutAt;

    private OffsetDateTime dueAt;

    private Long returnedBy;

    private OffsetDateTime returnedAt;

    @EnumValue
    private ReturnCheckResult returnCheckResult;

    private String returnNote;

    /** 软删除标记，项目未启用 @TableLogic，查询时手动 isNull("deleted_at")。 */
    private OffsetDateTime deletedAt;
}
