package com.archive.dto.request;

import com.archive.enums.FileCheckType;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FileCheckTriggerRequest {

    @NotEmpty(message = "检测类型不能为空")
    private List<FileCheckType> checkTypes;
}
