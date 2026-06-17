package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CompilationResponse {

    private Long id;
    private String compilationNo;
    private String title;
    private String compilationType;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    /** 入库后生成的正式档号（archived 态有值）。 */
    private String archiveNo;
    /** 入库后生成的正式档案 id（archived 态有值）。 */
    private Long archiveId;
    /** 素材引用数。 */
    private Integer materialCount;
    /** 正文附件信息（generated/archived 态有值）。 */
    private CompilationAttachmentResponse attachment;
}
