package com.archive.mapper;

import com.archive.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志 Mapper。
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
