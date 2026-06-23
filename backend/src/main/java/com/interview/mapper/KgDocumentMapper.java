package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.KgDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识图谱文档 Mapper
 */
@Mapper
public interface KgDocumentMapper extends BaseMapper<KgDocument> {
}
