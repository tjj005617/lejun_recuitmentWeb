package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 求职申请数据访问层 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    /**
     * 查询投递记录（包含已撤回的），绕过 @TableLogic
     */
    @Select("SELECT * FROM application WHERE user_id = #{userId} AND job_id = #{jobId} LIMIT 1")
    Application findAnyByUserAndJob(@Param("userId") Long userId, @Param("jobId") Long jobId);

    /**
     * 恢复投递记录（直接更新 deleted 字段），绕过 @TableLogic
     */
    @Update("UPDATE application SET deleted = 0, updated_at = NOW() WHERE id = #{id}")
    void restoreById(@Param("id") Long id);
}
