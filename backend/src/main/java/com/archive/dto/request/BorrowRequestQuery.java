package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 借阅申请列表查询参数。
 * 内部查阅者用 keyword（我的列表）；管理端用 borrowerKeyword/archiveKeyword。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowRequestQuery extends PageRequest {

    private String status;
    private String borrowerKeyword;
    private String archiveKeyword;
    private Boolean overdue;
    private String keyword;
}
