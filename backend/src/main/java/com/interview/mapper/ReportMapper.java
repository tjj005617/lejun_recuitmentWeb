package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Report;
import org.apache.ibatis.annotations.Mapper;

/** 面试评估报告数据访问层 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
