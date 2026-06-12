package com.archive.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建/更新征集清单请求。
 */
@Data
public class CollectionBatchCreateRequest {

    @NotBlank(message = "清单标题不能为空")
    private String title;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    private String contactPhone;

    private Integer archiveYear;

    @NotEmpty(message = "至少包含一条清单条目")
    @Valid
    private List<CollectionItemRequest> items;

    /**
     * 征集清单条目。
     */
    @Data
    public static class CollectionItemRequest {

        @NotBlank(message = "档案标题不能为空")
        private String inputTitle;

        @NotBlank(message = "载体状态不能为空")
        private String carrierStatus;

        private String electronicFormat;

        private String expectedFilename;

        private LocalDate formedDate;
    }
}
