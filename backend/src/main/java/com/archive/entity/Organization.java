package com.archive.entity;

import com.archive.enums.OrgType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("organizations")
public class Organization extends BaseEntity {

    private String orgName;

    @com.baomidou.mybatisplus.annotation.EnumValue
    private OrgType orgType;

    private String contactName;
    private String contactPhone;
    private String status;
    private OffsetDateTime deletedAt;
}
