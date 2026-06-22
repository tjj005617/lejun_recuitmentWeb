package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.JobBenefit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位-福利关联 Mapper
 */
@Mapper
public interface JobBenefitMapper extends BaseMapper<JobBenefit> {
}
