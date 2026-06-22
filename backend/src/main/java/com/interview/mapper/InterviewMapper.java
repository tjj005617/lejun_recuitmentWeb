package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Interview;
import org.apache.ibatis.annotations.Mapper;

/** 面试记录数据访问层 */
@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {
}
