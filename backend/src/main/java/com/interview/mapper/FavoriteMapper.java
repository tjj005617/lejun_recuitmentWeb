package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 职位收藏数据访问层 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 查询收藏记录（包含已软删除的），绕过 @TableLogic
     */
    @Select("SELECT * FROM favorite WHERE user_id = #{userId} AND job_id = #{jobId} LIMIT 1")
    Favorite findAny(@Param("userId") Long userId, @Param("jobId") Long jobId);

    /**
     * 恢复收藏（直接更新 deleted 字段），绕过 @TableLogic 的 WHERE deleted=0
     */
    @Update("UPDATE favorite SET deleted = 0, updated_at = NOW() WHERE id = #{id}")
    void restoreById(@Param("id") Long id);
}
