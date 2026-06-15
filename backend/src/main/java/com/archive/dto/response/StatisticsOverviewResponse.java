package com.archive.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsOverviewResponse {

    private Totals totals;
    private List<Trend> trends;
    private List<StorageUsage> storageUsage;

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
}
