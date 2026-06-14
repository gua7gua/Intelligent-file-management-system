package com.archive.dto.response;

import lombok.Data;

@Data
public class SystemConfigResponse {

    private Long id;
    private String configKey;
    /** 敏感键返回 ***。 */
    private String configValue;
    private String valueType;
    private String description;
    private Boolean editable;
}
