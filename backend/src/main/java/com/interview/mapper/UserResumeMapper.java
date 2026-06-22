package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.UserResume;
import org.apache.ibatis.annotations.Mapper;

/** 用户-简历关联数据访问层 */
@Mapper
public interface UserResumeMapper extends BaseMapper<UserResume> {
}
