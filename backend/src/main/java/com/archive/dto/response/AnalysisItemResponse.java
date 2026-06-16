package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 研判异常项响应（§20.6/20.7）。
 *
 * <p>保留 issueType/issueDetail（既有字段），同时补齐前端研判页所需的展示字段
 * archiveNo/title/problemType/problemDesc/suggestedAction/handleNote/handledBy。
 * problemType 与 issueType 同值（前端用 problemType）。</p>
 */
@Data
public class AnalysisItemResponse {

    private Long id;
    private Long taskId;
    private Long archiveId;
    private String issueType;
    private Map<String, Object> issueDetail;
    private Map<String, Object> suggestion;
    private String status;
    private OffsetDateTime handledAt;

    /** 档号（来自 archives.archive_no）。 */
    private String archiveNo;
    /** 题名（来自 archives.title）。 */
    private String title;
    /** 前端用 problemType；与 issueType 同值。 */
    private String problemType;
    /** 问题人类可读描述。 */
    private String problemDesc;
    /** 建议动作。 */
    private String suggestedAction;
    /** 处理备注（前端 handleAnalysisItem.note 暂未存，留空）。 */
    private String handleNote;
    /** 处理人 id。 */
    private Long handledBy;
}
