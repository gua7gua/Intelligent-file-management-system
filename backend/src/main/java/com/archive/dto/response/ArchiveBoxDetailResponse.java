package com.archive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 档案盒详情，含架位与盒内档案条目。
 */
@Data
public class ArchiveBoxDetailResponse {

    private Long id;
    private String boxNo;
    private Long locationId;
    private String locationCode;
    private String roomNo;
    private Integer categoryId;
    private Long fondsId;
    private String yearLabel;
    private String spineText;
    private Integer capacity;
    private Integer usedCount;
    private String status;

    private List<BoxItemView> items;

    /** 盒内档案条目视图。 */
    @Data
    public static class BoxItemView {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private Integer sortNo;
        private Integer pageCount;
        private String physicalStatus;
    }
}
