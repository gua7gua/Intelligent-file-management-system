package com.archive.dto.request;

import lombok.Data;

@Data
public class StatisticsOverviewQuery {

    private Integer yearStart;
    private Integer yearEnd;
    private Long organizationId;
    private Long fondsId;
    /** 导出格式：xlsx 或 pdf。 */
    private String format;
}
