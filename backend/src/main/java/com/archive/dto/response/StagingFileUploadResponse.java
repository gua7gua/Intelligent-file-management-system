package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 暂存文件上传接口返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StagingFileUploadResponse {

    private String uploadBatchNo;
    private List<StagingFileResponse> files;
    private MatchSummary matchSummary;
}
