package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 系统配置项。 */
@Data
@TableName("system_configs")
public class SystemConfig {

    private Long id;
    private String configKey;
    private String configValue;

    /** string / number / boolean / json。 */
    private String valueType;

    private String description;
    private Boolean editable;
}
