# 后台管理系统 — 需求设计与功能分析

## 一、项目概述

### 1.1 背景

当前招聘网站（ai-interview）已有完整的求职者端和 HR 端功能，但缺少后台管理入口。管理员需要一个统一的后台系统来管理平台上的所有数据（用户、公司、职位、面试、标签等），并能直观地通过图表掌握平台运营状态。

### 1.2 目标用户

- 超级管理员（roleType = 3）
- 运营人员

### 1.3 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **前端框架** | Vue 3 | 3.5.34 | UI 框架 |
| **UI 组件库** | Element Plus | 2.14.1 | 组件库 |
| **图表** | ECharts | 6.1.0 | 数据可视化 |
| **Excel 处理** | SheetJS (xlsx) | 0.18.5 | 导入导出 |
| **HTTP 客户端** | Axios | 1.17.0 | API 请求 |
| **状态管理** | Pinia | 3.0.4 | 全局状态 |
| **路由** | Vue Router | 5.1.0 | 前端路由 |
| **构建工具** | Vite | 8.0.12 | 开发/构建 |
| **后端框架** | Spring Boot | 3.2.5 | Web 框架 |
| **ORM** | MyBatis-Plus | 3.5.5 | 数据库操作 |
| **AI 能力** | Spring AI | 1.0.0 | AI 面试（OpenAI 兼容） |
| **实时通信** | WebSocket | — | 面试/聊天 |
| **认证** | JWT (jjwt) | 0.12.5 | Token 认证 |
| **数据库** | MySQL | 8.0 | 主数据库 |
| **缓存** | Redis + Redisson | 3.27.0 | 缓存/分布式锁 |
| **文档存储** | MongoDB | — | 聊天消息 |
| **消息队列** | RabbitMQ | — | 异步任务 |
| **搜索引擎** | Elasticsearch | 7.x | 全文搜索 |
| **文件存储** | MinIO | 8.5.7 | 简历/文件 |
| **文件解析** | Apache Tika | 2.9.1 | 简历解析 |
| **工具库** | Hutool | 5.8.25 | 通用工具 |

### 1.4 技术栈在管理后台中的应用

| 技术 | 管理后台用途 |
|------|-------------|
| **Redis** | 缓存仪表盘统计数据（用户总数、今日新增等），避免每次刷新都查库 |
| **MongoDB** | 存储 AI 面试聊天记录，管理员可查看面试详情中的问答历史 |
| **RabbitMQ** | 异步处理批量导入任务，避免大量数据导入时阻塞请求 |
| **MinIO** | 存储用户上传的简历文件，管理员可下载查看 |
| **Spring AI** | AI 面试评分和报告生成，管理员可查看 AI 评语和评分详情 |
| **Elasticsearch** | 支持用户/公司/职位的全文搜索和复杂筛选 |
| **WebSocket** | 管理后台实时接收通知（如新申请、新面试完成） |
| **Tika** | 解析用户上传的简历文件（PDF/Word），提取文本内容 |

---

## 二、数据库现状（22 张表）

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| user | 用户 | id, username, password, phone, role_type, company_id, status |
| role_type | 角色字典 | id, name |
| user_profile | 用户档案 | user_id, real_name, gender, birthday, education |
| resume | 简历 | id, user_id, title, content(JSON) |
| user_resume | 用户-简历关联 | user_id, resume_id, is_default |
| company | 公司 | id, user_id, name, logo, industry, scale, type, city, view_count, follow_count |
| user_company_follow | 用户关注公司 | user_id, company_id |
| job | 职位 | id, user_id, company_id, title, city, salary, job_type, experience, education, status |
| job_category | 职位分类 | id, name, parent_id |
| application | 求职申请 | id, user_id, job_id, status |
| interview | 面试 | id, application_id, status, start_time, duration |
| interview_qa | 面试问答 | id, interview_id, question, answer, score |
| report | 面试报告 | id, interview_id, overall_score, summary |
| message | 站内消息 | id, sender_id, receiver_id, content, is_read |
| favorite | 收藏 | user_id, job_id/company_id |
| region | 省市区 | id, name, parent_id, level |
| benefit_tag | 福利标签 | id, name |
| company_benefit | 公司-福利关联 | company_id, benefit_tag_id |
| job_benefit | 职位-福利关联 | job_id, benefit_tag_id |
| admin_operation_log | 管理员操作日志 | id, admin_id, module, action, target_id, detail, ip |
| system_config | 系统配置 | id, config_key, config_value, config_type |
| job_audit_log | 职位审核日志 | id, job_id, admin_id, action, reason |

---

## 三、前端页面结构（已实现）

### 3.1 路由结构

```
/admin/login              管理员登录页
/admin                    后台管理布局（AdminLayout.vue）
  /admin/dashboard        数据概览仪表盘
  /admin/users            用户管理
  /admin/companies        公司管理
  /admin/jobs             职位管理
  /admin/applications     申请管理
  /admin/interviews       面试管理
  /admin/benefit-tags     福利标签管理
  /admin/job-categories   职位分类管理
  /admin/regions          地区管理
```

### 3.2 页面组件清单

| 页面 | 组件路径 | 统计卡片 | 筛选条件 | 表格列 | 操作按钮 | 弹窗 | 导入 | 导出 |
|------|----------|---------|---------|--------|---------|------|------|------|
| 登录页 | `AdminLogin.vue` | — | — | — | — | — | — | — |
| 布局 | `AdminLayout.vue` | — | — | — | — | — | — | — |
| 仪表盘 | `AdminDashboard.vue` | 6 | — | — | — | — | — | — |
| 用户管理 | `AdminUsers.vue` | 5 | 3 | 8 | 4 | 3 | 有 | 有 |
| 公司管理 | `AdminCompanies.vue` | 5 | 5 | 10 | 4 | 2 | 有 | 有 |
| 职位管理 | `AdminJobs.vue` | 5 | 6 | 10 | 5 | 2 | 有 | 有 |
| 申请管理 | `AdminApplications.vue` | 6 | 4 | 9 | 6 | 1 | 无 | 有 |
| 面试管理 | `AdminInterviews.vue` | 6 | 5 | 12 | 4 | 1 | 无 | 有 |
| 福利标签 | `AdminBenefitTags.vue` | 4 | 3 | 10 | 4 | 2 | 无 | 无 |
| 职位分类 | `AdminJobCategories.vue` | 4 | 2 | 6 | 4 | 1 | 无 | 无 |
| 地区管理 | `AdminRegions.vue` | 4 | 2 | 7 | 4 | 1 | 无 | 无 |

### 3.3 布局设计（AdminLayout.vue）

```
┌──────────────────────────────────────────────────┐
│  [通知(5)] [全屏] [管理员 ▾]      面包屑导航      │  ← 顶栏 56px sticky
├──────────┬───────────────────────────────────────┤
│  A 管理后台 │                                   │
│  超级管理员  │                                   │
│  ───────── │                                   │
│ 📊 核心功能  │                                   │
│  数据概览   │         内容区域 (router-view)      │
│  用户管理(18)│                                   │
│  公司管理   │                                   │
│ 📋 招聘管理  │                                   │
│  职位管理(12)│                                   │
│  申请管理(6) │                                   │
│  面试管理(4) │                                   │
│ ⚙ 系统配置  │                                   │
│  ▸ 标签管理 │                                   │
│    福利标签  │                                   │
│    职位分类  │                                   │
│    地区管理  │                                   │
│ ─────────  │                                   │
│ v1.0.0 ·折叠│                                   │
└──────────┴───────────────────────────────────────┘
```

**侧边栏特性**：
- 宽度 220px，支持折叠至 64px（仅图标）
- 绿色渐变背景：`linear-gradient(180deg, #0a3d2c, #0f4a35, #12593f)`
- 菜单分 3 组：核心功能、招聘管理、系统配置
- 角标显示待处理数量：用户管理(红18)、职位管理(橙12)、申请管理(红6)、面试管理(紫4)
- 折叠时 tooltip 提示菜单名称（300ms 延迟）
- 响应式：<1024px 自动折叠

**顶栏特性**：
- 面包屑导航（首页 / 当前页面标题）
- 通知按钮（badge 角标）+ 全屏切换
- 管理员下拉菜单：个人信息、系统设置、返回前台、退出登录
- 页面切换过渡动画（page-fade）

### 3.4 登录页（AdminLogin.vue）

- 居中毛玻璃登录卡片（backdrop-filter: blur(12px)）
- 背景图片：`admin-login-bg.webp`
- 表单：管理员账号 + 密码 + 登录按钮
- 验证规则：用户名和密码必填
- 前端硬编码验证：`admin / admin123`
- 登录后存 `localStorage admin_token`

---

## 四、仪表盘详细设计（AdminDashboard.vue）

### 4.1 顶部统计卡片（6 个）

| 指标 | 颜色 | 趋势指示 | 数据来源 |
|------|------|---------|---------|
| 总用户数 | 蓝色 | +12.5% 上升 | `SELECT COUNT(*) FROM user` |
| 总公司数 | 绿色 | +2 上升 | `SELECT COUNT(*) FROM company` |
| 总职位数 | 橙色 | +8.3% 上升 | `SELECT COUNT(*) FROM job` |
| 今日新增用户 | 紫色 | +5 上升 | `WHERE DATE(created_at) = CURDATE()` |
| 今日新增职位 | 粉色 | -2 下降 | `WHERE DATE(created_at) = CURDATE()` |
| 待审核职位 | 红色 | 3 新增 下降 | `WHERE status = 'pending'` |

每张卡片包含：图标 + 数值 + 标签 + 趋势箭头（上/下）+ 百分比变化

