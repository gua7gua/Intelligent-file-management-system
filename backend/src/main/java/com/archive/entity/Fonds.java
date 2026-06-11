package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fonds")
public class Fonds extends BaseEntity {

    private String fondsNo;
    private String fondsName;
    private Long organizationId;
    private String description;
    private String status;
    private OffsetDateTime deletedAt;
}
