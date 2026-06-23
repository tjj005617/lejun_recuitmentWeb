package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.KgCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识图谱大类 Mapper
 */
@Mapper
public interface KgCategoryMapper extends BaseMapper<KgCategory> {
}
