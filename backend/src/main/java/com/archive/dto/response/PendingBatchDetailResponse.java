package com.archive.dto.response;

import com.archive.dto.response.PendingItemResponse;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 入库批次详情（9.2）。
 */
@Data
public class PendingBatchDetailResponse {

    private Long id;

    private String batchNo;

    private String sourceType;

    private String title;

    private String status;

    private String organizationName;

    private String contactName;

    private OffsetDateTime acceptedAt;

    /** 最近一次 AI 补全任务状态，与列表接口 latestAiTaskStatus 对齐，供前端在详情视图刷新 AI 标签。 */
    private String latestAiTaskStatus;

    /** 最近一次 AI 补全任务 ID，供前端失败后重试。 */
    private Long latestAiTaskId;

    private List<PendingItemResponse> items;

    /** 可用档案盒摘要（boxNo + categoryId + fondsId + usedCount/capacity）。 */
    private List<AvailableBoxSummary> availableBoxes;

    /** 可用架位摘要（locationCode + roomId）。 */
    private List<AvailableLocationSummary> availableLocations;

    @Data
    public static class AvailableBoxSummary {
        private Long id;
        private String boxNo;
        private Integer categoryId;
        private Integer usedCount;
        private Integer capacity;
    }

    @Data
    public static class AvailableLocationSummary {
        private Long id;
        private String locationCode;
        private Long roomId;
        private String roomNo;
    }
}
