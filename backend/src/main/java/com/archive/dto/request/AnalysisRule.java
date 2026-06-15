package com.archive.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AnalysisRule {

    private List<Integer> categoryIds;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    private Boolean includeAiSuggestion;
}
