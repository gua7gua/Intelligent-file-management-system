package com.archive.dto.response;

import lombok.Data;

@Data
public class CompilationMaterialResponse {

    private Long id;
    private Long archiveId;
    private Integer sortNo;
    private String quoteNote;
    /** 素材档案档号（前端展示用）。 */
    private String archiveNo;
    /** 素材档案题名（前端展示用）。 */
    private String title;
}
