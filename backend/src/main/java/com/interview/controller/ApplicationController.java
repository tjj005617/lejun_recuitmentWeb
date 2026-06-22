package com.interview.controller;

import com.interview.common.Result;
import com.interview.domain.po.Application;
import com.interview.domain.vo.ApplicationCompanyVO;
import com.interview.domain.vo.ApplicationDetailVO;
import com.interview.domain.vo.ApplicationMyVO;
import com.interview.service.ApplicationService;
import com.interview.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 应聘申请控制器
 */
@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * 投递简历（求职者）
     */
    @PostMapping
    public Result<?> applyJob(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Long jobId = Long.valueOf(request.get("jobId").toString());
        Long resumeId = Long.valueOf(request.get("resumeId").toString());

        Application application = applicationService.applyJob(userId, jobId, resumeId);
        return Result.ok(application);
    }

    /**
     * 撤回申请（求职者）
     */
    @DeleteMapping("/{id}")
    public Result<?> withdrawApplication(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        applicationService.withdrawApplication(userId, id);
        return Result.ok("已撤回申请");
    }

    /**
     * 获取我的投递记录（求职者）
     */
    @GetMapping("/my")
    public Result<?> getMyApplications() {
        Long userId = UserContext.getUserId();
        List<ApplicationMyVO> applications = applicationService.getMyApplications(userId);
        return Result.ok(applications);
    }

    /**
     * 获取某职位的投递列表（HR）
     */
    @GetMapping("/job/{jobId}")
    public Result<?> getJobApplications(@PathVariable Long jobId) {
        Long userId = UserContext.getUserId();
        List<ApplicationDetailVO> applications = applicationService.getJobApplications(userId, jobId);
        return Result.ok(applications);
    }

    /**
     * 更新投递状态（HR）
     */
    @PutMapping("/{id}/status")
    public Result<?> updateApplicationStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        String status = (String) request.get("status");
        String hrRemark = (String) request.get("hrRemark");
        String rejectReason = (String) request.get("rejectReason");

        applicationService.updateApplicationStatus(userId, id, status, hrRemark, rejectReason);
        return Result.ok("状态更新成功");
    }

    /**
     * 获取公司所有投递列表（HR工作台用）
     */
    @GetMapping("/company/{companyId}")
    public Result<?> getCompanyApplications(@PathVariable Long companyId) {
        Long userId = UserContext.getUserId();
        List<ApplicationCompanyVO> applications = applicationService.getCompanyApplications(userId, companyId);
        return Result.ok(applications);
    }
}
