package com.interview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.domain.po.Application;
import com.interview.domain.po.BenefitTag;
import com.interview.domain.po.Company;
import com.interview.domain.po.Favorite;
import com.interview.domain.po.Job;
import com.interview.domain.po.JobBenefit;
import com.interview.domain.po.JobCategory;
import com.interview.domain.po.User;
import com.interview.domain.vo.JobFavoriteVO;
import com.interview.domain.vo.JobListVO;
import com.interview.mapper.ApplicationMapper;
import com.interview.mapper.BenefitTagMapper;
import com.interview.mapper.CompanyMapper;
import com.interview.mapper.FavoriteMapper;
import com.interview.mapper.JobBenefitMapper;
import com.interview.mapper.JobCategoryMapper;
import com.interview.mapper.JobMapper;
import com.interview.mapper.UserMapper;
import com.interview.es.SearchService;
import com.interview.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位服务实现类
 */
@Slf4j
@Service
public class JobServiceImpl implements JobService {

    private final JobMapper jobMapper;
    private final JobBenefitMapper jobBenefitMapper;
    private final BenefitTagMapper benefitTagMapper;
    private final CompanyMapper companyMapper;
    private final FavoriteMapper favoriteMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final UserMapper userMapper;
    private final ApplicationMapper applicationMapper;
    private final SearchService searchService;

    public JobServiceImpl(JobMapper jobMapper,
                          JobBenefitMapper jobBenefitMapper,
                          BenefitTagMapper benefitTagMapper,
                          CompanyMapper companyMapper,
                          FavoriteMapper favoriteMapper,
                          JobCategoryMapper jobCategoryMapper,
                          UserMapper userMapper,
                          ApplicationMapper applicationMapper,
                          @Lazy SearchService searchService) {
        this.jobMapper = jobMapper;
        this.jobBenefitMapper = jobBenefitMapper;
        this.benefitTagMapper = benefitTagMapper;
        this.companyMapper = companyMapper;
        this.favoriteMapper = favoriteMapper;
        this.jobCategoryMapper = jobCategoryMapper;
        this.userMapper = userMapper;
        this.applicationMapper = applicationMapper;
        this.searchService = searchService;
    }

