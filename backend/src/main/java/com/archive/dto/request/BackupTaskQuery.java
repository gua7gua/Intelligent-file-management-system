package com.archive.dto.request;

import com.archive.common.PageRequest;
import com.archive.enums.BackupScope;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BackupTaskQuery extends PageRequest {

    private String status;

    private BackupScope backupScope;
}
