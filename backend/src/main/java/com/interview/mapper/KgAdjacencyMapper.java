package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.KgAdjacency;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邻接缓存表 Mapper
 */
@Mapper
public interface KgAdjacencyMapper extends BaseMapper<KgAdjacency> {
}
