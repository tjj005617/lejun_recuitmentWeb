package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Region;
import org.apache.ibatis.annotations.Mapper;

/**
 * 省市区 Mapper
 */
@Mapper
public interface RegionMapper extends BaseMapper<Region> {
}
