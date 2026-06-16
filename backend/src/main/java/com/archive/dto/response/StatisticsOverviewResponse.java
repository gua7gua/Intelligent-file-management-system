package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计总览响应（§20.1）。
 *
 * <p>保留早期实现返回的 {@code totals}/{@code trends}/{@code storageUsage}（向后兼容），
 * 同时补齐前端统计页（原型 statistics.html）所需的 {@code metrics}/{@code yearlyIntake}/
 * {@code categoryDistribution}/{@code carrierDistribution}/{@code businessBreakdown}/
 * {@code dataSources}/{@code summarizedAt} 等富字段。所有字段均来自实时聚合。</p>
 */
@Data
public class StatisticsOverviewResponse {

    /** 早期精简版（保留，避免破坏既有调用方）。 */
    private Totals totals;
    private List<Trend> trends;
    private List<StorageUsage> storageUsage;

    /** 前端统计页四张可下钻指标卡。 */
    private List<Metric> metrics;
    /** 年度进馆趋势（按 archived_at 年份聚合）。 */
    private List<YearlyIntake> yearlyIntake;
    /** 门类分布（label/百分比）。 */
    private List<Distribution> categoryDistribution;
    /** 载体分布（label/百分比）。 */
    private List<Distribution> carrierDistribution;
    /** 业务汇总（移交/征集/借阅/销毁/保存）。 */
    private List<BusinessBreakdown> businessBreakdown;
    /** 数据源健康度（按业务表统计最近写入时间）。 */
    private List<DataSource> dataSources;
    /** 统计生成时间。 */
    private OffsetDateTime summarizedAt;

    @Data
    public static class Totals {
        private long totalArchives;
        private long openArchives;
        private long borrowCount;
        private long destroyedCount;
    }

    @Data
    public static class Trend {
        private String month;
        private String sourceType;
        private long count;
    }

    @Data
    public static class StorageUsage {
        private Long locationId;
        private String locationCode;
        private int used;
        private int capacity;
        private double rate;
    }

    /** 原型「核心统计指标」可下钻卡片。 */
    @Data
    public static class Metric {
        private String key;
        private String label;
        private long value;
        private String source;
        /** 跳转路由（相对前端路径，可空）。 */
        private String targetRoute;
        /** 跳转参数。 */
        private Map<String, String> targetQuery;
    }

    @Data
    public static class YearlyIntake {
        private int year;
        private long count;
    }

    /** 通用分布项（门类/载体等）：label + 比例 0~1。 */
    @Data
    public static class Distribution {
        private String label;
        private long value;
        private double ratio;
    }

    /** 业务汇总行。 */
    @Data
    public static class BusinessBreakdown {
        private String key;
        private String domain;
        private long total;
        private List<Detail> details;
        private String sourceTable;

        @Data
        public static class Detail {
            private String label;
            private long count;
        }
    }

    /** 数据源健康项。 */
    @Data
    public static class DataSource {
        private String table;
        private String label;
        private boolean healthy;
        private String lastSyncedAt;
    }
}
