# 俊乐招聘网站 API 文档

## 目录

- [基础信息](#基础信息)
- [一、用户模块](#一用户模块)
  - [1. 用户注册](#1-用户注册)
  - [2. 用户登录](#2-用户登录)
  - [3. 获取用户信息](#3-获取用户信息)
  - [4. 获取用户简历列表](#4-获取用户简历列表)
  - [5. 添加简历到用户](#5-添加简历到用户最多3份)
  - [6. 移除用户简历](#6-移除用户简历)
  - [7. 更新用户信息](#7-更新用户信息)
- [二、简历模块](#二简历模块)
  - [1. 上传简历](#1-上传简历)
  - [2. 获取简历详情](#2-获取简历详情)
- [三、公司模块](#三公司模块)
  - [1. 创建/选择公司](#1-创建选择公司)
  - [2. 搜索公司](#2-搜索公司)
  - [2.1 公司列表（分页）](#21-公司列表分页)
  - [3. 获取公司详情](#3-获取公司详情)
  - [4. 获取公司职位列表](#4-获取公司职位列表)
  - [5. 获取我的公司信息](#5-获取我的公司信息)
  - [6. 获取热门公司](#6-获取热门公司)
  - [7. 增加公司浏览量](#7-增加公司浏览量)
  - [8. 关注/取消关注公司](#8-关注取消关注公司)
  - [9. 检查是否已关注](#9-检查是否已关注)
  - [10. 获取用户关注的公司列表](#10-获取用户关注的公司列表)
- [四、职位模块](#四职位模块)
  - [1. 发布职位](#1-发布职位)
  - [2. 编辑职位](#2-编辑职位)
  - [3. 删除职位](#3-删除职位)
  - [4. HR认领职位](#4-hr认领职位)
  - [5. 获取职位详情](#5-获取职位详情)
  - [6. 搜索职位](#6-搜索职位)
  - [7. 获取热门职位](#7-获取热门职位)
  - [8. 获取推荐职位](#8-获取推荐职位)
  - [8. 获取我发布的职位](#8-获取我发布的职位)
  - [9. 收藏/取消收藏职位](#9-收藏取消收藏职位)
  - [10. 检查是否已收藏职位](#10-检查是否已收藏职位)
  - [11. 获取收藏职位列表](#11-获取收藏职位列表)
  - [12. 猜你喜欢](#12-猜你喜欢)
- [五、职位分类模块](#五职位分类模块)
  - [1. 获取分类树形结构](#1-获取分类树形结构)
  - [2. 获取分类列表](#2-获取分类列表)
- [六、申请模块](#六申请模块)
  - [1. 投递简历](#1-投递简历)
  - [2. 撤回申请](#2-撤回申请)
  - [3. 我的投递记录](#3-我的投递记录)
  - [4. 获取职位的申请列表](#4-获取职位的申请列表)
  - [5. 获取公司所有投递列表](#5-获取公司所有投递列表)
  - [6. 更新申请状态](#6-更新申请状态)
- [七、消息模块](#七消息模块)
  - [1. 发送消息](#1-发送消息)
  - [2. 获取会话列表](#2-获取会话列表)
  - [3. 获取聊天记录](#3-获取聊天记录)
  - [4. 标记消息已读](#4-标记消息已读)
  - [5. 获取未读消息数](#5-获取未读消息数)
  - [6. 清理重复消息](#6-清理重复消息)
  - [7. 获取最新未读消息](#7-获取最新未读消息)
  - [8. 更新会话关联的职位](#8-更新会话关联的职位)
- [八、省市区模块](#八省市区模块)
  - [1. 获取省市区树形结构](#1-获取省市区树形结构)
  - [2. 根据父级ID获取下级区域](#2-根据父级id获取下级区域)
  - [3. 获取区域路径](#3-获取区域路径)
- [九、福利标签模块](#九福利标签模块)
  - [1. 获取启用的福利标签](#1-获取启用的福利标签)
  - [2. 获取全部福利标签](#2-获取全部福利标签)
  - [3. 新增福利标签](#3-新增福利标签)
  - [4. 更新福利标签](#4-更新福利标签)
  - [5. 删除福利标签](#5-删除福利标签)
- [十、面试模块](#十面试模块)
  - [1. 创建面试](#1-创建面试)
  - [2. 获取面试详情](#2-获取面试详情)
  - [3. 获取面试历史](#3-获取面试历史)
  - [4. 获取用户面试列表](#4-获取用户面试列表)
  - [5. 语义判断回答是否结束](#5-语义判断回答是否结束)
- [十一、AI 语音模块](#十一ai-语音模块)
  - [1. 问题语音朗读（TTS）](#1-问题语音朗读tts)
  - [2. 语音识别（STT）](#2-语音识别stt)
- [十二、WebSocket 接口](#十二websocket-接口)
  - [1. 面试 WebSocket](#1-面试-websocket)
  - [2. ASR WebSocket（实时语音识别）](#2-asr-websocket实时语音识别)
  - [3. 聊天 WebSocket](#3-聊天-websocket)
- [十三、SSE 接口](#十三sse-接口)
  - [1. 订阅消息通知](#1-订阅消息通知)
- [十四、ElasticSearch 搜索模块](#十四elasticsearch-搜索模块)
  - [1. 职位搜索（ES）](#1-职位搜索es)
  - [2. 公司搜索（ES）](#2-公司搜索es)
  - [3. 全量同步](#3-全量同步)
  - [4. 同步单个职位](#4-同步单个职位)
  - [5. 同步单个公司](#5-同步单个公司)
- [十五、用户档案模块](#十五用户档案模块)
  - [1. 获取用户档案](#1-获取用户档案)
  - [2. 保存/更新用户档案](#2-保存更新用户档案)
- [十六、管理后台模块](#十六管理后台模块)
  - [1. 管理员登录](#1-管理员登录)
  - [2. 获取管理员信息](#2-获取管理员信息)
  - [3. 数据概览仪表盘](#3-数据概览仪表盘)
  - [4. 用户管理](#4-用户管理)
  - [5. 公司管理](#5-公司管理)
  - [6. 职位管理](#6-职位管理)
  - [7. 申请管理](#7-申请管理)
  - [8. 面试管理](#8-面试管理)
  - [9. 福利标签管理](#9-福利标签管理)
  - [10. 职位分类管理](#10-职位分类管理)
  - [11. 地区管理](#11-地区管理)
- [十七、数据结构说明](#十七数据结构说明)
- [十八、状态码说明](#十八状态码说明)
- [十九、WebSocket 消息类型](#十九websocket-消息类型)
  - [视频面试信令流程](#视频面试信令流程)

---

## 基础信息

- 基础路径: `/api`
- 数据格式: JSON
- 字符编码: UTF-8
- 认证方式: JWT Token（Bearer Token）
- 统一响应格式: `Result<T>`

### 统一响应格式

所有接口返回统一的 `Result<T>` 格式：

```json
{
  "success": true/false,
  "message": "提示信息（可选）",
  "data": T
}
```

异常由 `GlobalExceptionHandler` 统一处理：
- `RuntimeException` → HTTP 400 + `Result.fail(message)`
- `Exception` → HTTP 500 + `Result.fail("系统内部错误")`

### 认证说明

- 登录/注册接口返回JWT Token
- 后续请求需在Header中携带: `Authorization: Bearer <token>`
- Token有效期: 7天
- 未携带Token或Token过期将返回401错误

### 放行接口（无需认证）

- `POST /api/user/login` - 登录
- `POST /api/user/register` - 注册
- `POST /api/resume/upload` - 上传简历
- `GET /api/resume/{id}` - 简历详情
- `GET /api/job/list` - 搜索职位
- `GET /api/job/hot` - 热门职位
- `GET /api/job/{id}` - 职位详情
- `GET /api/company/{id}` - 公司详情
- `GET /api/company/{id}/jobs` - 公司职位
- `GET /api/category/tree` - 职位分类
- `GET /api/category/list` - 分类列表
- `GET /api/region/tree` - 省市区
- `GET /api/region/children/*` - 下级区域
- `GET /api/region/path/*` - 省市区路径
- `GET /api/company/list` - 公司列表（分页）
- `GET /api/benefit-tag/list` - 福利标签
- `GET /api/region/path/*` - 省市区路径
- `GET /api/search/jobs` - ES职位搜索
- `GET /api/search/companies` - ES公司搜索
- `POST /api/search/sync` - 全量同步到ES
- `POST /api/search/sync/job/{jobId}` - 同步单个职位
- `POST /api/search/sync/company/{companyId}` - 同步单个公司
- `POST /api/interview/tts` - 问题语音朗读
- `POST /api/stt` - 语音识别

### 管理后台接口权限

- `/api/admin/**` 所有接口需要管理员角色（roleType=3）
- 非管理员角色访问将返回 HTTP 403
- 管理员登录复用 `POST /api/user/login`，无需单独登录接口

---

## 一、用户模块

### 1. 用户注册

- **URL**: `POST /api/user/register`
- **Content-Type**: `application/json`

**请求参数:**

```json
{
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三",
  "roleType": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名（唯一） |
| password | String | 是 | 密码 |
| nickname | String | 否 | 昵称（不填则用用户名） |
| roleType | Integer | 否 | 角色类型：1-求职者 2-HR（默认1） |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "roleType": 1,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**失败响应:**

```json
{
  "success": false,
  "message": "用户名已存在"
}
```

---

### 2. 用户登录

- **URL**: `POST /api/user/login`
- **Content-Type**: `application/json`

**请求参数:**

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "roleType": 1,
    "companyId": 4,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| roleType | Integer | 角色类型：1-求职者 2-HR 3-管理员 |
| companyId | Long | 关联公司ID（HR角色，无公司时为null） |
| token | String | JWT Token |

---

### 3. 获取用户信息

- **URL**: `GET /api/user/{id}`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户ID |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "phone": "",
    "email": "",
    "roleType": 1,
    "companyId": 4
  }
}
```

---

### 4. 获取用户简历列表

- **URL**: `GET /api/user/{id}/resumes`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户ID |

**成功响应:**

```json
{
  "success": true,
  "data": [1, 2, 3]
}
```

返回简历ID数组，前端需逐个调用 `/api/resume/{id}` 获取详情。

---

### 5. 添加简历到用户（最多3份）

- **URL**: `POST /api/user/{id}/resumes`
- **Content-Type**: `application/json`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户ID |

**请求参数:**

```json
{
  "resumeId": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| resumeId | Long | 是 | 简历ID |

**成功响应:**

```json
{
  "success": true,
  "message": "简历关联成功"
}
```

**失败响应:**

```json
{
  "success": false,
  "message": "最多只能上传3份简历"
}
```

---

### 6. 移除用户简历

- **URL**: `DELETE /api/user/{id}/resumes/{resumeId}`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户ID |
| resumeId | Long | 简历ID |

**成功响应:**

```json
{
  "success": true,
  "message": "简历关联已移除"
}
```

---

### 7. 更新用户信息

- **URL**: `PUT /api/user/{id}`
- **Content-Type**: `application/json`
- **需要登录**: 是
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户ID |

**请求参数:**

```json
{
  "nickname": "新昵称",
  "phone": "13800138000",
  "email": "zhangsan@example.com"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| nickname | String | 否 | 昵称 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |

**成功响应:**

```json
{
  "success": true,
  "message": "更新成功"
}
```

**失败响应:**

```json
{
  "success": false,
  "message": "用户不存在"
}
```

---

## 二、简历模块

### 1. 上传简历

- **URL**: `POST /api/resume/upload`
- **Content-Type**: `multipart/form-data`

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | File | 是 | 简历文件（支持 PDF、Word 等） |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "fileName": "张三简历.pdf",
    "objectName": "resumes/2026/06/14/xxx.pdf",
    "rawContent": "简历原始内容...",
    "name": "张三",
    "education": "本科",
    "skills": "[\"Java\",\"Spring Boot\"]",
    "workExperience": "3年工作经验",
    "projectExperience": "xxx项目",
    "createdAt": "2026-06-14T10:00:00",
    "createdBy": 1
  }
}
```

**失败响应:**

```json
{
  "success": false,
  "message": "文件格式不支持"
}
```

---

### 2. 获取简历详情

- **URL**: `GET /api/resume/{id}`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 简历ID |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "fileName": "张三简历.pdf",
    "name": "张三",
    "education": "本科",
    "skills": "[\"Java\",\"Spring Boot\"]",
    "workExperience": "...",
    "projectExperience": "...",
    "rawContent": "简历原始文本内容...",
    "createdAt": "2026-06-14T10:00:00",
    "fileUrl": "http://192.168.150.105:9000/resumes/xxx.pdf?signature=..."
  }
}
```

**说明:** `fileUrl` 为 MinIO 预签名 URL，有效期 7 天，可直接在 iframe 中预览 PDF。

---

## 三、公司模块

### 1. 保存公司信息（新建/更新）

- **URL**: `POST /api/company`
- **Content-Type**: `application/json`
- **需要登录**: 是（HR角色）

**说明：** 支持两种模式：
- **新建公司**：不传 `companyId`，创建新公司并关联到当前HR
- **更新公司**：传 `companyId` 及表单字段，更新已有公司信息

**请求参数（新建公司）：**

```json
{
  "name": "俊乐科技有限公司",
  "industry": "互联网/IT",
  "scale": "100-499人",
  "type": "民营",
  "regionId": 3101,
  "city": "广东省 深圳市",
  "address": "南山区xxx",
  "website": "https://www.junle.com",
  "description": "公司介绍...",
  "benefits": ["五险一金", "年终奖", "弹性工作"]
}
```

**请求参数（更新公司）：**

```json
{
  "companyId": 1,
  "name": "俊乐科技有限公司",
  "industry": "互联网/IT",
  "scale": "100-499人",
  "type": "民营",
  "regionId": 3101,
  "city": "广东省 深圳市",
  "address": "南山区xxx",
  "website": "https://www.junle.com",
  "description": "更新后的公司介绍...",
  "benefits": ["五险一金", "年终奖"]
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| companyId | Long | 否 | 已有公司ID（更新时必传） |
| name | String | 是 | 公司名称 |
| industry | String | 否 | 所属行业 |
| scale | String | 否 | 公司规模 |
| type | String | 否 | 公司类型 |
| regionId | Long | 否 | 区域ID（区级，用于省市区级联） |
| city | String | 否 | 所在城市 |
| address | String | 否 | 详细地址 |
| website | String | 否 | 公司官网 |
| description | String | 否 | 公司介绍 |
| benefits | List\<String\> | 否 | 福利待遇名称列表（字符串数组） |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "俊乐科技有限公司",
    ...
  }
}
```

---

### 2. 搜索公司

- **URL**: `GET /api/company/search`
- **需要登录**: 否

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | String | 否 | 公司名称关键词（模糊匹配） |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "俊乐科技有限公司",
      "industry": "互联网/IT",
      "scale": "100-499人",
      "type": "民营",
      "regionId": 3101,
      "regionPath": "广东省 深圳市 南山区",
      "city": "深圳",
      "address": "南山区xxx",
      "website": "https://www.junle.com",
      "description": "公司介绍...",
      "benefits": ["五险一金", "年终奖"]
    }
  ]
}
```

---

### 2.1 公司列表（分页）

- **URL**: `GET /api/company/list`
- **需要登录**: 否

**查询参数:**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | String | 否 | - | 公司名称关键词 |
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 20 | 每页数量 |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "name": "俊乐科技有限公司",
        "industry": "互联网/IT",
        "scale": "100-499人",
        "type": "民营",
        "regionId": 3101,
        "regionPath": "广东省 深圳市 南山区",
        "city": "深圳",
        "address": "南山区xxx",
        "website": "https://www.junle.com",
        "description": "公司介绍...",
        "benefits": ["五险一金", "年终奖"]
      }
    ],
    "total": 50,
    "page": 1,
    "size": 20
  }
}
```

---

### 3. 获取公司详情

- **URL**: `GET /api/company/{id}`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 公司ID |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 15,
    "name": "俊乐科技有限公司",
    "logo": "https://example.com/logo.png",
    "industry": "互联网",
    "scale": "100-499人",
    "type": "民营",
    "city": "北京",
    "regionId": 1101,
    "address": "北京市海淀区xxx",
    "website": "https://www.example.com",
    "description": "公司介绍...",
    "viewCount": 1024,
    "followCount": 56,
    "benefits": ["五险一金", "年终奖", "弹性工作"]
  }
}
```

---

### 4. 获取公司职位列表

- **URL**: `GET /api/company/{id}/jobs`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 公司ID |

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | String | 否 | 职位名称/描述关键词（模糊搜索） |
| city | String | 否 | 城市 |
| jobType | String | 否 | 工作类型：full_time/part_time/intern |
| experience | String | 否 | 经验要求 |
| education | String | 否 | 学历要求 |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页数量（默认8） |

**说明:** 只展示有人认领（`user_id` 不为空）且状态为 `active` 的职位，按发布时间倒序。内存分页。

**成功响应:**

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 15,
        "companyId": 1,
        "title": "Java开发工程师",
        "city": "北京",
        "address": "北京市朝阳区",
        "salaryMin": 15000,
        "salaryMax": 25000,
        "jobType": "full_time",
        "experience": "1-3年",
        "education": "本科",
        "status": "active",
        "viewCount": 128,
        "publishedAt": "2026-06-15T10:00:00"
      }
    ],
    "total": 12
  }
}
```

---

### 5. 获取我的公司信息

- **URL**: `GET /api/company/my`
- **需要登录**: 是（HR角色）

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "俊乐科技有限公司",
    ...
  }
}
```

---

### 6. 获取热门公司

- **URL**: `GET /api/company/hot`
- **需要登录**: 否

**请求参数:**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| limit | int | 否 | 6 | 返回数量 |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "俊乐科技有限公司",
      "industry": "互联网",
      "scale": "100-500人",
      "type": "民营",
      "city": "北京",
      "address": "北京市海淀区xxx",
      "description": "公司介绍...",
      "benefits": ["五险一金", "年终奖"]
    }
  ]
}
```

---

### 7. 增加公司浏览量

- **URL**: `POST /api/company/{id}/view`
- **需要登录**: 否
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 公司ID |

**成功响应:**

```json
{
  "success": true
}
```

---

### 8. 关注/取消关注公司

- **URL**: `POST /api/company/{id}/follow`
- **需要登录**: 是
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 公司ID |

**说明:** 切换关注状态，已关注则取消，未关注则添加。同时更新公司的关注数。

**成功响应:**

```json
{
  "success": true,
  "data": true  // true=已关注, false=已取消关注
}
```

---

### 9. 检查是否已关注

- **URL**: `GET /api/company/{id}/followed`
- **需要登录**: 是
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 公司ID |

**成功响应:**

```json
{
  "success": true,
  "data": true  // true=已关注, false=未关注
}
```

### 10. 获取用户关注的公司列表

- **URL**: `GET /api/company/followed`
- **需要登录**: 是

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "字节跳动",
      "industry": "互联网",
      "scale": "10000人以上",
      "type": "民营企业",
      "regionId": 1101,
      "regionPath": "北京市海淀区",
      "city": "北京",
      "address": "北京市海淀区中关村软件园",
      "website": "https://www.bytedance.com",
      "description": "公司介绍...",
      "benefits": ["五险一金", "带薪年假", "弹性工作"]
    }
  ]
}
```

---

## 四、职位模块

### 1. 发布职位

- **URL**: `POST /api/job`
- **Content-Type**: `application/json`
- **需要登录**: 是（HR角色）

**请求参数:**

```json
{
  "title": "Java开发工程师",
  "city": "北京",
  "address": "北京市朝阳区",
  "salaryMin": 15000,
  "salaryMax": 25000,
  "jobType": "full_time",
  "experience": "1-3年",
  "education": "本科",
  "categoryId": 6,
  "description": "职位描述...",
  "requirements": "任职要求...",
  "benefits": ["五险一金", "年终奖"]
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 是 | 职位名称 |
| city | String | 是 | 工作城市 |
| salaryMin | Integer | 否 | 薪资下限（元/月） |
| salaryMax | Integer | 否 | 薪资上限（元/月） |
| jobType | String | 是 | 工作类型：full_time/part_time/intern |
| experience | String | 否 | 经验要求 |
| education | String | 否 | 学历要求 |
| categoryId | Long | 否 | 职位分类ID |
| description | String | 是 | 职位描述 |
| requirements | String | 否 | 任职要求 |
| benefits | List\<String\> | 否 | 职位福利名称列表（字符串数组） |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Java开发工程师",
    ...
  }
}
```

---

### 2. 编辑职位

- **URL**: `PUT /api/job/{id}`
- **Content-Type**: `application/json`
- **需要登录**: 是（HR角色，需属于该公司）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 职位ID |

请求参数同发布职位。

---

### 3. 删除职位

- **URL**: `DELETE /api/job/{id}`
- **需要登录**: 是（HR角色，需属于该公司）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 职位ID |

**说明:** 软删除职位，同时同步标记关联的申请记录和收藏记录为`job_deleted=1`。

**成功响应:**

```json
{
  "success": true,
  "message": "删除成功"
}
```

---

### 4. HR认领职位

- **URL**: `POST /api/job/{id}/claim`
- **需要登录**: 是（HR角色，需属于该公司）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 职位ID（需无负责HR） |

**成功响应:**

```json
{
  "success": true,
  "message": "认领成功"
}
```

---

### 5. 获取职位详情

- **URL**: `GET /api/job/{id}`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 职位ID |

**说明:** 会自动增加浏览次数。返回 `JobDetailVO`，包含嵌套的公司信息和 HR 信息。

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "companyId": 1,
    "userId": 15,
    "title": "Java开发工程师",
    "city": "北京",
    "address": "北京市朝阳区",
    "salaryMin": 15000,
    "salaryMax": 25000,
    "jobType": "full_time",
    "experience": "1-3年",
    "education": "本科",
    "description": "职位描述...",
    "requirements": "任职要求...",
    "benefits": ["五险一金", "年终奖"],
    "status": "active",
    "viewCount": 128,
    "applyCount": 15,
    "publishedAt": "2026-06-15T10:00:00",
    "company": {
      "id": 1,
      "name": "俊乐科技有限公司",
      "logo": "https://example.com/logo.png",
      "industry": "互联网",
      "scale": "100-499人",
      "type": "民营",
      "city": "北京",
      "address": "北京市海淀区xxx",
      "website": "https://www.example.com",
      "description": "公司介绍...",
      "benefits": ["五险一金", "年终奖", "弹性工作"]
    },
    "hr": {
      "id": 15,
      "nickname": "HR张三",
      "username": "hr_zhangsan",
      "avatar": "https://example.com/avatar.png"
    }
  }
}
```

---

### 5. 搜索职位

- **URL**: `GET /api/job/list`
- **查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | String | 否 | 搜索关键词 |
| city | String | 否 | 城市 |
| salaryMin | Integer | 否 | 最低薪资 |
| salaryMax | Integer | 否 | 最高薪资 |
| jobType | String | 否 | 工作类型 |
| categoryId | Long | 否 | 职位分类ID |
| experience | String | 否 | 经验要求 |
| education | String | 否 | 学历要求 |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页数量（默认20） |
| sort | String | 否 | 排序方式：published_at_desc/salary_desc/view_count_desc |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "title": "Java开发工程师",
        "companyName": "俊乐科技有限公司",
        "city": "北京",
        "salaryMin": 15000,
        "salaryMax": 25000,
        "publishedAt": "2026-06-15T10:00:00"
      }
    ],
    "total": 128,
    "page": 1,
    "size": 20
  }
}
```

---

### 6. 获取热门职位

- **URL**: `GET /api/job/hot`

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Java开发工程师",
      "companyName": "俊乐科技有限公司",
      "city": "北京",
      "salaryMin": 15000,
      "salaryMax": 25000,
      "viewCount": 128,
      "applyCount": 15
    }
  ]
}
```

---

### 7. 获取推荐职位

- **URL**: `GET /api/job/recommend`
- **需要登录**: 是（求职者角色）

**说明:** 根据用户简历中的技能和工作经历智能推荐匹配职位。

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Java开发工程师",
      "companyName": "俊乐科技有限公司",
      "city": "北京",
      "salaryMin": 15000,
      "salaryMax": 25000,
      "matchScore": 85
    }
  ]
}
```

---

### 8. 获取我发布的职位

- **URL**: `GET /api/job/my`
- **需要登录**: 是（HR角色）
- **查询参数:**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| companyId | Long | 是 | - | 公司ID |
| keyword | String | 否 | - | 职位名称模糊搜索 |
| city | String | 否 | - | 城市精确匹配 |
| status | String | 否 | - | 状态筛选：active/paused/closed |
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 20 | 每页数量 |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "title": "Java开发工程师",
        "city": "北京",
        "salaryMin": 15000,
        "salaryMax": 25000,
        "jobType": "full_time",
        "status": "active",
        "viewCount": 128,
        "applyCount": 15,
        "publishedAt": "2026-06-15T10:00:00",
        "companyId": 1,
        "userId": 2,
        "hrName": "张三"
      }
    ],
    "total": 50,
    "page": 1,
    "size": 20
  }
}
```

---

### 9. 收藏/取消收藏职位

- **URL**: `POST /api/job/{id}/favorite`
- **需要登录**: 是
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 职位ID |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "favorited": true
  }
}
```

---

### 10. 检查是否已收藏职位

- **URL**: `GET /api/job/{id}/favorite`
- **需要登录**: 是
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 职位ID |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "favorited": true
  }
}
```

---

### 11. 获取收藏职位列表

- **URL**: `GET /api/job/favorites`
- **需要登录**: 是

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Java开发工程师",
      "companyName": "俊乐科技有限公司",
      "city": "北京",
      "salaryMin": 15000,
      "salaryMax": 25000,
      "jobType": "full_time",
      "status": "active",
      "viewCount": 128,
      "applyCount": 15,
      "publishedAt": "2026-06-15T10:00:00"
    }
  ]
}
```

---

### 12. 猜你喜欢

- **URL**: `GET /api/job/guess`
- **需要登录**: 否

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| currentId | Long | 否 | 当前职位ID（排除自身，用于详情页） |

**说明:** 随机推荐3个活跃职位，用于首页"猜你喜欢"模块。

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Java开发工程师",
      "companyName": "俊乐科技有限公司",
      "city": "北京",
      "salaryMin": 15000,
      "salaryMax": 25000,
      "jobType": "full_time",
      "status": "active",
      "viewCount": 128,
      "applyCount": 15,
      "publishedAt": "2026-06-15T10:00:00"
    }
  ]
}
```

---

## 五、职位分类模块

### 1. 获取分类树形结构

- **URL**: `GET /api/category/tree`

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "技术类",
      "children": [
        {"id": 6, "name": "Java开发"},
        {"id": 7, "name": "前端开发"},
        {"id": 8, "name": "Python开发"}
      ]
    },
    {
      "id": 2,
      "name": "产品类",
      "children": [
        {"id": 9, "name": "产品经理"}
      ]
    }
  ]
}
```

---

### 2. 获取分类列表

- **URL**: `GET /api/category/list`

**成功响应:**

```json
{
  "success": true,
  "data": [
    {"id": 1, "name": "技术类", "parentId": 0},
    {"id": 6, "name": "Java开发", "parentId": 1},
    {"id": 7, "name": "前端开发", "parentId": 1}
  ]
}
```

---

## 六、申请模块

### 1. 投递简历

- **URL**: `POST /api/application`
- **Content-Type**: `application/json`
- **需要登录**: 是（求职者角色）

**请求参数:**

```json
{
  "jobId": 1,
  "resumeId": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| jobId | Long | 是 | 职位ID |
| resumeId | Long | 是 | 简历ID |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "jobId": 1,
    "status": "pending",
    "appliedAt": "2026-06-15T10:00:00"
  }
}
```

**失败响应:**

```json
{
  "success": false,
  "message": "您已投递过该职位"
}
```

---

### 2. 撤回申请

- **URL**: `DELETE /api/application/{id}`
- **需要登录**: 是（求职者角色）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 申请ID |

**成功响应:**

```json
{
  "success": true,
  "message": "已撤回申请"
}
```

---

### 3. 我的投递记录

- **URL**: `GET /api/application/my`
- **需要登录**: 是（求职者角色）

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "jobId": 1,
      "jobTitle": "Java开发工程师",
      "companyName": "俊乐科技有限公司",
      "status": "pending",
      "appliedAt": "2026-06-15T10:00:00",
      "jobDeleted": false
    }
  ]
}
```

**说明:** 当职位被软删除时，`jobDeleted` 为 `true`，`jobTitle` 显示为"该职位已删除"。

---

### 4. 获取职位的申请列表

- **URL**: `GET /api/application/job/{jobId}`
- **需要登录**: 是（HR角色，需属于该公司）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| jobId | Long | 职位ID |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 2,
      "username": "张三",
      "resumeId": 1,
      "status": "pending",
      "appliedAt": "2026-06-15T10:00:00"
    }
  ]
}
```

---

### 5. 获取当前HR的投递列表

- **URL**: `GET /api/application/company/{companyId}`
- **需要登录**: 是（HR角色，需属于该公司）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| companyId | Long | 公司ID |

**说明:** 返回当前HR负责的职位的所有投递记录（只查`job.user_id = 当前HR`的职位）。

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "jobId": 10,
      "jobTitle": "Java开发工程师",
      "userId": 2,
      "username": "张三",
      "resumeId": 1,
      "status": "pending",
      "appliedAt": "2026-06-15T10:00:00"
    }
  ]
}
```

---

### 6. 更新申请状态

- **URL**: `PUT /api/application/{id}/status`
- **Content-Type**: `application/json`
- **需要登录**: 是（HR角色，需属于该公司）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 申请ID |

**请求参数:**

```json
{
  "status": "screening",
  "hrRemark": "简历不错，安排面试"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | String | 是 | 新状态：screening/interview/offer/rejected |
| hrRemark | String | 否 | HR备注 |
| rejectReason | String | 否 | 拒绝原因（status=rejected时） |

**成功响应:**

```json
{
  "success": true,
  "message": "状态更新成功"
}
```

---

## 七、消息模块

### 1. 发送消息

- **URL**: `POST /api/message`
- **Content-Type**: `application/json`
- **需要登录**: 是

**请求参数:**

```json
{
  "receiverId": 2,
  "jobId": 1,
  "type": "chat",
  "content": "您好，我对这个职位很感兴趣"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| receiverId | Long | 是 | 接收者ID |
| jobId | Long | 否 | 关联职位ID |
| type | String | 是 | 消息类型：chat/application/system |
| content | String | 是 | 消息内容 |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "senderId": 1,
    "receiverId": 2,
    "jobId": 10,
    "type": "chat",
    "content": "您好，我对这个职位很感兴趣",
    "isRead": 0,
    "createdAt": "2026-06-15T10:00:00"
  }
}
```

---

### 2. 获取会话列表

- **URL**: `GET /api/message/conversations`
- **需要登录**: 是

**说明:** 返回当前用户的所有会话，按最新消息时间倒序排列。`jobId` 取自该会话中最新一条携带职位ID的消息（而非最后一条消息），确保聊天页面始终能展示关联岗位卡片。

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "userId": 2,
      "nickname": "张三",
      "avatar": "...",
      "lastMessage": "好的，我们约个时间面试",
      "lastTime": "2026-06-15T10:30:00",
      "unreadCount": 2,
      "jobId": 10,
      "jobTitle": "Java开发工程师"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| userId | Long | 对方用户ID |
| nickname | String | 对方昵称 |
| avatar | String | 对方头像 |
| lastMessage | String | 最后一条消息内容 |
| lastTime | String | 最后一条消息时间 |
| unreadCount | Integer | 未读消息数 |
| jobId | Long | 关联职位ID（可能为null） |
| jobTitle | String | 关联职位标题（可能为null） |

---

### 3. 获取聊天记录

- **URL**: `GET /api/message/conversation/{userId}`
- **需要登录**: 是
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | Long | 对方用户ID |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "senderId": 1,
      "receiverId": 2,
      "jobId": 10,
      "resumeId": null,
      "type": "chat",
      "content": "您好",
      "isRead": 1,
      "createdAt": "2026-06-15T10:00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 消息ID |
| senderId | Long | 发送者ID |
| receiverId | Long | 接收者ID |
| jobId | Long | 关联职位ID（可能为null） |
| resumeId | Long | 关联简历ID（可能为null） |
| type | String | 消息类型：chat/application/system |
| content | String | 消息内容 |
| isRead | Integer | 是否已读：0-未读 1-已读 |
| createdAt | String | 发送时间 |

---

### 4. 标记消息已读

- **URL**: `PUT /api/message/read/{senderId}`
- **需要登录**: 是
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| senderId | Long | 发送者ID |

**成功响应:**

```json
{
  "success": true,
  "message": "已标记为已读"
}
```

---

### 5. 获取未读消息数

- **URL**: `GET /api/message/unread-count`
- **需要登录**: 是

**成功响应:**

```json
{
  "success": true,
  "data": 5
}
```

---

### 6. 清理重复消息

- **URL**: `POST /api/message/cleanup-duplicates`
- **需要登录**: 是

**说明:** 清理 MongoDB 中的重复聊天消息。按 conversationId + senderId + content 分组，5秒内的消息视为重复，只保留最早的一条。

**成功响应:**

```json
{
  "success": true,
  "data": "清理完成，删除111条重复消息"
}
```

---

### 7. 获取最新未读消息

- **URL**: `GET /api/message/latest-unread`
- **需要登录**: 是

**说明:** 获取当前用户最新一条未读消息，用于轮询通知弹窗。无未读消息时返回 null。

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "senderId": 2,
    "senderName": "张HR",
    "content": "你好，收到了你的简历",
    "type": "text",
    "createdAt": "2026-06-20T10:30:00"
  }
}
```

### 8. 更新会话关联的职位

- **URL**: `PUT /api/message/conversation/job`
- **需要登录**: 是
- **请求体:**

```json
{
  "otherUserId": 2,
  "jobId": 10
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| otherUserId | Long | 是 | 对方用户ID |
| jobId | Long | 是 | 职位ID |

**说明:** 更新与某用户会话关联的职位（修改双方最新消息的 jobId）。

**成功响应:**

```json
{
  "success": true,
  "data": "职位更新成功"
}
```

---

## 八、省市区模块

### 1. 获取省市区树形结构

- **URL**: `GET /api/region/tree`
- **需要登录**: 否

**说明:** 返回三级省市区树形结构，用于前端级联选择器（el-cascader）。

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "value": 1,
      "label": "北京市",
      "children": [
        {
          "value": 101,
          "label": "东城区",
          "children": []
        },
        {
          "value": 103,
          "label": "朝阳区",
          "children": []
        }
      ]
    },
    {
      "value": 3,
      "label": "广东省",
      "children": [
        {
          "value": 301,
          "label": "广州市",
          "children": [
            {"value": 3101, "label": "天河区", "children": []}
          ]
        }
      ]
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| value | Long | 区域ID |
| label | String | 区域名称 |
| children | Array | 下级区域列表（区级为空数组） |

---

### 2. 根据父级ID获取下级区域

- **URL**: `GET /api/region/children/{parentId}`
- **需要登录**: 否
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| parentId | Long | 父级区域ID（省级ID获取城市，市级ID获取区县） |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {"id": 101, "name": "东城区", "parentId": 1, "level": 2, "areaCode": "110101"},
    {"id": 103, "name": "朝阳区", "parentId": 1, "level": 2, "areaCode": "110105"}
  ]
}
```

---

### 3. 获取区域路径

- **URL**: `GET /api/region/path/{regionId}`
- **需要登录**: 否
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| regionId | Long | 区域ID（区级） |

**说明:** 返回 `[provinceId, cityId, districtId]` 数组，用于级联选择器回填。

**成功响应:**

```json
{
  "success": true,
  "data": [3, 301, 3101]
}
```

---

## 九、福利标签模块

### 1. 获取启用的福利标签

- **URL**: `GET /api/benefit-tag/list`
- **需要登录**: 否

**说明:** 返回所有已启用的福利标签，按排序权重降序排列。用于公司/职位编辑页的福利选择。

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | String | 否 | 标签类型筛选：company-公司福利/job-职位福利（不传返回全部） |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {"id": 1, "name": "五险一金", "sortOrder": 100, "enabled": 1},
    {"id": 2, "name": "六险二金", "sortOrder": 99, "enabled": 1}
  ]
}
```

---

### 2. 获取全部福利标签

- **URL**: `GET /api/benefit-tag/all`
- **需要登录**: 是（管理员）

**说明:** 返回所有标签（包括禁用的），用于后台管理。

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | String | 否 | 标签类型筛选：company-公司福利/job-职位福利（不传返回全部） |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {"id": 1, "name": "五险一金", "sortOrder": 100, "enabled": 1},
    {"id": 2, "name": "过时福利", "sortOrder": 0, "enabled": 0}
  ]
}
```

---

### 3. 新增福利标签

- **URL**: `POST /api/benefit-tag`
- **Content-Type**: `application/json`
- **需要登录**: 是（管理员）

**请求参数:**

```json
{
  "name": "新福利标签",
  "type": "company",
  "sortOrder": 50
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | String | 是 | 标签名称（唯一） |
| type | String | 否 | 标签类型：company/job，默认 company |
| sortOrder | Integer | 否 | 排序权重，默认0 |

**成功响应:**

```json
{
  "success": true,
  "data": {"id": 26, "name": "新福利标签", "sortOrder": 50, "enabled": 1}
}
```

**失败响应:**

```json
{
  "success": false,
  "message": "标签名称已存在"
}
```

---

### 4. 更新福利标签

- **URL**: `PUT /api/benefit-tag/{id}`
- **Content-Type**: `application/json`
- **需要登录**: 是（管理员）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 标签ID |

**请求参数:**

```json
{
  "name": "修改后的名称",
  "sortOrder": 80,
  "enabled": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | String | 否 | 标签名称 |
| sortOrder | Integer | 否 | 排序权重 |
| enabled | Integer | 否 | 是否启用：1=启用 0=禁用 |

**成功响应:**

```json
{
  "success": true,
  "message": "更新成功"
}
```

---

### 5. 删除福利标签

- **URL**: `DELETE /api/benefit-tag/{id}`
- **需要登录**: 是（管理员）
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 标签ID |

**成功响应:**

```json
{
  "success": true,
  "message": "删除成功"
}
```

---

## 十、面试模块

### 1. 创建面试

- **URL**: `POST /api/interview/create`
- **Content-Type**: `application/json`

**请求参数:**

```json
{
  "userId": 1,
  "resumeId": 1,
  "jobType": "后端开发"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户ID |
| resumeId | Long | 是 | 简历ID |
| jobType | String | 是 | 岗位类型（如：前端工程师、后端工程师、全栈工程师） |

**说明:** 创建面试时会自动生成10轮面试问题并存库。

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "resumeId": 1,
    "jobType": "后端开发",
    "totalRounds": 10,
    "currentRound": 0,
    "status": "PENDING",
    "createdAt": "2026-06-14T10:00:00"
  }
}
```

---

### 2. 获取面试详情

- **URL**: `GET /api/interview/{id}`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 面试ID |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "resumeId": 1,
    "jobType": "后端开发",
    "totalRounds": 10,
    "currentRound": 5,
    "status": "IN_PROGRESS",
    "totalScore": 7.5,
    "createdAt": "2026-06-14T10:00:00",
    "completedAt": null
  }
}
```

---

### 3. 获取面试历史

- **URL**: `GET /api/interview/{id}/history`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 面试ID |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "interviewId": 1,
      "round": 1,
      "question": "请介绍一下你自己",
      "answer": "我是...",
      "scores": "{\"accuracy\":8,\"clarity\":7,\"logic\":8,\"depth\":7,\"practice\":6,\"totalScore\":7.2}",
      "feedback": "回答较好，但可以更具体",
      "referenceAnswer": "参考答案：..."
    }
  ]
}
```

---

### 4. 获取用户面试列表

- **URL**: `GET /api/interview/user/{userId}`
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户ID |

**成功响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "resumeId": 1,
      "jobType": "后端开发",
      "totalRounds": 10,
      "currentRound": 10,
      "status": "COMPLETED",
      "totalScore": 7.5,
      "createdAt": "2026-06-14T10:00:00",
      "completedAt": "2026-06-14T10:30:00"
    }
  ]
}
```

---

### 5. 语义判断回答是否结束

- **URL**: `POST /api/interview/check-complete`
- **Content-Type**: `application/json`
- **需要登录**: 否（白名单）

**说明:** 前端录音过程中每隔几秒调用，由 DeepSeek 语义模型判断面试者的回答是否已结束。采用严格模式：只有包含明确结束语（如"回答完毕"、"我说完了"等）才返回 `true`。

**请求参数:**

```json
{
  "text": "我认为Java的垃圾回收机制主要依赖于..."
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| text | String | 是 | 面试者当前累积的回答文本 |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "complete": false
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| complete | Boolean | true=回答已结束，false=回答未结束 |

---

## 十一、AI 语音模块

### 1. 问题语音朗读（TTS）

- **URL**: `POST /api/interview/tts`
- **Content-Type**: `application/json`
- **需要登录**: 否（白名单）
- **响应**: `audio/mpeg`（MP3 音频字节流）

**说明:** 将面试问题文本转为语音，使用 MiMo-V2.5-TTS-VoiceDesign 模型，生成专业面试官语气的 MP3 音频。

**请求参数:**

```json
{
  "text": "请介绍一下你最熟悉的项目，你在其中扮演了什么角色？"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| text | String | 是 | 要朗读的文本 |

**成功响应:**

- HTTP 200
- Content-Type: `audio/mpeg`
- Body: MP3 音频字节流

**失败响应:**

- HTTP 500（空响应体）

---

### 2. 语音识别（STT）

- **URL**: `POST /api/stt`
- **Content-Type**: `multipart/form-data`
- **需要登录**: 否（白名单）

**说明:** 将音频文件转为文字，使用 MiMo-V2.5-ASR 模型。前端录音后需将 webm 格式转换为 WAV 再上传。

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | File | 是 | WAV 格式音频文件（Base64 编码后上限 10MB） |

**成功响应:**

```json
{
  "success": true,
  "data": "识别出的文字内容"
}
```

**失败响应:**

```json
{
  "success": false,
  "message": "语音识别失败: ASR 未返回识别结果"
}
```

---

## 十二、WebSocket 接口

### 1. 面试 WebSocket

- **连接地址**: `ws://{host}/ws/interview?id={interviewId}`

#### 1.1 文字面试消息

**开始面试（发送）:**

```json
{
  "type": "start",
  "interviewId": 1
}
```

**接收（第一个问题）:**

```json
{
  "type": "question",
  "round": 1,
  "question": "请介绍一下你自己"
}
```

**提交回答（发送）:**

```json
{
  "type": "answer",
  "interviewId": 1,
  "round": 1,
  "content": "我的回答内容...",
  "question": "请介绍一下你自己"
}
```

**接收（评分反馈）:**

```json
{
  "type": "evaluation",
  "round": 1,
  "score": {
    "accuracy": 8,
    "clarity": 7,
    "logic": 8,
    "depth": 7,
    "practice": 6,
    "totalScore": 7.2,
    "feedback": "回答较好",
    "referenceAnswer": "参考答案..."
  },
  "feedback": "回答较好",
  "referenceAnswer": "参考答案..."
}
```

**接收（下一个问题）:**

```json
{
  "type": "question",
  "round": 2,
  "question": "请描述一下你最熟悉的项目"
}
```

---

### 3. 提前结束面试

**发送:**

```json
{
  "type": "exit",
  "interviewId": 1
}
```

**接收（面试完成）:**

```json
{
  "type": "completed",
  "feedback": "面试已提前结束，正在生成评估报告...",
  "report": "报告内容..."
}
```

---

### 4. 面试完成（正常结束）

**接收:**

```json
{
  "type": "completed",
  "report": "报告内容..."
}
```

---

#### 1.2 沉浸式面试消息

**启动沉浸式面试（发送）:**

```json
{
  "type": "immersive_start",
  "interviewId": 1
}
```

**接收（下一个问题）:**

```json
{
  "type": "next_question",
  "round": 1,
  "question": "请介绍一下你自己",
  "totalRounds": 10
}
```

**沉浸式回答评分（接收）:**

```json
{
  "type": "evaluation",
  "round": 1,
  "score": {
    "accuracy": 8,
    "clarity": 7,
    "logic": 8,
    "depth": 7,
    "practice": 6,
    "totalScore": 7.2,
    "feedback": "回答较好",
    "referenceAnswer": "参考答案..."
  },
  "feedback": "回答较好",
  "referenceAnswer": "参考答案...",
  "command": true
}
```

**沉浸式退出（发送）:**

```json
{
  "type": "immersive_exit",
  "interviewId": 1
}
```

---

### 2. ASR WebSocket（实时语音识别）

- **连接地址**: `ws://{host}/ws/asr`
- **说明**: 每轮面试建立一次连接，用于实时语音识别和后端编排评分

#### 2.1 初始化（每轮开始时发送）

**发送:**

```json
{
  "type": "asr_init",
  "interviewId": 1,
  "round": 1
}
```

#### 2.2 发送音频分片（录音过程中）

**发送:**

```json
{
  "type": "audio_chunk",
  "audio": "base64_wav音频数据"
}
```

#### 2.3 发送最终音频（录音结束时）

**发送:**

```json
{
  "type": "audio_final",
  "audio": "base64_wav完整音频数据"
}
```

#### 2.4 接收实时识别结果

```json
{
  "type": "asr_result",
  "text": "累积识别文本",
  "isFinal": false
}
```

#### 2.5 接收最终识别结果

```json
{
  "type": "asr_final",
  "text": "最终识别文本",
  "isFinal": true
}
```

#### 2.6 接收评分结果（后端自动编排）

```json
{
  "type": "asr_evaluation",
  "round": 1,
  "score": {
    "accuracy": 8,
    "clarity": 7,
    "logic": 8,
    "depth": 7,
    "practice": 6,
    "totalScore": 7.2
  },
  "feedback": "回答较好",
  "referenceAnswer": "参考答案...",
  "isLastRound": false
}
```

#### 2.7 接收错误

```json
{
  "type": "asr_error",
  "message": "错误信息"
}
```

---

### 3. 聊天 WebSocket

- **连接地址**: `ws://{host}/ws/chat?token={jwtToken}&conversationId={conversationId}`
- **说明**: conversationId 由两个用户ID拼接（小ID在前），如 `3_5`

#### 客户端→服务端

| type | 说明 | 字段 |
|------|------|------|
| text | 文字消息 | content, jobId(可选) |
| resume | 简历附件 | resumeId, content(描述), jobId(可选) |
| read | 标记已读 | 无 |
| ack | 消息确认 | messageId |
| video_invite | 发起视频面试 | jobId(可选) |
| video_accept | 接受视频面试 | 无 |
| video_reject | 拒绝视频面试 | 无 |
| video_offer | WebRTC Offer | receiverId, sdp |
| video_answer | WebRTC Answer | receiverId, sdp |
| video_ice | ICE Candidate | receiverId, candidate |
| video_hangup | 挂断视频通话 | duration(可选, 通话时长) |
| video_media | 视频中继数据 | data(base64), init(boolean) |

**发送文字消息:**

```json
{
  "type": "text",
  "content": "你好，想了解一下这个职位",
  "jobId": 5
}
```

**发送简历附件:**

```json
{
  "type": "resume",
  "resumeId": 1,
  "content": "请查看我的简历",
  "jobId": 5
}
```

**标记已读:**

```json
{
  "type": "read"
}
```

#### 服务端→客户端

| type | 说明 | 字段 |
|------|------|------|
| history | 历史消息列表 | messages: ChatMessage[] |
| message | 新消息推送 | data: ChatMessage |
| read | 对方已读通知 | userId |
| ack | 消息已送达 | messageId, userId |
| ack_timeout | 消息投递超时 | data: ChatMessage |
| video_invite | 视频面试邀请 | senderId, jobId(可选) |
| video_accept | 对方接受了视频面试 | senderId |
| video_reject | 对方拒绝了视频面试 | senderId |
| video_hangup | 对方挂断了视频通话 | senderId |
| video_offer | WebRTC Offer | senderId, sdp |
| video_answer | WebRTC Answer | senderId, sdp |
| video_ice | ICE Candidate | senderId, candidate |
| video_media | 视频中继数据 | senderId, data(base64), init(boolean) |
| error | 错误 | message |

**接收历史消息:**

```json
{
  "type": "history",
  "messages": [
    {
      "id": "xxx",
      "conversationId": "3_5",
      "senderId": 3,
      "receiverId": 5,
      "jobId": 5,
      "type": "resume",
      "content": "这是我的简历",
      "resumeId": 1,
      "resumeName": "张三简历.pdf",
      "read": true,
      "status": "sent",
      "createdAt": "2026-06-18T10:00:00"
    }
  ]
}
```

**接收新消息:**

```json
{
  "type": "message",
  "data": {
    "id": "xxx",
    "conversationId": "3_5",
    "senderId": 5,
    "receiverId": 3,
    "type": "text",
    "content": "你好，收到简历了",
    "status": "sending",
    "createdAt": "2026-06-18T10:05:00"
  }
}
```

#### 二进制消息（视频中继）

WebSocket 支持二进制帧传输，用于 WebRTC P2P 连接失败时的视频中继（Media Relay）回退模式。

**帧格式:**

```
[1B flags][2B codec长度][NB codec字符串][8B senderId][剩余: 视频数据]
```

| 字节偏移 | 长度 | 说明 |
|---|---|---|
| 0 | 1 字节 | 标志位（保留） |
| 1-2 | 2 字节 | codec 字符串长度（大端序） |
| 3-N | N 字节 | codec 字符串（如 "video/webm; codecs=vp8,opus"） |
| N+1-N+8 | 8 字节 | 发送者用户 ID（大端序 long） |
| N+9+ | 剩余 | 编码后的视频数据 |

**服务端处理:** 零解析转发 — 仅校验最小帧长度（11 字节），然后将原始二进制帧转发给会话中的另一方。服务端不解析视频内容。

**视频中继参数（P2P 失败时降级）:**

| 参数 | 值 | 说明 |
|---|---|---|
| 分辨率 | 320x240 | 降低带宽占用 |
| 帧率 | 15fps | 减少每秒数据量 |
| 码率 | 200kbps | 总带宽约 232kbps |
| 音频 | opus 32kbps | 低码率语音编码 |
| 分片间隔 | 200ms | MediaRecorder timeslice |

**video_media 文本消息（Base64 中继）:**

当使用文本消息通道中继视频时，消息格式如下：

```json
{
  "type": "video_media",
  "data": "<base64 编码的 webm/opus 数据>",
  "init": false
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data | String | base64 编码的媒体分片数据 |
| init | Boolean | true 表示初始化段（MediaSource init segment），false 表示媒体数据段 |

---

## 十三、SSE 接口

### 1. 订阅消息通知

- **URL**: `GET /api/message/subscribe?token={jwtToken}`
- **需要登录**: 是（通过 URL 参数携带 Token，因为浏览器 EventSource 不支持设置 Header）
- **Content-Type**: `text/event-stream`

**说明:** 建立 SSE 长连接，用于接收实时消息通知。Token 通过 query 参数传递。

**前端行为:**
- 收到 `message` 事件后，弹出通知弹窗（非消息页时）并立即调用 `GET /api/message/unread-count` 刷新未读数 badge
- 收到 `video_invite` 事件后，弹出视频邀约对话框
- SSE 连接成功后停止轮询兜底；SSE 断开后自动启动 5 秒轮询并 3 秒后重连

**事件类型:**

| event | 说明 | data格式 |
|-------|------|----------|
| message | 新消息通知 | `{senderId, senderName, content, jobId, type}` |
| video_invite | 视频面试邀请 | `{type, senderId, conversationId, jobId}` |

**message 事件 data 字段说明:**

| 字段 | 类型 | 说明 |
|---|---|---|
| senderId | Long | 发送者ID |
| senderName | String | 发送者昵称 |
| content | String | 消息内容 |
| jobId | Long | 关联职位ID（可选） |
| type | String | 消息类型：chat/application/system |

**video_invite 事件 data 字段说明:**

| 字段 | 类型 | 说明 |
|---|---|---|
| type | String | 固定值 "video_invite" |
| senderId | Long | 发起者（HR）用户ID |
| conversationId | String | 会话ID |
| jobId | Long | 关联职位ID（可选） |

---

## 十四、ElasticSearch 搜索模块

> 基于 ElasticSearch 8.12.0 + IK 分词器，提供全文搜索能力。
> 所有搜索接口均在 JWT 白名单中，无需登录即可访问。

### 1. 职位搜索（ES）

- **URL**: `GET /api/search/jobs`
- **需要登录**: 否

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | String | 否 | 搜索关键词（多字段匹配：title、description、requirements、companyName） |
| city | String | 否 | 城市精确匹配 |
| categoryId | Long | 否 | 职位分类ID精确匹配 |
| experience | String | 否 | 经验要求精确匹配 |
| education | String | 否 | 学历要求精确匹配 |
| jobType | String | 否 | 工作类型精确匹配（full_time/part_time/intern） |
| salaryMin | Integer | 否 | 薪资下限（筛选 salaryMax >= 该值） |
| salaryMax | Integer | 否 | 薪资上限（筛选 salaryMin <= 该值） |
| sort | String | 否 | 排序方式：publishedAt（默认）/ salary / viewCount / applyCount |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页数量（默认20） |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "total": 1989,
    "totalHitsRelation": "eq",
    "records": [
      {
        "id": 1,
        "companyId": 5,
        "companyName": "字节跳动",
        "categoryId": 6,
        "categoryName": "Java开发",
        "title": "Java开发工程师",
        "city": "北京",
        "address": "北京市海淀区科技园",
        "salaryMin": 15000,
        "salaryMax": 30000,
        "jobType": "full_time",
        "experience": "3-5年",
        "education": "本科",
        "description": "负责核心业务系统的开发和维护...",
        "requirements": "1. 3年以上Java开发经验...",
        "benefits": "[\"五险一金\",\"带薪年假\"]",
        "status": "active",
        "viewCount": 256,
        "applyCount": 18,
        "publishedAt": "2026-06-18"
      }
    ],
    "page": 1,
    "size": 20,
    "totalPages": 100
  }
}
```

**totalHitsRelation 说明:**

| 值 | 含义 |
|---|---|
| `eq` | 精确匹配，total 为准确数量 |
| `gte` | 大于等于，total 为 ES 默认上限 10000，实际数量可能更多 |

---

### 2. 公司搜索（ES）

- **URL**: `GET /api/search/companies`
- **需要登录**: 否

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | String | 否 | 搜索关键词（多字段匹配：name、description） |
| city | String | 否 | 城市精确匹配 |
| industry | String | 否 | 行业精确匹配 |
| scale | String | 否 | 公司规模精确匹配 |
| sort | String | 否 | 排序方式：jobCount（默认）/ viewCount / followCount |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页数量（默认20） |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "total": 22,
    "totalHitsRelation": "eq",
    "records": [
      {
        "id": 1,
        "name": "字节跳动",
        "industry": "互联网",
        "scale": "10000人以上",
        "type": "民营企业",
        "city": "北京",
        "address": "北京市海淀区中关村软件园",
        "description": "字节跳动是一家全球性的科技公司...",
        "benefits": "[\"五险一金\",\"带薪年假\"]",
        "viewCount": 1256,
        "followCount": 89,
        "jobCount": 128
      }
    ],
    "page": 1,
    "size": 20,
    "totalPages": 2
  }
}
```

---

### 3. 全量同步

- **URL**: `POST /api/search/sync`
- **需要登录**: 否
- **说明**: 将 MySQL 中所有职位和公司数据全量同步到 ES（开发/测试用）

**成功响应:**

```json
{
  "success": true,
  "data": "全量同步完成"
}
```

---

### 4. 同步单个职位

- **URL**: `POST /api/search/sync/job/{jobId}`
- **需要登录**: 否
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| jobId | Long | 职位ID |

**成功响应:**

```json
{
  "success": true,
  "data": "职位同步完成"
}
```

---

### 5. 同步单个公司

- **URL**: `POST /api/search/sync/company/{companyId}`
- **需要登录**: 否
- **路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| companyId | Long | 公司ID |

**成功响应:**

```json
{
  "success": true,
  "data": "公司同步完成"
}
```

---

## 十五、用户档案模块

### 1. 获取用户档案

- **URL**: `GET /api/user-profile/{userId}`
- **需要登录**: 是

**说明:** 获取用户的扩展档案信息（学历、求职意向、经历等）。若用户尚无档案，返回空对象。

**路径参数:**

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户ID |

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 10,
    "education": "本科",
    "personalSummary": "3年前端开发经验，熟悉 Vue、React...",
    "jobStatus": "looking",
    "expectCity": "成都",
    "expectSalaryMin": 10000,
    "expectSalaryMax": 20000,
    "expectJobType": "前端开发",
    "workExperience": "[{\"company\":\"XX科技\",\"position\":\"前端工程师\",\"startDate\":\"2023-07\",\"endDate\":\"\",\"description\":\"负责公司后台管理系统开发\"}]",
    "projectExperience": "[{\"name\":\"XX平台\",\"role\":\"前端负责人\",\"startDate\":\"2024-01\",\"endDate\":\"2024-06\",\"description\":\"使用Vue3+TypeScript开发\"}]",
    "educationExperience": "[{\"school\":\"XX大学\",\"major\":\"计算机科学\",\"degree\":\"本科\",\"startDate\":\"2019-09\",\"endDate\":\"2023-06\"}]",
    "certificates": "[{\"name\":\"PMP\",\"issuer\":\"PMI\",\"obtainDate\":\"2024-05\"}]",
    "createdAt": "2026-06-20T10:00:00",
    "updatedAt": "2026-06-20T12:00:00"
  }
}
```

### 2. 保存/更新用户档案

- **URL**: `PUT /api/user-profile/{userId}`
- **需要登录**: 是
- **请求体:**

```json
{
  "education": "本科",
  "personalSummary": "3年前端开发经验",
  "jobStatus": "looking",
  "expectCity": "成都",
  "expectSalaryMin": 10000,
  "expectSalaryMax": 20000,
  "expectJobType": "前端开发",
  "workExperience": "[{\"company\":\"XX科技\",\"position\":\"前端工程师\",\"startDate\":\"2023-07\",\"endDate\":\"\",\"description\":\"负责后台管理系统\"}]",
  "projectExperience": "[]",
  "educationExperience": "[{\"school\":\"XX大学\",\"major\":\"计算机科学\",\"degree\":\"本科\",\"startDate\":\"2019-09\",\"endDate\":\"2023-06\"}]",
  "certificates": "[]"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| education | String | 否 | 最高学历：高中/大专/本科/硕士/博士 |
| personalSummary | String | 否 | 个人优势/自我介绍 |
| jobStatus | String | 否 | 求职状态：looking-找工作 not_looking-暂不考虑 available-随时到岗 |
| expectCity | String | 否 | 期望城市 |
| expectSalaryMin | Integer | 否 | 期望最低薪资（元/月） |
| expectSalaryMax | Integer | 否 | 期望最高薪资（元/月） |
| expectJobType | String | 否 | 期望职位类型 |
| workExperience | String(JSON) | 否 | 工作/实习经历 JSON 数组 |
| projectExperience | String(JSON) | 否 | 项目经历 JSON 数组 |
| educationExperience | String(JSON) | 否 | 教育经历 JSON 数组 |
| certificates | String(JSON) | 否 | 资格证书 JSON 数组 |

**说明:** 使用 upsert 语义——首次调用创建档案，后续调用更新已有档案。经历类字段为 JSON 字符串，前端需 `JSON.stringify` 后提交。

**JSON 数组结构:**

```json
// workExperience 工作经历
[{
  "company": "公司名称",
  "position": "职位",
  "startDate": "2023-07",
  "endDate": "2025-01",
  "description": "工作描述"
}]

// projectExperience 项目经历
[{
  "name": "项目名称",
  "role": "担任角色",
  "startDate": "2024-01",
  "endDate": "2024-06",
  "description": "项目描述"
}]

// educationExperience 教育经历
[{
  "school": "学校名称",
  "major": "专业",
  "degree": "学历",
  "startDate": "2019-09",
  "endDate": "2023-06"
}]

// certificates 资格证书
[{
  "name": "证书名称",
  "issuer": "颁发机构",
  "obtainDate": "2024-05"
}]
```

**成功响应:**

```json
{
  "success": true,
  "data": "档案保存成功"
}
```

---

## 十六、管理后台模块

> 管理后台所有接口路径前缀为 `/api/admin/**`，需要管理员角色（roleType=3）。
> 管理员登录复用 `POST /api/user/login`，前端根据返回的 `roleType === 3` 判断权限。
> 管理后台接口统一使用独立响应格式：`{code: 200, msg: "success", data: ...}`。

### 1. 管理员登录

- **URL**: `POST /api/user/login`
- **Content-Type**: `application/json`
- **说明**: 复用现有登录接口，前端判断 roleType

**请求参数:**

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**成功响应:**

```json
{
  "success": true,
  "data": {
    "id": 35,
    "username": "admin",
    "nickname": "超级管理员",
    "roleType": 3,
    "companyId": null,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**前端逻辑:**
- `roleType === 3` → 跳转 `/admin/dashboard`，存储 `admin_token`
- `roleType === 1/2` → 跳转前台首页

---

### 2. 获取管理员信息

- **URL**: `GET /api/admin/profile`
- **需要登录**: 是（管理员角色）
- **响应格式**: `{code, msg, data}`

**成功响应:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 35,
    "username": "admin",
    "nickname": "超级管理员",
    "avatar": null,
    "roleType": 3
  }
}
```

---

### 3. 数据概览仪表盘

#### 3.1 获取统计数据

- **URL**: `GET /api/admin/dashboard/summary`
- **需要登录**: 是（管理员角色）

**响应:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "totalUsers": 1234,
    "todayUsers": 12,
    "totalCompanies": 56,
    "todayCompanies": 2,
    "totalJobs": 789,
    "todayJobs": 8,
    "totalApplications": 456,
    "todayApplications": 15,
    "totalInterviews": 234,
    "todayInterviews": 5,
    "pendingJobs": 12,
    "pendingApplications": 23
  }
}
```

#### 3.2 用户增长趋势

- **URL**: `GET /api/admin/dashboard/user-trend`
- **参数**: `days` (int, 默认30)

**响应:**

```json
{
  "code": 200,
  "data": [
    {"date": "2026-06-01", "count": 12},
    {"date": "2026-06-02", "count": 8}
  ]
}
```

#### 3.3 投递趋势

- **URL**: `GET /api/admin/dashboard/application-trend`
- **参数**: `days` (int, 默认30)

#### 3.4 职位类型分布

- **URL**: `GET /api/admin/dashboard/job-type`

**响应:**

```json
{
  "code": 200,
  "data": [
    {"name": "全职", "value": 450},
    {"name": "兼职", "value": 120},
    {"name": "实习", "value": 80}
  ]
}
```

#### 3.5 面试状态分布

- **URL**: `GET /api/admin/dashboard/interview-status`

#### 3.6 行业职位 TOP10

- **URL**: `GET /api/admin/dashboard/job-industry`

#### 3.7 城市职位 TOP10

- **URL**: `GET /api/admin/dashboard/job-city`

#### 3.8 待处理事项

- **URL**: `GET /api/admin/dashboard/pending-tasks`

**响应:**

```json
{
  "code": 200,
  "data": {
    "pendingJobs": 12,
    "pendingApplications": 23,
    "disabledUsers": 5,
    "pendingDescriptions": [
      {"type": "job_audit", "count": 12, "description": "待审核职位"},
      {"type": "application", "count": 23, "description": "待处理投递"}
    ]
  }
}
```

#### 3.9 最近活动

- **URL**: `GET /api/admin/dashboard/recent-activity`

---

### 4. 用户管理

#### 4.1 用户列表

- **URL**: `GET /api/admin/users`
- **需要登录**: 是（管理员角色）

**参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（用户名/手机/邮箱） |
| roleType | Integer | 否 | 角色类型筛选：1-求职者 2-HR 3-管理员 |
| status | String | 否 | 状态筛选：active/disabled |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页条数（默认10） |

**响应:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "zhangsan",
        "nickname": "张三",
        "phone": "13800138000",
        "email": "zhangsan@example.com",
        "roleType": 1,
        "roleName": "求职者",
        "status": "active",
        "createdAt": "2026-01-01 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

#### 4.2 用户详情

- **URL**: `GET /api/admin/users/{id}`

**响应:**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "avatar": "https://...",
    "roleType": 1,
    "roleName": "求职者",
    "status": "active",
    "companyId": null,
    "createdAt": "2026-01-01 10:00:00",
    "profile": {
      "education": "本科",
      "jobStatus": "looking",
      "expectCity": "北京"
    },
    "resumeCount": 2
  }
}
```

#### 4.3 修改用户状态

- **URL**: `PUT /api/admin/users/{id}/status`
- **Content-Type**: `application/json`

**请求:**

```json
{
  "status": "disabled"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 是 | active-启用 disabled-禁用 |

#### 4.4 修改用户角色

- **URL**: `PUT /api/admin/users/{id}/role`
- **Content-Type**: `application/json`

**请求:**

```json
{
  "roleType": 2
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleType | Integer | 是 | 1-求职者 2-HR 3-管理员 |

#### 4.5 删除用户

- **URL**: `DELETE /api/admin/users/{id}`

#### 4.6 批量导入用户

- **URL**: `POST /api/admin/users/import`
- **Content-Type**: `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | Excel文件（.xlsx） |

**响应:**

```json
{
  "code": 200,
  "data": {
    "successCount": 48,
    "failCount": 2,
    "failures": [
      {"row": 3, "reason": "用户名已存在"},
      {"row": 15, "reason": "手机号格式错误"}
    ]
  }
}
```

#### 4.7 导出用户列表

- **URL**: `GET /api/admin/users/export`
- **响应**: Excel 文件流

---

### 5. 公司管理

#### 5.1 公司列表

- **URL**: `GET /api/admin/companies`
- **需要登录**: 是（管理员角色）

**参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（公司名称） |
| industry | String | 否 | 行业筛选 |
| city | String | 否 | 城市筛选 |
| scale | String | 否 | 规模筛选 |
| status | String | 否 | 状态筛选：active/disabled |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页条数（默认10） |

**响应:**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "name": "阿里巴巴",
        "industry": "互联网",
        "scale": "10000人以上",
        "city": "杭州",
        "status": "active",
        "jobCount": 45,
        "viewCount": 1234,
        "followCount": 89,
        "createdAt": "2026-01-01"
      }
    ],
    "total": 56,
    "page": 1,
    "size": 10
  }
}
```

#### 5.2 公司详情

- **URL**: `GET /api/admin/companies/{id}`

#### 5.3 修改公司状态

- **URL**: `PUT /api/admin/companies/{id}/status`
- **Content-Type**: `application/json`

**请求:**

```json
{
  "status": "disabled"
}
```

#### 5.4 删除公司

- **URL**: `DELETE /api/admin/companies/{id}`

#### 5.5 批量导入公司

- **URL**: `POST /api/admin/companies/import`
- **Content-Type**: `multipart/form-data`

#### 5.6 导出公司列表

- **URL**: `GET /api/admin/companies/export`

---

### 6. 职位管理

#### 6.1 职位列表

- **URL**: `GET /api/admin/jobs`
- **需要登录**: 是（管理员角色）

**参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（职位名称） |
| companyName | String | 否 | 公司名称筛选 |
| city | String | 否 | 城市筛选 |
| jobType | String | 否 | 工作类型筛选 |
| education | String | 否 | 学历要求筛选 |
| status | String | 否 | 状态筛选：pending/active/paused/closed/rejected |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页条数（默认10） |

**响应:**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "title": "Java开发工程师",
        "companyName": "阿里巴巴",
        "companyLogo": "https://...",
        "city": "杭州",
        "salary": "20-40K",
        "jobType": "full_time",
        "education": "本科",
        "experience": "3-5年",
        "status": "active",
        "viewCount": 567,
        "applyCount": 23,
        "publishedAt": "2026-01-15"
      }
    ],
    "total": 789,
    "page": 1,
    "size": 10
  }
}
```

#### 6.2 职位详情

- **URL**: `GET /api/admin/jobs/{id}`

#### 6.3 审核职位

- **URL**: `PUT /api/admin/jobs/{id}/status`
- **Content-Type**: `application/json`

**请求:**

```json
{
  "status": "active",
  "reason": ""
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 是 | active=通过 rejected=拒绝 |
| reason | String | 否 | 审核原因（拒绝时必填） |

**说明:** 审核通过/拒绝时自动记录到 `job_audit_log` 表。

#### 6.4 删除职位

- **URL**: `DELETE /api/admin/jobs/{id}`

#### 6.5 获取职位申请列表

- **URL**: `GET /api/admin/jobs/{id}/applications`

#### 6.6 批量导入职位

- **URL**: `POST /api/admin/jobs/import`
- **Content-Type**: `multipart/form-data`

#### 6.7 导出职位列表

- **URL**: `GET /api/admin/jobs/export`

---

### 7. 申请管理

#### 7.1 申请列表

- **URL**: `GET /api/admin/applications`
- **需要登录**: 是（管理员角色）

**参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（求职者/职位） |
| company | String | 否 | 公司筛选 |
| status | String | 否 | 状态筛选 |
| startDate | String | 否 | 开始日期 |
| endDate | String | 否 | 结束日期 |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页条数（默认10） |

**响应:**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "jobId": 10,
        "jobTitle": "Java开发工程师",
        "companyName": "阿里巴巴",
        "userId": 2,
        "username": "张三",
        "nickname": "张三",
        "resumeId": 1,
        "status": "pending",
        "hrRemark": null,
        "appliedAt": "2026-06-15 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

#### 7.2 申请详情

- **URL**: `GET /api/admin/applications/{id}`

#### 7.3 更新申请状态

- **URL**: `PUT /api/admin/applications/{id}/status`
- **Content-Type**: `application/json`

**请求:**

```json
{
  "status": "screening",
  "hrRemark": "简历不错，安排面试"
}
```

#### 7.4 删除申请

- **URL**: `DELETE /api/admin/applications/{id}`

#### 7.5 导出申请列表

- **URL**: `GET /api/admin/applications/export`

---

### 8. 面试管理

#### 8.1 面试列表

- **URL**: `GET /api/admin/interviews`
- **需要登录**: 是（管理员角色）

**参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（求职者/职位） |
| company | String | 否 | 公司筛选 |
| type | String | 否 | 面试类型：ai/video/onsite |
| status | String | 否 | 状态筛选：pending/in_progress/completed/cancelled |
| startDate | String | 否 | 开始日期 |
| endDate | String | 否 | 结束日期 |
| page | Integer | 否 | 页码（默认1） |
| size | Integer | 否 | 每页条数（默认10） |

**响应:**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 2,
        "username": "张三",
        "nickname": "张三",
        "avatar": "https://...",
        "education": "本科",
        "experience": "3年",
        "jobTitle": "Java开发工程师",
        "companyName": "阿里巴巴",
        "companyLogo": "https://...",
        "round": 1,
        "type": "ai",
        "totalScore": 85,
        "techScore": 90,
        "commScore": 80,
        "logicScore": 85,
        "status": "completed",
        "duration": 45,
        "startTime": "2026-06-15 10:00:00"
      }
    ],
    "total": 234,
    "page": 1,
    "size": 10
  }
}
```

#### 8.2 面试详情

- **URL**: `GET /api/admin/interviews/{id}`

#### 8.3 面试报告

- **URL**: `GET /api/admin/interviews/{id}/report`

**响应:**

```json
{
  "code": 200,
  "data": {
    "interviewId": 1,
    "overallScore": 85,
    "techScore": 90,
    "communicationScore": 80,
    "logicScore": 85,
    "aiComment": "候选人技术能力较强，沟通表达清晰...",
    "summary": "整体表现优秀",
    "strengths": ["Java基础扎实", "项目经验丰富"],
    "weaknesses": ["系统设计经验不足"],
    "suggestions": ["建议加强分布式系统学习"]
  }
}
```

#### 8.4 删除面试记录

- **URL**: `DELETE /api/admin/interviews/{id}`

#### 8.5 导出面试列表

- **URL**: `GET /api/admin/interviews/export`

---

### 9. 福利标签管理

#### 9.1 标签列表（含使用统计）

- **URL**: `GET /api/admin/benefit-tags`
- **需要登录**: 是（管理员角色）

**响应:**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "五险一金",
      "type": "company",
      "color": "#409EFF",
      "sortOrder": 100,
      "enabled": 1,
      "companyCount": 45,
      "jobCount": 120,
      "createdAt": "2026-01-01"
    }
  ]
}
```

#### 9.2 新增标签

- **URL**: `POST /api/admin/benefit-tags`
- **Content-Type**: `application/json`

**请求:**

```json
{
  "name": "新福利标签",
  "type": "company",
  "sortOrder": 50,
  "description": "标签描述"
}
```

#### 9.3 编辑标签

- **URL**: `PUT /api/admin/benefit-tags/{id}`
- **Content-Type**: `application/json`

#### 9.4 删除标签

- **URL**: `DELETE /api/admin/benefit-tags/{id}`

**说明:** 删除时同步清理 `company_benefit` 和 `job_benefit` 中间表。

#### 9.5 标签使用详情

- **URL**: `GET /api/admin/benefit-tags/{id}/usage`

**响应:**

```json
{
  "code": 200,
  "data": {
    "tagId": 1,
    "tagName": "五险一金",
    "companies": [
      {"id": 1, "name": "阿里巴巴"},
      {"id": 2, "name": "腾讯"}
    ],
    "jobs": [
      {"id": 1, "title": "Java开发", "companyName": "阿里巴巴"}
    ]
  }
}
```

---

### 10. 职位分类管理

#### 10.1 分类树（含职位数统计）

- **URL**: `GET /api/admin/job-categories`
- **需要登录**: 是（管理员角色）

**响应:**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "技术开发",
      "parentId": 0,
      "icon": "Monitor",
      "color": "#409EFF",
      "sortOrder": 1,
      "status": "active",
      "description": "技术开发类职位",
      "jobCount": 234,
      "createdAt": "2026-01-01",
      "children": [
        {
          "id": 6,
          "name": "Java开发",
          "parentId": 1,
          "jobCount": 120,
          "children": []
        }
      ]
    }
  ]
}
```

#### 10.2 新增分类

- **URL**: `POST /api/admin/job-categories`
- **Content-Type**: `application/json`

**请求:**

```json
{
  "name": "新分类",
  "parentId": 0,
  "icon": "Monitor",
  "sortOrder": 10,
  "description": "分类描述"
}
```

#### 10.3 编辑分类

- **URL**: `PUT /api/admin/job-categories/{id}`
- **Content-Type**: `application/json`

#### 10.4 删除分类

- **URL**: `DELETE /api/admin/job-categories/{id}`

**说明:** 有关联职位时拒绝删除。

---

### 11. 地区管理

#### 11.1 地区树（含关联统计）

- **URL**: `GET /api/admin/regions`
- **需要登录**: 是（管理员角色）

**响应:**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "浙江省",
      "parentId": 0,
      "level": 1,
      "areaCode": "330000",
      "sortOrder": 1,
      "status": "active",
      "jobCount": 567,
      "companyCount": 89,
      "children": [
        {
          "id": 2,
          "name": "杭州市",
          "parentId": 1,
          "level": 2,
          "jobCount": 345,
          "companyCount": 56,
          "children": []
        }
      ]
    }
  ]
}
```

#### 11.2 新增地区

- **URL**: `POST /api/admin/regions`
- **Content-Type**: `application/json`

**请求:**

```json
{
  "name": "新地区",
  "parentId": 0,
  "areaCode": "330000",
  "sortOrder": 10
}
```

#### 11.3 编辑地区

- **URL**: `PUT /api/admin/regions/{id}`
- **Content-Type**: `application/json`

#### 11.4 删除地区

- **URL**: `DELETE /api/admin/regions/{id}`

**说明:** 有子节点时连同子节点一起删除。

---

## 十七、数据结构说明

> **审计字段说明**: 所有表均包含以下审计字段，后续表格中省略不列：
> - `createdAt` (LocalDateTime) - 创建时间，自动填充
> - `createdBy` (Long) - 创建人ID，自动填充当前登录用户
> - `updatedAt` (LocalDateTime) - 更新时间，自动填充
> - `updatedBy` (Long) - 修改人ID，自动填充当前登录用户

### User（用户）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| username | String | 用户名（唯一） |
| password | String | 密码 |
| nickname | String | 昵称 |
| avatar | String | 头像URL |
| phone | String | 手机号 |
| email | String | 邮箱 |
| roleType | Integer | 角色类型：1-求职者 2-HR 3-管理员 |
| companyId | Long | 关联公司ID（HR角色） |
| status | String | 账号状态：active/disabled |
| createdAt | LocalDateTime | 创建时间 |
| createdBy | Long | 创建人ID |
| updatedAt | LocalDateTime | 更新时间 |
| updatedBy | Long | 修改人ID |

---

### UserProfile（用户档案）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| userId | Long | 关联用户ID（唯一） |
| education | String | 最高学历：高中/大专/本科/硕士/博士 |
| personalSummary | String | 个人优势/自我介绍 |
| jobStatus | String | 求职状态：looking/not_looking/available |
| expectCity | String | 期望城市 |
| expectSalaryMin | Integer | 期望最低薪资 |
| expectSalaryMax | Integer | 期望最高薪资 |
| expectJobType | String | 期望职位类型 |
| workExperience | String(JSON) | 工作/实习经历 JSON 数组 |
| projectExperience | String(JSON) | 项目经历 JSON 数组 |
| educationExperience | String(JSON) | 教育经历 JSON 数组 |
| certificates | String(JSON) | 资格证书 JSON 数组 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

### Company（公司）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| userId | Long | 创建者HR用户ID |
| name | String | 公司名称 |
| logo | String | 公司Logo URL |
| industry | String | 所属行业 |
| scale | String | 公司规模 |
| type | String | 公司类型 |
| regionId | Long | 区域ID（区级，关联region表） |
| city | String | 所在城市 |
| address | String | 详细地址 |
| website | String | 公司官网 |
| description | String | 公司介绍 |
| status | String | 状态：active-正常 disabled-禁用 |
| viewCount | Long | 浏览量 |
| followCount | Long | 关注数 |
| createdAt | LocalDateTime | 创建时间 |
| createdBy | Long | 创建人ID |
| updatedAt | LocalDateTime | 更新时间 |
| updatedBy | Long | 修改人ID |

---

### JobCategory（职位分类）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| name | String | 分类名称 |
| parentId | Long | 父分类ID（0为顶级） |
| sortOrder | Integer | 排序权重 |
| status | String | 状态：active-启用 disabled-禁用 |
| description | String | 分类描述 |
| createdAt | LocalDateTime | 创建时间 |

---

### Job（职位）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| companyId | Long | 公司ID |
| userId | Long | 负责HR用户ID（NULL表示待认领） |
| categoryId | Long | 职位分类ID |
| title | String | 职位名称 |
| city | String | 工作城市 |
| address | String | 工作地点 |
| salaryMin | Integer | 薪资下限 |
| salaryMax | Integer | 薪资上限 |
| jobType | String | 工作类型：full_time/part_time/intern |
| experience | String | 经验要求 |
| education | String | 学历要求 |
| description | String | 职位描述 |
| requirements | String | 任职要求 |
| status | String | 状态：pending-待审核 active-招聘中 paused-暂停 closed-已关闭 rejected-已拒绝 |
| viewCount | Integer | 浏览次数 |
| applyCount | Integer | 申请次数 |
| publishedAt | LocalDateTime | 发布时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

### Application（申请）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| jobId | Long | 职位ID |
| userId | Long | 求职者用户ID |
| resumeId | Long | 简历ID |
| status | String | 状态：pending/screening/interview/offer/rejected/withdrawn |
| hrRemark | String | HR备注 |
| jobDeleted | Integer | 职位是否已删除：0-未删除 1-已删除 |
| rejectReason | String | 拒绝原因 |
| appliedAt | LocalDateTime | 投递时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

### Message（消息）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| senderId | Long | 发送者ID |
| receiverId | Long | 接收者ID |
| jobId | Long | 关联职位ID |
| resumeId | Long | 关联简历ID（投递消息时携带） |
| type | String | 消息类型：chat/application/system/video_invite |
| content | String | 消息内容 |
| isRead | Integer | 是否已读：0-未读 1-已读 |
| createdAt | LocalDateTime | 创建时间 |

---

### ChatMessage（聊天消息 - MongoDB）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | String | 主键ID（MongoDB ObjectId） |
| conversationId | String | 会话ID（如 "3_5"） |
| senderId | Long | 发送者ID |
| receiverId | Long | 接收者ID |
| jobId | Long | 关联职位ID |
| type | String | 消息类型：text-文字消息 resume-简历附件 video_invite-视频邀约 video_accept-接受邀约 video_reject-拒绝邀约 video_hangup-挂断通话 |
| content | String | 消息内容 |
| videoCallStatus | String | 视频通话状态（type=video_call时有值）：ringing/accepted/rejected/ended |
| resumeId | Long | 简历ID（type=resume时有值） |
| resumeName | String | 简历名称（冗余） |
| read | Boolean | 是否已读 |
| status | String | 投递状态：sending-发送中 sent-已送达 failed-发送失败 |
| createdAt | LocalDateTime | 创建时间 |

**说明:** 视频相关事件（video_invite/accept/reject/hangup）会作为独立的 ChatMessage 持久化到 MongoDB，用于聊天记录回放。每次视频信令事件的 `content` 字段存储人类可读的描述文本（如"发起了视频面试"、"接受了视频面试"、"挂断了视频面试"等）。

---

### Favorite（收藏）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| userId | Long | 用户ID |
| jobId | Long | 职位ID |
| createdAt | LocalDateTime | 创建时间 |
| jobDeleted | Integer | 职位是否已删除：0-未删除 1-已删除 |

---

### Region（省市区）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| name | String | 区域名称 |
| parentId | Long | 父级ID（0为省级） |
| level | Integer | 层级：1-省 2-市 3-区 |
| status | String | 状态：active-启用 disabled-禁用 |
| sortOrder | Integer | 排序权重 |
| areaCode | String | 行政区划代码 |

---

### BenefitTag（福利标签）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| name | String | 标签名称（唯一） |
| type | String | 标签类型：company-公司福利/job-职位福利 |
| sortOrder | Integer | 排序权重（越大越靠前） |
| enabled | Integer | 是否启用：1=启用 0=禁用 |
| createdAt | LocalDateTime | 创建时间 |

---

### CompanyBenefit（公司-福利关联）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| companyId | Long | 公司ID |
| benefitTagId | Long | 福利标签ID |

---

### JobBenefit（职位-福利关联）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| jobId | Long | 职位ID |
| benefitTagId | Long | 福利标签ID |

---

### UserResume（用户-简历关系）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| userId | Long | 用户ID |
| resumeId | Long | 简历ID |
| createdAt | LocalDateTime | 创建时间 |

---

### UserCompanyFollow（用户-公司关注关系）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| userId | Long | 用户ID |
| companyId | Long | 公司ID |
| createdAt | LocalDateTime | 创建时间 |

---

### Resume（简历）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| fileName | String | 文件名 |
| objectName | String | MinIO对象名称 |
| rawContent | String | 原始文本内容 |
| name | String | 姓名 |
| education | String | 学历 |
| skills | String | 技能JSON数组 |
| workExperience | String | 工作经历 |
| projectExperience | String | 项目经历 |
| createdAt | LocalDateTime | 创建时间 |
| createdBy | Long | 创建人ID |
| updatedAt | LocalDateTime | 更新时间 |
| updatedBy | Long | 修改人ID |

---

### ScoreResult（评分结果）

| 字段 | 类型 | 说明 |
|---|---|---|
| accuracy | int | 准确性 (1-10) |
| clarity | int | 清晰度 (1-10) |
| logic | int | 逻辑性 (1-10) |
| depth | int | 深度 (1-10) |
| practice | int | 实践能力 (1-10) |
| totalScore | double | 总分 (1-10) |
| feedback | String | 反馈评语 |
| referenceAnswer | String | 参考答案 |

---

### Interview（面试记录）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| userId | Long | 用户ID |
| resumeId | Long | 关联简历ID |
| jobType | String | 岗位类型 |
| totalRounds | Integer | 总面试轮数 |
| currentRound | Integer | 当前轮数 |
| status | String | 状态：PENDING/IN_PROGRESS/COMPLETED |
| totalScore | Double | 总分 |
| createdAt | LocalDateTime | 创建时间 |
| completedAt | LocalDateTime | 完成时间 |

---

### InterviewQA（面试问答）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| interviewId | Long | 关联面试ID |
| round | Integer | 轮次 |
| question | String | 问题 |
| answer | String | 回答 |
| scores | String | 评分JSON |
| feedback | String | 反馈 |
| referenceAnswer | String | 参考答案 |

---

### AdminOperationLog（管理员操作日志）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| adminId | Long | 操作管理员ID |
| module | String | 操作模块：user/company/job/application/interview/benefit_tag/job_category/region |
| action | String | 操作类型：create/update/delete/status_change/role_change/import/export/approve/reject |
| targetId | Long | 操作对象ID |
| targetName | String | 操作对象名称（冗余） |
| detail | String | 操作详情JSON：{before: ..., after: ...} |
| ip | String | 操作IP地址 |
| createdAt | LocalDateTime | 操作时间 |

---

### SystemConfig（系统配置）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| configKey | String | 配置键（唯一） |
| configValue | String | 配置值 |
| configType | String | 值类型：string/number/boolean/json |
| description | String | 配置说明 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| updatedBy | Long | 修改人ID |

---

### JobAuditLog（职位审核日志）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键ID |
| jobId | Long | 职位ID |
| adminId | Long | 审核管理员ID |
| action | String | 操作：approve/reject |
| reason | String | 审核原因（拒绝时必填） |
| createdAt | LocalDateTime | 审核时间 |

---

## 十八、状态码说明

| HTTP状态码 | 说明 |
|---|---|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证（Token无效或过期） |
| 403 | 无权限（角色权限不足，非管理员访问admin接口） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 十九、WebSocket 消息类型

### 面试 WebSocket

| type | 方向 | 说明 |
|---|---|---|
| start | 客户端→服务端 | 开始面试 |
| answer | 客户端→服务端 | 提交回答 |
| exit | 客户端→服务端 | 提前结束 |
| immersive_start | 客户端→服务端 | 启动沉浸式面试 |
| immersive_answer | 客户端→服务端 | 沉浸式回答评分 |
| immersive_exit | 客户端→服务端 | 沉浸式退出 |
| next_question | 服务端→客户端 | 下一个问题（沉浸式） |
| question | 服务端→客户端 | 问题（文字面试） |
| evaluation | 服务端→客户端 | 评分反馈 |
| completed | 服务端→客户端 | 面试完成 |
| error | 服务端→客户端 | 错误信息 |

### ASR WebSocket

| type | 方向 | 说明 |
|---|---|---|
| asr_init | 客户端→服务端 | 初始化ASR会话（interviewId, round） |
| audio_chunk | 客户端→服务端 | 音频分片（base64 WAV） |
| audio_final | 客户端→服务端 | 最终完整音频 |
| asr_result | 服务端→客户端 | 实时识别结果（中间文本） |
| asr_final | 服务端→客户端 | 最终识别结果 |
| asr_evaluation | 服务端→客户端 | 后端自动评分结果 |
| asr_error | 服务端→客户端 | 错误信息 |

### 聊天 WebSocket

| type | 方向 | 说明 |
|---|---|---|
| text | 客户端→服务端 | 发送文字消息 |
| resume | 客户端→服务端 | 发送简历附件 |
| read | 客户端→服务端 | 标记消息已读 |
| ack | 客户端→服务端 | 确认收到消息 |
| video_invite | 客户端→服务端 | 发起视频面试 |
| video_accept | 客户端→服务端 | 接受视频面试 |
| video_reject | 客户端→服务端 | 拒绝视频面试 |
| video_offer | 客户端→服务端 | WebRTC Offer（定向） |
| video_answer | 客户端→服务端 | WebRTC Answer（定向） |
| video_ice | 客户端→服务端 | ICE Candidate（定向） |
| video_hangup | 客户端→服务端 | 挂断视频通话（可携带 duration） |
| video_media | 客户端→服务端 | 视频中继数据（Base64 编码） |
| (binary) | 客户端→服务端 | 视频中继数据（二进制帧转发） |
| history | 服务端→客户端 | 历史消息列表 |
| message | 服务端→客户端 | 新消息推送 |
| read | 服务端→客户端 | 对方已读通知 |
| ack | 服务端→客户端 | 消息已送达 |
| ack_timeout | 服务端→客户端 | 消息投递超时 |
| video_invite | 服务端→客户端 | 视频面试邀请 |
| video_accept | 服务端→客户端 | 对方接受了视频面试 |
| video_reject | 服务端→客户端 | 对方拒绝了视频面试 |
| video_hangup | 服务端→客户端 | 对方挂断了视频通话 |
| video_offer | 服务端→客户端 | WebRTC Offer |
| video_answer | 服务端→客户端 | WebRTC Answer |
| video_ice | 服务端→客户端 | ICE Candidate |
| video_media | 服务端→客户端 | 视频中继数据 |
| (binary) | 服务端→客户端 | 视频中继数据（二进制帧转发） |
| error | 服务端→客户端 | 错误信息 |

### 视频面试信令流程

```
HR(发起方)              ChatWebSocket              求职者(接收方)
    │                        │                          │
    │ {type:"video_invite"}  │                          │
    │───────────────────────►│  broadcastToConversation  │
    │                        │─────────────────────────►│
    │                        │                          │  弹窗：是否接受面试？
    │                        │                          │
    │                        │  {type:"video_accept"}   │
    │                        │◄─────────────────────────│
    │◄───────────────────────│                          │
    │                        │                          │
    │  [WebRTC: 创建Offer]   │                          │
    │ {type:"video_offer",   │                          │
    │  sdp:...}              │                          │
    │───────────────────────►│  sendToUser(receiverId)  │
    │                        │─────────────────────────►│
    │                        │                          │  [WebRTC: 创建Answer]
    │                        │  {type:"video_answer",   │
    │                        │   sdp:...}               │
    │                        │◄─────────────────────────│
    │◄───────────────────────│                          │
    │                        │                          │
    │ {type:"video_ice",     │                          │
    │  candidate:...}        │                          │
    │───────────────────────►│─────────────────────────►│
    │                        │                          │
    │                        │  {type:"video_ice",      │
    │                        │   candidate:...}         │
    │                        │◄─────────────────────────│
    │◄───────────────────────│                          │
    │                        │                          │
    │  ═══ P2P 连接建立，音视频直传 ═══                  │
    │  ◄═══════════════════════════════════════════════►│
    │                        │                          │
    │ {type:"video_hangup"}  │                          │
    │───────────────────────►│─────────────────────────►│
```

**说明:**
- `video_invite/accept/reject/hangup` 通过 `broadcastToConversation` 广播给会话中的所有人
- `video_offer/answer/ice` 通过 `sendToUser` 定向发送给对方
- `video_invite` 额外通过 SSE 推送通知，确保对方在任意页面都能收到邀请弹窗
- P2P 连接建立后，音视频数据直接在浏览器之间传输，不经过服务器
- 所有视频信令事件（invite/accept/reject/hangup）都会持久化到 MongoDB ChatMessage，用于聊天记录回放

**P2P 失败时的中继（Relay）回退:**

当 WebRTC P2P 连接失败（ICE restart 耗尽后），自动回退到通过 WebSocket 中继编码后的音视频数据：

```
发送方 --[MediaRecorder 编码 webm/opus]--> base64 JSON --[WebSocket]--> 后端转发 --> 接收方 --[MediaSource 解码]--> 播放
```

- 发送方每 200ms 切割一次 MediaRecorder 数据，base64 编码后通过 `video_media` 文本消息或二进制帧发送
- 后端零解析转发给对方
- 接收方用 `MediaSource` + `SourceBuffer` 重建媒体流播放
- 降级参数：320x240 / 15fps / 200kbps / opus 32kbps
- 连接建立后前端显示提示 "P2P 连接失败，已切换到中继模式"
