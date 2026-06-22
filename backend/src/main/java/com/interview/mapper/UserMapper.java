package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.User;
import org.apache.ibatis.annotations.Mapper;

/** 用户数据访问层 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
