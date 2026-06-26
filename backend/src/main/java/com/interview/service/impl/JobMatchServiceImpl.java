package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.Company;
import com.interview.domain.po.Job;
import com.interview.domain.po.Resume;
import com.interview.mapper.CompanyMapper;
import com.interview.mapper.JobCategoryMapper;
import com.interview.mapper.JobMapper;
import com.interview.mapper.ResumeMapper;
import com.interview.service.AIService;
import com.interview.service.EmbeddingService;
import com.interview.service.JobMatchService;
import com.interview.service.MilvusService;
import com.interview.vo.JobMatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 岗位匹配服务实现
 * 核心流程：简历 → AI摘要 → Embedding → Milvus ANN搜索 → MySQL补全详情 → 返回匹配结果
 */
@Slf4j
@Service
public class JobMatchServiceImpl implements JobMatchService {

    private final ResumeMapper resumeMapper;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final AIService aiService;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;

    public JobMatchServiceImpl(ResumeMapper resumeMapper, JobMapper jobMapper,
                               CompanyMapper companyMapper, JobCategoryMapper jobCategoryMapper,
                               AIService aiService, EmbeddingService embeddingService,
                               MilvusService milvusService) {
        this.resumeMapper = resumeMapper;
        this.jobMapper = jobMapper;
        this.companyMapper = companyMapper;
        this.jobCategoryMapper = jobCategoryMapper;
        this.aiService = aiService;
        this.embeddingService = embeddingService;
        this.milvusService = milvusService;
    }

    @Override
    public List<JobMatchResult> matchJobs(Long resumeId, int topN, Map<String, String> filters) {
        log.info("开始岗位匹配: resumeId={}, topN={}", resumeId, topN);

        // 1. 获取简历
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new RuntimeException("简历不存在: " + resumeId);
        }

        // 2. AI 生成简历摘要
        String resumeSummary = aiService.generateResumeSummary(resume);
        log.debug("简历摘要: {}", resumeSummary.substring(0, Math.min(100, resumeSummary.length())));

        // 3. Embedding 转向量
        List<Float> queryVector = embeddingService.embed(resumeSummary);

        // 4. Milvus 向量搜索
        List<Long> matchedJobIds = milvusService.searchSimilarJobs(queryVector, filters, topN);

        if (matchedJobIds.isEmpty()) {
            log.info("未找到匹配的岗位");
            return List.of();
        }

        // 5. MySQL 补全岗位详情
        return buildMatchResults(matchedJobIds);
    }

    @Override
    public void syncJobToMilvus(Long jobId) {
        log.info("同步岗位到 Milvus: jobId={}", jobId);

        Job job = jobMapper.selectById(jobId);
        if (job == null || !"active".equals(job.getStatus())) {
            // 岗位不存在或非 active 状态，从 Milvus 删除
            milvusService.deleteJobVectors(List.of(jobId));
            log.debug("岗位非 active，已从 Milvus 删除: jobId={}", jobId);
            return;
        }

        // AI 生成岗位摘要
        String jobSummary = aiService.generateJobSummary(job);

        // Embedding 转向量
        List<Float> vector = embeddingService.embed(jobSummary);

        // 插入/更新到 Milvus
        milvusService.upsertJobVectors(
                List.of(jobId),
                List.of(vector),
                List.of(job.getCategoryId()),
                List.of(job.getCity() != null ? job.getCity() : ""),
                List.of(job.getStatus())
        );

        log.info("岗位同步完成: jobId={}, 维度={}", jobId, vector.size());
    }

    @Override
    public void syncAllJobsToMilvus() {
        log.info("开始全量同步岗位到 Milvus");

        // 查询所有 active 岗位
        List<Job> activeJobs = jobMapper.selectList(
                new LambdaQueryWrapper<Job>()
                        .eq(Job::getStatus, "active")
                        .isNotNull(Job::getUserId)
        );

        log.info("待同步岗位数量: {}", activeJobs.size());

        int successCount = 0;
        int failCount = 0;

        for (Job job : activeJobs) {
            try {
                syncJobToMilvus(job.getId());
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("同步岗位失败: jobId={}, error={}", job.getId(), e.getMessage());
            }
        }

        log.info("全量同步完成: 成功={}, 失败={}", successCount, failCount);
    }

    @Override
    public void deleteJobFromMilvus(Long jobId) {
        milvusService.deleteJobVectors(List.of(jobId));
        log.info("已从 Milvus 删除岗位向量: jobId={}", jobId);
    }

    /**
     * 根据岗位 ID 列表构建匹配结果（补全公司名、分类名等详情）
     */
    private List<JobMatchResult> buildMatchResults(List<Long> jobIds) {
        // 批量查询岗位
        List<Job> jobs = jobMapper.selectBatchIds(jobIds);
        if (jobs.isEmpty()) {
            return List.of();
        }

        // 建立 jobId -> Job 映射
        Map<Long, Job> jobMap = jobs.stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        // 批量查询公司
        Set<Long> companyIds = jobs.stream()
                .map(Job::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Company> companyMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            companyMapper.selectBatchIds(companyIds).forEach(c -> companyMap.put(c.getId(), c));
        }

        // 批量查询分类名
        Set<Long> categoryIds = jobs.stream()
                .map(Job::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            // 通过 SQL 查询分类名
            for (Long catId : categoryIds) {
                try {
                    var cat = jobCategoryMapper.selectById(catId);
                    if (cat != null) {
                        categoryMap.put(catId, cat.getName());
                    }
                } catch (Exception e) {
                    // 忽略查询失败的分类
                }
            }
        }

        // 构建结果（保持 Milvus 返回的排序）
        List<JobMatchResult> results = new ArrayList<>();
        for (Long jobId : jobIds) {
            Job job = jobMap.get(jobId);
            if (job == null) continue;

            JobMatchResult result = new JobMatchResult();
            result.setJobId(job.getId());
            result.setTitle(job.getTitle());
            result.setCity(job.getCity());
            result.setSalaryMin(job.getSalaryMin());
            result.setSalaryMax(job.getSalaryMax());
            result.setExperience(job.getExperience());
            result.setEducation(job.getEducation());
            result.setDescription(job.getDescription() != null
                    ? job.getDescription().substring(0, Math.min(200, job.getDescription().length()))
                    : "");

            // 补全公司信息
            Company company = companyMap.get(job.getCompanyId());
            if (company != null) {
                result.setCompanyName(company.getName());
                result.setCompanyLogo(company.getLogo());
            }

            // 补全分类名
            result.setCategoryName(categoryMap.get(job.getCategoryId()));

            results.add(result);
        }

        return results;
    }
}
