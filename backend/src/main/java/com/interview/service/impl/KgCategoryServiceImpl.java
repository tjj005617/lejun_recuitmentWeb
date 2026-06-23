package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.KgCategory;
import com.interview.mapper.KgCategoryMapper;
import com.interview.service.KgCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识图谱大类服务实现
 */
@Service
@RequiredArgsConstructor
public class KgCategoryServiceImpl implements KgCategoryService {

    private final KgCategoryMapper categoryMapper;

    @Override
    public List<KgCategory> listEnabled() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<KgCategory>()
                        .eq(KgCategory::getStatus, "active")
                        .eq(KgCategory::getDeleted, 0)
                        .orderByAsc(KgCategory::getSortOrder)
        );
    }

    @Override
    public List<KgCategory> listAll() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<KgCategory>()
                        .eq(KgCategory::getDeleted, 0)
                        .orderByAsc(KgCategory::getSortOrder)
        );
    }

    @Override
    public KgCategory getById(Long id) {
        return categoryMapper.selectById(id);
    }
}
