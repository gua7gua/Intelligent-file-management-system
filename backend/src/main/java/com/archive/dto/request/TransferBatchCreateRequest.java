package com.archive.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建/更新移交清单请求。
 */
@Data
public class TransferBatchCreateRequest {

    @NotBlank(message = "清单标题不能为空")
    private String title;

    private String departmentName;

    private String contactPhone;

    private Integer archiveYear;

    private LocalDate expectedTransferDate;

    @NotEmpty(message = "至少包含一条清单条目")
    @Valid
    private List<TransferItemRequest> items;
}
