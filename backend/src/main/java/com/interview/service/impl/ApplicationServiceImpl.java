package com.interview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.Application;
import com.interview.domain.po.Company;
import com.interview.domain.po.Job;
import com.interview.domain.po.Message;
import com.interview.domain.po.User;
import com.interview.domain.vo.ApplicationCompanyVO;
import com.interview.domain.vo.ApplicationDetailVO;
import com.interview.domain.vo.ApplicationMyVO;
import com.interview.mapper.ApplicationMapper;
import com.interview.mapper.CompanyMapper;
import com.interview.mapper.JobMapper;
import com.interview.mapper.UserMapper;
import com.interview.domain.doc.ChatMessage;
import com.interview.mq.ChatMessageProducer;
import com.interview.service.ApplicationService;
import com.interview.service.ChatService;
import com.interview.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应聘申请服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final ChatService chatService;
    private final ChatMessageProducer chatMessageProducer;

    /**
     * 投递简历（校验重复投递，支持撤回后重新投递）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Application applyJob(Long userId, Long jobId, Long resumeId) {
        // 校验职位是否存在
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("职位不存在");
        }

        // 查询是否已投递（包含已撤回的记录，绕过 @TableLogic）
        Application existing = applicationMapper.findAnyByUserAndJob(userId, jobId);
        if (existing != null) {
            if (existing.getDeleted() != null && existing.getDeleted() == 1) {
                // 存在已撤回的记录 → 恢复并更新简历、状态、时间
                applicationMapper.restoreById(existing.getId());
                existing.setResumeId(resumeId);
                existing.setStatus("pending");
                existing.setAppliedAt(LocalDateTime.now());
                existing.setDeleted(0);
                applicationMapper.updateById(existing);

                // 通知负责HR
                notifyJobHR(userId, job, jobId, resumeId);

                log.info("恢复投递: userId={}, jobId={}, applicationId={}", userId, jobId, existing.getId());
                return existing;
            }
            // 存在未撤回的记录 → 重复投递
            throw new RuntimeException("您已投递过该职位");
        }

        // 创建新申请
        Application application = new Application();
        application.setJobId(jobId);
        application.setUserId(userId);
        application.setResumeId(resumeId);
        application.setStatus("pending");
        application.setAppliedAt(LocalDateTime.now());
        applicationMapper.insert(application);

        // 更新职位投递数
        jobMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Job>()
                .eq(Job::getId, jobId)
                .setSql("apply_count = apply_count + 1")
        );

        // 通知负责HR
        notifyJobHR(userId, job, jobId, resumeId);

        log.info("简历投递成功: userId={}, jobId={}, resumeId={}", userId, jobId, resumeId);
        return application;
    }

    /**
     * 撤回申请（求职者只能撤回自己的申请）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawApplication(Long userId, Long applicationId) {
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!application.getUserId().equals(userId)) {
            throw new RuntimeException("无权撤回该申请");
        }

        applicationMapper.deleteById(applicationId);

        // 同步删除 MongoDB 中的投递聊天消息
        chatService.deleteApplicationMessage(userId, application.getJobId());

        log.info("申请撤回: id={}, userId={}", applicationId, userId);
    }

    /**
     * 获取我的投递记录（join job + company 取标题和公司名）
     */
    @Override
    public List<ApplicationMyVO> getMyApplications(Long userId) {
        List<Application> applications = applicationMapper.selectList(
            new LambdaQueryWrapper<Application>()
                .eq(Application::getUserId, userId)
                .orderByDesc(Application::getAppliedAt)
        );

        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询职位信息（@TableLogic会过滤已删除记录，null表示职位已删除）
        Set<Long> jobIds = applications.stream()
            .map(Application::getJobId)
            .collect(Collectors.toSet());
        Map<Long, Job> jobMap = jobMapper.selectBatchIds(jobIds).stream()
            .collect(Collectors.toMap(Job::getId, job -> job));

        // 批量查询公司信息
        Set<Long> companyIds = jobMap.values().stream()
            .map(Job::getCompanyId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? Collections.emptyMap() :
            companyMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));

        return applications.stream().map(app -> {
            ApplicationMyVO vo = new ApplicationMyVO();
            vo.setId(app.getId());
            vo.setJobId(app.getJobId());
            vo.setStatus(app.getStatus());
            vo.setAppliedAt(app.getAppliedAt());

            Job job = jobMap.get(app.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
                vo.setJobDeleted(false);
                Company company = companyMap.get(job.getCompanyId());
                if (company != null) {
                    vo.setCompanyName(company.getName());
                }
            } else {
                vo.setJobTitle("该职位已删除");
                vo.setJobDeleted(true);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 校验HR是否属于指定公司
     */
    private void checkCompanyMembership(Long userId, Long companyId) {
        User user = userMapper.selectById(userId);
        if (user == null || !companyId.equals(user.getCompanyId())) {
            throw new RuntimeException("无权操作该公司的数据");
        }
    }

    /**
     * 通知负责该岗位的HR：有新的投递消息
     * 同时写 MySQL 通知表 + MongoDB 聊天消息
     */
    private void notifyJobHR(Long applicantUserId, Job job, Long jobId, Long resumeId) {
        if (job.getUserId() == null) return;

        User applicant = userMapper.selectById(applicantUserId);
        String applicantName = applicant != null
            ? (applicant.getNickname() != null ? applicant.getNickname() : applicant.getUsername())
            : "未知用户";

        // 1. MySQL 通知表（导航栏红点 + SSE弹窗）
        Message notification = new Message();
        notification.setSenderId(applicantUserId);
        notification.setReceiverId(job.getUserId());
        notification.setJobId(jobId);
        notification.setResumeId(resumeId);
        notification.setType("application");
        notification.setContent(applicantName + " 投递了 " + job.getTitle());
        messageService.sendMessage(applicantUserId, notification);

        // 2. 投递到 RabbitMQ（消费者异步存 MongoDB + WebSocket 推送）
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderId(applicantUserId);
        chatMessage.setReceiverId(job.getUserId());
        chatMessage.setJobId(jobId);
        chatMessage.setType("resume");
        chatMessage.setContent("这是我的简历，希望能加入公司的大家庭");
        chatMessage.setResumeId(resumeId);
        chatMessageProducer.sendChatMessage(chatMessage);
    }

    /**
     * 获取某职位的投递列表（HR查看，同一公司可互相查看）
     */
    @Override
    public List<ApplicationDetailVO> getJobApplications(Long userId, Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("职位不存在");
        }
        checkCompanyMembership(userId, job.getCompanyId());

        List<Application> applications = applicationMapper.selectList(
            new LambdaQueryWrapper<Application>()
                .eq(Application::getJobId, jobId)
                .orderByDesc(Application::getAppliedAt)
        );

        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = applications.stream()
            .map(Application::getUserId)
            .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        return applications.stream().map(app -> {
            ApplicationDetailVO vo = BeanUtil.copyProperties(app, ApplicationDetailVO.class);
            User user = userMap.get(app.getUserId());
            if (user != null) {
                vo.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 更新投递状态（HR操作，同一公司可互相操作）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApplicationStatus(Long userId, Long applicationId, String status, String hrRemark, String rejectReason) {
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }

        // 验证HR属于职位所在公司
        Job job = jobMapper.selectById(application.getJobId());
        if (job == null) {
            throw new RuntimeException("职位不存在");
        }
        checkCompanyMembership(userId, job.getCompanyId());

        application.setStatus(status);
        if (hrRemark != null) {
            application.setHrRemark(hrRemark);
        }
        if (rejectReason != null) {
            application.setRejectReason(rejectReason);
        }
        applicationMapper.updateById(application);

        log.info("申请状态更新: id={}, status={}", applicationId, status);
    }

    /**
     * 获取公司所有职位的投递列表（HR工作台用）
     */
    @Override
    public List<ApplicationCompanyVO> getCompanyApplications(Long userId, Long companyId) {
        checkCompanyMembership(userId, companyId);

        List<Job> companyJobs = jobMapper.selectList(
            new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, companyId)
                .eq(Job::getUserId, userId)
        );
        if (companyJobs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> jobIds = companyJobs.stream()
            .map(Job::getId)
            .collect(Collectors.toSet());

        List<Application> applications = applicationMapper.selectList(
            new LambdaQueryWrapper<Application>()
                .in(Application::getJobId, jobIds)
                .orderByDesc(Application::getAppliedAt)
        );

        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Job> jobMap = companyJobs.stream()
            .collect(Collectors.toMap(Job::getId, j -> j));

        Set<Long> userIds = applications.stream()
            .map(Application::getUserId)
            .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        return applications.stream().map(app -> {
            ApplicationCompanyVO vo = BeanUtil.copyProperties(app, ApplicationCompanyVO.class);
            Job job = jobMap.get(app.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
            }
            User user = userMap.get(app.getUserId());
            if (user != null) {
                vo.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
