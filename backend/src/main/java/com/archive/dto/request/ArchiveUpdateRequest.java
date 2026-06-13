package com.archive.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 编辑非受保护元数据（10.3）。
 */
@Data
public class ArchiveUpdateRequest {

    private String title;

    private String responsibleText;

    private LocalDate formedDate;

    private Integer categoryId;

    private Long fondsId;

    private List<String> tagNames;

    private String changeReason;
}
