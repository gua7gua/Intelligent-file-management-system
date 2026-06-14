package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CompilationSaveRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String compilationType;
    private String dateRangeText;
    private String keywords;
    private String summary;
    private String contentHtml;

    private List<Long> materialArchiveIds;
}
