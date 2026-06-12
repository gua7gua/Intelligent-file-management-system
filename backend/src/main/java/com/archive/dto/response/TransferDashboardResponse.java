package com.archive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 移交工作台统计响应。
 */
@Data
public class TransferDashboardResponse {

    private Summary summary;
    private List<IntakeBatchResponse> recentBatches;

    @Data
    public static class Summary {
        private long draft;
        private long pendingTransfer;
        private long partiallyReceived;
        private long received;
        private long archived;
        private long shelved;
        private long rejected;
    }
}
