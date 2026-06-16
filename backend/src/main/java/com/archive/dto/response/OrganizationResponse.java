package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class OrganizationResponse {
    private Long id;
    private String orgName;
    private String orgType;
    private String contactName;
    private String contactPhone;
    private String status;
    private OffsetDateTime createdAt;
    /** 关联全宗数（前端据此判断显示「删除」还是「停用」） */
    private long fondsCount;
    /** 关联用户数 */
    private long userCount;
}
