package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 标签字典。
 */
@Data
@TableName("tags")
public class Tag {

    private Long id;

    private String tagName;

    private OffsetDateTime createdAt;
}
