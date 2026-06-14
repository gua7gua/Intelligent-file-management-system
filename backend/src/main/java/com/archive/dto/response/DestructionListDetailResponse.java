package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class DestructionListDetailResponse {

    private Long id;
    private String listNo;
    private String listName;
    private Long appraisalBatchId;
    private String status;
    private Long approvalRequestId;
    private String approvalStatus;
    private String destroyMethod;
    private String supervisorName1;
    private String supervisorName2;
    private String destroyNote;
    private OffsetDateTime destroyedAt;

    private List<ItemView> items;
    private List<PhotoView> photos;

    @Data
    public static class ItemView {
        private Long archiveId;
        private String archiveNoSnapshot;
        private String titleSnapshot;
        private String categorySnapshot;
        private Integer pageCountSnapshot;
        private String retentionSnapshot;
        private Integer securityLevelSnapshot;
        private String appraisalOpinionSnapshot;
        private String fileDeleteStatus;
    }

    @Data
    public static class PhotoView {
        private Long id;
        private String originalFilename;
        private String mimeType;
        private Long fileSize;
        private String sha256;
    }
}
