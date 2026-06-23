package com.interview.service;

import com.interview.domain.po.KgCategory;

import java.util.List;

/**
 * 知识图谱大类服务接口
 */
public interface KgCategoryService {

    /**
     * 获取所有启用的分类
     */
    List<KgCategory> listEnabled();

    /**
     * 获取所有分类（包括禁用的，管理员用）
     */
    List<KgCategory> listAll();

    /**
     * 根据ID获取分类
     */
    KgCategory getById(Long id);
}
