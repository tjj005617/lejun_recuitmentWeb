package com.interview.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.Result;
import com.interview.domain.po.Company;
import com.interview.domain.po.Job;
import com.interview.domain.po.User;
import com.interview.domain.vo.JobDetailVO;
import com.interview.domain.vo.JobListVO;
import com.interview.mapper.CompanyMapper;
import com.interview.mapper.UserMapper;
import com.interview.service.CompanyService;
import com.interview.service.JobService;
import com.interview.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位管理控制器
 */
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final CompanyService companyService;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;

    /**
     * 发布职位（HR）
     */
    @PostMapping
    public Result<?> publishJob(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Job job = buildJobFromRequest(request);
        Job saved = jobService.publishJob(userId, job);

        // 保存福利（中间表）
        saveJobBenefitsFromRequest(saved.getId(), request);

        return Result.ok(saved);
    }

    /**
     * 编辑职位（HR）
     */
    @PutMapping("/{id}")
    public Result<?> updateJob(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Job job = buildJobFromRequest(request);
        Job updated = jobService.updateJob(userId, id, job);

        // 更新福利（中间表）
        saveJobBenefitsFromRequest(id, request);

        return Result.ok(updated);
    }

    /**
     * 从请求中提取福利列表并保存到中间表
     */
    private void saveJobBenefitsFromRequest(Long jobId, Map<String, Object> request) {
        Object benefitsObj = request.get("benefits");
        if (benefitsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> benefitNames = ((List<Object>) benefitsObj).stream()
                .map(Object::toString).collect(Collectors.toList());
            jobService.saveJobBenefits(jobId, benefitNames);
        }
    }

    /**
     * 删除职位（HR）
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteJob(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        jobService.deleteJob(userId, id);
        return Result.ok("删除成功");
    }

    /**
     * 获取职位详情（公开接口，包含公司信息）
     */
    @GetMapping("/{id}")
    public Result<?> getJobDetail(@PathVariable Long id) {
        Job job = jobService.getJobDetail(id);
        if (job == null) {
            return Result.fail("职位不存在");
        }

        // 构建职位VO
        JobDetailVO vo = BeanUtil.copyProperties(job, JobDetailVO.class);

        // 加载岗位福利（中间表）
        vo.setBenefits(jobService.getJobBenefitNames(id));

        // 查询关联的公司信息
        if (job.getCompanyId() != null) {
            Company company = companyMapper.selectById(job.getCompanyId());
            if (company != null) {
                JobDetailVO.CompanyVO companyVO = BeanUtil.copyProperties(company, JobDetailVO.CompanyVO.class);
                // 加载公司福利
                companyVO.setBenefits(companyService.getCompanyBenefitNames(job.getCompanyId()));
                vo.setCompany(companyVO);
            }
        }

        // 查询负责该岗位的HR信息
        if (job.getUserId() != null) {
            User hrUser = userMapper.selectById(job.getUserId());
            if (hrUser != null) {
                JobDetailVO.HrVO hrVO = new JobDetailVO.HrVO();
                hrVO.setId(hrUser.getId());
                hrVO.setNickname(hrUser.getNickname());
                hrVO.setUsername(hrUser.getUsername());
                hrVO.setAvatar(hrUser.getAvatar());
                vo.setHr(hrVO);
            }
        }

        return Result.ok(vo);
    }

    /**
     * 搜索职位（公开接口，分页+多条件筛选）
     */
    @GetMapping("/list")
    public Result<?> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Integer salaryMax,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String education,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "published_at_desc") String sort) {

        Page<JobListVO> result = jobService.searchJobs(
            keyword, city, salaryMin, salaryMax, jobType, categoryId,
            experience, education, page, size, sort
        );

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return Result.ok(data);
    }

    /**
     * 获取热门职位（公开接口）
     */
    @GetMapping("/hot")
    public Result<?> getHotJobs() {
        return Result.ok(jobService.getHotJobs(10));
    }

    /**
     * 获取推荐职位（求职者，AI匹配）
     */
    @GetMapping("/recommend")
    public Result<?> getRecommendJobs() {
        Long userId = UserContext.getUserId();
        return Result.ok(jobService.getRecommendJobs(userId));
    }

    /**
     * 获取公司下的职位列表（分页+条件搜索，含负责HR信息）
     */
    @GetMapping("/my")
    public Result<?> getMyJobs(
            @RequestParam Long companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(jobService.getMyJobs(companyId, keyword, city, status, page, size));
    }

    /**
     * 获取当前用户收藏的职位列表
     */
    @GetMapping("/favorites")
    public Result<?> getMyFavorites() {
        Long userId = UserContext.getUserId();
        return Result.ok(jobService.getMyFavorites(userId));
    }

    /**
     * 猜你喜欢（随机推荐3个职位）
     */
    @GetMapping("/guess")
    public Result<?> getGuessYouLike(@RequestParam(required = false) Long currentId) {
        return Result.ok(jobService.getGuessYouLike(currentId));
    }

    /**
     * HR认领职位（无负责人的职位）
     */
    @PostMapping("/{id}/claim")
    public Result<?> claimJob(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        jobService.claimJob(userId, id);
        return Result.ok("认领成功");
    }

    /**
     * 切换职位收藏状态
     */
    @PostMapping("/{id}/favorite")
    public Result<?> toggleFavorite(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean favorited = jobService.toggleFavorite(userId, id);

        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favorited);
        return Result.ok(data);
    }

    /**
     * 查询当前用户是否已收藏该职位
     */
    @GetMapping("/{id}/favorite")
    public Result<?> checkFavorite(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean favorited = jobService.isFavorited(userId, id);

        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favorited);
        return Result.ok(data);
    }

    /**
     * 从请求Map构建Job对象
     */
    private Job buildJobFromRequest(Map<String, Object> request) {
        Job job = new Job();
        job.setTitle((String) request.get("title"));
        job.setCity((String) request.get("city"));
        job.setAddress((String) request.get("address"));
        job.setJobType((String) request.get("jobType"));
        job.setExperience((String) request.get("experience"));
        job.setEducation((String) request.get("education"));
        job.setDescription((String) request.get("description"));
        job.setRequirements((String) request.get("requirements"));

        if (request.get("salaryMin") != null) {
            job.setSalaryMin(Integer.valueOf(request.get("salaryMin").toString()));
        }
        if (request.get("salaryMax") != null) {
            job.setSalaryMax(Integer.valueOf(request.get("salaryMax").toString()));
        }
        if (request.get("categoryId") != null) {
            job.setCategoryId(Long.valueOf(request.get("categoryId").toString()));
        }

        return job;
    }
}
