package com.interview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.interview.domain.po.Company;
import com.interview.domain.po.CompanyBenefit;
import com.interview.domain.po.BenefitTag;
import com.interview.domain.po.Job;
import com.interview.domain.po.User;
import com.interview.domain.vo.CompanyListVO;
import com.interview.mapper.CompanyBenefitMapper;
import com.interview.mapper.CompanyMapper;
import com.interview.mapper.BenefitTagMapper;
import com.interview.mapper.JobMapper;
import com.interview.mapper.UserMapper;
import com.interview.mapper.UserCompanyFollowMapper;
import com.interview.domain.po.UserCompanyFollow;
import com.interview.service.CompanyService;
import com.interview.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公司服务实现类
 * 公司与HR为1:N关系，一个公司可有多个HR，HR通过 user.company_id 关联公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;
    private final CompanyBenefitMapper companyBenefitMapper;
    private final BenefitTagMapper benefitTagMapper;
    private final JobMapper jobMapper;
    private final UserMapper userMapper;
    private final UserCompanyFollowMapper userCompanyFollowMapper;
    private final RegionService regionService;

    /**
     * 释放HR在旧公司负责的职位（user_id置为NULL，等待其他HR认领）
     */
    private void releaseJobsFromCompany(Long userId, Long oldCompanyId) {
        jobMapper.update(null,
            new LambdaUpdateWrapper<Job>()
                .eq(Job::getUserId, userId)
                .eq(Job::getCompanyId, oldCompanyId)
                .set(Job::getUserId, null)
        );
        log.info("已释放HR在旧公司的职位: userId={}, companyId={}", userId, oldCompanyId);
    }

    /**
     * 保存公司福利（从名称列表写入中间表）
     */
    private void saveCompanyBenefitsInternal(Long companyId, List<String> benefitNames) {
        // 先删旧关联
        companyBenefitMapper.delete(
            new LambdaQueryWrapper<CompanyBenefit>()
                .eq(CompanyBenefit::getCompanyId, companyId)
        );
        if (benefitNames == null || benefitNames.isEmpty()) {
            return;
        }
        // 按名称查询标签ID
        List<BenefitTag> tags = benefitTagMapper.selectList(
            new LambdaQueryWrapper<BenefitTag>()
                .in(BenefitTag::getName, benefitNames)
        );
        Map<String, Long> nameToId = tags.stream()
            .collect(Collectors.toMap(BenefitTag::getName, BenefitTag::getId));
        // 插入中间表
        for (String name : benefitNames) {
            Long tagId = nameToId.get(name);
            if (tagId != null) {
                CompanyBenefit cb = new CompanyBenefit();
                cb.setCompanyId(companyId);
                cb.setBenefitTagId(tagId);
                companyBenefitMapper.insert(cb);
            }
        }
    }

    /**
     * 查询公司福利名称列表
     */
    @Override
    public List<String> getCompanyBenefitNames(Long companyId) {
        List<CompanyBenefit> list = companyBenefitMapper.selectList(
            new LambdaQueryWrapper<CompanyBenefit>()
                .eq(CompanyBenefit::getCompanyId, companyId)
        );
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = list.stream()
            .map(CompanyBenefit::getBenefitTagId)
            .collect(Collectors.toList());
        List<BenefitTag> tags = benefitTagMapper.selectBatchIds(tagIds);
        return tags.stream().map(BenefitTag::getName).collect(Collectors.toList());
    }

    /**
     * 批量查询多个公司的福利名称列表
     */
    private Map<Long, List<String>> batchGetCompanyBenefits(List<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CompanyBenefit> list = companyBenefitMapper.selectList(
            new LambdaQueryWrapper<CompanyBenefit>()
                .in(CompanyBenefit::getCompanyId, companyIds)
        );
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = list.stream()
            .map(CompanyBenefit::getBenefitTagId)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, String> tagMap = benefitTagMapper.selectBatchIds(tagIds).stream()
            .collect(Collectors.toMap(BenefitTag::getId, BenefitTag::getName));

        Map<Long, List<String>> result = new HashMap<>();
        for (CompanyBenefit cb : list) {
            String name = tagMap.get(cb.getBenefitTagId());
            if (name != null) {
                result.computeIfAbsent(cb.getCompanyId(), k -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    /**
     * 保存公司信息：有id则更新，无id则新建
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Company saveCompany(Long userId, Company company) {
        User currentUser = userMapper.selectById(userId);

        if (company.getId() != null) {
            // 编辑已有公司
            companyMapper.updateById(company);

            // 如果公司ID发生了变化（切换公司），释放旧公司职位并更新关联
            if (currentUser != null && !company.getId().equals(currentUser.getCompanyId())) {
                if (currentUser.getCompanyId() != null) {
                    releaseJobsFromCompany(userId, currentUser.getCompanyId());
                }
                User updateUser = new User();
                updateUser.setId(userId);
                updateUser.setCompanyId(company.getId());
                userMapper.updateById(updateUser);
                log.info("HR切换公司: userId={}, oldCompanyId={}, newCompanyId={}",
                    userId, currentUser.getCompanyId(), company.getId());
            }
            log.info("公司更新成功: id={}, name={}", company.getId(), company.getName());
        } else {
            // 创建新公司前，释放旧公司负责的职位
            if (currentUser != null && currentUser.getCompanyId() != null) {
                releaseJobsFromCompany(userId, currentUser.getCompanyId());
            }

            // 创建新公司
            companyMapper.insert(company);
            // 回写 user 表的 companyId
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setCompanyId(company.getId());
            userMapper.updateById(updateUser);
            log.info("公司创建成功: id={}, name={}, userId={}", company.getId(), company.getName(), userId);
        }
        return company;
    }

    /**
     * 保存公司福利（供 Controller 调用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCompanyBenefits(Long companyId, List<String> benefitNames) {
        saveCompanyBenefitsInternal(companyId, benefitNames);
    }

    /**
     * HR选择已有公司（1:N关联，不创建新记录）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Company selectCompany(Long userId, Long companyId) {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new RuntimeException("公司不存在");
        }

        // 切换公司前，释放旧公司负责的职位
        User currentUser = userMapper.selectById(userId);
        if (currentUser != null && currentUser.getCompanyId() != null
                && !currentUser.getCompanyId().equals(companyId)) {
            releaseJobsFromCompany(userId, currentUser.getCompanyId());
        }

        // 更新 user 表的 companyId
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setCompanyId(companyId);
        userMapper.updateById(updateUser);
        log.info("HR选择已有公司: userId={}, companyId={}", userId, companyId);
        return company;
    }

    /**
     * 根据ID获取公司详情
     */
    @Override
    public Company getCompanyById(Long id) {
        return companyMapper.selectById(id);
    }

    /**
     * 获取HR所属公司（通过 user.company_id 查询）
     */
    @Override
    public Company getCompanyByUserId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCompanyId() == null) {
            return null;
        }
        Company company = companyMapper.selectById(user.getCompanyId());
        if (company != null) {
            // 填充完整省市区路径
            String regionPath = regionService.getFullPath(company.getRegionId());
            company.setRegionPath(regionPath != null ? regionPath : company.getCity());
        }
        return company;
    }

    /**
     * 获取公司下的职位列表
     */
    @Override
    public List<Job> getCompanyJobs(Long companyId, String keyword, String city,
                                     String jobType, String experience, String education) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, companyId)
                .isNotNull(Job::getUserId)       // 只展示有人认领的岗位
                .eq(Job::getStatus, "active")     // 只展示活跃岗位
                .orderByDesc(Job::getPublishedAt);

        // 关键词模糊搜索（标题/描述）
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                .like(Job::getTitle, kw)
                .or().like(Job::getDescription, kw)
            );
        }
        if (city != null && !city.isEmpty()) {
            wrapper.eq(Job::getCity, city);
        }
        if (jobType != null && !jobType.isEmpty()) {
            wrapper.eq(Job::getJobType, jobType);
        }
        if (experience != null && !experience.isEmpty()) {
            wrapper.eq(Job::getExperience, experience);
        }
        if (education != null && !education.isEmpty()) {
            wrapper.eq(Job::getEducation, education);
        }

        return jobMapper.selectList(wrapper);
    }

    /**
     * Company → CompanyListVO 转换（填充省市区路径 + 福利列表）
     */
    private CompanyListVO toCompanyListVO(Company c) {
        return toCompanyListVO(c, null);
    }

    /**
     * Company → CompanyListVO 转换（可复用已查到的福利列表）
     */
    private CompanyListVO toCompanyListVO(Company c, List<String> benefits) {
        CompanyListVO vo = BeanUtil.copyProperties(c, CompanyListVO.class);
        String regionPath = regionService.getFullPath(c.getRegionId());
        vo.setRegionPath(regionPath != null ? regionPath : c.getCity());
        vo.setBenefits(benefits != null ? benefits : getCompanyBenefitNames(c.getId()));
        return vo;
    }

    /**
     * 按名称模糊搜索公司（返回精简字段 + 省市区路径）
     */
    @Override
    public List<CompanyListVO> searchCompanies(String keyword) {
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Company::getName, keyword.trim());
        }
        wrapper.orderByDesc(Company::getId);
        wrapper.last("LIMIT 20");

        List<Company> companies = companyMapper.selectList(wrapper);
        List<Long> companyIds = companies.stream().map(Company::getId).collect(Collectors.toList());
        Map<Long, List<String>> benefitsMap = batchGetCompanyBenefits(companyIds);
        return companies.stream()
            .map(c -> toCompanyListVO(c, benefitsMap.getOrDefault(c.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
    }

    /**
     * 分页获取公司列表（支持关键词搜索）
     */
    @Override
    public List<CompanyListVO> listCompanies(String keyword, int page, int size) {
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Company::getName, keyword.trim());
        }
        wrapper.orderByDesc(Company::getId);
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        List<Company> companies = companyMapper.selectList(wrapper);
        List<Long> companyIds = companies.stream().map(Company::getId).collect(Collectors.toList());
        Map<Long, List<String>> benefitsMap = batchGetCompanyBenefits(companyIds);
        return companies.stream()
            .map(c -> toCompanyListVO(c, benefitsMap.getOrDefault(c.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
    }

    /**
     * 获取热门公司（按职位数降序）
     */
    @Override
    public List<CompanyListVO> getHotCompanies(int limit) {
        List<Map<String, Object>> rawList = companyMapper.selectHotCompanies(limit);
        List<Long> companyIds = rawList.stream()
            .map(item -> item.get("id") != null ? ((Number) item.get("id")).longValue() : null)
            .filter(id -> id != null)
            .collect(Collectors.toList());
        Map<Long, List<String>> benefitsMap = batchGetCompanyBenefits(companyIds);

        return rawList.stream().map(item -> {
            CompanyListVO vo = new CompanyListVO();
            Long id = item.get("id") != null ? ((Number) item.get("id")).longValue() : null;
            vo.setId(id);
            vo.setName((String) item.get("name"));
            vo.setIndustry((String) item.get("industry"));
            vo.setScale((String) item.get("scale"));
            vo.setType((String) item.get("type"));
            vo.setCity((String) item.get("city"));
            vo.setAddress((String) item.get("address"));
            vo.setDescription((String) item.get("description"));
            vo.setBenefits(id != null ? benefitsMap.getOrDefault(id, Collections.emptyList()) : Collections.emptyList());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 增加公司浏览量
     */
    @Override
    public void increaseViewCount(Long companyId) {
        companyMapper.update(null,
            new LambdaUpdateWrapper<Company>()
                .eq(Company::getId, companyId)
                .setSql("view_count = view_count + 1")
        );
    }

    /**
     * 关注/取消关注公司
     */
    @Override
    public boolean toggleFollow(Long userId, Long companyId) {
        LambdaQueryWrapper<UserCompanyFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCompanyFollow::getUserId, userId)
               .eq(UserCompanyFollow::getCompanyId, companyId);
        UserCompanyFollow existing = userCompanyFollowMapper.selectOne(wrapper);

        if (existing != null) {
            // 已关注，取消关注
            userCompanyFollowMapper.deleteById(existing.getId());
            // 关注数 -1
            companyMapper.update(null,
                new LambdaUpdateWrapper<Company>()
                    .eq(Company::getId, companyId)
                    .setSql("follow_count = GREATEST(follow_count - 1, 0)")
            );
            return false;
        } else {
            // 未关注，添加关注
            UserCompanyFollow follow = new UserCompanyFollow();
            follow.setUserId(userId);
            follow.setCompanyId(companyId);
            userCompanyFollowMapper.insert(follow);
            // 关注数 +1
            companyMapper.update(null,
                new LambdaUpdateWrapper<Company>()
                    .eq(Company::getId, companyId)
                    .setSql("follow_count = follow_count + 1")
            );
            return true;
        }
    }

    /**
     * 检查是否已关注
     */
    @Override
    public boolean isFollowed(Long userId, Long companyId) {
        LambdaQueryWrapper<UserCompanyFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCompanyFollow::getUserId, userId)
               .eq(UserCompanyFollow::getCompanyId, companyId);
        return userCompanyFollowMapper.selectCount(wrapper) > 0;
    }

    /**
     * 获取用户关注的公司列表
     */
    @Override
    public List<CompanyListVO> getUserFollowedCompanies(Long userId) {
        // 查询用户关注的所有公司ID
        LambdaQueryWrapper<UserCompanyFollow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(UserCompanyFollow::getUserId, userId)
                     .orderByDesc(UserCompanyFollow::getCreatedAt);
        List<UserCompanyFollow> follows = userCompanyFollowMapper.selectList(followWrapper);

        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询公司信息
        List<Long> companyIds = follows.stream()
            .map(UserCompanyFollow::getCompanyId)
            .collect(Collectors.toList());
        List<Company> companies = companyMapper.selectBatchIds(companyIds);

        // 批量查询福利
        Map<Long, List<String>> benefitsMap = batchGetCompanyBenefits(companyIds);

        // 转换为VO并保持关注顺序
        Map<Long, Company> companyMap = companies.stream()
            .collect(Collectors.toMap(Company::getId, c -> c));
        return follows.stream()
            .map(f -> companyMap.get(f.getCompanyId()))
            .filter(c -> c != null)
            .map(c -> toCompanyListVO(c, benefitsMap.getOrDefault(c.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
    }
}
