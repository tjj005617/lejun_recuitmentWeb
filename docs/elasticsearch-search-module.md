# ElasticSearch 搜索模块设计与实现

## 一、整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                         │
│  JobSearch.vue (三栏)  │  CompanySearch.vue (单栏)          │
│  左:分类 | 中:职位 | 右:公司                                 │
└──────────┬──────────────────────────┬───────────────────────┘
           │ GET /api/search/jobs     │ GET /api/search/companies
           ▼                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  SearchController (REST)                    │
└──────────┬──────────────────────────┬───────────────────────┘
           │                          │
           ▼                          ▼
┌─────────────────────────────────────────────────────────────┐
│              SearchServiceImpl (Bool复合查询)                │
│  must: 关键词多字段匹配    filter: 精确过滤(城市/经验/学历)   │
└──────────┬──────────────────────────┬───────────────────────┘
           │                          │
           ▼                          ▼
┌──────────────────┐      ┌──────────────────┐
│  job_index (ES)  │      │ company_index(ES)│
│  IK分词器索引     │      │  IK分词器索引     │
└──────────────────┘      └──────────────────┘
           ▲                          ▲
           │  全量同步 / 增量同步       │
┌──────────────────┐      ┌──────────────────┐
│  MySQL job 表    │      │ MySQL company 表 │
└──────────────────┘      └──────────────────┘
```

## 二、技术选型

| 组件 | 版本 | 说明 |
|------|------|------|
| ElasticSearch | 8.12.0 | 搜索引擎 |
| IK Analysis | latest | 中文分词（ik_max_word 索引 / ik_smart 搜索） |
| Spring Data ES | 5.2.5 | Java 客户端，NativeQuery API |
| Docker | - | 容器化部署，命名卷持久化数据 |

## 三、后端实现

### 3.1 ES Document 实体类

**JobDocument.java** — 职位搜索文档

```java
@Document(indexName = "job_index")
public class JobDocument {
    @Id
    private Long id;
    private Long companyId;

    // 冗余字段，搜索展示用
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String companyName;

    private Long categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;

    // 核心搜索字段
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String address;

    @Field(type = FieldType.Integer)
    private Integer salaryMin;
    @Field(type = FieldType.Integer)
    private Integer salaryMax;

    @Field(type = FieldType.Keyword)
    private String jobType;      // full_time / part_time / intern
    @Field(type = FieldType.Keyword)
    private String experience;
    @Field(type = FieldType.Keyword)
    private String education;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String requirements;

    @Field(type = FieldType.Keyword, index = false)
    private String benefits;     // JSON数组，不索引

    @Field(type = FieldType.Keyword)
    private String status;       // active / paused / closed

    @Field(type = FieldType.Integer)
    private Integer viewCount;
    @Field(type = FieldType.Integer)
    private Integer applyCount;

    @Field(type = FieldType.Date, format = DateFormat.date, pattern = "uuuu-MM-dd")
    private String publishedAt;  // String类型，避免日期转换问题
}
```

**CompanyDocument.java** — 公司搜索文档

```java
@Document(indexName = "company_index")
public class CompanyDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    @Field(type = FieldType.Keyword)
    private String industry;
    @Field(type = FieldType.Keyword)
    private String scale;
    @Field(type = FieldType.Keyword)
    private String type;
    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String address;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Keyword, index = false)
    private String benefits;

    @Field(type = FieldType.Integer)
    private Integer viewCount;
    @Field(type = FieldType.Integer)
    private Integer followCount;
    @Field(type = FieldType.Integer)
    private Integer jobCount;    // 冗余，在招职位数
}
```

### 3.2 搜索 DTO 和结果封装

**JobSearchDTO.java**

```java
@Data
public class JobSearchDTO {
    private String keyword;        // 搜索关键词
    private String city;           // 城市
    private Long categoryId;       // 分类ID
    private String experience;     // 经验要求
    private String education;      // 学历要求
    private String jobType;        // 工作类型
    private Integer salaryMin;     // 薪资下限
    private Integer salaryMax;     // 薪资上限
    private String sort;           // 排序字段
    private Integer page = 1;      // 页码
    private Integer size = 20;     // 每页条数
}
```

**CompanySearchDTO.java**

```java
@Data
public class CompanySearchDTO {
    private String keyword;
    private String city;
    private String industry;
    private String scale;
    private String sort;
    private Integer page = 1;
    private Integer size = 20;
}
```

**SearchResult.java** — 通用分页结果

```java
@Data
public class SearchResult<T> {
    private long total;
    private List<T> records;
    private int page;
    private int size;
    private int totalPages;

