package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户档案 Mapper
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