### 4.2 中间区域（3 栏布局）

**左栏：待处理事项面板**
- 待审核职位 (12) → 跳转 `/admin/jobs`
- 新注册用户 (18) → 跳转 `/admin/users`
- 举报处理 (3) → 跳转 `/admin/users`
- 面试异常 (2) → 跳转 `/admin/interviews`

**中栏：最近活动面板**
- 时间线展示 6 条系统动态（带彩色圆点）
- 包含用户注册、职位发布、面试完成等活动

**右栏：快捷操作面板（2×2 网格）**
- 审核职位 → `/admin/jobs`
- 用户管理 → `/admin/users`
- 公司管理 → `/admin/companies`
- 标签管理 → `/admin/benefit-tags`

### 4.3 趋势图表区（2 个图表 + 时间范围切换）

支持 7/30/90 天时间范围切换（radio button）：

| 图表 | 类型 | X 轴 | Y 轴 | 样式 |
|------|------|------|------|------|
| 用户增长趋势 | 折线图 | 最近 N 天日期 | 每天新增用户数 | 蓝色面积渐变，smooth |
| 投递趋势 | 折线图 | 最近 N 天日期 | 每天新增投递数 | 绿色面积渐变，smooth |

### 4.4 分布图表区（2×2 网格，4 个图表）

| 图表 | 类型 | 数据 | 样式 |
|------|------|------|------|
| 职位类型分布 | 环形饼图 | 全职/兼职/实习 各占比 | radius: ['40%', '70%'] |
| 面试状态分布 | 环形饼图 | 已完成/进行中/已取消 | radius: ['40%', '70%'] |
| 行业职位 TOP10 | 水平柱状图 | 行业名称 + 职位数量 | 绿色柱体 |
| 城市职位 TOP10 | 垂直柱状图 | 城市名称 + 职位数量 | 蓝色柱体 |

---

## 五、用户管理详细设计（AdminUsers.vue）

### 5.1 统计卡片（5 个）

| 指标 | 计算方式 | 颜色 |
|------|---------|------|
| 总用户数 | `tableData.length` | 蓝色 |
| 求职者 | `roleType === 1` | 绿色 |
| HR | `roleType === 2` | 橙色 |
| 管理员 | `roleType === 3` | 红色 |
| 今日新增 | 固定值 | 紫色 |

### 5.2 筛选条件（3 个）

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 搜索框 | input | 搜用户名/昵称/手机号 |
| 角色 | select | 求职者(1) / HR(2) / 管理员(3) |
| 状态 | select | 正常(active) / 禁用(disabled) |

### 5.3 表格列（8 列）

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 多选框 | selection | 45px | 批量操作 |
| ID | id | 65px | 居中 |
| 用户信息 | 复合列 | min-width 200 | 头像(颜色按状态) + 昵称 + @用户名 |
| 手机号 | phone | 130px | |
| 角色 | roleType | — | el-tag: info(求职者)/warning(HR)/danger(管理员) |
| 所属公司 | companyName | min-width 130 | 空值显示"---" |
| 状态 | status | — | 红绿圆点 + 文字 |
| 注册时间 | createdAt | 160px | |
| 操作 | — | 200px | fixed right |

### 5.4 操作按钮（每行 4 个）

| 按钮 | 图标 | 说明 |
|------|------|------|
| 编辑 | EditPen | 打开编辑弹窗 |
| 角色变更 | Switch | 打开角色变更弹窗 |
| 禁用/启用 | Lock/Unlock | 切换 status，条件确认框 |
| 删除 | Delete | 红色，条件确认框 |

### 5.5 批量操作

- 批量禁用：选中多条后可批量将 status 设为 disabled

### 5.6 导入导出

**导出**：用 XLSX 库将 filteredData 导出为 Excel
- 列：ID / 用户名 / 昵称 / 手机号 / 邮箱 / 角色 / 所属公司 / 状态 / 注册时间
- 文件名：`用户列表_YYYY-MM-DD.xlsx`

**导入**：
- 支持 .xlsx / .xls / .csv 格式
- 拖拽上传或点击上传（el-upload）
- 单次最多 500 条
- 支持下载导入模板
- FileReader 解析后推入 tableData，自动设置默认状态

### 5.7 弹窗（3 个）

**编辑用户弹窗**（480px）：
- 用户名（disabled）/ 昵称 / 手机号 / 邮箱

**角色变更弹窗**（400px）：
- radio-group 三选一（求职者/HR/管理员）
- 每个选项带描述说明权限范围

**导入用户弹窗**（480px）：
- 蓝色提示信息
- el-upload 拖拽上传区
- 下载模板链接

---

## 六、公司管理详细设计（AdminCompanies.vue）

### 6.1 统计卡片（5 个）

| 指标 | 计算方式 | 颜色 |
|------|---------|------|
| 总公司数 | `tableData.length` | 蓝色 |
| 在招职位 | `reduce(sum + jobCount)` | 绿色 |
| 总浏览量 | `reduce(sum + viewCount)` 超1万显示"xx万" | 橙色 |
| 总关注数 | `reduce(sum + followCount)` 超1万显示"xx万" | 紫色 |
| 今日新增 | 固定值 | 红色 |

### 6.2 筛选条件（5 个）

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 搜索框 | input | 搜公司名称 |
| 行业 | select | 互联网/人工智能/大数据/企业服务/金融/教育/游戏/电商/医疗/其他 |
| 城市 | select | 北京/上海/广州/深圳/杭州/成都/武汉/南京 |
| 规模 | select | 10000人以上 / 1000-9999人 / 100-999人 / 100人以下 |
| 状态 | select | 正常(active) / 禁用(disabled) |

### 6.3 表格列（10 列）

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 多选框 | selection | 45px | |
| ID | id | 60px | 居中 |
| 公司信息 | 复合列 | min-width 200 | 行业色 logo首字母 + 公司名 + 行业名 |
| 规模 | scale | 130px | |
| 城市 | city | 90px | |
| 状态 | status | — | 红绿圆点 + 文字 |
| 在招职位 | jobCount | 90px | sortable |
| 浏览量 | viewCount | 90px | sortable |
| 关注数 | followCount | 90px | sortable |
| 创建时间 | createdAt | 170px | |
| 操作 | — | 220px | fixed right |

### 6.4 操作按钮（每行 4 个）

| 按钮 | 图标 | 说明 |
|------|------|------|
| 编辑 | EditPen | 打开编辑弹窗 |
| 职位 | Suitcase | 跳转 `/admin/jobs?company=公司名` |
| 禁用/启用 | Lock/Unlock | 切换 status |
| 删除 | Delete | 红色，条件确认框 |

### 6.5 批量操作

- 批量禁用

### 6.6 导入导出

**导出**：Excel
- 列：ID / 公司名称 / 行业 / 规模 / 城市 / 状态 / 在招职位 / 浏览量 / 关注数 / 创建时间
- 文件名：`公司列表_YYYY-MM-DD.xlsx`

**导入**：
- 拖拽上传 + 下载模板
- 模板列：公司名称 / 行业 / 规模 / 城市 / 描述
- 字段映射：`公司名称` → name, `行业` → industry, `规模` → scale, `城市` → city, `描述` → description

### 6.7 弹窗（2 个）

**编辑公司弹窗**（520px）：
- 公司名称 / 行业(select) / 规模(select) / 城市 / 描述(textarea)

**导入公司弹窗**（520px）：
- 蓝色提示信息 + el-upload 拖拽区 + 下载模板按钮

---

## 七、职位管理详细设计（AdminJobs.vue）

### 7.1 统计卡片（5 个）

| 指标 | 计算方式 | 颜色 |
|------|---------|------|
| 总职位数 | `tableData.length` | 蓝色 |
| 已发布 | `status === 'active'` | 绿色 |
| 待审核 | `status === 'pending'` | 橙色 |
| 今日新增 | 固定值 | 紫色 |
| 总申请数 | `reduce(sum + appCount)` | 红色 |

### 7.2 筛选条件（6 个）

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 搜索框 | input | 搜职位名称 |
| 公司 | select | 14 个公司选项 |
| 城市 | select | 8 个城市 |
| 类型 | select | 全职 / 兼职 / 实习 |
| 学历 | select | 大专 / 本科 / 硕士 / 博士 |
| 状态 | select | 已发布(active) / 待审核(pending) / 已关闭(closed) / 审核不通过(rejected) |

### 7.3 表格列（10 列）

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 多选框 | selection | 45px | |
| ID | id | 60px | 居中 |
| 职位信息 | 复合列 | min-width 200 | 类型色 icon + 职位名 + 类型tag/experience tag |
| 公司 | 复合列 | 140px | 行业色 logo首字母 + 公司名 |
| 城市 | city | 80px | |
| 薪资 | salary | 100px | 红色加粗 |
| 学历 | education | 70px | 居中 |
| 申请数 | appCount | 80px | sortable |
| 状态 | status | — | el-tag: success/warning/info/danger |
| 发布时间 | publishedAt | 110px | |
| 操作 | — | 240px | fixed right |

### 7.4 操作按钮（每行最多 6 个，条件显示）

| 按钮 | 图标 | 显示条件 | 说明 |
|------|------|---------|------|
| 通过 | Check | status === 'pending' | 审核通过 |
| 驳回 | Close | status === 'pending' | 审核驳回 |
| 编辑 | EditPen | 始终 | 打开编辑弹窗 |
| 下架 | Hide | status === 'active' | 关闭职位 |
| 申请 | Document | 始终 | 查看该职位的申请 |
| 删除 | Delete | 始终 | 红色，条件确认框 |

### 7.5 职位状态流转

