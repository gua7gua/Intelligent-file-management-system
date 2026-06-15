package com.archive.dto.request;

import com.archive.common.CursorPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveAccessLogQuery extends CursorPage {
    private Long userId;
    private Long archiveId;
    private String accessType;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
