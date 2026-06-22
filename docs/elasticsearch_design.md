# ElasticSearch 索引设计方案

## 一、整体架构

```
前端导航栏：首页 | 职位搜索 | 公司搜索 | 个人中心 | 消息
                    ↓              ↓
            SearchController (统一搜索入口)
                    ↓
┌─────────────────────────────────────────┐
│           SearchService                 │
│  ┌─────────────┐    ┌─────────────┐    │
│  │ job_index   │    │company_index│    │
│  │ (职位索引)   │    │ (公司索引)   │    │
│  └─────────────┘    └─────────────┘    │
└─────────────────────────────────────────┘
    ↓                         ↓
  MySQL (业务数据)      Redis (缓存/热门数据)
```

## 二、索引设计

### 1. job_index（职位索引）

| 字段 | 类型 | 分词 | 说明 |
|------|------|------|------|
| id | long | - | 职位ID（主键） |
| companyId | long | - | 公司ID |
| companyName | text + keyword | IK分词 | 公司名称（冗余，搜索展示用） |
| categoryId | long | - | 职位分类ID |
| categoryName | keyword | - | 分类名称（冗余） |
| title | text + keyword | IK分词 | 职位名称 |
| city | keyword | - | 城市（精确匹配） |
| address | text | IK分词 | 工作地址 |
| salaryMin | integer | - | 薪资下限（元/月） |
| salaryMax | integer | - | 薪资上限（元/月） |
| jobType | keyword | - | 工作类型：full_time/part_time/intern |
| experience | keyword | - | 经验要求：1-3年/3-5年/5-10年/10年以上/在校 |
| education | keyword | - | 学历要求：大专/本科/硕士/博士 |
| description | text | IK分词 | 职位描述 |
| requirements | text | IK分词 | 任职要求 |
| benefits | keyword | - | 职位福利（JSON数组） |
| status | keyword | - | 状态：active/paused/closed |
| viewCount | integer | - | 浏览次数 |
| applyCount | integer | - | 申请次数 |
| publishedAt | date | - | 发布时间 |

**搜索场景：**
- 关键词搜索：title + description + requirements（IK分词）
- 城市过滤：city（keyword精确匹配）
- 薪资筛选：salaryMin / salaryMax（范围查询）
- 经验筛选：experience（keyword精确匹配）
- 学历筛选：education（keyword精确匹配）
- 分类筛选：categoryId（精确匹配）
- 排序：publishedAt / salaryMax / viewCount / applyCount

### 2. company_index（公司索引）

| 字段 | 类型 | 分词 | 说明 |
|------|------|------|------|
| id | long | - | 公司ID（主键） |
| name | text + keyword | IK分词 | 公司名称 |
| industry | keyword | - | 所属行业 |
| scale | keyword | - | 公司规模 |
| type | keyword | - | 公司类型 |
| city | keyword | - | 所在城市 |
| address | text | IK分词 | 详细地址 |
| description | text | IK分词 | 公司介绍 |
| benefits | keyword | - | 福利待遇（JSON数组） |
| viewCount | integer | - | 浏览量 |
| followCount | integer | - | 关注数 |
| jobCount | integer | - | 在招职位数（冗余，定时更新） |

**搜索场景：**
- 关键词搜索：name + description（IK分词）
- 行业过滤：industry（keyword精确匹配）
- 城市过滤：city（keyword精确匹配）
- 规模筛选：scale（keyword精确匹配）
- 排序：jobCount / viewCount / followCount

### 3. job_category（职位分类）— 不需要 ES

分类数据量小（几十条），直接从 MySQL 查询即可，无需同步到 ES。

### 4. region（省市区）— 不需要 ES

省市区数据是固定的行政区划，直接从 MySQL 查询，前端缓存即可。

## 三、IK 分词器配置

### 索引时分词
```json
"analyzer": "ik_max_word"
```
- 最细粒度切分，提高召回率
- 例："Java开发工程师" → "java", "开发", "工程师", "java开发", "开发工程师"

### 搜索时分词
```json
"analyzer": "ik_smart"
```
- 智能切分，提高准确率
- 例："Java开发" → "java", "开发"

### 自定义词典（可选）
```
# ik_ext_dict.txt
字节跳动
阿里巴巴
腾讯
美团
拼多多
米哈游
华为
...
```

## 四、数据同步策略

### 方案：应用层双写（MySQL + ES）

