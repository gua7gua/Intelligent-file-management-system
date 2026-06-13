package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 档案-标签关系，复合主键。
 */
@Data
@TableName("archive_tags")
public class ArchiveTag {

    private Long archiveId;

    private Long tagId;

    private OffsetDateTime createdAt;
}
