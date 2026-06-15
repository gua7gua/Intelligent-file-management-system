package com.archive.dto.request;

import com.archive.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizationQuery extends PageRequest {
    private String orgType;
    private String status;
    private String keyword;
}
