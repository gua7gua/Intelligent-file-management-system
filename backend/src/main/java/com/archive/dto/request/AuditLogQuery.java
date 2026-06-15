package com.archive.dto.request;

import com.archive.common.CursorPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQuery extends CursorPage {
    private Long actorUserId;
    private String actorType;
    private String moduleName;
    private String operationType;
    private String businessType;
    private Long businessId;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
