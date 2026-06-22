package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.JobCategory;
import org.apache.ibatis.annotations.Mapper;

/** 职位分类数据访问层 */
@Mapper
public interface JobCategoryMapper extends BaseMapper<JobCategory> {
}
