package com.archive.mapper;

import com.archive.entity.ArchiveAccessLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
