package com.interview.service;

import com.interview.domain.po.Company;
import com.interview.domain.po.Job;
import com.interview.domain.vo.CompanyListVO;

import java.util.List;

/**
 * 公司服务接口
 */
public interface CompanyService {

    /**
     * 保存公司信息（有则更新，无则创建）
     * @param userId HR用户ID
     * @param company 公司信息
     * @return 保存后的公司
     */
    Company saveCompany(Long userId, Company company);

    /**
     * HR选择已有公司（1:N关联）
     * @param userId HR用户ID
     * @param companyId 已有公司ID
     * @return 公司信息
     */
    Company selectCompany(Long userId, Long companyId);

    /**
     * 根据ID获取公司详情
     */
    Company getCompanyById(Long id);

    /**
     * 获取HR所属公司（通过 user.company_id）
     */
    Company getCompanyByUserId(Long userId);

    /**
     * 获取公司下的职位列表（支持筛选）
     */
    List<Job> getCompanyJobs(Long companyId, String keyword, String city,
                             String jobType, String experience, String education);

    /**
     * 按名称模糊搜索公司
     * @param keyword 关键词
     * @return 公司精简信息列表
     */
    List<CompanyListVO> searchCompanies(String keyword);

    /**
     * 分页获取公司列表（支持关键词搜索）
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 公司精简信息列表
     */
    List<CompanyListVO> listCompanies(String keyword, int page, int size);

    /**
     * 获取热门公司（按职位数降序）
     * @param limit 返回数量
     * @return 公司列表
     */
    List<CompanyListVO> getHotCompanies(int limit);

    /**
     * 增加公司浏览量
     * @param companyId 公司ID
     */
    void increaseViewCount(Long companyId);

    /**
     * 关注/取消关注公司
     * @param userId 用户ID
     * @param companyId 公司ID
     * @return true=已关注, false=已取消关注
     */
    boolean toggleFollow(Long userId, Long companyId);

    /**
     * 检查是否已关注
     * @param userId 用户ID
     * @param companyId 公司ID
     * @return true=已关注
     */
    boolean isFollowed(Long userId, Long companyId);

    /**
     * 获取用户关注的公司列表
     * @param userId 用户ID
     * @return 关注的公司列表
     */
    List<CompanyListVO> getUserFollowedCompanies(Long userId);

    /**
     * 保存公司福利（通过福利标签名称列表）
     * @param companyId 公司ID
     * @param benefitNames 福利标签名称列表
     */
    void saveCompanyBenefits(Long companyId, List<String> benefitNames);

    /**
     * 查询公司福利名称列表
     * @param companyId 公司ID
     * @return 福利标签名称列表
     */
    List<String> getCompanyBenefitNames(Long companyId);
}
