package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Message;
import org.apache.ibatis.annotations.Mapper;

/** 消息数据访问层 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
