package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FondsQuery extends PageRequest {
    private String status;
    private Long organizationId;
    private String keyword;
}
