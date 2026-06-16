package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArchiveSummaryResponse {
    private Long archiveId;
    private String archiveNo;
    private String title;
    private String responsibleText;
    private Integer formedYear;
    private String categoryName;
    private String carrierStatus;
    private Boolean hasElectronicFile;
    /** 来源类型：transfer/collection/compilation（公众门户"来源"列展示） */
    private String sourceType;
    /** 密级（0非密 1内部 2秘密 3机密 4绝密），内部查阅者检索展示 */
    private Integer securityLevel;
}
