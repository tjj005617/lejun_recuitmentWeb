package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.BenefitTag;
import com.interview.mapper.BenefitTagMapper;
import com.interview.service.BenefitTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 福利标签服务实现
 */
@Service
@RequiredArgsConstructor
public class BenefitTagServiceImpl implements BenefitTagService {

    private final BenefitTagMapper benefitTagMapper;

    /**
     * 获取所有启用的福利标签（按类型和排序权重筛选）
     */
    @Override
    public List<BenefitTag> listEnabled(String type) {
        LambdaQueryWrapper<BenefitTag> wrapper = new LambdaQueryWrapper<BenefitTag>()
            .eq(BenefitTag::getEnabled, 1)
            .orderByDesc(BenefitTag::getSortOrder)
            .orderByAsc(BenefitTag::getId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(BenefitTag::getType, type);
        }
        return benefitTagMapper.selectList(wrapper);
    }

    /**
     * 获取全部福利标签（管理员用，按类型筛选）
     */
    @Override
    public List<BenefitTag> listAll(String type) {
        LambdaQueryWrapper<BenefitTag> wrapper = new LambdaQueryWrapper<BenefitTag>()
            .orderByDesc(BenefitTag::getSortOrder)
            .orderByAsc(BenefitTag::getId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(BenefitTag::getType, type);
        }
        return benefitTagMapper.selectList(wrapper);
    }

    /**
     * 新增福利标签
     */
    @Override
    public BenefitTag save(String name, String type, Integer sortOrder) {
        Long count = benefitTagMapper.selectCount(
            new LambdaQueryWrapper<BenefitTag>()
                .eq(BenefitTag::getName, name)
                .eq(BenefitTag::getType, type)
        );
        if (count > 0) {
            throw new RuntimeException("标签名称已存在");
        }

        BenefitTag tag = new BenefitTag();
        tag.setName(name);
        tag.setType(type != null ? type : "company");
        tag.setSortOrder(sortOrder != null ? sortOrder : 0);
        tag.setEnabled(1);
        benefitTagMapper.insert(tag);
        return tag;
    }

    /**
     * 更新福利标签
     */
    @Override
    public void update(Long id, String name, Integer sortOrder, Integer enabled) {
        BenefitTag tag = benefitTagMapper.selectById(id);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }

        if (name != null && !name.equals(tag.getName())) {
            Long count = benefitTagMapper.selectCount(
                new LambdaQueryWrapper<BenefitTag>()
                    .eq(BenefitTag::getName, name)
                    .ne(BenefitTag::getId, id)
            );
            if (count > 0) {
                throw new RuntimeException("标签名称已存在");
            }
            tag.setName(name);
        }

        if (sortOrder != null) {
            tag.setSortOrder(sortOrder);
        }
        if (enabled != null) {
            tag.setEnabled(enabled);
        }

        benefitTagMapper.updateById(tag);
    }

    /**
     * 删除福利标签
     */
    @Override
    public void delete(Long id) {
        benefitTagMapper.deleteById(id);
    }

    /**
     * 根据名称列表批量查询福利标签
     */
    @Override
    public List<BenefitTag> listByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return benefitTagMapper.selectList(
            new LambdaQueryWrapper<BenefitTag>()
                .in(BenefitTag::getName, names)
        );
    }
}