```
写入流程：
  1. 写 MySQL（业务数据）
  2. 写 ES（搜索数据）
  3. 保证最终一致性

更新流程：
  1. 更新 MySQL
  2. 更新 ES

删除流程：
  1. 逻辑删除 MySQL（deleted=1）
  2. 删除 ES 文档
```

### 同步时机
- 职位发布/编辑/关闭 → 同步 job_index
- 公司信息修改 → 同步 company_index
- 定时任务：每小时同步 jobCount（公司在招职位数）

## 五、查询结构设计（bool 复合查询）

### 核心思路
```
bool 查询
  ├── must:     关键词搜索（IK分词，影响相关性分数）
  ├── filter:   精确过滤（城市/经验/学历/分类/薪资，不计分，性能好）
  ├── should:   可选加分（公司名匹配，低权重）
  └── must_not: 排除条件（如排除已关闭职位）
```

### 职位搜索查询示例
```json
{
  "bool": {
    "must": [
      { "match": { "title": { "query": "Java", "boost": 2.0 }}},
      { "match": { "description": "Java" }},
      { "match": { "requirements": "Java" }}
    ],
    "filter": [
      { "term": { "status": "active" }},
      { "term": { "city": "北京" }},
      { "term": { "experience": "3-5年" }},
      { "term": { "education": "本科" }},
      { "term": { "categoryId": 6 }},
      { "range": { "salaryMin": { "gte": 15000 }}},
      { "range": { "salaryMax": { "lte": 30000 }}}
    ],
    "should": [
      { "match": { "companyName": { "query": "Java", "boost": 0.5 }}}
    ]
  },
  "sort": [
    { "_score": "desc" },
    { "publishedAt": "desc" }
  ]
}
```

### 公司搜索查询示例
```json
{
  "bool": {
    "must": [
      { "match": { "name": { "query": "字节", "boost": 2.0 }}},
      { "match": { "description": "字节" }}
    ],
    "filter": [
      { "term": { "city": "北京" }},
      { "term": { "industry": "互联网" }},
      { "term": { "scale": "10000人以上" }}
    ]
  },
  "sort": [
    { "_score": "desc" },
    { "jobCount": "desc" }
  ]
}
```

### 字段权重配置
| 字段 | 权重 | 说明 |
|------|------|------|
| title | 2.0 | 职位名称最重要 |
| companyName | 0.5 | 公司名匹配次要 |
| description | 1.0 | 默认权重 |
| requirements | 1.0 | 默认权重 |

### 动态构建查询的伪代码
```java
BoolQueryBuilder query = QueryBuilders.boolQuery();

// 1. 关键词搜索（must，影响分数）
if (keyword != null) {
    query.must(QueryBuilders.matchQuery("title", keyword).boost(2.0));
    query.must(QueryBuilders.matchQuery("description", keyword));
    // 公司名匹配（低权重）
    query.should(QueryBuilders.matchQuery("companyName", keyword).boost(0.5));
}

// 2. 精确过滤（filter，不影响分数）
if (city != null) query.filter(QueryBuilders.termQuery("city", city));
if (experience != null) query.filter(QueryBuilders.termQuery("experience", experience));
if (education != null) query.filter(QueryBuilders.termQuery("education", education));
if (categoryId != null) query.filter(QueryBuilders.termQuery("categoryId", categoryId));

// 3. 薪资范围过滤
if (salaryMin != null) query.filter(QueryBuilders.rangeQuery("salaryMin").gte(salaryMin));
if (salaryMax != null) query.filter(QueryBuilders.rangeQuery("salaryMax").lte(salaryMax));

// 4. 默认过滤：只搜活跃职位
query.filter(QueryBuilders.termQuery("status", "active"));
```

## 六、搜索 API 设计

### 1. 职位搜索
```
GET /api/search/jobs
参数：
  keyword: 关键词（可选）
  city: 城市（可选）
  categoryId: 分类ID（可选）
  experience: 经验要求（可选）
  education: 学历要求（可选）
  salaryMin: 最低薪资（可选）
  salaryMax: 最高薪资（可选）
  jobType: 工作类型（可选）
  sort: 排序方式（publishedAt/salary/viewCount/applyCount）
  page: 页码（默认1）
  size: 每页数量（默认20）
```