```
pending（待审核）→ active（已发布）→ closed（已关闭）
                → rejected（审核不通过）
active → closed（手动关闭）
```

### 7.6 批量操作

- 批量通过：仅选中含 pending 时启用，显示 pending 数量
- 批量删除

### 7.7 导入导出

**导出**：Excel
- 列：ID / 职位名称 / 公司 / 城市 / 薪资 / 职位类型 / 学历要求 / 经验要求 / 申请数 / 状态 / 发布时间

**导入**：
- 模板列：职位名称 / 公司名称 / 城市 / 薪资 / 职位类型 / 学历要求 / 经验要求
- 导入后自动标记为 `pending`（待审核）

### 7.8 弹窗（2 个）

**编辑职位弹窗**（560px）：
- 职位名称 / 城市 / 薪资 / 职位类型(select) / 学历要求(select) / 经验要求

**导入职位弹窗**（520px）：
- 提示信息 + 拖拽上传 + 下载模板

---

## 八、申请管理详细设计（AdminApplications.vue）

### 8.1 统计卡片（6 个）

| 指标 | 计算方式 | 颜色 |
|------|---------|------|
| 总申请数 | `tableData.length` | 蓝色 |
| 待处理 | `status === 'pending'` | 橙色 |
| 面试中 | `status === 'interviewing'` | 紫色 |
| 已录用 | `status === 'accepted'` | 绿色 |
| 已拒绝 | `status === 'rejected'` | 红色 |
| 今日新增 | 固定值 | cyan |

### 8.2 筛选条件（4 个）

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 搜索框 | input | 搜求职者/职位 |
| 公司 | select | 10 个公司 |
| 状态 | select | 待处理/已查看/面试中/已录用/已拒绝 |
| 日期范围 | date-range | el-date-picker daterange |

### 8.3 表格列（9 列）

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 多选框 | selection | 45px | |
| ID | id | 60px | 居中 |
| 求职者 | 复合列 | min-width 180 | 头像(按状态着色) + 姓名 + 电话·邮箱 |
| 申请职位 | 复合列 | min-width 180 | 职位名 + mini-logo + 公司名 |
| 简历 | resumeName | 100px | 链接按钮或"未上传" |
| HR | hrName | 100px | 或"未分配" |
| 状态 | statusCode | — | 圆点 + el-tag |
| 申请时间 | createdAt | 110px | |
| 操作 | — | 260px | fixed right |

### 8.4 操作按钮（每行最多 6 个，条件显示）

| 按钮 | 图标 | 显示条件 | 说明 |
|------|------|---------|------|
| 查看 | View | status === 'pending' | 标记为已查看 |
| 面试 | Calendar | pending / viewed | 安排面试 |
| 录用 | Check | status === 'interviewing' | 录用 |
| 拒绝 | Close | 非 accepted/rejected | 拒绝 |
| 简历 | Document | 始终 | 查看简历 |
| 备注 | ChatDotRound | 始终 | 打开备注弹窗 |

### 8.5 批量操作

- 批量标记已查看
- 批量拒绝

### 8.6 导出（仅导出，无导入）

**导出**：Excel
- 列：ID / 求职者 / 手机号 / 邮箱 / 申请职位 / 公司 / 简历 / HR / 状态 / 备注 / 申请时间

### 8.7 弹窗（1 个）

**申请备注弹窗**（480px）：
- 标题：`求职者 -- 职位`
- textarea 输入备注内容

---

## 九、面试管理详细设计（AdminInterviews.vue）

### 9.1 统计卡片（6 个）

| 指标 | 计算方式 | 颜色 |
|------|---------|------|
| 总面试数 | `tableData.length` | 蓝色 |
| 已完成 | `status === 'completed'` | 绿色 |
| 进行中 | `status === 'in_progress'` | 紫色 |
| 已取消 | `status === 'cancelled'` | 红色 |
| 平均分 | `avgScore` computed | 橙色 |
| 今日面试 | 固定值 | cyan |

### 9.2 筛选条件（5 个）

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 搜索框 | input | 搜求职者/职位 |
| 公司 | select | 8 个公司 |
| 面试类型 | select | AI面试(ai) / 视试面试(video) / 现场面试(onsite) |
| 状态 | select | 已完成 / 进行中 / 已取消 |
| 日期范围 | date-range | el-date-picker daterange |

### 9.3 表格列（12 列）

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 多选框 | selection | 45px | |
| ID | id | 60px | 居中 |
| 求职者 | 复合列 | min-width 160 | 头像 + 姓名 + 学历 · 经验 |
| 申请职位 | 复合列 | min-width 170 | 职位名 + mini-logo + 公司名 |
| 面试轮次 | round | 85px | el-tag: 一面/二面/终面 |
| 面试类型 | typeCode | 100px | 自定义 badge + 图标（AI/视频/现场） |
| AI评分 | score | 100px | 大号数字 + 进度条，颜色分级(>=80绿/>=60橙/<60红) |
| 评分详情 | techScore/commScore/logicScore | 180px | 三列：技术分/沟通分/逻辑分 |
| 状态 | statusCode | — | 圆点 + 文字 |
| 时长 | duration | 70px | |
| 面试时间 | startTime | 110px | |
| 操作 | — | 220px | fixed right |

### 9.4 操作按钮（每行最多 4 个，条件显示）

| 按钮 | 图标 | 显示条件 | 说明 |
|------|------|---------|------|
| 报告 | DataAnalysis | status === 'completed' | 查看 AI 面试报告 |
| 取消 | CircleClose | status === 'in_progress' | 取消面试 |
| 重面 | RefreshRight | completed / cancelled | 重新安排面试 |
| 删除 | Delete | 始终 | 红色，条件确认框 |

### 9.5 批量操作

- 批量取消

### 9.6 导出（仅导出，无导入）

**导出**：Excel
- 列：ID / 求职者 / 学历 / 经验 / 申请职位 / 公司 / 面试轮次 / 面试类型 / AI评分 / 技术分 / 沟通分 / 逻辑分 / 时长 / 状态 / 面试时间 / AI评语

### 9.7 弹窗（1 个）

**AI 面试报告弹窗**（560px）：
- 顶部：候选人头像 + 姓名 + 职位·公司 + 大号评分（颜色分级）
- 进度条区：技术能力 / 沟通表达 / 逻辑思维（el-progress）
- AI 评语段落

---

## 十、福利标签管理详细设计（AdminBenefitTags.vue）

### 10.1 统计卡片（4 个）

| 指标 | 计算方式 | 颜色 |
|------|---------|------|
| 总标签数 | `tableData.length` | 蓝色 |
| 公司使用次数 | `reduce(sum + companyUsage)` | 绿色 |
| 职位使用次数 | `reduce(sum + jobUsage)` 超1万显示"xx万" | 橙色 |
| 最热门标签 | `mostPopular` computed | 紫色 |

### 10.2 筛选条件（3 个）

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 搜索框 | input | 搜标签名称 |
| 状态 | select | 启用(active) / 禁用(disabled) |
| 排序 | select | 默认排序 / 使用量降序 / 使用量升序 / 名称排序 / 创建时间 |

### 10.3 表格列（10 列）

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 多选框 | selection | 45px | |
| ID | id | 60px | 居中 |
| 标签预览 | — | 160px | 带背景色的圆角 tag 样式 |
| 标签名称 | name | 130px | |
| 使用量 | — | 200px | 进度条 + "公司 X · 职位 Y" 文字 |
| 使用公司 | companyUsage | 90px | sortable |
| 使用职位 | jobUsage | 90px | sortable |
| 权重 | sort | 70px | |
| 状态 | status | — | 圆点 + 文字 |
| 创建时间 | createdAt | 110px | |
| 操作 | — | 220px | fixed right |

### 10.4 操作按钮（每行 4 个）

| 按钮 | 图标 | 说明 |
|------|------|------|
| 编辑 | EditPen | 打开编辑弹窗 |
| 使用 | DataAnalysis | 查看标签使用详情 |
| 禁用/启用 | Lock/Unlock | 切换 status |
| 删除 | Delete | 红色，条件确认框 |

### 10.5 批量操作

- 批量删除

### 10.6 弹窗（2 个）

**新增/编辑标签弹窗**（480px）：
- 标签名称 / 标签颜色（8 个颜色选项圆角预览）/ 排序权重(InputNumber) / 描述(textarea)

**标签使用详情弹窗**（520px）：
- 标签预览 + 统计摘要
- 关联公司列表（el-tag）
- 关联职位列表（el-tag）

---

## 十一、职位分类管理详细设计（AdminJobCategories.vue）

### 11.1 统计卡片（4 个）

| 指标 | 计算方式 | 颜色 |
|------|---------|------|
| 总分类数 | 递归计算所有节点 | 蓝色 |
| 一级分类 | `level1Count` | 绿色 |
| 二级分类 | `level2Count` | 橙色 |
| 关联职位 | `totalJobs` computed | 紫色 |

### 11.2 筛选条件（2 个 + 1 按钮）

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 搜索框 | input | 搜分类名称 |
| 状态 | select | 启用(active) / 禁用(disabled) |
| 展开/收起 | button | 切换全部展开/收起（FolderOpened/Folder 图标） |

### 11.3 树形表格（row-key="id", tree-props: { children }）

**表格列（6 列）**：

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 分类名称 | 复合列 | 260px | 图标色块 + 名称 + 描述 |
| 职位数 | jobCount | 200px | 进度条 + 数字 |
| 权重 | sort | 70px | |
| 状态 | status | — | 圆点 + 文字 |
| 创建时间 | createdAt | 110px | |
| 操作 | — | 280px | fixed right |

