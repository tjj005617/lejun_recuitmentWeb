package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.InterviewQA;
import org.apache.ibatis.annotations.Mapper;

/** 面试问答记录数据访问层 */
@Mapper
public interface InterviewQAMapper extends BaseMapper<InterviewQA> {
}
