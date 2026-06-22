package com.interview.controller;

import com.interview.common.Result;
import com.interview.es.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索控制器（ES）
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 职位搜索
     */
    @GetMapping("/jobs")
    public Result<SearchResult<JobDocument>> searchJobs(JobSearchDTO dto) {
        return Result.ok(searchService.searchJobs(dto));
    }

    /**
     * 公司搜索
     */
    @GetMapping("/companies")
    public Result<SearchResult<CompanyDocument>> searchCompanies(CompanySearchDTO dto) {
        return Result.ok(searchService.searchCompanies(dto));
    }

    /**
     * 手动触发全量同步（开发/测试用）
     */
    @PostMapping("/sync")
    public Result<String> syncAll() {
        searchService.syncAllJobs();
        searchService.syncAllCompanies();
        return Result.ok("全量同步完成");
    }

    /**
     * 同步单个职位
     */
    @PostMapping("/sync/job/{jobId}")
    public Result<String> syncJob(@PathVariable Long jobId) {
        searchService.syncJob(jobId);
        return Result.ok("职位同步完成");
    }

    /**
     * 同步单个公司
     */
    @PostMapping("/sync/company/{companyId}")
    public Result<String> syncCompany(@PathVariable Long companyId) {
        searchService.syncCompany(companyId);
        return Result.ok("公司同步完成");
    }
}
