package com.archive.mapper;

import com.archive.entity.StagingFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 暂存电子文件 Mapper。
 */
@Mapper
public interface StagingFileMapper extends BaseMapper<StagingFile> {
}
