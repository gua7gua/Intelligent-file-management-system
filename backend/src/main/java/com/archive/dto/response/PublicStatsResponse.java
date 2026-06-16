package com.archive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 公开馆藏统计（§5.1.1 GET /api/public/stats）。
 * 口径与 §5.2 公开检索一致：security_level=0 AND open_status=open AND lifecycle_status=normal。
 */
@Data
public class PublicStatsResponse {

    /** 公开档案数。 */
    private long openArchiveCount;
    /** 关联到公开档案的电子文件数。 */
    private long electronicFileCount;
    /** 全部征集批次总数。 */
    private long collectionCount;
    /** 近 30 天开放的公开档案数。 */
    private long latestOpenCount;
    /** 公开档案按门类分组。 */
    private List<CategoryCount> categories;

    @Data
    public static class CategoryCount {
        private String name;
        private long count;

        public CategoryCount() {
        }

        public CategoryCount(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }
}