    public static <T> SearchResult<T> of(List<T> records, long total, int page, int size) {
        SearchResult<T> result = new SearchResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        return result;
    }
}
```

### 3.3 SearchService 接口

```java
public interface SearchService {
    SearchResult<JobDocument> searchJobs(JobSearchDTO dto);
    SearchResult<CompanyDocument> searchCompanies(CompanySearchDTO dto);
    void syncAllJobs();
    void syncAllCompanies();
    void syncJob(Long jobId);
    void syncCompany(Long companyId);
    void deleteJob(Long jobId);
    void deleteCompany(Long companyId);
}
```

### 3.4 SearchServiceImpl — Bool 复合查询核心

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations esOperations;
    private final JobSearchRepository jobSearchRepository;
    private final CompanySearchRepository companySearchRepository;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final JobCategoryMapper jobCategoryMapper;

    @Override
    public SearchResult<JobDocument> searchJobs(JobSearchDTO dto) {
        // 1. 构建 BoolQuery
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // must: 关键词多字段匹配（影响相关性评分）
        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            String kw = dto.getKeyword().trim();
            boolBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(kw)
                    .fields("title^2.0", "description", "requirements", "companyName^0.5")
            ));
        }

        // filter: 精确过滤（不影响评分）
        boolBuilder.filter(f -> f.term(t -> t.field("status").value("active")));

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

        // 薪资范围过滤：要求对方薪资区间与搜索区间有交集
        if (dto.getSalaryMin() != null) {
            boolBuilder.filter(f -> f.range(r -> r
                    .field("salaryMax").gte(JsonData.of(dto.getSalaryMin()))
            ));
        }
        if (dto.getSalaryMax() != null) {
            boolBuilder.filter(f -> f.range(r -> r
                    .field("salaryMin").lte(JsonData.of(dto.getSalaryMax()))
            ));
        }

        // 2. 构建 NativeQuery
        Query query = Query.of(q -> q.bool(boolBuilder.build()));
        NativeQueryBuilder nativeQueryBuilder = new NativeQueryBuilder();
        nativeQueryBuilder.withQuery(query);

        // 排序
        String sortField = dto.getSort() != null ? dto.getSort() : "publishedAt";
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

        // 分页（NativeQuery 继承 BaseQuery，有 setPageable 方法）
        NativeQuery nativeQuery = nativeQueryBuilder.build();
        nativeQuery.setPageable(PageRequest.of(dto.getPage() - 1, dto.getSize()));

        // 3. 执行查询
        SearchHits<JobDocument> hits = esOperations.search(
                nativeQuery, JobDocument.class, IndexCoordinates.of("job_index")
        );

        List<JobDocument> records = hits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());

        long total = hits.getTotalHits();
        return SearchResult.of(records, total, dto.getPage(), dto.getSize());
    }
}
```

### 3.5 数据同步

```java
@Override
public void syncAllJobs() {
    List<Job> jobs = jobMapper.selectList(null);

    // 批量查询公司和分类，用于冗余字段
    List<Long> companyIds = jobs.stream().map(Job::getCompanyId).distinct().collect(Collectors.toList());
    Map<Long, Company> companyMap = companyMapper.selectBatchIds(companyIds).stream()
            .collect(Collectors.toMap(Company::getId, c -> c));
    Map<Long, JobCategory> categoryMap = jobCategoryMapper.selectList(null).stream()
            .collect(Collectors.toMap(JobCategory::getId, c -> c));

    List<JobDocument> documents = jobs.stream().map(job -> {
        JobDocument doc = new JobDocument();
        doc.setId(job.getId());
        doc.setCompanyId(job.getCompanyId());
        // ... 填充所有字段
        doc.setPublishedAt(job.getPublishedAt() != null
                ? job.getPublishedAt().toString().substring(0, 10) : null);
        return doc;
    }).collect(Collectors.toList());

    jobSearchRepository.saveAll(documents);
}
```

### 3.6 SearchController

```java
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/jobs")
    public Result<SearchResult<JobDocument>> searchJobs(JobSearchDTO dto) {
        return Result.ok(searchService.searchJobs(dto));
    }

    @GetMapping("/companies")
    public Result<SearchResult<CompanyDocument>> searchCompanies(CompanySearchDTO dto) {
        return Result.ok(searchService.searchCompanies(dto));
    }

    @PostMapping("/sync")
    public Result<String> syncAll() {
        searchService.syncAllJobs();
        searchService.syncAllCompanies();
        return Result.ok("全量同步完成");
    }
}
```

### 3.7 JWT 白名单

`JwtFilter.WHITE_LIST` 中添加：

```java
"/api/search/**"
```

### 3.8 Docker 部署

```bash
docker run -d \
  --name elasticsearch \
  --network junle \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  -v es-data:/usr/share/elasticsearch/data \
  -v /var/lib/docker/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
  elasticsearch:8.12.0
```

