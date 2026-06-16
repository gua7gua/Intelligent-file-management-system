package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 编研正文附件摘要（§19.5 生成后挂在 business_attachments，attachmentType=report）。
 */
@Data
public class CompilationAttachmentResponse {

    private Long id;
    private String fileName;
    private String fileUrl;
    /** 固定 "report"。 */
    private String attachmentType;
    private OffsetDateTime generatedAt;
}
