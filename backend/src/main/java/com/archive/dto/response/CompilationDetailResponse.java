package com.archive.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CompilationDetailResponse extends CompilationResponse {

    private String dateRangeText;
    private String keywords;
    private String summary;
    private String contentHtml;
    private Long generatedFileAttachmentId;
    private Long generatedArchiveId;
    private List<CompilationMaterialResponse> materials;
}
