package com.archive.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 确认条目入库字段（9.6）。
 */
@Data
public class ItemConfirmationRequest {

    private String confirmedTitle;

    private String confirmedResponsibleText;

    private LocalDate confirmedFormedDate;

    private Integer confirmedCategoryId;

    private List<String> confirmedTags;
}
