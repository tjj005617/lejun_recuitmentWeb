package com.interview.service;

import com.interview.domain.po.JobCategory;

import java.util.List;
import java.util.Map;

/**
 * 职位分类服务接口
 */
public interface JobCategoryService {

    /**
     * 获取分类树形结构
     */
    List<Map<String, Object>> getCategoryTree();

    /**
     * 获取分类扁平列表
     */
    List<JobCategory> getCategoryList();
}
