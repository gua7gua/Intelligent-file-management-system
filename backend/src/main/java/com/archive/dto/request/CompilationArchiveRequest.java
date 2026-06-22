package com.archive.dto.request;

import com.archive.enums.RetentionPeriod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CompilationArchiveRequest {

    /** 全宗可留空（编研成果允许暂不归属全宗） */
    private Long fondsId;

    @NotNull(message = "门类不能为空")
    private Integer categoryId;

    private LocalDate formedDate;

    @NotNull(message = "保管期限不能为空")
    private RetentionPeriod retentionPeriod;

    @NotNull(message = "公开状态不能为空")
    private String openStatus;

    private List<String> tagNames;
}