### 11.4 操作按钮（每行最多 4 个）

| 按钮 | 图标 | 显示条件 | 说明 |
|------|------|---------|------|
| 子分类 | Plus | level < 3（非最深层） | 新增子分类 |
| 编辑 | EditPen | 始终 | |
| 禁用/启用 | Lock/Unlock | 始终 | |
| 删除 | Delete | 始终 | 红色，有子节点时提示连同删除 |

### 11.5 弹窗（1 个）

**新增/编辑分类弹窗**（480px）：
- 分类名称 / 父分类（disabled）/ 分类图标（8 个图标选项：Monitor/Cpu/Edit/TrendCharts/Connection/Promotion/Setting/Star）/ 排序权重 / 描述(textarea)

### 11.6 树形数据

2 级树结构，6 个顶级分类：技术开发 / 人工智能 / 产品设计 / 市场运营 / 数据 / 其他

---

## 十二、地区管理详细设计（AdminRegions.vue）

### 12.1 统计卡片（4 个）

| 指标 | 计算方式 | 颜色 |
|------|---------|------|
| 总地区数 | 递归计算所有节点 | 蓝色 |
| 省级 | `level1Count` | 红色 |
| 市级 | `level2Count` | 橙色 |
| 区县级 | `level3Count` | 绿色 |

### 12.2 筛选条件（2 个 + 1 按钮）

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 搜索框 | input | 搜地区名称 |
| 级别 | select | 省级(1) / 市级(2) / 区县(3) |
| 展开/收起 | button | 切换全部展开/收起 |

### 12.3 树形表格

**表格列（7 列）**：

| 列名 | 字段 | 宽度 | 说明 |
|------|------|------|------|
| 地区名称 | 复合列 | 240px | 级别色 icon + 名称 + 行政区划码（monospace） |
| 级别 | level | 85px | el-tag: danger(省)/warning(市)/info(区) |
| 关联职位 | jobCount | 100px | sortable |
| 关联公司 | companyCount | 100px | sortable |
| 权重 | sort | 70px | |
| 状态 | status | — | 圆点 + 文字 |
| 创建时间 | createdAt | 110px | |
| 操作 | — | 240px | fixed right |

### 12.4 操作按钮（每行最多 4 个）

| 按钮 | 图标 | 显示条件 | 说明 |
|------|------|---------|------|
| 下级 | Plus | level < 3 | 新增下级地区 |
| 编辑 | EditPen | 始终 | |
| 禁用/启用 | Lock/Unlock | 始终 | |
| 删除 | Delete | 始终 | 有子节点时提示连同删除 |

### 12.5 弹窗（1 个）

**新增/编辑地区弹窗**（480px）：
- 地区名称 / 父地区（disabled）/ 行政区划码（maxlength 6，提示"6位国标行政区划代码"）/ 排序权重

### 12.6 树形数据

3 级树结构：省 → 市 → 区

---

## 十三、后端接口清单

### 13.1 管理员认证

```
POST   /api/admin/login          管理员登录
GET    /api/admin/profile        获取当前管理员信息
POST   /api/admin/logout         退出登录
```

### 13.2 数据概览仪表盘

```
GET  /api/admin/dashboard/summary          顶部 6 个统计卡片
GET  /api/admin/dashboard/user-trend       用户增长趋势（7/30/90天）
GET  /api/admin/dashboard/application-trend 投递趋势（7/30/90天）
GET  /api/admin/dashboard/job-type         职位类型分布（饼图）
GET  /api/admin/dashboard/interview-status 面试状态分布（饼图）
GET  /api/admin/dashboard/job-industry     行业职位 TOP10（柱状图）
GET  /api/admin/dashboard/job-city         城市职位 TOP10（柱图）
GET  /api/admin/dashboard/pending-tasks    待处理事项（数量+描述）
GET  /api/admin/dashboard/recent-activity  最近活动（时间线）
```

### 13.3 用户管理

```
GET    /api/admin/users                  分页+搜索用户列表
GET    /api/admin/users/{id}             用户详情（含档案、简历摘要）
PUT    /api/admin/users/{id}/status      启用/禁用
PUT    /api/admin/users/{id}/role        修改角色
DELETE /api/admin/users/{id}             删除用户
POST   /api/admin/users/import           批量导入用户（Excel）
GET    /api/admin/users/export           导出用户列表（Excel）
```

### 13.4 公司管理

```
GET    /api/admin/companies              分页+搜索公司列表
GET    /api/admin/companies/{id}         公司详情
PUT    /api/admin/companies/{id}/status  启用/禁用
DELETE /api/admin/companies/{id}         删除公司
POST   /api/admin/companies/import       批量导入公司（Excel）
GET    /api/admin/companies/export       导出公司列表（Excel）
```

### 13.5 职位管理

```
GET    /api/admin/jobs                   分页+搜索职位列表
GET    /api/admin/jobs/{id}              职位详情
PUT    /api/admin/jobs/{id}/status       审核/上下架（status 枚举）
DELETE /api/admin/jobs/{id}              删除职位
GET    /api/admin/jobs/{id}/applications 该职位的申请列表
POST   /api/admin/jobs/import            批量导入职位（Excel）
GET    /api/admin/jobs/export            导出职位列表（Excel）
```

### 13.6 申请管理

```
GET    /api/admin/applications           分页申请列表
GET    /api/admin/applications/{id}      申请详情
PUT    /api/admin/applications/{id}/status  更新申请状态
DELETE /api/admin/applications/{id}      删除申请
GET    /api/admin/applications/export    导出申请列表（Excel）
```

### 13.7 面试管理

```
GET    /api/admin/interviews             分页面试列表
GET    /api/admin/interviews/{id}        面试详情（含问答记录）
GET    /api/admin/interviews/{id}/report 面试报告
DELETE /api/admin/interviews/{id}        删除面试记录
GET    /api/admin/interviews/export      导出面试列表（Excel）
```

### 13.8 福利标签管理

```
GET    /api/admin/benefit-tags           标签列表（含使用统计）
POST   /api/admin/benefit-tags           新增标签
PUT    /api/admin/benefit-tags/{id}      编辑标签
DELETE /api/admin/benefit-tags/{id}      删除标签（同时清理关联中间表）
```

### 13.9 职位分类管理

```
GET    /api/admin/job-categories         分类树（含职位数统计）
POST   /api/admin/job-categories         新增分类
PUT    /api/admin/job-categories/{id}    编辑分类
DELETE /api/admin/job-categories/{id}    删除分类（有关联职位时拒绝）
```

### 13.10 地区管理

```
GET    /api/admin/regions                地区树（含关联统计）
POST   /api/admin/regions                新增地区
PUT    /api/admin/regions/{id}           编辑地区
DELETE /api/admin/regions/{id}           删除地区（有子节点时连同删除）
```

---

## 十四、后端实现方案

### 14.1 新建 Controller 清单

```
com.interview.controller.admin/
  AdminAuthController.java          登录认证
  AdminDashboardController.java     仪表盘数据
  AdminUserController.java          用户管理
  AdminCompanyController.java       公司管理
  AdminJobController.java           职位管理
  AdminApplicationController.java   申请管理
  AdminInterviewController.java     面试管理
  AdminBenefitTagController.java    福利标签管理
  AdminJobCategoryController.java   职位分类管理
  AdminRegionController.java        地区管理
```

### 14.2 拦截器配置

```java
// AdminAuthInterceptor.java
// 1. 从 Header 中取 JWT Token
// 2. 解析 userId
// 3. 查询 user 表确认 role_type = 3
// 4. 放行或返回 403

// 拦截路径：/api/admin/**
// 排除路径：/api/admin/login
```

### 14.3 接口返回格式

所有 admin 接口统一返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

分页接口：

