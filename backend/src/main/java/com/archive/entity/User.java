package com.archive.entity;

import com.archive.enums.DataScope;
import com.archive.enums.UserStatus;
import com.archive.enums.UserType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    @com.baomidou.mybatisplus.annotation.EnumValue
    private UserType userType;

    private String loginName;
    private String employeeNo;
    private String phone;
    private String passwordHash;
    private String realName;
    private Long organizationId;
    private String departmentName;
    private Integer maxSecurityLevel;

    @com.baomidou.mybatisplus.annotation.EnumValue
    private DataScope dataScope;

    @com.baomidou.mybatisplus.annotation.EnumValue
    private UserStatus status;

    private OffsetDateTime deletedAt;
}
