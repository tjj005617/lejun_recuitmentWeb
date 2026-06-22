package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.RoleType;
import org.apache.ibatis.annotations.Mapper;

/** 角色类型数据访问层 */
@Mapper
public interface RoleTypeMapper extends BaseMapper<RoleType> {
}
