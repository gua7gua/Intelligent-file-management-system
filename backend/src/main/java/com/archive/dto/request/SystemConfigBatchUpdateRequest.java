package com.archive.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SystemConfigBatchUpdateRequest {

    @NotEmpty(message = "配置项不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        private String configKey;
        private String configValue;
    }
}