### 2. 公司搜索
```
GET /api/search/companies
参数：
  keyword: 关键词（可选）
  city: 城市（可选）
  industry: 行业（可选）
  scale: 规模（可选）
  sort: 排序方式（jobCount/viewCount/followCount）
  page: 页码（默认1）
  size: 每页数量（默认20）
```

### 3. 综合搜索（职位搜索页右侧公司列表）
```
GET /api/search/companies
参数：
  keyword: 关键词（必填）
  page: 1
  size: 10
说明：职位搜索时同步搜索公司，展示在右侧
```

## 六、前端交互设计

### 导航栏结构
```
首页 | 职位搜索 | 公司搜索 | 个人中心 | 消息
```

### 职位搜索页（双栏布局）
```
┌─────────────────────────────────────────────────────────────────┐
│  搜索框：输入关键词（Java、前端、字节跳动...）  [搜索]            │
├─────────────────────────────────────────────────────────────────┤
│  筛选栏：                                                       │
│  城市：[全部] [北京] [上海] [杭州] [深圳] [更多...]               │
│  分类：[全部] [技术类] [产品类] [设计类] [运营类]...              │
│  经验：[全部] [1-3年] [3-5年] [5-10年] [10年以上]               │
│  学历：[全部] [大专] [本科] [硕士] [博士]                        │
│  薪资：[不限] [10K以下] [10-20K] [20-30K] [30K以上]             │
├──────────────────────────────────┬──────────────────────────────┤
│  左侧：职位列表（70%宽度）        │  右侧：公司列表（30%宽度）    │
│  ┌──────────────────────────┐   │  ┌────────────────────────┐  │
│  │ Java开发工程师            │   │  │  字节跳动               │  │
│  │ 字节跳动 · 北京 · 15-30K  │   │  │  互联网 · 10000人以上   │  │
│  ├──────────────────────────┤   │  │  在招职位: 10           │  │
│  │ 前端开发工程师            │   │  ├────────────────────────┤  │
│  │ 阿里巴巴 · 杭州 · 18-35K  │   │  │  阿里巴巴               │  │
│  ├──────────────────────────┤   │  │  互联网 · 10000人以上   │  │
│  │ ...                      │   │  │  在招职位: 8            │  │
│  └──────────────────────────┘   │  └────────────────────────┘  │
│  [分页: 1 2 3 ... 10]           │                              │
└──────────────────────────────────┴──────────────────────────────┘
```

### 公司搜索页（单栏布局）
```
┌─────────────────────────────────────────┐
│  搜索框：输入公司名称  [搜索]             │
├─────────────────────────────────────────┤
│  筛选栏：                                │
│  城市：[全部] [北京] [上海] [杭州] [深圳]  │
│  行业：[全部] [互联网] [金融] [汽车]...   │
│  规模：[全部] [100-499人] [1000-9999人]  │
├─────────────────────────────────────────┤
│  ┌─────────────────────────────────┐    │
│  │  字节跳动                        │    │
│  │  互联网 · 10000人以上 · 北京      │    │
│  │  在招职位: 10 | 浏览: 1256       │    │
│  ├─────────────────────────────────┤    │
│  │  阿里巴巴                        │    │
│  │  互联网 · 10000人以上 · 杭州      │    │
│  │  在招职位: 8 | 浏览: 2340        │    │
│  └─────────────────────────────────┘    │
│  [分页: 1 2 3 ... 5]                     │
└─────────────────────────────────────────┘
```

### 首页搜索（快捷入口）
```
首页搜索框输入关键词 → 跳转到职位搜索页并带上keyword参数
```

## 七、已确认事项

1. ✅ **两个索引**：job_index + company_index，职责清晰
2. ✅ **导航栏**：首页 | 职位搜索 | 公司搜索 | 个人中心 | 消息
3. ✅ **职位搜索页**：左侧职位列表 + 右侧公司列表（双栏布局）
4. ✅ **公司搜索页**：专门搜索公司（单栏布局）
5. ✅ **首页搜索**：输入关键词跳转到职位搜索页

## 八、待实现功能（后续版本）

1. **搜索高亮**：搜索关键词高亮显示
2. **搜索自动补全**：输入时自动补全（ES completion suggester）
3. **搜索历史**：记录用户搜索历史（Redis存储）
4. **热门搜索**：展示热门搜索词（Redis统计）
5. **城市三级联动**：省/市/区三级筛选（第一版只支持城市级）
