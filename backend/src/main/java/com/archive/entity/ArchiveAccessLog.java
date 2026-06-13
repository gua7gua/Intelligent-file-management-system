package com.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 档案访问日志。记录公众/内部对档案元数据的查看、文件预览、文件下载。
 */
@Data
@TableName("archive_access_logs")
public class ArchiveAccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户 ID；未登录公众为 null */
    private Long userId;
    /** 用户类型：internal / public / anonymous */
    private String userType;
    /** 档案 ID */
    private Long archiveId;
    /** 档案文件 ID；元数据查看时为 null */
    private Long archiveFileId;
    /** 访问类型：view_metadata / preview / download */
    private String accessType;
    /** IP 地址（写入 PG INET） */
    private String ipAddress;
    /** 访问时间 */
    private OffsetDateTime accessedAt;
    /** 创建时间（数据库 DEFAULT now()） */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private OffsetDateTime createdAt;
    /** 更新时间（数据库 DEFAULT now()） */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
