package com.archive.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FondsUpdateRequest {
    @Size(max = 200, message = "全宗名称长度不能超过 200")
    private String fondsName;
    private Long organizationId;
    @Size(max = 1000, message = "备注长度不能超过 1000")
    private String description;
    /** active / disabled；为空则不修改 */
    private String status;
}
