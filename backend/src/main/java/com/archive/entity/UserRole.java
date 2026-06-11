package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("user_roles")
public class UserRole {

    private Long userId;
    private Short roleId;
    private OffsetDateTime createdAt;
}
