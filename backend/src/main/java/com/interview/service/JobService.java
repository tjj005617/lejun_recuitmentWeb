package com.interview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.domain.po.Job;
import com.interview.domain.vo.JobFavoriteVO;
import com.interview.domain.vo.JobListVO;

import java.util.List;

/**
 * 职位服务接口
 */
public interface JobService {

    /**
     * 发布职位
     */
    Job publishJob(Long userId, Job job);

    /**
     * 编辑职位
     */
    Job updateJob(Long userId, Long jobId, Job job);

    /**
     * 删除职位（软删除）
     */
    void deleteJob(Long userId, Long jobId);

    /**
     * 获取职位详情（浏览量+1）
     */
    Job getJobDetail(Long jobId);

    /**
     * 搜索职位（分页+多条件）
     */
    Page<JobListVO> searchJobs(String keyword, String city,
                                Integer salaryMin, Integer salaryMax,
                                String jobType, Long categoryId,
                                String experience, String education,
                                int page, int size, String sort);

    /**
     * 获取热门职位
     */
    List<JobListVO> getHotJobs(int limit);

    /**
     * 获取推荐职位（AI匹配，暂返回空列表）
     */
    List<JobListVO> getRecommendJobs(Long userId);

    /**
     * 获取公司下的职位列表（分页+条件搜索，含负责HR信息）
     */
    Page<JobListVO> getMyJobs(Long companyId, String keyword, String city, String status, int page, int size);

    /**
     * HR认领职位（将无负责人的职位分配给自己）
     */
    void claimJob(Long userId, Long jobId);

    /**
     * 切换职位收藏状态
     */
    boolean toggleFavorite(Long userId, Long jobId);

    /**
     * 查询当前用户是否已收藏该职位
     */
    boolean isFavorited(Long userId, Long jobId);

    /**
     * 获取用户收藏的职位列表
     */
    List<JobFavoriteVO> getMyFavorites(Long userId);

    /**
     * 猜你喜欢（随机推荐3个职位）
     */
    List<JobListVO> getGuessYouLike(Long currentJobId);

    /**
     * 保存职位福利（通过福利标签名称列表）
     */
    void saveJobBenefits(Long jobId, List<String> benefitNames);

    /**
     * 查询职位福利名称列表
     */
    List<String> getJobBenefitNames(Long jobId);
}
