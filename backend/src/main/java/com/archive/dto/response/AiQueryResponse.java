package com.archive.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiQueryResponse {
    private String ruleType;
    private JsonNode conditions;
    private JsonNode rawJson;
}
