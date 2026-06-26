package com.interview.controller;

import com.interview.common.Result;
import com.interview.service.JobMatchService;
import com.interview.vo.JobMatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位匹配控制器
 * 提供简历-岗位智能匹配 API
 */
@Slf4j
@RestController
@RequestMapping("/api/job-match")
public class JobMatchController {

    private final JobMatchService jobMatchService;

    public JobMatchController(JobMatchService jobMatchService) {
        this.jobMatchService = jobMatchService;
    }

    /**
     * 根据简历匹配岗位
     * POST /api/job-match/resume/{resumeId}
     *
     * @param resumeId 简历 ID
     * @param topN 返回数量（默认10）
     * @param city 城市过滤（可选）
     * @param categoryId 分类过滤（可选）
     */
    @PostMapping("/resume/{resumeId}")
    public Result<Map<String, Object>> matchJobs(
            @PathVariable Long resumeId,
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String categoryId) {

        log.info("岗位匹配请求: resumeId={}, topN={}, city={}, categoryId={}",
                resumeId, topN, city, categoryId);

        // 构建过滤条件
        Map<String, String> filters = new HashMap<>();
        if (city != null && !city.isEmpty()) {
            filters.put("city", city);
        }
        if (categoryId != null && !categoryId.isEmpty()) {
            filters.put("categoryId", categoryId);
        }

        List<JobMatchResult> matches = jobMatchService.matchJobs(resumeId, topN, filters);

        Map<String, Object> data = new HashMap<>();
        data.put("matches", matches);
        data.put("total", matches.size());

        return Result.ok(data);
    }

    /**
     * 同步单个岗位到 Milvus
     * POST /api/job-match/sync/{jobId}
     */
    @PostMapping("/sync/{jobId}")
    public Result<String> syncJob(@PathVariable Long jobId) {
        log.info("同步岗位到 Milvus: jobId={}", jobId);
        jobMatchService.syncJobToMilvus(jobId);
        return Result.ok("同步成功");
    }

    /**
     * 全量同步所有 active 岗位到 Milvus
     * POST /api/job-match/sync-all
     */
    @PostMapping("/sync-all")
    public Result<String> syncAllJobs() {
        log.info("全量同步岗位到 Milvus");
        // 异步执行，避免超时
        new Thread(() -> {
            try {
                jobMatchService.syncAllJobsToMilvus();
            } catch (Exception e) {
                log.error("全量同步失败", e);
            }
        }).start();
        return Result.ok("全量同步已启动，请查看日志");
    }

    /**
     * 从 Milvus 删除岗位向量
     * POST /api/job-match/delete/{jobId}
     */
    @PostMapping("/delete/{jobId}")
    public Result<String> deleteJob(@PathVariable Long jobId) {
        log.info("从 Milvus 删除岗位: jobId={}", jobId);
        jobMatchService.deleteJobFromMilvus(jobId);
        return Result.ok("删除成功");
    }
}