    /**
     * 保存职位福利（从名称列表写入中间表）
     */
    private void saveJobBenefitsInternal(Long jobId, List<String> benefitNames) {
        // 先删旧关联
        jobBenefitMapper.delete(
            new LambdaQueryWrapper<JobBenefit>()
                .eq(JobBenefit::getJobId, jobId)
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
                JobBenefit jb = new JobBenefit();
                jb.setJobId(jobId);
                jb.setBenefitTagId(tagId);
                jobBenefitMapper.insert(jb);
            }
        }
    }

    /**
     * 查询职位福利名称列表
     */
    @Override
    public List<String> getJobBenefitNames(Long jobId) {
        List<JobBenefit> list = jobBenefitMapper.selectList(
            new LambdaQueryWrapper<JobBenefit>()
                .eq(JobBenefit::getJobId, jobId)
        );
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = list.stream()
            .map(JobBenefit::getBenefitTagId)
            .collect(Collectors.toList());
        List<BenefitTag> tags = benefitTagMapper.selectBatchIds(tagIds);
        return tags.stream().map(BenefitTag::getName).collect(Collectors.toList());
    }

    /**
     * 批量查询多个职位的福利名称列表
     */
    private Map<Long, List<String>> batchGetJobBenefits(List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<JobBenefit> list = jobBenefitMapper.selectList(
            new LambdaQueryWrapper<JobBenefit>()
                .in(JobBenefit::getJobId, jobIds)
        );
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = list.stream()
            .map(JobBenefit::getBenefitTagId)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, String> tagMap = benefitTagMapper.selectBatchIds(tagIds).stream()
            .collect(Collectors.toMap(BenefitTag::getId, BenefitTag::getName));

        Map<Long, List<String>> result = new HashMap<>();
        for (JobBenefit jb : list) {
            String name = tagMap.get(jb.getBenefitTagId());
            if (name != null) {
                result.computeIfAbsent(jb.getJobId(), k -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    /**
     * 保存职位福利（供 Controller 调用）
     */
    @Override
    public void saveJobBenefits(Long jobId, List<String> benefitNames) {
        saveJobBenefitsInternal(jobId, benefitNames);
    }

    /**
     * 发布职位
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Job publishJob(Long userId, Job job) {
        // 通过 user.company_id 查找HR所属公司（1:N关系）
        User user = userMapper.selectById(userId);
        if (user == null || user.getCompanyId() == null) {
            throw new RuntimeException("请先创建公司信息");
        }
        Company company = companyMapper.selectById(user.getCompanyId());
        if (company == null) {
            throw new RuntimeException("请先创建公司信息");
        }

        job.setCompanyId(company.getId());
        job.setUserId(userId);
        job.setStatus("active");
        job.setViewCount(0);
        job.setApplyCount(0);
        job.setPublishedAt(LocalDateTime.now());
        jobMapper.insert(job);

        // 同步到ES
        try {
            searchService.syncJob(job.getId());
        } catch (Exception e) {
            log.warn("ES同步职位失败(发布): jobId={}", job.getId(), e);
        }

        log.info("职位发布成功: id={}, title={}, userId={}", job.getId(), job.getTitle(), userId);
        return job;
    }

    /**
     * 校验HR是否属于指定公司
     */
    private void checkCompanyMembership(Long userId, Long companyId) {
        User user = userMapper.selectById(userId);
        if (user == null || !companyId.equals(user.getCompanyId())) {
            throw new RuntimeException("无权操作该公司的职位");
        }
    }

    /**
     * 编辑职位（同一公司HR可互相编辑）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Job updateJob(Long userId, Long jobId, Job job) {
        Job existing = jobMapper.selectById(jobId);
        if (existing == null) {
            throw new RuntimeException("职位不存在");
        }
        // 验证HR属于职位所在公司
        checkCompanyMembership(userId, existing.getCompanyId());

        job.setId(jobId);
        jobMapper.updateById(job);

        // 同步到ES
        try {
            searchService.syncJob(jobId);
        } catch (Exception e) {
            log.warn("ES同步职位失败(更新): jobId={}", jobId, e);
        }

        log.info("职位更新成功: id={}, title={}", jobId, job.getTitle());
        return jobMapper.selectById(jobId);
    }

    /**
     * 删除职位（同一公司HR可互相删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(Long userId, Long jobId) {
        Job existing = jobMapper.selectById(jobId);
        if (existing == null) {
            throw new RuntimeException("职位不存在");
        }
        // 验证HR属于职位所在公司
        checkCompanyMembership(userId, existing.getCompanyId());

        // 软删除职位
        jobMapper.deleteById(jobId);

        // 同步标记关联的投递记录为职位已删除
        applicationMapper.update(null,
            new LambdaUpdateWrapper<Application>()
                .eq(Application::getJobId, jobId)
                .set(Application::getJobDeleted, 1)
        );

        // 同步标记关联的收藏记录为职位已删除
        favoriteMapper.update(null,
            new LambdaUpdateWrapper<Favorite>()
                .eq(Favorite::getJobId, jobId)
                .set(Favorite::getJobDeleted, 1)
        );

        // 从ES删除
        try {
            searchService.deleteJob(jobId);
        } catch (Exception e) {
            log.warn("ES删除职位失败: jobId={}", jobId, e);
        }

        log.info("职位删除成功: id={}, title={}", jobId, existing.getTitle());
    }

    /**
     * 获取职位详情（浏览量+1）
     */
    @Override
    public Job getJobDetail(Long jobId) {
        // 原子递增浏览量，避免并发问题
        jobMapper.update(null,
            new LambdaUpdateWrapper<Job>()
                .eq(Job::getId, jobId)
                .setSql("view_count = view_count + 1")
        );
        return jobMapper.selectById(jobId);
    }

    /**
     * 搜索职位（分页+多条件筛选+排序）
     */
    @Override
    public Page<JobListVO> searchJobs(String keyword, String city,
                                       Integer salaryMin, Integer salaryMax,
                                       String jobType, Long categoryId,
                                       String experience, String education,
                                       int page, int size, String sort) {
        Page<Job> jobPage = new Page<>(page, size);

        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getStatus, "active");
        wrapper.isNotNull(Job::getUserId);

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Job::getTitle, keyword);
        }
        if (StringUtils.hasText(city)) {
            wrapper.eq(Job::getCity, city);
        }
        if (salaryMin != null) {
            wrapper.ge(Job::getSalaryMax, salaryMin);
        }
        if (salaryMax != null) {
            wrapper.le(Job::getSalaryMin, salaryMax);
        }
        if (StringUtils.hasText(jobType)) {
            wrapper.eq(Job::getJobType, jobType);
        }
        if (categoryId != null) {
            List<Long> categoryIds = new ArrayList<>();
            categoryIds.add(categoryId);
            List<JobCategory> children = jobCategoryMapper.selectList(
                new LambdaQueryWrapper<JobCategory>()
                    .eq(JobCategory::getParentId, categoryId)
            );
            for (JobCategory child : children) {
                categoryIds.add(child.getId());
            }
            wrapper.in(Job::getCategoryId, categoryIds);
        }
        if (StringUtils.hasText(experience)) {
            wrapper.eq(Job::getExperience, experience);
        }
        if (StringUtils.hasText(education)) {
            wrapper.eq(Job::getEducation, education);
        }

        if ("salary_desc".equals(sort)) {
            wrapper.orderByDesc(Job::getSalaryMax);
        } else if ("view_count_desc".equals(sort)) {
            wrapper.orderByDesc(Job::getViewCount);
        } else {
            wrapper.orderByDesc(Job::getPublishedAt);
        }

        Page<Job> result = jobMapper.selectPage(jobPage, wrapper);

        // 批量查询公司名称
        Map<Long, String> companyNameMap = batchGetCompanyNames(result.getRecords());

        // 转换为 VO
        Page<JobListVO> responsePage = new Page<>(page, size, result.getTotal());
        List<JobListVO> records = result.getRecords().stream().map(job -> {
            JobListVO vo = BeanUtil.copyProperties(job, JobListVO.class);
            vo.setCompanyName(companyNameMap.get(job.getCompanyId()));
            return vo;
        }).collect(Collectors.toList());
        responsePage.setRecords(records);

        return responsePage;
    }

    /**
     * 批量查询公司名称，返回 companyId → name 映射
     */
    private Map<Long, String> batchGetCompanyNames(List<Job> jobs) {
        Set<Long> companyIds = jobs.stream()
            .map(Job::getCompanyId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return companyMapper.selectBatchIds(companyIds).stream()
            .collect(Collectors.toMap(Company::getId, Company::getName));
    }

    /**
     * 获取热门职位（按浏览量降序）
     */
    @Override
    public List<JobListVO> getHotJobs(int limit) {
        Page<Job> page = new Page<>(1, limit);
        Page<Job> result = jobMapper.selectPage(page,
            new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, "active")
                .isNotNull(Job::getUserId)
                .orderByDesc(Job::getViewCount)
        );

        Map<Long, String> companyNameMap = batchGetCompanyNames(result.getRecords());

        return result.getRecords().stream().map(job -> {
            JobListVO vo = BeanUtil.copyProperties(job, JobListVO.class);
            vo.setCompanyName(companyNameMap.get(job.getCompanyId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取推荐职位（AI匹配，暂返回空列表，后续迭代）
     */
    @Override
    public List<JobListVO> getRecommendJobs(Long userId) {
        return Collections.emptyList();
    }

    /**
     * 获取公司下的职位列表（分页+条件搜索，含负责HR信息）
     */
    @Override
    public Page<JobListVO> getMyJobs(Long companyId, String keyword, String city, String status, int page, int size) {
        // 构建查询条件
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
            .eq(Job::getCompanyId, companyId)
            .like(StringUtils.hasText(keyword), Job::getTitle, keyword)
            .eq(StringUtils.hasText(city), Job::getCity, city)
            .eq(StringUtils.hasText(status), Job::getStatus, status)
            .orderByDesc(Job::getPublishedAt);

        // 分页查询
        Page<Job> pageParam = new Page<>(page, size);
        Page<Job> jobPage = jobMapper.selectPage(pageParam, wrapper);

        if (jobPage.getRecords().isEmpty()) {
            Page<JobListVO> emptyPage = new Page<>(page, size, 0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 批量查询负责HR信息
        Set<Long> userIds = jobPage.getRecords().stream()
            .map(Job::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为VO
        List<JobListVO> voList = jobPage.getRecords().stream().map(job -> {
            JobListVO vo = BeanUtil.copyProperties(job, JobListVO.class);
            User hr = userMap.get(job.getUserId());
            vo.setHrName(hr != null ? (hr.getNickname() != null ? hr.getNickname() : hr.getUsername()) : null);
            return vo;
        }).collect(Collectors.toList());

        Page<JobListVO> result = new Page<>(page, size, jobPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    /**
     * HR认领职位（将无负责人的职位分配给自己）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimJob(Long userId, Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("职位不存在");
        }
        // 验证HR属于职位所在公司
        checkCompanyMembership(userId, job.getCompanyId());
        // 只有没有负责HR的职位才能认领
        if (job.getUserId() != null) {
            throw new RuntimeException("该职位已有负责HR，无法认领");
        }
        Job update = new Job();
        update.setId(jobId);
        update.setUserId(userId);
        jobMapper.updateById(update);
        log.info("HR认领职位成功: jobId={}, userId={}", jobId, userId);
    }

    /**
     * 切换职位收藏状态（true=已收藏，false=已取消）
     * 收藏/取消收藏主要看 deleted 字段，取消=软删除，收藏=恢复或新增
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long userId, Long jobId) {
        // 用自定义SQL查询，包含已软删除的记录（绕过 @TableLogic）
        Favorite existing = favoriteMapper.findAny(userId, jobId);

        if (existing != null) {
            if (existing.getDeleted() == 0) {
                // 当前已收藏 → 取消（软删除）
                favoriteMapper.deleteById(existing.getId());
                log.info("取消收藏: userId={}, jobId={}", userId, jobId);
                return false;
            } else {
                // 存在已取消的记录 → 恢复（直接更新 deleted=0，绕过 @TableLogic）
                favoriteMapper.restoreById(existing.getId());
                log.info("恢复收藏: userId={}, jobId={}", userId, jobId);
                return true;
            }
        }

        // 没有任何记录 → 新增收藏，显式设置 deleted=0 避免 NULL
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setJobId(jobId);
        favorite.setDeleted(0);
        favoriteMapper.insert(favorite);
        log.info("添加收藏: userId={}, jobId={}, deleted={}", userId, jobId, favorite.getDeleted());
        return true;
    }

    /**
     * 查询当前用户是否已收藏该职位
     * 只看 deleted=0 的记录（@TableLogic 自动过滤）
     */
    @Override
    public boolean isFavorited(Long userId, Long jobId) {
        if (userId == null) return false;
        // 用自定义SQL查询所有记录（包括已删除的），检查 deleted 字段
        Favorite any = favoriteMapper.findAny(userId, jobId);
        log.info("isFavorited check: userId={}, jobId={}, found={}, deleted={}",
            userId, jobId, any != null, any != null ? any.getDeleted() : "null");
        if (any == null) return false;
        return any.getDeleted() == null || any.getDeleted() == 0;
    }

    /**
     * 获取用户收藏的职位列表
     */
    @Override
    public List<JobFavoriteVO> getMyFavorites(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreatedAt)
        );

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询所有职位（包含已删除的，用于显示原始信息）
        Set<Long> jobIds = favorites.stream()
            .map(Favorite::getJobId)
            .collect(Collectors.toSet());
        Map<Long, Job> jobMap = jobMapper.selectByIdsIncludeDeleted(new ArrayList<>(jobIds)).stream()
            .collect(Collectors.toMap(Job::getId, j -> j));

        // 批量查询公司名称
        Set<Long> companyIds = jobMap.values().stream()
            .map(Job::getCompanyId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? Collections.emptyMap() :
            companyMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));

        return favorites.stream().map(fav -> {
            JobFavoriteVO vo = new JobFavoriteVO();
            Job job = jobMap.get(fav.getJobId());
            if (job != null) {
                BeanUtil.copyProperties(job, vo);
                Company company = companyMap.get(job.getCompanyId());
                if (company != null) {
                    vo.setCompanyName(company.getName());
                }
                // 通过 @TableLogic 的 deleted 字段判断是否已删除
                vo.setJobDeleted(job.getDeleted() != null && job.getDeleted() == 1);
            } else {
                // 职位彻底不存在
                vo.setId(fav.getJobId());
                vo.setTitle("该职位已删除");
                vo.setJobDeleted(true);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 猜你喜欢（随机推荐3个职位）
     */
    @Override
    public List<JobListVO> getGuessYouLike(Long currentJobId) {
        List<Job> jobs = jobMapper.randomJobs(currentJobId, 3);
        if (jobs.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> companyNameMap = batchGetCompanyNames(jobs);
        return jobs.stream().map(job -> {
            JobListVO vo = BeanUtil.copyProperties(job, JobListVO.class);
            vo.setCompanyName(companyNameMap.get(job.getCompanyId()));
            return vo;
        }).collect(Collectors.toList());
    }
}
