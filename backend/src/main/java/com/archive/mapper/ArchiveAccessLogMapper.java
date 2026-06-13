package com.archive.mapper;

import com.archive.entity.ArchiveAccessLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ArchiveAccessLogMapper extends BaseMapper<ArchiveAccessLog> {

    /**
     * 查询某用户最近查阅的档案（每个 archive_id 取最近一次，去重，取前 10）。
     * 返回每条：archiveId、archiveNo、title、accessedAt。
     */
    @Select("""
            SELECT DISTINCT ON (a.id) a.id AS "archiveId", a.archive_no AS "archiveNo",
                   a.title AS "title", l.accessed_at AS "accessedAt"
            FROM archive_access_logs l JOIN archives a ON a.id = l.archive_id
            WHERE l.user_id = #{userId} AND l.access_type = 'view_metadata'
            ORDER BY a.id, l.accessed_at DESC
            LIMIT 10
            """)
    List<Map<String, Object>> findRecentViews(@Param("userId") Long userId);

    /**
     * 写入一条访问日志。ip_address 用 ::inet cast 写入 PG INET。
     */
    @Insert("""
            INSERT INTO archive_access_logs (user_id, user_type, archive_id, archive_file_id,
                access_type, ip_address, accessed_at)
            VALUES (#{userId}, #{userType}, #{archiveId}, #{fileId}, #{accessType}, #{ip}::inet, #{at})
            """)
    void insertAccessLog(@Param("userId") Long userId,
                         @Param("userType") String userType,
                         @Param("archiveId") Long archiveId,
                         @Param("fileId") Long fileId,
                         @Param("accessType") String accessType,
                         @Param("ip") String ip,
                         @Param("at") OffsetDateTime at);
}
