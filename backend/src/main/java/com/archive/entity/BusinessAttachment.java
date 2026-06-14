package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 业务附件（回执/签字件/协议/现场照片/报告等，多态 business_type+business_id）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("business_attachments")
public class BusinessAttachment extends BaseEntity {

    /** destruction_list / borrow_request / intake_batch 等。 */
    private String businessType;
    private Long businessId;

    /** destruction_photo / borrow_voucher_pdf 等。 */
    private String attachmentType;

    private String bucketName;
    private String objectKey;
    private String originalFilename;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String sha256;
}
