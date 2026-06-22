package com.interview.service;

import com.interview.domain.po.BenefitTag;

import java.util.List;

/**
 * 福利标签服务接口
 */
public interface BenefitTagService {

    /**
     * 获取所有启用的福利标签（按类型和排序权重筛选）
     */
    List<BenefitTag> listEnabled(String type);

    /**
     * 获取全部福利标签（管理员用，按类型筛选）
     */
    List<BenefitTag> listAll(String type);

    /**
     * 新增福利标签
     */
    BenefitTag save(String name, String type, Integer sortOrder);

    /**
     * 更新福利标签
     */
    void update(Long id, String name, Integer sortOrder, Integer enabled);

    /**
     * 删除福利标签
     */
    void delete(Long id);

    /**
     * 根据名称列表批量查询福利标签ID
     */
    List<BenefitTag> listByNames(List<String> names);
}
