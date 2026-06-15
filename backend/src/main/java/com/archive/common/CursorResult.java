package com.archive.common;

import lombok.Data;
import java.util.List;

@Data
public class CursorResult<T> {
    private List<T> records;
    private String nextCursor;
    private Boolean hasNext;

    public CursorResult() {}

    public CursorResult(List<T> records, String nextCursor, boolean hasNext) {
        this.records = records;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
