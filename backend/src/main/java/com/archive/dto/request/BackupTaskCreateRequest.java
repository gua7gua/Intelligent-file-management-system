package com.archive.dto.request;

import com.archive.enums.BackupScope;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BackupTaskCreateRequest {

    @NotNull(message = "备份范围不能为空")
    private BackupScope backupScope;
}
