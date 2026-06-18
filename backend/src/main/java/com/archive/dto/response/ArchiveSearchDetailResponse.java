package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 档案详情响应。公众版不填 securityLevel/openStatus/retentionPeriod（配合 R 的 @JsonInclude(NON_NULL) 脱敏）。
 */
@Data
@AllArgsConstructor
public class ArchiveSearchDetailResponse {
    private Long archiveId;
    private String archiveNo;
    private String title;
    private String responsibleText;
    private Integer formedYear;
    private LocalDate formedDate;
    private String categoryName;
    private String carrierStatus;
    /** 内部版可见，公众版不填 */
    private Integer securityLevel;
    /** 内部版可见，公众版不填 */
    private String openStatus;
    /** 内部版可见，公众版不填 */
    private String retentionPeriod;
    /** 可预览/可下载文件摘要 */
    private List<FileSummary> files;

    @Data
    @AllArgsConstructor
    public static class FileSummary {
        private Long fileId;
        private String originalFilename;
        private String fileExt;
        private Long fileSize;
        private String mimeType;
        private String fileRole;
        /** 该文件在当前上下文是否可预览（受 open_status 等限制） */
        private Boolean canPreview;
        /** 该文件在当前上下文是否可下载 */
        private Boolean canDownload;
    }
}
