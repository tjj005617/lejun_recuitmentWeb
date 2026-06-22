package com.interview.es;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.interview.domain.po.Company;
import com.interview.domain.po.Job;
import com.interview.domain.po.JobCategory;
import com.interview.mapper.CompanyMapper;
import com.interview.mapper.JobCategoryMapper;
import com.interview.mapper.JobMapper;
import com.interview.service.CompanyService;
import com.interview.service.JobService;
import org.springframework.context.annotation.Lazy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索服务实现（bool复合查询 + 数据同步）
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations esOperations;
    private final JobSearchRepository jobSearchRepository;
    private final CompanySearchRepository companySearchRepository;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final CompanyService companyService;
    private final JobService jobService;

    public SearchServiceImpl(ElasticsearchOperations esOperations,
                             JobSearchRepository jobSearchRepository,
                             CompanySearchRepository companySearchRepository,
                             JobMapper jobMapper,
                             CompanyMapper companyMapper,
                             JobCategoryMapper jobCategoryMapper,
                             CompanyService companyService,
                             @Lazy JobService jobService) {
        this.esOperations = esOperations;
        this.jobSearchRepository = jobSearchRepository;
        this.companySearchRepository = companySearchRepository;
        this.jobMapper = jobMapper;
        this.companyMapper = companyMapper;
        this.jobCategoryMapper = jobCategoryMapper;
        this.companyService = companyService;
        this.jobService = jobService;
    }

    // ==================== 职位搜索 ====================

    @Override
    public SearchResult<JobDocument> searchJobs(JobSearchDTO dto) {
        // 构建bool查询
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 1. 关键词搜索（must，影响分数）
        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            String kw = dto.getKeyword().trim();
            boolBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(kw)
                    .fields("title^5.0", "description", "requirements", "companyName^0.5")
                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
            ));
        }

        // 2. 精确过滤（filter，不影响分数）
        // 默认只搜活跃职位
        boolBuilder.filter(f -> f.term(t -> t.field("status").value("active")));
        // 只搜有人认领的岗位（userId不为null）
        boolBuilder.filter(f -> f.exists(e -> e.field("userId")));

        if (dto.getCity() != null && !dto.getCity().isEmpty()) {
            boolBuilder.filter(f -> f.term(t -> t.field("city").value(dto.getCity())));
        }
        if (dto.getCategoryId() != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("categoryId").value(dto.getCategoryId())));
        }
        if (dto.getExperience() != null && !dto.getExperience().isEmpty()) {
            boolBuilder.filter(f -> f.term(t -> t.field("experience").value(dto.getExperience())));
        }
        if (dto.getEducation() != null && !dto.getEducation().isEmpty()) {
            boolBuilder.filter(f -> f.term(t -> t.field("education").value(dto.getEducation())));
        }
        if (dto.getJobType() != null && !dto.getJobType().isEmpty()) {
            boolBuilder.filter(f -> f.term(t -> t.field("jobType").value(dto.getJobType())));
        }
        if (dto.getHrUserId() != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("userId").value(dto.getHrUserId())));
        }

        // 3. 薪资范围过滤
        if (dto.getSalaryMin() != null) {
            boolBuilder.filter(f -> f.range(r -> r
                    .field("salaryMax")
                    .gte(JsonData.of(dto.getSalaryMin()))
            ));
        }
        if (dto.getSalaryMax() != null) {
            boolBuilder.filter(f -> f.range(r -> r
                    .field("salaryMin")
                    .lte(JsonData.of(dto.getSalaryMax()))
            ));
        }

        // 构建查询
        Query query = Query.of(q -> q.bool(boolBuilder.build()));

        // 使用 NativeQueryBuilder 构建完整查询
        NativeQueryBuilder nativeQueryBuilder = new NativeQueryBuilder();
        nativeQueryBuilder.withQuery(query);

        // 排序：有关键词时按相关性优先，无关键词时按指定字段排序
        String sortField = dto.getSort() != null ? dto.getSort() : "publishedAt";
        boolean hasKeyword = dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty();
        if (hasKeyword) {
            // 有关键词：先按相关性排序，再按发布时间
            nativeQueryBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
            nativeQueryBuilder.withSort(s -> s.field(f -> f.field("publishedAt").order(SortOrder.Desc)));
        } else {
            switch (sortField) {
                case "salary":
                    nativeQueryBuilder.withSort(s -> s.field(f -> f.field("salaryMax").order(SortOrder.Desc)));
                    break;
                case "viewCount":
                    nativeQueryBuilder.withSort(s -> s.field(f -> f.field("viewCount").order(SortOrder.Desc)));
                    break;
                case "applyCount":
                    nativeQueryBuilder.withSort(s -> s.field(f -> f.field("applyCount").order(SortOrder.Desc)));
                    break;
                default:
                    nativeQueryBuilder.withSort(s -> s.field(f -> f.field("publishedAt").order(SortOrder.Desc)));
                    break;
            }
        }

        // 分页
        NativeQuery nativeQuery = nativeQueryBuilder.build();
        nativeQuery.setPageable(PageRequest.of(dto.getPage() - 1, dto.getSize()));

        // 执行查询
        SearchHits<JobDocument> hits = esOperations.search(
                nativeQuery,
                JobDocument.class,
                IndexCoordinates.of("job_index")
        );

        // 转换结果
        List<JobDocument> records = hits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());

        long total = hits.getTotalHits();
        // ES默认track_total_hits上限10000，超过则为gte
        String relation = total >= 10000 ? "gte" : "eq";
        return SearchResult.of(records, total, relation, dto.getPage(), dto.getSize());
    }

    // ==================== 公司搜索 ====================

    @Override
    public SearchResult<CompanyDocument> searchCompanies(CompanySearchDTO dto) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 1. 关键词搜索
        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            String kw = dto.getKeyword().trim();
            boolBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(kw)
                    .fields("name^2.0", "description")
            ));
        }

        // 2. 精确过滤
        if (dto.getCity() != null && !dto.getCity().isEmpty()) {
            boolBuilder.filter(f -> f.term(t -> t.field("city").value(dto.getCity())));
        }
        if (dto.getIndustry() != null && !dto.getIndustry().isEmpty()) {
            boolBuilder.filter(f -> f.term(t -> t.field("industry").value(dto.getIndustry())));
        }
        if (dto.getScale() != null && !dto.getScale().isEmpty()) {
            boolBuilder.filter(f -> f.term(t -> t.field("scale").value(dto.getScale())));
        }

        Query query = Query.of(q -> q.bool(boolBuilder.build()));

        NativeQueryBuilder nativeQueryBuilder = new NativeQueryBuilder();
        nativeQueryBuilder.withQuery(query);

        // 排序
        String sortField = dto.getSort() != null ? dto.getSort() : "jobCount";
        switch (sortField) {
            case "viewCount":
                nativeQueryBuilder.withSort(s -> s.field(f -> f.field("viewCount").order(SortOrder.Desc)));
                break;
            case "followCount":
                nativeQueryBuilder.withSort(s -> s.field(f -> f.field("followCount").order(SortOrder.Desc)));
                break;
            default:
                nativeQueryBuilder.withSort(s -> s.field(f -> f.field("jobCount").order(SortOrder.Desc)));
                break;
        }

        // 分页
        NativeQuery nativeQuery = nativeQueryBuilder.build();
        nativeQuery.setPageable(PageRequest.of(dto.getPage() - 1, dto.getSize()));

        SearchHits<CompanyDocument> hits = esOperations.search(
                nativeQuery,
                CompanyDocument.class,
                IndexCoordinates.of("company_index")
        );

        List<CompanyDocument> records = hits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());

        long total = hits.getTotalHits();
        String relation = total >= 10000 ? "gte" : "eq";
        return SearchResult.of(records, total, relation, dto.getPage(), dto.getSize());
    }

    // ==================== 数据同步 ====================

    @Override
    public void syncAllJobs() {
        log.info("开始全量同步职位数据到ES...");
        List<Job> jobs = jobMapper.selectList(null);
        if (jobs.isEmpty()) {
            log.info("没有职位数据需要同步");
            return;
        }

        // 查询所有公司（用于冗余companyName）
        List<Long> companyIds = jobs.stream().map(Job::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));

        // 查询所有分类（用于冗余categoryName）
        Map<Long, JobCategory> categoryMap = jobCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(JobCategory::getId, c -> c));

        List<JobDocument> documents = jobs.stream().map(job -> {
            JobDocument doc = new JobDocument();
            doc.setId(job.getId());
            doc.setUserId(job.getUserId());
            doc.setCompanyId(job.getCompanyId());
            Company company = companyMap.get(job.getCompanyId());
            if (company != null) {
                doc.setCompanyName(company.getName());
            }
            doc.setCategoryId(job.getCategoryId());
            if (job.getCategoryId() != null) {
                JobCategory category = categoryMap.get(job.getCategoryId());
                if (category != null) {
                    doc.setCategoryName(category.getName());
                }
            }
            doc.setTitle(job.getTitle());
            doc.setCity(job.getCity());
            doc.setAddress(job.getAddress());
            doc.setSalaryMin(job.getSalaryMin());
            doc.setSalaryMax(job.getSalaryMax());
            doc.setJobType(job.getJobType());
            doc.setExperience(job.getExperience());
            doc.setEducation(job.getEducation());
            doc.setDescription(job.getDescription());
            doc.setRequirements(job.getRequirements());
            doc.setBenefits(jobService.getJobBenefitNames(job.getId()));
            doc.setStatus(job.getStatus());
            doc.setViewCount(job.getViewCount());
            doc.setApplyCount(job.getApplyCount());
            doc.setPublishedAt(job.getPublishedAt() != null ? job.getPublishedAt().toString().substring(0, 10) : null);
            return doc;
        }).collect(Collectors.toList());

        jobSearchRepository.saveAll(documents);
        log.info("职位数据同步完成，共{}条", documents.size());
    }

    @Override
    public void syncAllCompanies() {
        log.info("开始全量同步公司数据到ES...");
        List<Company> companies = companyMapper.selectList(null);
        if (companies.isEmpty()) {
            log.info("没有公司数据需要同步");
            return;
        }

        List<Job> allJobs = jobMapper.selectList(null);
        Map<Long, Long> jobCountMap = allJobs.stream()
                .filter(j -> "active".equals(j.getStatus()))
                .collect(Collectors.groupingBy(Job::getCompanyId, Collectors.counting()));

        List<CompanyDocument> documents = companies.stream().map(company -> {
            CompanyDocument doc = new CompanyDocument();
            doc.setId(company.getId());
            doc.setName(company.getName());
            doc.setIndustry(company.getIndustry());
            doc.setScale(company.getScale());
            doc.setType(company.getType());
            doc.setCity(company.getCity());
            doc.setAddress(company.getAddress());
            doc.setDescription(company.getDescription());
            doc.setBenefits(companyService.getCompanyBenefitNames(company.getId()));
            doc.setViewCount(company.getViewCount());
            doc.setFollowCount(company.getFollowCount());
            doc.setJobCount(jobCountMap.getOrDefault(company.getId(), 0L).intValue());
            return doc;
        }).collect(Collectors.toList());

        companySearchRepository.saveAll(documents);
        log.info("公司数据同步完成，共{}条", documents.size());
    }

    @Override
    public void syncJob(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            deleteJob(jobId);
            return;
        }

        JobDocument doc = new JobDocument();
        doc.setId(job.getId());
        doc.setUserId(job.getUserId());
        doc.setCompanyId(job.getCompanyId());
        Company company = companyMapper.selectById(job.getCompanyId());
        if (company != null) {
            doc.setCompanyName(company.getName());
        }
        doc.setCategoryId(job.getCategoryId());
        if (job.getCategoryId() != null) {
            JobCategory category = jobCategoryMapper.selectById(job.getCategoryId());
            if (category != null) {
                doc.setCategoryName(category.getName());
            }
        }
        doc.setTitle(job.getTitle());
        doc.setCity(job.getCity());
        doc.setAddress(job.getAddress());
        doc.setSalaryMin(job.getSalaryMin());
        doc.setSalaryMax(job.getSalaryMax());
        doc.setJobType(job.getJobType());
        doc.setExperience(job.getExperience());
        doc.setEducation(job.getEducation());
        doc.setDescription(job.getDescription());
        doc.setRequirements(job.getRequirements());
        doc.setBenefits(jobService.getJobBenefitNames(jobId));
        doc.setStatus(job.getStatus());
        doc.setViewCount(job.getViewCount());
        doc.setApplyCount(job.getApplyCount());
        doc.setPublishedAt(job.getPublishedAt() != null ? job.getPublishedAt().toString().substring(0, 10) : null);

        jobSearchRepository.save(doc);
        log.info("职位{}同步到ES成功", jobId);
    }

    @Override
    public void syncCompany(Long companyId) {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            deleteCompany(companyId);
            return;
        }

        CompanyDocument doc = new CompanyDocument();
        doc.setId(company.getId());
        doc.setName(company.getName());
        doc.setIndustry(company.getIndustry());
        doc.setScale(company.getScale());
        doc.setType(company.getType());
        doc.setCity(company.getCity());
        doc.setAddress(company.getAddress());
        doc.setDescription(company.getDescription());
        doc.setBenefits(companyService.getCompanyBenefitNames(companyId));
        doc.setViewCount(company.getViewCount());
        doc.setFollowCount(company.getFollowCount());
        long jobCount = jobMapper.selectList(null).stream()
                .filter(j -> j.getCompanyId().equals(companyId) && "active".equals(j.getStatus()))
                .count();
        doc.setJobCount((int) jobCount);

        companySearchRepository.save(doc);
        log.info("公司{}同步到ES成功", companyId);
    }

    @Override
    public void deleteJob(Long jobId) {
        jobSearchRepository.deleteById(jobId);
        log.info("职位{}从ES删除", jobId);
    }

    @Override
    public void deleteCompany(Long companyId) {
        companySearchRepository.deleteById(companyId);
        log.info("公司{}从ES删除", companyId);
    }
}
