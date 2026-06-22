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
    /** 密级上限（<=该值），与权限上限 maxSecurityLevel 取交集 */
    private Integer securityLevelMax;
    private String openStatus;
    /** 借阅状态：available / on_loan / not_on_shelf */
    private String loanStatus;
    /** 保管期限：10y / 30y / permanent */
    private String retentionPeriod;
    /** 形成/移交单位名称（模糊匹配 organizations.org_name） */
    private String organizationName;
    /** 所属全宗名称（模糊匹配 fonds.fonds_name） */
    private String fondsName;
    /** 文件格式（模糊匹配 archive_files.file_ext / original_filename） */
    private String fileExt;
    /** 排序：relevance / formed_desc / archived_desc / archiveNo_asc */
    private String sortBy;

    private int pageNo = 1;
    private int pageSize = 20;
}