IK 插件目录结构：

```
/var/lib/docker/elasticsearch/plugins/
└── elasticsearch-analysis-ik/
    ├── elasticsearch-analysis-ik-8.12.0.zip
    ├── plugin-descriptor.properties
    └── config/
        ├── ikanzi.dic
        ├── main.dic
        ├── ... (其他词典文件)
```

## 四、前端实现

### 4.1 ES 搜索 API 封装

**frontend/src/api/search.js**

```javascript
import request from './request'

// ES职位搜索
export const searchJobsByES = (params) => {
  return request.get('/search/jobs', { params })
}

// ES公司搜索
export const searchCompaniesByES = (params) => {
  return request.get('/search/companies', { params })
}
```

### 4.2 职位搜索页（三栏布局）

**frontend/src/views/JobSearch.vue**

布局结构：

```
┌──────────────────────────────────────────────────┐
│                   搜索栏                          │
├──────────────────────────────────────────────────┤
│  城市: [北京] [上海] [广州] ...                    │
│  经验: [不限] [应届生] [1-3年] ...                 │
│  学历: [不限] [大专] [本科] ...                    │
├──────────┬────────────────────┬──────────────────┤
│ 职位分类  │    职位列表(70%)    │   公司推荐(30%)  │
│          │                    │                  │
│ > 技术   │  [职位卡片]         │  公司A           │
│   前端   │  [职位卡片]         │  20个在招职位     │
│   后端   │  [职位卡片]         │                  │
│ > 产品   │                    │  公司B           │
│   ...    │  ◀ 1 2 3 ... ▶    │  15个在招职位     │
└──────────┴────────────────────┴──────────────────┘
```

关键代码：

```javascript
<script setup>
import { searchJobsByES, searchCompaniesByES } from '@/api/search'

const keyword = ref('')
const sortBy = ref('publishedAt')  // ES字段名
const jobList = ref([])
const companyList = ref([])

// 职位搜索（ES）
const handleSearch = async () => {
  const params = {
    keyword: keyword.value || undefined,    // 空值不传
    city: selectedCity.value || undefined,
    experience: selectedExperience.value === '不限' ? undefined : selectedExperience.value,
    education: selectedEducation.value === '不限' ? undefined : selectedEducation.value,
    sort: sortBy.value,
    page: currentPage.value,
    size: pageSize.value
  }
  const res = await searchJobsByES(params)
  jobList.value = res.data.records || []
  totalCount.value = res.data.total || 0
}

// 右侧公司推荐
const handleCompanySearch = async () => {
  const res = await searchCompaniesByES({ page: 1, size: 10, sort: 'jobCount' })
  companyList.value = res.data.records || []
}
</script>
```

排序选项映射：

| 前端选项 | ES 字段 |
|---------|---------|
| 最新发布 | publishedAt |
| 薪资最高 | salary |
| 最多浏览 | viewCount |
| 最多申请 | applyCount |

### 4.3 公司搜索页（单栏布局）

**frontend/src/views/CompanySearch.vue**

布局结构：

```
┌──────────────────────────────────────────┐
│               搜索栏                      │
├──────────────────────────────────────────┤
│  城市: [北京] [上海] [广州] ...            │
│  行业: [互联网] [金融] [教育] ...          │
│  规模: [0-20人] [20-99人] ...            │
├──────────────────────────────────────────┤
│  共找到 22 家公司        排序: [职位最多]  │
├──────────────────────────────────────────┤
│  ┌──────────────────────────────────────┐│
│  │ 公司A        20 在招  1500 浏览       ││
│  │ 互联网 · 北京                        ││
│  │ 公司简介...                          ││
│  └──────────────────────────────────────┘│
│  ┌──────────────────────────────────────┐│
│  │ 公司B        15 在招  800 浏览        ││
│  │ 金融 · 上海                          ││
│  └──────────────────────────────────────┘│
│              ◀ 1 2 3 ... ▶               │
└──────────────────────────────────────────┘
```

### 4.4 路由配置

```javascript
// router/index.js
{ path: '/companies', name: 'CompanySearch', component: () => import('../views/CompanySearch.vue') }
```

### 4.5 导航栏更新

```html
<!-- AppShell.vue -->
<router-link to="/jobs" class="navbar__link">职位搜索</router-link>
<router-link to="/companies" class="navbar__link">公司搜索</router-link>
```

## 五、API 接口汇总

| 方法 | 路径 | 说明 | 白名单 |
|------|------|------|--------|
| GET | `/api/search/jobs` | 职位搜索（ES） | 是 |
| GET | `/api/search/companies` | 公司搜索（ES） | 是 |
| POST | `/api/search/sync` | 全量同步（开发用） | 是 |
| POST | `/api/search/sync/job/{jobId}` | 同步单个职位 | 是 |
| POST | `/api/search/sync/company/{companyId}` | 同步单个公司 | 是 |

