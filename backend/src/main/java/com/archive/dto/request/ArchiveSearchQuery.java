package com.archive.dto.request;

import lombok.Data;

/**
 * 5.2/11.2 检索查询参数。用 @ModelAttribute 绑定。
 * 内部版（11.2）额外接受 securityLevel、openStatus 作为用户主动筛选。
 */
@Data
public class ArchiveSearchQuery {

    /** 题名/档号/责任者 模糊检索 */
    private String keyword;
    private String archiveNo;
    private String title;
    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    private String responsibleText;
    /** 逗号分隔的标签 ID */
    private String tagIds;
    private String sourceType;
    private String carrierStatus;
    private Boolean hasElectronicFile;

    /** 内部版额外筛选 */
    private Integer securityLevel;
    private String openStatus;

    private int pageNo = 1;
    private int pageSize = 20;
}
