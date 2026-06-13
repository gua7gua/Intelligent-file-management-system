package com.archive.mapper;

import com.archive.entity.ArchiveTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArchiveTagMapper extends BaseMapper<ArchiveTag> {

    @Delete("DELETE FROM archive_tags WHERE archive_id = #{archiveId}")
    int deleteByArchiveId(@Param("archiveId") Long archiveId);
}
