package com.archive.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsCategoryResponse {

    private List<Group> byCategory;
    private List<Group> byYear;
    private List<Group> bySource;
    private List<Group> byCarrier;
    private List<Group> bySecurity;
    private List<Group> byOpenStatus;

    @Data
    public static class Group {
        private String label;
        private long count;

        public Group() {}

        public Group(String label, long count) {
            this.label = label;
            this.count = count;
        }
    }
}
