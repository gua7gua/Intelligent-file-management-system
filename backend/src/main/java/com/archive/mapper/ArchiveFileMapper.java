package com.archive.mapper;

import com.archive.entity.ArchiveFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArchiveFileMapper extends BaseMapper<ArchiveFile> {

    /** 已存储电子文件总字节（概览存储用量取数）。 */
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM archive_files")
    long sumFileSize();
}
