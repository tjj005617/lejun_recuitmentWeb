package com.interview.service;

import com.interview.domain.po.Application;
import com.interview.domain.vo.ApplicationCompanyVO;
import com.interview.domain.vo.ApplicationDetailVO;
import com.interview.domain.vo.ApplicationMyVO;

import java.util.List;

/**
 * 应聘申请服务接口
 */
public interface ApplicationService {

    /**
     * 投递简历
     */
    Application applyJob(Long userId, Long jobId, Long resumeId);

    /**
     * 撤回申请
     */
    void withdrawApplication(Long userId, Long applicationId);

    /**
     * 获取我的投递记录
     */
    List<ApplicationMyVO> getMyApplications(Long userId);

    /**
     * 获取某职位的投递列表（HR）
     */
    List<ApplicationDetailVO> getJobApplications(Long userId, Long jobId);

    /**
     * 更新投递状态（HR）
     */
    void updateApplicationStatus(Long userId, Long applicationId, String status, String hrRemark, String rejectReason);

    /**
     * 获取公司所有职位的投递列表（HR工作台用）
     */
    List<ApplicationCompanyVO> getCompanyApplications(Long userId, Long companyId);
}
