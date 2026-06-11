package com.archive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 服务配置属性。
 * 对应 application.yml 中 ai.* 前缀的配置项。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 是否启用 AI 功能。关闭后隐藏所有 AI 入口，不影响核心业务流程。
     */
    private boolean enabled = false;

    /**
     * AI API 基础 URL（OpenAI 兼容接口）。
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * API Key，通过环境变量注入，不写入代码仓库。
     */
    private String apiKey = "";

    /**
     * 调用模型名称。
     */
    private String model = "deepseek-chat";

    /**
     * 连接超时（秒）。
     */
    private int connectTimeout = 5;

    /**
     * 读取超时（秒）。
     */
    private int readTimeout = 15;

    /**
     * 检索类 JSON 校验失败时最大重试次数。
     */
    private int searchMaxRetries = 3;

    /**
     * 批处理默认每批条目数。
     */
    private int batchSize = 50;
}