```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

### 14.4 Excel 导入导出方案

**导出接口**：
- 返回 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- 后端使用 Apache POI 或 EasyExcel 生成
- 前端也可用 XLSX 库纯前端导出（当前实现方式）

**导入接口**：
- 接收 multipart/form-data 文件
- 后端用 EasyExcel 解析后批量插入
- 返回导入成功/失败数量
- 校验：必填字段、格式、重复数据

---

## 十五、现有后端分析与差距

### 15.1 现有后端架构

| 组件 | 现状 |
|------|------|
| 鉴权 | JwtFilter 解析 Token → UserContext(ThreadLocal)，**无角色校验** |
| 用户上下文 | `UserContext.getUserId()` / `getRoleType()` / `getUsername()` |
| JWT | `JwtUtil.generateToken(userId, username, roleType)`，claims 含角色 |
| Controller | 14 个，全部挂在 `/api/**`，无 `/api/admin/**` 路径 |
| Service | 15 个，无管理员专用方法 |
| 数据库 | 19 张表，company 表无 status 字段，job 表无 pending/rejected 状态 |

### 15.2 现有接口权限分析

**关键发现：整个后端没有任何 roleType 校验。** JwtFilter 仅解析 Token 有效性，不检查角色是否匹配接口要求。

| 现有接口 | 权限控制 | 管理员能否使用 |
|---------|---------|--------------|
| `GET /api/user/{id}` | 无校验 | 能查但无分页列表 |
| `GET /api/application/my` | 按 userId 过滤 | 只能看自己的 |
| `GET /api/application/job/{jobId}` | 校验同一公司 | 不能跨公司 |
| `GET /api/company/my` | 按 userId 过滤 | 只能看自己的 |
| `GET /api/job/my` | 按 companyId 过滤 | 只能看一个公司 |
| `POST /api/benefit-tag` | 无校验 | 能用但无权限控制 |
| `GET /api/category/tree` | 公开 | 能用 |
| `GET /api/region/tree` | 公开 | 能用 |

### 15.3 数据库差距

**需要修改的表**：

| 表 | 当前状态 | 需要修改 |
|----|---------|---------|
| `company` | 无 status 字段 | 新增 `status VARCHAR(20) DEFAULT 'active'` |
| `job` | status 枚举: active/paused/closed | 新增 `pending`、`rejected` 状态值 |
| `application` | status 枚举: pending/screening/interview/offer/rejected/withdrawn | 前端用 viewed 状态，需对齐 |
| `benefit_tag` | 有 enabled 字段 | 已满足，无需修改 |
| `job_category` | 无 status/sort/description 字段 | 新增 `status`、`sort`、`description` |
| `region` | 无 status/sort/code 字段 | 新增 `status`、`sort`、`code` |
| `user` | 已有 status 字段 | 已满足，无需修改 |

**需要新增的表**（已添加到 schema.sql）：

| 表 | 说明 | 状态 |
|----|------|------|
| `admin_operation_log` | 管理员操作日志 | **已实现** |
| `system_config` | 系统配置 | **已实现** |
| `job_audit_log` | 职位审核日志 | **已实现** |

**可选扩展表**（按需）：

| 表 | 说明 | 场景 |
|----|------|------|
| `banner` | 首页轮播图 | 需要轮播管理功能时 |
| `feedback` | 用户反馈 | 需要反馈管理功能时 |
| `faq` | 常见问题 | 需要 FAQ 管理功能时 |

### 15.4 新增表详细设计

#### 15.4.1 admin_operation_log — 管理员操作日志

```sql
CREATE TABLE IF NOT EXISTS admin_operation_log (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    admin_id INTEGER NOT NULL COMMENT '操作管理员ID',
    module VARCHAR(50) NOT NULL COMMENT '操作模块：user/company/job/application/interview/benefit_tag/job_category/region',
    action VARCHAR(50) NOT NULL COMMENT '操作类型：create/update/delete/status_change/role_change/import/export',
    target_id INTEGER COMMENT '操作对象ID',
    target_name VARCHAR(200) COMMENT '操作对象名称（冗余，方便查询）',
    detail TEXT COMMENT '操作详情JSON：{before: ..., after: ...}',
    ip VARCHAR(50) COMMENT '操作IP地址',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_admin_id (admin_id),
    INDEX idx_module (module),
    INDEX idx_action (action),
    INDEX idx_target (module, target_id),
    INDEX idx_created_at (created_at)
) COMMENT '管理员操作日志表';
```

**用途**：
- 记录管理员的所有增删改操作
- 支持操作回溯和审计
- detail 字段存储变更前后数据快照

#### 15.4.2 system_config — 系统配置

```sql
CREATE TABLE IF NOT EXISTS system_config (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'string' COMMENT '值类型：string/number/boolean/json',
    description VARCHAR(200) COMMENT '配置说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INTEGER COMMENT '修改人ID'
) COMMENT '系统配置表';
```

**初始化数据**：

```sql
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('site_name', 'AI面试官', 'string', '站点名称'),
('site_logo', '/logo.png', 'string', '站点Logo'),
('maintenance_mode', 'false', 'boolean', '是否维护模式'),
('registration_enabled', 'true', 'boolean', '是否开放注册'),
('max_resume_count', '3', 'number', '每用户最大简历数'),
('max_import_rows', '500', 'number', '单次导入最大行数'),
('admin_email', 'admin@ai-interview.com', 'string', '管理员邮箱')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);
```

#### 15.4.3 job_audit_log — 职位审核日志

```sql
CREATE TABLE IF NOT EXISTS job_audit_log (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    job_id INTEGER NOT NULL COMMENT '职位ID',
    admin_id INTEGER NOT NULL COMMENT '审核管理员ID',
    action VARCHAR(20) NOT NULL COMMENT '操作：approve/reject',
    reason TEXT COMMENT '审核原因（拒绝时必填）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    INDEX idx_job_id (job_id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) COMMENT '职位审核日志表';
```

**用途**：
- 记录职位审核历史（通过/驳回）
- 支持查看某个职位的完整审核轨迹
- 驳回时记录原因，方便 HR 了解修改方向

### 15.5 现有代码需要修改

#### 15.5.1 JwtFilter — 新增管理员路径鉴权

当前 JwtFilter 只解析 Token，不校验角色。需要在 `/api/admin/**` 路径上增加 roleType = 3 校验：

```java
// 伪代码
if (path.startsWith("/api/admin/") && !path.equals("/api/admin/login")) {
    Integer roleType = UserContext.getRoleType();
    if (roleType == null || roleType != 3) {
        response.setStatus(403);
        return;
    }
}
```

#### 15.5.2 AdminLogin — 复用现有登录接口

管理员登录可复用 `POST /api/user/login`，前端判断返回的 roleType：
- roleType = 3 → 跳转管理后台，存 `admin_token`
- roleType = 1/2 → 跳转前台

**无需新建 `/api/admin/login` 接口**，只需前端逻辑调整。

#### 15.5.3 需要修改的 Service

| Service | 需要修改的内容 |
|---------|--------------|
| `UserServiceImpl` | 新增 `listAllUsers(keyword, roleType, status, page, size)` 分页查询 |
| `UserServiceImpl` | 新增 `updateUserRole(userId, roleType)` |
| `UserServiceImpl` | 新增 `updateUserStatus(userId, status)` |
| `UserServiceImpl` | 新增 `deleteUser(userId)` |
| `UserServiceImpl` | 新增 `batchUpdateStatus(userIds, status)` |
| `CompanyServiceImpl` | 新增 `listAllCompanies(keyword, industry, city, scale, status, page, size)` |
| `CompanyServiceImpl` | 新增 `updateCompanyStatus(companyId, status)` — 需先给 company 表加 status 字段 |
| `CompanyServiceImpl` | 新增 `deleteCompany(companyId)` |
| `JobServiceImpl` | 新增 `listAllJobs(keyword, company, city, jobType, education, status, page, size)` |
| `JobServiceImpl` | 新增 `updateJobStatus(jobId, status)` — 支持 pending/active/rejected/closed |
| `JobServiceImpl` | 新增 `deleteJobById(jobId)` — 管理员删除，无需公司校验 |
| `JobServiceImpl` | 新增 `batchUpdateStatus(jobIds, status)` |
| `ApplicationServiceImpl` | 新增 `listAllApplications(keyword, company, status, startDate, endDate, page, size)` |
| `ApplicationServiceImpl` | 修改：管理员查询时绕过 `checkCompanyMembership` 校验 |
| `InterviewServiceImpl` | 新增 `listAllInterviews(keyword, company, type, status, startDate, endDate, page, size)` |
| `BenefitTagService` | 新增 `listWithStats()` — 返回含使用统计的标签列表 |
| `BenefitTagService` | 新增 `getUsageDetail(tagId)` — 返回关联的公司和职位列表 |
| `JobCategoryService` | 新增 `saveCategory(name, parentId, icon, sort, description)` |
| `JobCategoryService` | 新增 `updateCategory(id, name, icon, sort, description)` |
| `JobCategoryService` | 新增 `deleteCategory(id)` — 有关联职位时拒绝 |
| `JobCategoryService` | 新增 `getCategoryTreeWithStats()` — 含职位数统计 |
| `RegionService` | 新增 `saveRegion(name, parentId, code, sort)` |
| `RegionService` | 新增 `updateRegion(id, name, code, sort)` |
| `RegionService` | 新增 `deleteRegion(id)` — 有子节点时连同删除 |
| `RegionService` | 新增 `getRegionTreeWithStats()` — 含关联统计 |

### 15.6 需要新增的后端文件

#### 15.6.1 拦截器

| 文件 | 说明 |
|------|------|
| `filter/AdminAuthInterceptor.java` | 管理员鉴权拦截器，校验 roleType = 3 |
| `config/AdminWebConfig.java` | 注册拦截器，拦截 `/api/admin/**`，排除 `/api/admin/login` |

#### 15.6.2 Controller（10 个）

| 文件 | 前缀 | 方法数 |
|------|------|--------|
| `AdminDashboardController.java` | `/api/admin/dashboard` | 9 个 |
| `AdminUserController.java` | `/api/admin/users` | 7 个 |
| `AdminCompanyController.java` | `/api/admin/companies` | 6 个 |
| `AdminJobController.java` | `/api/admin/jobs` | 8 个 |
| `AdminApplicationController.java` | `/api/admin/applications` | 5 个 |
| `AdminInterviewController.java` | `/api/admin/interviews` | 5 个 |
| `AdminBenefitTagController.java` | `/api/admin/benefit-tags` | 5 个 |
| `AdminJobCategoryController.java` | `/api/admin/job-categories` | 4 个 |
| `AdminRegionController.java` | `/api/admin/regions` | 4 个 |

#### 15.6.3 Service 方法（不新建 Service 文件，在现有 Service 中新增方法）

#### 15.6.4 DTO / VO

| 文件 | 说明 |
|------|------|
| `dto/admin/AdminUserQueryDTO.java` | 用户列表查询参数 |
| `dto/admin/AdminCompanyQueryDTO.java` | 公司列表查询参数 |
| `dto/admin/AdminJobQueryDTO.java` | 职位列表查询参数 |
| `dto/admin/AdminApplicationQueryDTO.java` | 申请列表查询参数 |
| `dto/admin/AdminInterviewQueryDTO.java` | 面试列表查询参数 |
| `vo/admin/AdminDashboardVO.java` | 仪表盘统计数据 |
| `vo/admin/AdminUserListVO.java` | 用户列表项 |
| `vo/admin/AdminCompanyListVO.java` | 公司列表项 |
| `vo/admin/AdminJobListVO.java` | 职位列表项 |
| `vo/admin/AdminApplicationListVO.java` | 申请列表项 |
| `vo/admin/AdminInterviewListVO.java` | 面试列表项 |
| `vo/admin/AdminBenefitTagVO.java` | 福利标签（含使用统计） |
| `vo/admin/AdminJobCategoryVO.java` | 职位分类（含职位数） |
| `vo/admin/AdminRegionVO.java` | 地区（含关联统计） |

#### 15.6.5 需要新增的 Maven 依赖

| 依赖 | 用途 | 是否必须 |
|------|------|---------|
| `easyexcel` | 后端 Excel 导入导出（替代纯前端 XLSX） | **推荐** |
| `spring-boot-starter-aop` | 操作日志切面（自动记录管理员操作） | **推荐** |

**EasyExcel 依赖**（如需后端处理导入导出）：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.4</version>
</dependency>
```

**AOP 依赖**（用于操作日志切面）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

> **说明**：当前前端已实现纯 XLSX 导入导出，可先上线。如需后端处理（大数据量、数据校验），再引入 EasyExcel。

---

## 十六、前端需要适配的改动

### 16.1 AdminLogin.vue — 接入真实登录

当前前端硬编码验证 `admin / admin123`，需要改为调用后端接口：

```javascript
// 改造前（当前）
if (form.username === 'admin' && form.password === 'admin123') {
  localStorage.setItem('admin_token', 'mock_admin_token')
  router.push('/admin/dashboard')
}

// 改造后
const res = await axios.post('/api/user/login', {
  username: form.username,
  password: form.password
})
if (res.data.roleType === 3) {
  localStorage.setItem('admin_token', res.data.token)
  router.push('/admin/dashboard')
} else {
  ElMessage.error('非管理员账号')
}
```

### 16.2 各管理页面 — 接入真实 API

当前所有管理页面使用硬编码 `ref([...])` 模拟数据，需要改造为：

1. **onMounted** 时调用 API 加载数据
2. **筛选条件变化**时重新请求 API（带分页参数）
3. **操作按钮**调用对应 API 后刷新列表
4. **导入功能**改为上传到后端处理（或保持前端 XLSX 处理）

### 16.3 新建前端 API 封装

需要新建 `frontend/src/api/admin.js`，封装所有管理后台接口：

```javascript
// admin.js
import request from '@/utils/request'

// 仪表盘
export const getDashboardSummary = () => request.get('/api/admin/dashboard/summary')
export const getUserTrend = (days) => request.get('/api/admin/dashboard/user-trend', { params: { days } })
// ... 其他图表接口

// 用户管理
export const getAdminUsers = (params) => request.get('/api/admin/users', { params })
export const updateAdminUser = (id, data) => request.put(`/api/admin/users/${id}`, data)
export const updateUserRole = (id, roleType) => request.put(`/api/admin/users/${id}/role`, { roleType })
export const updateUserStatus = (id, status) => request.put(`/api/admin/users/${id}/status`, { status })
export const deleteAdminUser = (id) => request.delete(`/api/admin/users/${id}`)

// 公司管理
export const getAdminCompanies = (params) => request.get('/api/admin/companies', { params })
// ... 其他接口

// 职位管理
export const getAdminJobs = (params) => request.get('/api/admin/jobs', { params })
// ... 其他接口
```

### 16.4 Axios 请求拦截器 — 自动携带 Token

```javascript
// request.js 拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token')
  if (token && config.url.startsWith('/api/admin')) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

### 16.5 路由守卫 — Token 有效性校验

当前路由守卫只检查 `admin_token` 是否存在，需要增加：
- 调用 `/api/admin/profile` 验证 Token 有效性
- Token 失效时跳转登录页

---

## 十七、实施计划

### 阶段一：基础设施（1-2 天）

1. 修改 `company` 表，新增 `status` 字段
2. 修改 `job` 表，新增 `pending`、`rejected` 状态
3. 修改 `job_category` 表，新增 `status`、`sort`、`description` 字段
4. 修改 `region` 表，新增 `status`、`sort`、`code` 字段
5. 新建 `admin_operation_log` 表（**已完成**）
6. 新建 `system_config` 表（**已完成**）
7. 新建 `job_audit_log` 表（**已完成**）
8. 新建 `AdminAuthInterceptor` + `AdminWebConfig`
9. 修改 `JwtFilter`，增加管理员路径校验

### 阶段二：管理员认证（0.5 天）

8. 修改 `AdminLogin.vue`，接入真实登录接口
9. 修改路由守卫，验证 Token 有效性
10. 新建 `admin.js` API 封装文件

### 阶段三：数据管理 API（3-4 天）

11. 实现 `AdminUserController` + Service 方法
12. 实现 `AdminCompanyController` + Service 方法
13. 实现 `AdminJobController` + Service 方法（含审核流程）
14. 实现 `AdminApplicationController` + Service 方法
15. 实现 `AdminInterviewController` + Service 方法

### 阶段四：标签分类地区 API（1-2 天）

16. 实现 `AdminBenefitTagController` + Service 方法
17. 实现 `AdminJobCategoryController` + Service 方法
18. 实现 `AdminRegionController` + Service 方法

### 阶段五：仪表盘 API（1-2 天）

19. 实现 `AdminDashboardController` + 统计查询 SQL
20. 实现趋势图数据接口（30 天聚合查询）
21. 实现分布图数据接口（分组统计）

### 阶段六：前端联调（2-3 天）

22. 各管理页面接入真实 API
23. 导入导出功能联调
24. 批量操作联调
25. 仪表盘图表联调

### 阶段七：优化与测试（1 天）

26. 操作日志记录
27. 权限测试（非法角色访问 admin 路由）
28. 数据准确性验证
29. UI 细节打磨

---

## 十八、注意事项

1. **安全**：管理员接口必须校验 role_type = 3，防止普通用户越权操作
2. **日志**：所有管理员操作（审核、删除、修改状态）需记录操作日志，便于审计
3. **删除确认**：所有删除操作前端弹出二次确认框（ElMessageBox）
4. **数据一致性**：删除福利标签时，同步清理 company_benefit 和 job_benefit 中间表
5. **密码存储**：管理员密码使用 BCrypt 加密存储
6. **默认管理员**：数据库初始化时创建默认管理员账号 `admin / admin123`
7. **批量导入**：单次最多 500 条，前端校验文件格式，后端校验数据合法性
8. **树形数据**：职位分类和地区使用递归树结构，删除父节点时连同子节点一起删除
9. **Token 隔离**：管理员 Token 和前台用户 Token 使用同一个 JWT 体系，通过 roleType 区分
10. **前端导入导出**：当前使用 XLSX 库纯前端处理，可先上线，后续根据需求改为后端处理

---

## 十九、数据字典

### 19.1 用户状态（user.status）

| 值 | 含义 | 说明 |
|----|------|------|
| `active` | 正常 | 可正常使用所有功能 |
| `disabled` | 禁用 | 无法登录，管理员可恢复 |

### 19.2 角色类型（user.role_type）

| 值 | 含义 | 权限范围 |
|----|------|---------|
| `1` | 求职者 | 投递、面试、聊天 |
| `2` | HR | 发布职位、管理申请、视频面试 |
| `3` | 管理员 | 全平台管理权限 |

### 19.3 公司状态（company.status）— 需新增

| 值 | 含义 | 说明 |
|----|------|------|
| `active` | 正常 | 可被搜索、展示 |
| `disabled` | 禁用 | 不展示，管理员可恢复 |

### 19.4 职位状态（job.status）

| 值 | 含义 | 说明 |
|----|------|------|
| `pending` | 待审核 | HR 发布后待管理员审核（需新增） |
| `active` | 招聘中 | 正常展示，可投递 |
| `paused` | 暂停 | HR 主动暂停，不展示 |
| `closed` | 已关闭 | 职位已结束 |
| `rejected` | 已拒绝 | 管理员审核不通过（需新增） |

### 19.5 申请状态（application.status）

| 值 | 含义 | 说明 |
|----|------|------|
| `pending` | 待处理 | 新投递，HR 未查看 |
| `viewed` | 已查看 | HR 已查看简历 |
| `screening` | 筛选中 | HR 正在评估 |
| `interview` | 面试中 | 已安排面试 |
| `offer` | 已录用 | 发放 Offer |
| `rejected` | 已拒绝 | HR 拒绝 |
| `withdrawn` | 已撤回 | 求职者主动撤回 |

### 19.6 面试状态（interview.status）

| 值 | 含义 | 说明 |
|----|------|------|
| `pending` | 待开始 | 已安排未开始 |
| `in_progress` | 进行中 | 面试进行中 |
| `completed` | 已完成 | 面试结束 |
| `cancelled` | 已取消 | 主动取消 |

### 19.7 面试类型（interview.type）— 前端新增

| 值 | 含义 | 说明 |
|----|------|------|
| `ai` | AI 面试 | 系统自动面试 |
| `video` | 视频面试 | HR 在线面试 |
| `onsite` | 现场面试 | 线下面试 |

### 19.8 福利标签状态（benefit_tag.enabled）

| 值 | 含义 | 说明 |
|----|------|------|
| `1` | 启用 | 可被选择 |
| `0` | 禁用 | 不可选择 |

### 19.9 职位分类状态（job_category.status）— 需新增

| 值 | 含义 | 说明 |
|----|------|------|
| `active` | 启用 | 可被选择 |
| `disabled` | 禁用 | 不可选择 |

### 19.10 地区状态（region.status）— 需新增

| 值 | 含义 | 说明 |
|----|------|------|
| `active` | 启用 | 可被选择 |
| `disabled` | 禁用 | 不可选择 |

### 19.11 操作日志模块（admin_operation_log.module）

| 值 | 含义 |
|----|------|
| `user` | 用户管理 |
| `company` | 公司管理 |
| `job` | 职位管理 |
| `application` | 申请管理 |
| `interview` | 面试管理 |
| `benefit_tag` | 福利标签管理 |
| `job_category` | 职位分类管理 |
| `region` | 地区管理 |
| `system` | 系统配置 |

### 19.12 操作日志动作（admin_operation_log.action）

| 值 | 含义 |
|----|------|
| `create` | 新增 |
| `update` | 修改 |
| `delete` | 删除 |
| `status_change` | 状态变更 |
| `role_change` | 角色变更 |
| `import` | 批量导入 |
| `export` | 导出 |
| `approve` | 审核通过 |
| `reject` | 审核拒绝 |

---

## 二十、API 接口详细定义

### 20.1 统一响应格式

**成功响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

**分页响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

**错误响应**：

```json
{
  "code": 400,
  "msg": "参数错误：用户名不能为空",
  "data": null
}
```

### 20.2 错误码定义

| 错误码 | 含义 | 说明 |
|--------|------|------|
| `200` | 成功 | |
| `400` | 参数错误 | 请求参数校验失败 |
| `401` | 未认证 | Token 无效或过期 |
| `403` | 无权限 | 角色权限不足 |
| `404` | 资源不存在 | |
| `500` | 服务器错误 | 内部异常 |

### 20.3 管理员认证接口

#### POST /api/user/login — 管理员登录

**请求**：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应**：

```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "admin",
    "roleType": 3
  }
}
```

#### GET /api/admin/profile — 获取管理员信息

**响应**：

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "超级管理员",
    "avatar": null,
    "roleType": 3
  }
}
```

### 20.4 仪表盘接口

#### GET /api/admin/dashboard/summary

**响应**：

```json
{
  "code": 200,
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

#### GET /api/admin/dashboard/user-trend

**参数**：`days` (int, 默认 30)

**响应**：

```json
{
  "code": 200,
  "data": [
    { "date": "2024-01-01", "count": 12 },
    { "date": "2024-01-02", "count": 8 }
  ]
}
```

#### GET /api/admin/dashboard/job-type

**响应**：

```json
{
  "code": 200,
  "data": [
    { "name": "全职", "value": 450 },
    { "name": "兼职", "value": 120 },
    { "name": "实习", "value": 80 }
  ]
}
```

### 20.5 用户管理接口

#### GET /api/admin/users — 用户列表

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（用户名/手机/邮箱） |
| roleType | int | 否 | 角色类型筛选 |
| status | string | 否 | 状态筛选 |
| page | int | 是 | 页码，默认 1 |
| size | int | 是 | 每页条数，默认 10 |

**响应**：

```json
{
  "code": 200,
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
        "createdAt": "2024-01-01 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

#### PUT /api/admin/users/{id}/status — 修改用户状态

**请求**：

```json
{
  "status": "disabled"
}
```

#### PUT /api/admin/users/{id}/role — 修改用户角色

**请求**：

```json
{
  "roleType": 2
}
```

### 20.6 公司管理接口

#### GET /api/admin/companies — 公司列表

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（公司名称） |
| industry | string | 否 | 行业筛选 |
| city | string | 否 | 城市筛选 |
| scale | string | 否 | 规模筛选 |
| status | string | 否 | 状态筛选 |
| page | int | 是 | 页码 |
| size | int | 是 | 每页条数 |

**响应**：

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
        "createdAt": "2024-01-01"
      }
    ],
    "total": 56,
    "page": 1,
    "size": 10
  }
}
```

#### PUT /api/admin/companies/{id}/status — 修改公司状态

### 20.7 职位管理接口

#### GET /api/admin/jobs — 职位列表

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（职位名称） |
| companyName | string | 否 | 公司名称筛选 |
| city | string | 否 | 城市筛选 |
| jobType | string | 否 | 职位类型筛选 |
| education | string | 否 | 学历要求筛选 |
| status | string | 否 | 状态筛选 |
| page | int | 是 | 页码 |
| size | int | 是 | 每页条数 |

**响应**：

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
        "jobType": "全职",
        "education": "本科",
        "experience": "3-5年",
        "status": "active",
        "viewCount": 567,
        "applyCount": 23,
        "publishedAt": "2024-01-15"
      }
    ],
    "total": 789,
    "page": 1,
    "size": 10
  }
}
```

#### PUT /api/admin/jobs/{id}/status — 审核职位

**请求**：

```json
{
  "status": "active",
  "reason": ""
}
```

**说明**：
- `status`: active=通过, rejected=拒绝
- `reason`: 拒绝时必填

### 20.8 申请管理接口

#### GET /api/admin/applications — 申请列表

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（求职者/职位） |
| company | string | 否 | 公司筛选 |
| status | string | 否 | 状态筛选 |
| startDate | string | 否 | 开始日期 |
| endDate | string | 否 | 结束日期 |
| page | int | 是 | 页码 |
| size | int | 是 | 每页条数 |

### 20.9 面试管理接口

#### GET /api/admin/interviews — 面试列表

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词（求职者/职位） |
| company | string | 否 | 公司筛选 |
| type | string | 否 | 面试类型筛选 |
| status | string | 否 | 状态筛选 |
| startDate | string | 否 | 开始日期 |
| endDate | string | 否 | 结束日期 |
| page | int | 是 | 页码 |
| size | int | 是 | 每页条数 |

#### GET /api/admin/interviews/{id}/report — 面试报告

**响应**：

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

### 20.10 福利标签管理接口

#### GET /api/admin/benefit-tags — 标签列表（含统计）

**响应**：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "五险一金",
      "color": "#409EFF",
      "sort": 1,
      "enabled": true,
      "companyCount": 45,
      "jobCount": 120,
      "createdAt": "2024-01-01"
    }
  ]
}
```

#### GET /api/admin/benefit-tags/{id}/usage — 标签使用详情

**响应**：

```json
{
  "code": 200,
  "data": {
    "tagId": 1,
    "tagName": "五险一金",
    "companies": [
      { "id": 1, "name": "阿里巴巴" },
      { "id": 2, "name": "腾讯" }
    ],
    "jobs": [
      { "id": 1, "title": "Java开发", "companyName": "阿里巴巴" }
    ]
  }
}
```

### 20.11 职位分类管理接口

#### GET /api/admin/job-categories — 分类树（含统计）

**响应**：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "技术开发",
      "icon": "Monitor",
      "color": "#409EFF",
      "sort": 1,
      "enabled": true,
      "jobCount": 234,
      "children": [
        {
          "id": 6,
          "name": "Java开发",
          "jobCount": 120
        }
      ]
    }
  ]
}
```

### 20.12 地区管理接口

#### GET /api/admin/regions — 地区树（含统计）

**响应**：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "浙江省",
      "level": 1,
      "areaCode": "330000",
      "sort": 1,
      "enabled": true,
      "jobCount": 567,
      "companyCount": 89,
      "children": [
        {
          "id": 2,
          "name": "杭州市",
          "level": 2,
          "jobCount": 345,
          "companyCount": 56
        }
      ]
    }
  ]
}
```

---

## 二十一、数据库修改 DDL

### 21.1 company 表 — 新增 status 字段

```sql
ALTER TABLE company
ADD COLUMN status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-正常 disabled-禁用'
AFTER description;

-- 为现有数据设置默认状态
UPDATE company SET status = 'active' WHERE status IS NULL;

-- 添加索引
ALTER TABLE company ADD INDEX idx_status (status);
```

### 21.2 job 表 — 新增 pending/rejected 状态

```sql
-- 修改 status 字段注释，明确支持的状态值
ALTER TABLE job
MODIFY COLUMN status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待审核 active-招聘中 paused-暂停 closed-已关闭 rejected-已拒绝';

-- 将现有 active 状态的职位改为 pending（需要管理员重新审核）
-- 注意：根据业务需求决定是否执行此语句
-- UPDATE job SET status = 'pending' WHERE status = 'active';
```

### 21.3 job_category 表 — 新增字段

```sql
ALTER TABLE job_category
ADD COLUMN status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-启用 disabled-禁用' AFTER sort_order,
ADD COLUMN description VARCHAR(200) COMMENT '分类描述' AFTER status;

-- 添加索引
ALTER TABLE job_category ADD INDEX idx_status (status);
```

### 21.4 region 表 — 新增字段

```sql
ALTER TABLE region
ADD COLUMN status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-启用 disabled-禁用' AFTER level,
ADD COLUMN sort_order INTEGER DEFAULT 0 COMMENT '排序权重' AFTER status,
ADD COLUMN area_code VARCHAR(20) COMMENT '行政区划代码' AFTER sort_order;

-- 添加索引
ALTER TABLE region ADD INDEX idx_status (status);
```

### 21.5 完整修改脚本

```sql
-- ============================================
-- 管理后台数据库修改脚本
-- 执行时间：2024-XX-XX
-- ============================================

-- 1. company 表新增 status
ALTER TABLE company
ADD COLUMN status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-正常 disabled-禁用'
AFTER description;
UPDATE company SET status = 'active' WHERE status IS NULL;
ALTER TABLE company ADD INDEX idx_status (status);

-- 2. job 表修改 status 默认值
ALTER TABLE job
MODIFY COLUMN status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待审核 active-招聘中 paused-暂停 closed-已关闭 rejected-已拒绝';

-- 3. job_category 表新增字段
ALTER TABLE job_category
ADD COLUMN status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-启用 disabled-禁用' AFTER sort_order,
ADD COLUMN description VARCHAR(200) COMMENT '分类描述' AFTER status;
ALTER TABLE job_category ADD INDEX idx_status (status);

-- 4. region 表新增字段
ALTER TABLE region
ADD COLUMN status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-启用 disabled-禁用' AFTER level,
ADD COLUMN sort_order INTEGER DEFAULT 0 COMMENT '排序权重' AFTER status;
ALTER TABLE region ADD INDEX idx_status (status);

-- 5. 默认管理员账号（如不存在）
INSERT INTO user (username, password, nickname, role_type, status)
SELECT 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 3, 'active'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'admin');

-- 注意：密码为 admin123 的 BCrypt 加密值
-- 如需重新生成，使用 BCryptPasswordEncoder.encode("admin123")
```

---

## 二十二、权限矩阵

### 22.1 角色权限对照表

| 功能模块 | 接口 | 求职者(1) | HR(2) | 管理员(3) |
|----------|------|-----------|-------|-----------|
| **用户管理** | GET /api/admin/users | ✗ | ✗ | ✓ |
| | PUT /api/admin/users/{id}/status | ✗ | ✗ | ✓ |
| | PUT /api/admin/users/{id}/role | ✗ | ✗ | ✓ |
| **公司管理** | GET /api/admin/companies | ✗ | ✗ | ✓ |
| | PUT /api/admin/companies/{id}/status | ✗ | ✗ | ✓ |
| | GET /api/company/my | ✗ | ✓ | ✗ |
| | POST /api/company | ✗ | ✓ | ✗ |
| **职位管理** | GET /api/admin/jobs | ✗ | ✗ | ✓ |
| | PUT /api/admin/jobs/{id}/status | ✗ | ✗ | ✓ |
| | GET /api/job/my | ✗ | ✓ | ✗ |
| | POST /api/job | ✗ | ✓ | ✗ |
| **申请管理** | GET /api/admin/applications | ✗ | ✗ | ✓ |
| | PUT /api/admin/applications/{id}/status | ✗ | ✗ | ✓ |
| | GET /api/application/my | ✓ | ✗ | ✗ |
| | GET /api/application/job/{jobId} | ✗ | ✓ | ✓ |
| **面试管理** | GET /api/admin/interviews | ✗ | ✗ | ✓ |
| | GET /api/admin/interviews/{id}/report | ✗ | ✗ | ✓ |
| **福利标签** | GET /api/admin/benefit-tags | ✗ | ✗ | ✓ |
| | POST /api/admin/benefit-tags | ✗ | ✗ | ✓ |
| | GET /api/benefit-tag | ✓ | ✓ | ✓ |
| **职位分类** | GET /api/admin/job-categories | ✗ | ✗ | ✓ |
| | POST /api/admin/job-categories | ✗ | ✗ | ✓ |
| | GET /api/category/tree | ✓ | ✓ | ✓ |
| **地区管理** | GET /api/admin/regions | ✗ | ✗ | ✓ |
| | POST /api/admin/regions | ✗ | ✗ | ✓ |
| | GET /api/region/tree | ✓ | ✓ | ✓ |
| **仪表盘** | GET /api/admin/dashboard/* | ✗ | ✗ | ✓ |

### 22.2 接口路径权限规则

| 路径模式 | 权限要求 | 说明 |
|----------|---------|------|
| `/api/admin/**` | roleType = 3 | 仅管理员可访问 |
| `/api/admin/login` | 无需认证 | 登录接口白名单 |
| `/api/user/login` | 无需认证 | 通用登录接口 |
| `/api/user/register` | 无需认证 | 注册接口 |
| `/api/**` | 需要登录 | 其他接口需有效 Token |

---

## 二十三、业务流程图

### 23.1 职位审核流程

```
┌─────────────┐
│  HR 发布职位  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ status=pending │
└──────┬──────┘
       │
       ▼
┌─────────────┐    拒绝     ┌─────────────┐
│  管理员审核   │ ─────────→ │ status=rejected │
└──────┬──────┘            │  记录拒绝原因  │
       │ 通过               └─────────────┘
       ▼
┌─────────────┐
│ status=active │
│  职位正常展示  │
└─────────────┘
```

### 23.2 用户禁用/恢复流程

```
┌─────────────┐
│  管理员操作   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ 选择禁用/恢复 │
└──────┬──────┘
       │
       ▼
┌─────────────────┐    禁用     ┌─────────────────┐
│  更新 user.status │ ─────────→ │ status=disabled  │
└─────────────────┘            │ 用户无法登录     │
       │ 恢复                  └─────────────────┘
       ▼
┌─────────────────┐
│  status=active   │
│  用户可正常登录   │
└─────────────────┘
```

### 23.3 批量导入流程

```
┌─────────────────┐
│  管理员上传 Excel  │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  前端 XLSX 解析   │
│  格式校验        │
└──────┬──────────┘
       │ 校验通过
       ▼
┌─────────────────┐
│  调用后端导入 API  │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  后端逐条校验     │
│  数据合法性       │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  批量插入数据库   │
│  返回成功/失败数  │
└─────────────────┘
```

### 23.4 管理员操作日志记录流程

```
┌─────────────────┐
│  管理员执行操作   │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  AOP 切面拦截     │
│  记录操作信息     │
└──────┬──────────┘
       │
       ▼
┌─────────────────────┐
│  写入 admin_operation_log │
│  - admin_id           │
│  - module             │
│  - action             │
│  - target_id          │
│  - detail (JSON)      │
│  - ip                 │
└─────────────────────┘
```

---

## 二十四、ER 图（文本描述）

### 24.1 核心实体关系

```
┌──────────┐     1:N     ┌──────────┐     N:1     ┌──────────┐
│   user   │ ──────────→ │ company  │ ←────────── │   job    │
└────┬─────┘             └────┬─────┘             └────┬─────┘
     │                        │                        │
     │ 1:N                    │ 1:N                    │ 1:N
     ▼                        ▼                        ▼
┌──────────┐          ┌──────────────┐          ┌──────────────┐
│   resume │          │company_benefit│          │  job_benefit │
└──────────┘          └──────────────┘          └──────────────┘
     │                                                │
     │ N:1                                            │ N:1
     ▼                                                ▼
┌──────────────┐                              ┌──────────────┐
│  user_resume │                              │ benefit_tag  │
└──────────────┘                              └──────────────┘
```

### 24.2 面试相关关系

```
┌──────────────┐     1:N     ┌──────────────┐
│ application  │ ──────────→ │  interview   │
└──────────────┘             └──────┬───────┘
                                    │ 1:N
                                    ▼
                             ┌──────────────┐
                             │ interview_qa │
                             └──────────────┘
                                    │ 1:1
                                    ▼
                             ┌──────────────┐
                             │    report    │
                             └──────────────┘
```

### 24.3 管理后台新增关系

```
┌──────────┐     1:N     ┌─────────────────────┐
│   user   │ ──────────→ │ admin_operation_log  │
│(admin)   │             └─────────────────────┘
└──────────┘
     │
     │ 1:N
     ▼
┌──────────────┐
│job_audit_log │
└──────────────┘

┌──────────────┐
│system_config │  独立配置表
└──────────────┘
```

---

## 二十五、性能设计

### 25.1 索引设计

| 表 | 索引 | 类型 | 用途 |
|----|------|------|------|
| user | idx_username | UNIQUE | 登录查询 |
| user | idx_role_type | NORMAL | 角色筛选 |
| user | idx_status | NORMAL | 状态筛选 |
| company | idx_user_id | NORMAL | HR 查询自己的公司 |
| company | idx_city | NORMAL | 城市筛选 |
| company | idx_status | NORMAL | 状态筛选 |
| job | idx_company_id | NORMAL | 公司职位查询 |
| job | idx_status | NORMAL | 状态筛选 |
| job | ft_title_desc | FULLTEXT | 全文搜索 |
| application | uk_job_user | UNIQUE | 防止重复投递 |
| application | idx_user_id | NORMAL | 求职者申请查询 |
| admin_operation_log | idx_admin_id | NORMAL | 操作人查询 |
| admin_operation_log | idx_module | NORMAL | 模块筛选 |
| admin_operation_log | idx_created_at | NORMAL | 时间范围查询 |

### 25.2 缓存策略

| 数据 | 缓存 Key | 过期时间 | 更新时机 |
|------|----------|---------|---------|
| 仪表盘统计 | `admin:dashboard:summary` | 5 分钟 | 数据变更时主动刷新 |
| 用户总数 | `admin:stats:users` | 5 分钟 | 用户增删时刷新 |
| 公司总数 | `admin:stats:companies` | 5 分钟 | 公司增删时刷新 |
| 职位总数 | `admin:stats:jobs` | 5 分钟 | 职位增删时刷新 |
| 系统配置 | `system:config:*` | 30 分钟 | 配置修改时刷新 |

### 25.3 分页优化

- **深分页问题**：超过 1000 页时使用游标分页（基于 ID）
- ** COUNT 优化**：大数据量时使用近似值（`EXPLAIN` 估算）
- **列表查询**：只查询必要字段，避免 `SELECT *`

### 25.4 批量操作限制

| 操作 | 单次上限 | 说明 |
|------|---------|------|
| Excel 导入 | 500 条 | 超过需分批 |
| 批量删除 | 100 条 | 避免长事务 |
| 批量状态变更 | 200 条 | 避免长事务 |

### 25.5 慢查询监控

建议对以下查询添加监控：
- 仪表盘统计 SQL
- 全文搜索查询
- 树形数据递归查询
- 跨表关联查询
