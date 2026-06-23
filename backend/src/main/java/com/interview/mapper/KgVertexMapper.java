package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.KgVertex;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识图谱顶点 Mapper
 */
@Mapper
public interface KgVertexMapper extends BaseMapper<KgVertex> {
}
