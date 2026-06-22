package com.archive.dto.response;

import lombok.Data;

/**
 * 鉴定工作台顶部真实统计（替代前端硬编码假值）。
 * 全部来自实时聚合，不含任何写死数字。
 */
@Data
public class AppraisalStatsResponse {
    /** 即将到期档案数：retention_until 在今天+365天内（含已过期未处理），未销毁。 */
    private long expiringCount;
    /** 待销毁档案数：已鉴定为销毁（lifecycle_status=pending_destruction），尚未生成清册/审批。 */
    private long pendingDestructionCount;
    /** 已生成销毁清册数：destruction_lists 未删除条数。 */
    private long generatedListCount;
}
