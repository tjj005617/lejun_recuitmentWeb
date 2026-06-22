package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Resume;
import org.apache.ibatis.annotations.Mapper;

/** 简历数据访问层 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
