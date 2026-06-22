package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 职位信息数据访问层 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {

    /**
     * 随机获取指定数量的在线职位（排除当前职位）
     */
    @Select("SELECT * FROM job WHERE status = 'active' AND user_id IS NOT NULL AND id != #{currentJobId} ORDER BY RAND() LIMIT #{limit}")
    List<Job> randomJobs(@Param("currentJobId") Long currentJobId, @Param("limit") int limit);

    /**
     * 根据ID列表查询职位（包含已删除的，绕过 @TableLogic）
     */
    @Select("<script>SELECT * FROM job WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Job> selectByIdsIncludeDeleted(@Param("ids") List<Long> ids);
}
