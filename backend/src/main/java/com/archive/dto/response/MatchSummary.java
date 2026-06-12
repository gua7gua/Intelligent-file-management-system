package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 上传后文件名匹配摘要。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchSummary {

    private int matched;
    private int unmatched;
    private int duplicate;
    private int failed;
    /** 仍缺少电子文件的条目 ID 列表。 */
    private List<Long> missingItems;
}
