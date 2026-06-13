package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArchiveSummaryResponse {
    private Long archiveId;
    private String archiveNo;
    private String title;
    private String responsibleText;
    private Integer formedYear;
    private String categoryName;
    private String carrierStatus;
    private Boolean hasElectronicFile;
}
