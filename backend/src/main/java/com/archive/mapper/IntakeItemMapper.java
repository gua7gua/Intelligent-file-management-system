package com.archive.mapper;

import com.archive.entity.IntakeItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IntakeItemMapper extends BaseMapper<IntakeItem> {

    @Select("SELECT COALESCE(MAX(item_no), 0) FROM intake_items WHERE batch_id = #{batchId}")
    int maxItemNo(@Param("batchId") Long batchId);
}
