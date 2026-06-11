package com.archive.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应结构。
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private Boolean hasNext;

    public PageResult() {}

    public PageResult(List<T> records, Integer pageNo, Integer pageSize, Long total) {
        this.records = records;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.hasNext = (long) pageNo * pageSize < total;
    }
}
