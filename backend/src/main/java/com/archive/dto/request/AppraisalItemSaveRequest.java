package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AppraisalItemSaveRequest {

    @NotNull(message = "明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "档案不能为空")
        private Long archiveId;

        /** extend / destroy。 */
        @NotNull(message = "鉴定结论不能为空")
        private String appraisalResult;

        /** 延长后的保管期限 10y/30y/permanent。 */
        private String newRetentionPeriod;

        private String opinion;
    }
}
