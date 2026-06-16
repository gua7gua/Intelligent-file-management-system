package com.archive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 分类统计响应（§20.2）。
 *
 * <p>保留早期 {@code bySource/bySecurity} 字段名（向后兼容），同时补齐前端
 * {@code bySourceType/bySecurityLevel} 别名（前端 types 用后者）。两个别名指向同一份数据，
 * 由 Service 同时 set，不增加查询成本。</p>
 */
@Data
public class StatisticsCategoryResponse {

    private List<Group> byCategory;
    private List<Group> byYear;
    /** 前端用 bySourceType；保留 bySource 旧名。 */
    private List<Group> bySource;
    private List<Group> byCarrier;
    /** 前端用 bySecurityLevel；保留 bySecurity 旧名。 */
    private List<Group> bySecurity;
    private List<Group> byOpenStatus;

    /** 前端别名（与上方旧字段同源，由 Service 同时 set）。 */
    private List<Group> bySourceType;
    private List<Group> bySecurityLevel;

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
