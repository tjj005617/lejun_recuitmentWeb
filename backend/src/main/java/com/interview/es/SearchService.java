package com.interview.es;

/**
 * 搜索服务接口
 */
public interface SearchService {

    /**
     * 职位搜索（bool复合查询）
     */
    SearchResult<JobDocument> searchJobs(JobSearchDTO dto);

    /**
     * 公司搜索（bool复合查询）
     */
    SearchResult<CompanyDocument> searchCompanies(CompanySearchDTO dto);

    /**
     * 全量同步职位数据到ES
     */
    void syncAllJobs();

    /**
     * 全量同步公司数据到ES
     */
    void syncAllCompanies();

    /**
     * 同步单个职位到ES
     */
    void syncJob(Long jobId);

    /**
     * 同步单个公司到ES
     */
    void syncCompany(Long companyId);

    /**
     * 从ES删除职位
     */
    void deleteJob(Long jobId);

    /**
     * 从ES删除公司
     */
    void deleteCompany(Long companyId);
}
