package com.archive.dto.request;

import com.archive.common.PageRequest;
import com.archive.enums.FileCheckResult;
import com.archive.enums.FileCheckTargetType;
import com.archive.enums.FileCheckType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileCheckRecordQuery extends PageRequest {

    private FileCheckTargetType targetType;
    private Long targetId;
    private FileCheckType checkType;
    private FileCheckResult checkResult;
}
