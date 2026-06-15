package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CompilationResponse {

    private Long id;
    private String compilationNo;
    private String title;
    private String compilationType;
    private String status;
    private OffsetDateTime createdAt;
}
