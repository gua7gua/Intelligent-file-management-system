package com.archive.dto.response;

import lombok.Data;

@Data
public class CompilationMaterialResponse {

    private Long id;
    private Long archiveId;
    private Integer sortNo;
    private String quoteNote;
}
