package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class AnalysisItemResponse {

    private Long id;
    private Long taskId;
    private Long archiveId;
    private String issueType;
    private Map<String, Object> issueDetail;
    private Map<String, Object> suggestion;
    private String status;
    private OffsetDateTime handledAt;
}
