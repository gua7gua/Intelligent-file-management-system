package com.archive.dto.request;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 征集约定到馆时间请求。
 */
@Data
public class CollectionScheduleRequest {

    private OffsetDateTime scheduledReceiveAt;

    private String contactNote;
}
