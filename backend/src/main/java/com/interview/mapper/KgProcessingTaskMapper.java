package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.KgProcessingTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识图谱处理任务 Mapper
 */
@Mapper
public interface KgProcessingTaskMapper extends BaseMapper<KgProcessingTask> {
}
