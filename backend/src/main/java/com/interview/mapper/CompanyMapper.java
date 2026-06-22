package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.domain.po.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 公司信息数据访问层 */
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {

    /**
     * 获取热门公司（按职位总浏览量降序）
     */
    @Select("SELECT c.id, c.name, c.industry, c.scale, c.type, c.city, c.address, " +
            "COUNT(j.id) AS job_count, IFNULL(SUM(j.view_count), 0) AS view_count " +
            "FROM company c LEFT JOIN job j ON c.id = j.company_id AND j.deleted = 0 " +
            "WHERE c.deleted = 0 " +
            "GROUP BY c.id " +
            "ORDER BY view_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectHotCompanies(int limit);
}