### 请求参数

**职位搜索** `GET /api/search/jobs`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（多字段匹配） |
| city | String | 否 | 城市精确匹配 |
| categoryId | Long | 否 | 分类ID精确匹配 |
| experience | String | 否 | 经验要求精确匹配 |
| education | String | 否 | 学历要求精确匹配 |
| jobType | String | 否 | 工作类型精确匹配 |
| salaryMin | Integer | 否 | 薪资下限（筛选salaryMax >= 该值） |
| salaryMax | Integer | 否 | 薪资上限（筛选salaryMin <= 该值） |
| sort | String | 否 | 排序：publishedAt/salary/viewCount/applyCount |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页条数，默认20 |

**公司搜索** `GET /api/search/companies`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词 |
| city | String | 否 | 城市 |
| industry | String | 否 | 行业 |
| scale | String | 否 | 公司规模 |
| sort | String | 否 | 排序：jobCount/viewCount/followCount |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |

### 响应格式

```json
{
  "success": true,
  "data": {
    "total": 213,
    "totalHitsRelation": "eq",
    "records": [
      {
        "id": 1,
        "companyId": 5,
        "companyName": "俊乐科技",
        "title": "Java高级工程师",
        "city": "北京",
        "salaryMin": 15000,
        "salaryMax": 25000,
        "experience": "3-5年",
        "education": "本科",
        "jobType": "full_time",
        "status": "active",
        "viewCount": 150,
        "applyCount": 12,
        "publishedAt": "2026-06-18"
      }
    ],
    "page": 1,
    "size": 20,
    "totalPages": 11
  }
}
```

## 六、ES 查询原理

### Bool 复合查询结构

```json
{
  "bool": {
    "must": [
      {
        "multi_match": {
          "query": "Java",
          "fields": ["title^2.0", "description", "requirements", "companyName^0.5"]
        }
      }
    ],
    "filter": [
      { "term": { "status": "active" } },
      { "term": { "city": "北京" } },
      { "term": { "experience": "3-5年" } },
      { "range": { "salaryMax": { "gte": 15000 } } },
      { "range": { "salaryMin": { "lte": 25000 } } }
    ]
  }
}
```

- **must**: 关键词匹配，影响 `_score` 评分（title 权重 2.0，companyName 权重 0.5）
- **filter**: 精确过滤，不影响评分，可利用缓存提升性能

### 薪资范围交集算法

```
用户搜索: salaryMin=15000, salaryMax=25000
筛选条件:
  salaryMax >= 15000  (职位最高薪 >= 用户最低期望)
  salaryMin <= 25000  (职位最低薪 <= 用户最高预算)
→ 匹配有薪资交集的职位
```

## 七、文件清单

```
backend/src/main/java/com/interview/
├── es/
│   ├── JobDocument.java          # 职位ES文档
│   ├── CompanyDocument.java      # 公司ES文档
│   ├── JobSearchRepository.java  # 职位ES仓库
│   ├── CompanySearchRepository.java
│   ├── JobSearchDTO.java         # 职位搜索参数
│   ├── CompanySearchDTO.java     # 公司搜索参数
│   ├── SearchResult.java         # 通用分页结果
│   ├── SearchService.java        # 搜索服务接口
│   └── SearchServiceImpl.java    # 搜索服务实现
├── controller/
│   └── SearchController.java     # 搜索REST接口
└── filter/
    └── JwtFilter.java            # 白名单添加 /api/search/**

frontend/src/
├── api/
│   └── search.js                 # ES搜索API封装
├── views/
│   ├── JobSearch.vue             # 职位搜索页（三栏）
│   └── CompanySearch.vue         # 公司搜索页（单栏）
├── router/
│   └── index.js                  # 新增 /companies 路由
└── components/
    └── AppShell.vue              # 导航栏新增"公司搜索"
```

## 八、已知注意事项

1. **publishedAt 类型**: MySQL 中是 `LocalDateTime`，同步到 ES 时转为 `String`（`"2026-06-18"`），避免日期格式转换问题
2. **Spring Data ES 版本**: 5.2.5 使用 `NativeQueryBuilder`，不是 `NativeQuery.Builder`
3. **分页**: `NativeQuery` 继承 `BaseQuery`，通过 `setPageable(PageRequest.of(page-1, size))` 设置分页
4. **ES 白名单**: 搜索接口在 JWT 白名单中，无需登录即可访问
5. **IK 分词**: 索引用 `ik_max_word`（最细粒度），搜索用 `ik_smart`（智能分词）
