package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.config.AiProperties;
import com.archive.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI HTTP 客户端。
 * 调用 OpenAI 兼容的 /v1/chat/completions 接口，提取 <JSON> 标签内容。
 * 超时、重试和降级由上层 Service 控制，AiClient 只负责单次调用。
 */
@Slf4j
@Service
public class AiClient {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(aiProperties.getConnectTimeout()))
                .readTimeout(Duration.ofSeconds(aiProperties.getReadTimeout()))
                .build();
    }

    /**
     * 调用 AI 接口并提取 <JSON> 标签中的结构化内容。
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return 解析后的 JSON 节点
     * @throws BusinessException AI 不可用、调用失败或 JSON 格式不合法时抛出
     */
    public JsonNode callAndExtractJson(String systemPrompt, String userMessage) {
        if (!aiProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 功能未启用");
        }

        String rawContent = callAiApi(systemPrompt, userMessage);
        String jsonStr = extractJsonBlock(rawContent);
        return parseJson(jsonStr);
    }

    /**
     * 检查 AI 是否可用。
     */
    public boolean isAvailable() {
        return aiProperties.isEnabled() && aiProperties.getApiKey() != null
                && !aiProperties.getApiKey().isBlank();
    }

    /**
     * 获取检索类最大重试次数。
     */
    public int getSearchMaxRetries() {
        return aiProperties.getSearchMaxRetries();
    }

    /**
     * 获取批处理默认批大小。
     */
    public int getBatchSize() {
        return aiProperties.getBatchSize();
    }

    /**
     * 调用 OpenAI 兼容的 /v1/chat/completions 接口，返回 AI 文本内容。
     */
    private String callAiApi(String systemPrompt, String userMessage) {
        String url = aiProperties.getBaseUrl().replaceAll("/+$", "") + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("AI API 返回非 2xx 状态: {}", response.getStatusCode());
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 服务响应异常");
            }
            return extractContentFromResponse(response.getBody());
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("AI API 调用失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 服务不可用，请稍后重试");
        }
    }

    /**
     * 从 OpenAI 兼容响应体中提取 assistant message content。
     */
    private String extractContentFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
            log.error("AI 响应格式异常，无法提取 content: {}", responseBody);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 返回内容格式异常");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 AI 响应失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 返回内容解析失败");
        }
    }

    /**
     * 从 AI 文本内容中提取 JSON 字符串。
     * 优先匹配 &lt;JSON&gt;...&lt;/JSON&gt; 包裹；若缺失（部分模型如 DeepSeek 常省略包裹），
     * 回退取第一个 '{' 到最后一个 '}' 之间的裸 JSON。
     */
    private String extractJsonBlock(String content) {
        int start = content.indexOf("<JSON>");
        int end = content.indexOf("</JSON>");
        if (start >= 0 && end > start) {
            return content.substring(start + "<JSON>".length(), end).trim();
        }
        // 容错：回退到裸 JSON（首 '{' 到末 '}'）
        int braceStart = content.indexOf('{');
        int braceEnd = content.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return content.substring(braceStart, braceEnd + 1).trim();
        }
        log.error("AI 返回内容未包含有效的 JSON: {}", content);
        throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 返回内容未包含有效的 JSON 块");
    }

    /**
     * 将提取的 JSON 字符串解析为 JsonNode。
     */
    private JsonNode parseJson(String jsonStr) {
        try {
            return objectMapper.readTree(jsonStr);
        } catch (Exception e) {
            log.error("AI 返回的 JSON 解析失败: {}, 原始内容: {}", e.getMessage(), jsonStr);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 返回的 JSON 格式不合法");
        }
    }
}
