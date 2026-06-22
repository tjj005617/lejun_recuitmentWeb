package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.JobCategory;
import com.interview.mapper.JobCategoryMapper;
import com.interview.service.JobCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位分类服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobCategoryServiceImpl implements JobCategoryService {

    private final JobCategoryMapper jobCategoryMapper;

    /**
     * 获取分类树形结构
     * 查全部分类后在内存中按 parentId 分组构建树
     */
    @Override
    public List<Map<String, Object>> getCategoryTree() {
        // 查询所有分类，按排序权重升序
        List<JobCategory> allCategories = jobCategoryMapper.selectList(
            new LambdaQueryWrapper<JobCategory>()
                .orderByAsc(JobCategory::getSortOrder)
        );

        // 按 parentId 分组
        Map<Long, List<JobCategory>> groupByParent = allCategories.stream()
            .collect(Collectors.groupingBy(c -> c.getParentId() != null ? c.getParentId() : 0L));

        // 递归构建树
        return buildTree(0L, groupByParent);
    }

    /**
     * 递归构建树形结构
     * @param parentId 父分类ID
     * @param groupByParent 按parentId分组的分类Map
     */
    private List<Map<String, Object>> buildTree(Long parentId, Map<Long, List<JobCategory>> groupByParent) {
        List<JobCategory> children = groupByParent.getOrDefault(parentId, Collections.emptyList());
        List<Map<String, Object>> tree = new ArrayList<>();

        for (JobCategory category : children) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", category.getId());
            node.put("name", category.getName());

            // 递归获取子分类
            List<Map<String, Object>> subChildren = buildTree(category.getId(), groupByParent);
            if (!subChildren.isEmpty()) {
                node.put("children", subChildren);
            }

            tree.add(node);
        }
        return tree;
    }

    /**
     * 获取分类扁平列表
     */
    @Override
    public List<JobCategory> getCategoryList() {
        return jobCategoryMapper.selectList(
            new LambdaQueryWrapper<JobCategory>()
                .orderByAsc(JobCategory::getSortOrder)
        );
    }
}
