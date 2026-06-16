package com.archive.mapper;

import com.archive.entity.Archive;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ArchiveMapper extends BaseMapper<Archive> {

    /** 公开档案数（与 §5.2 公开检索三条件一致）。 */
    @Select("SELECT COUNT(*) FROM archives "
            + "WHERE security_level = 0 AND open_status = 'open' AND lifecycle_status = 'normal'")
    long countPublicArchives();

    /** 关联到公开档案的电子文件数。 */
    @Select("SELECT COUNT(*) FROM archive_files af "
            + "JOIN archives a ON af.archive_id = a.id "
            + "WHERE a.security_level = 0 AND a.open_status = 'open' AND a.lifecycle_status = 'normal' "
            + "AND af.deleted_at IS NULL")
    long countPublicElectronicFiles();

    /** 指定时间点及之后有更新的公开档案数（用于「近 N 天开放」统计）。 */
    @Select("SELECT COUNT(*) FROM archives "
            + "WHERE security_level = 0 AND open_status = 'open' AND lifecycle_status = 'normal' "
            + "AND updated_at >= #{cutoff}")
    long countPublicArchivesOpenedSince(@Param("cutoff") OffsetDateTime cutoff);

    /** 公开档案按门类分组计数：返回 {category_id, cnt}。 */
    @Select("SELECT category_id, COUNT(*) AS cnt FROM archives "
            + "WHERE security_level = 0 AND open_status = 'open' AND lifecycle_status = 'normal' "
            + "GROUP BY category_id")
    List<Map<String, Object>> countPublicByCategory();
}
