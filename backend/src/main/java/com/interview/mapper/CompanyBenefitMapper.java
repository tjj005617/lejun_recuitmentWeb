package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.CompanyBenefit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公司-福利关联 Mapper
 */
@Mapper
public interface CompanyBenefitMapper extends BaseMapper<CompanyBenefit> {
}
