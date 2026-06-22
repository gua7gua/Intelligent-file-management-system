package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

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
    /**
     * 该档案的标签名列表（检索列表"标签"列展示）。
     * 脱敏由档案级过滤天然保证：公众检索已强制 security_level=0 + open_status=open，
     * 内部检索已按 maxSecurityLevel + 数据范围过滤，故结果档案均在用户可见范围内，
     * 直接展示其自身标签不会泄露非公开档案的标签（仅出现在非公开档案上的标签永远不会进入公众结果）。
     */
    private List<String> tagNames;
    /** 所属全宗名称（检索列表"全宗"列展示，便于用户从结果发现值后缩小搜索） */
    private String fondsName;
    /** 形成/移交单位名称（检索列表"单位"列展示） */
    private String organizationName;
}
