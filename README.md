<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/2e159bf0-81b1-4d6c-bd8a-4658a1cdc6bf" /># 乐俊招聘平台

基于 AI 大模型的智能招聘面试系统，支持文字/沉浸式/八股选择题三种面试模式，集成语音识别（STT）、语音合成（TTS）、视频通话、简历智能匹配等能力，为企业招聘和个人求职提供智能化面试解决方案。

效果图展示
三栏功能分类 简历分析 - 根据简历分析，生成面试题 | 八股栏目，针对性生成八股题 | 混合模式 简历分析 + 八股栏目混合出题

面试面板选择 - 混合模式为例
<img width="1209" height="1187" alt="image" src="https://github.com/user-attachments/assets/6573c5a0-d2fe-49ec-9f8e-19c9eb69c664" />


AI模拟面试对话 - 简历分析
<img width="1209" height="939" alt="image" src="https://github.com/user-attachments/assets/a697f979-69d6-4216-bdd8-93365ec61f33" />
面试评估报告
<img width="1428" height="1208" alt="image" src="https://github.com/user-attachments/assets/ac72a80d-b0b4-4893-8a3e-0f918e53f66e" />

八股选择题 - 八股栏目
<img width="2559" height="1439" alt="image" src="https://github.com/user-attachments/assets/c35a5359-6f05-47a3-a151-389a053b8a99" />
答卷报告
<img width="2511" height="1401" alt="image" src="https://github.com/user-attachments/assets/6e5636e7-c214-455b-b811-03891879d110" />

AI语音模拟面试 - 混合模式
<img width="2519" height="1296" alt="image" src="https://github.com/user-attachments/assets/ce9b1482-c837-4e4d-971a-201bce8cd57a" />
面试评估报告
<img width="1875" height="1228" alt="image" src="https://github.com/user-attachments/assets/d86372d4-4193-47a2-ae92-b5795c74184f" />






## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | 应用框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Spring AI | 1.0.0 | AI 大模型接入（MiMo / DeepSeek） |
| Elasticsearch | 8.12.0 | 全文搜索 |
| Redis | 7.2.4 | 缓存、会话管理 |
| MongoDB | 7.0 | 聊天消息存储 |
| RabbitMQ | 3.13 | 消息队列 |
| MinIO | Latest | 文件存储（简历等） |
| MySQL | 8.0.36 | 主数据库 |
| Milvus | 2.4 | 向量数据库（简历-岗位匹配） |
| Java | 17 | 运行环境 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| Element Plus | 2.14.x | UI 组件库 |
| Vite | - | 构建工具 |
| Pinia | 3.x | 状态管理 |
| Vue Router | 5.x | 路由管理 |
| ECharts | 6.x | 数据可视化 |
| PeerJS | - | WebRTC 视频通话封装 |

## 项目结构

```
ai-interview/
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/interview/
│       ├── controller/         # 控制器（REST API）
│       ├── service/            # 业务逻辑层
│       ├── domain/             # 实体类
│       ├── dto/                # 数据传输对象
│       ├── mapper/             # MyBatis-Plus Mapper
│       ├── es/                 # Elasticsearch 文档与仓库
│       ├── config/             # 配置类
│       ├── filter/             # JWT 过滤器
│       ├── websocket/          # WebSocket 处理器
│       ├── sse/                # SSE 推送
│       ├── mq/                 # RabbitMQ 消息处理
│       ├── repository/         # MongoDB 仓库
│       └── util/               # 工具类
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── views/              # 页面组件
│       │   ├── admin/          # 管理后台页面
│       │   └── hr/             # HR 管理页面
│       ├── api/                # API 请求封装
│       ├── stores/             # Pinia 状态管理
│       ├── router/             # 路由配置
│       └── assets/             # 静态资源
├── docs/                       # 设计文档
├── docker-compose.yml          # 容器编排
└── .env.example                # 环境变量模板
```

## 核心功能

### 求职者端
- **智能搜索**：基于 Elasticsearch 的职位/公司全文搜索，支持多条件筛选
- **AI 文字面试**：AI 根据简历和岗位 JD 自动生成面试题，实时对话评分
- **AI 沉浸式面试**：语音识别 + 语音合成 + 可视化 AI 面试官，模拟真实面试场景
- **八股选择题面试**：批量生成技术选择题，自动评分
- **简历管理**：上传简历至 MinIO，AI 解析提取关键信息
- **简历智能匹配**：基于 Milvus 向量相似度的简历-岗位推荐
- **视频通话面试**：基于 WebRTC（PeerJS）的实时视频面试
- **消息中心**：与 HR 实时聊天，WebSocket 消息推送 + SSE 通知

### HR 端
- **职位管理**：发布、编辑、上下架职位
- **公司信息管理**：维护公司资料，上传 Logo
- **申请管理**：查看求职者投递，筛选候选人
- **聊天沟通**：与求职者实时在线沟通

### 管理员端
- **数据仪表盘**：用户增长趋势、投递趋势、职位分布等可视化统计
- **用户/公司/职位/申请/面试管理**：全量数据的增删改查
- **福利标签管理**：自定义福利标签字典
- **职位分类管理**：树形职位分类体系
- **地区管理**：省市区三级联动数据
- **批量导入导出**：支持 Excel 批量导入导出
- **知识图谱管理**：AI 文档处理与知识抽取

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Docker & Docker Compose（推荐用于中间件）

### 1. 启动中间件

```bash
# 启动 MySQL、Redis、MongoDB、RabbitMQ、Elasticsearch、MinIO
docker compose up -d
```

各服务端口及默认账号：

| 服务 | 端口 | 账号/密码 |
|------|------|----------|
| MySQL | 3306 | root / root123 |
| Redis | 6379 | redis123 |
| MongoDB | 27017 | admin / admin123 |
| RabbitMQ | 5672 / 15672 | guest / guest |
| Elasticsearch | 9200 | - |
| MinIO | 9000 / 9001 | minioadmin / minioadmin123 |

### 2. 配置环境变量

复制 `.env.example` 为 `.env` 并填入实际值：

```bash
cp .env.example .env
```

主要配置项：
- `AI_API_KEY` - MiMo API 密钥
- `DEEPSEEK_API_KEY` - DeepSeek API 密钥
- `JWT_SECRET` - JWT 签名密钥

### 3. 启动后端

```bash
cd backend

# 编译启动
mvn spring-boot:run
```

后端启动后监听 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动后监听 `http://localhost:5173`。

### 5. Docker 部署（生产环境）

```bash
# 打包后端
cd backend
mvn clean package -DskipTests

# 打包前端
cd frontend
npm run build

# 构建并启动所有服务
docker compose up -d --build
```

访问 http://localhost 即可

## 默认账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | admin123 |

## 管理后台

访问 `/admin/login` 进入管理后台登录页。管理员账号（`roleType = 3`）登录后自动跳转仪表盘。

## AI 配置

系统支持接入多个 AI 大模型，通过 Spring AI OpenAI SDK 兼容接口对接：

- **MiMo**（默认主模型）：用于面试对话、问题生成、简历解析
- **DeepSeek**：用于答案评分
- **DashScope Embedding**：用于简历-岗位向量匹配

配置项位于 `application.yml` 或通过环境变量注入。

## 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `MYSQL_USERNAME` | MySQL 用户名 | root |
| `MYSQL_PASSWORD` | MySQL 密码 | root123 |
| `REDIS_PASSWORD` | Redis 密码 | redis123 |
| `MONGO_USERNAME` | MongoDB 用户名 | admin |
| `MONGO_PASSWORD` | MongoDB 密码 | admin123 |
| `AI_API_KEY` | MiMo API 密钥 | - |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | - |
| `DASHSCOPE_API_KEY` | DashScope Embedding 密钥 | - |
| `JWT_SECRET` | JWT 签名密钥 | - |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | minioadmin |
| `MINIO_SECRET_KEY` | MinIO 秘密密钥 | minioadmin123 |

## 文档

项目设计文档位于 `docs/` 目录：

- `八股栏目与混合模式设计文档.md` — 八股选择题模块
- `岗位匹配模块设计文档.md` — Milvus 向量匹配模块
- `需求分析与功能设计.md` — 整体需求与功能规划
- `admin-backend-design.md` — 管理后台设计文档
- `AI文字模拟面试-完整设计与实现.md` — 文字面试模块
- `沉浸式AI模拟面试-完整设计与实现.md` — 沉浸式面试模块
- `elasticsearch-design.md` — ES 搜索模块设计
- `tts-design.md` / `stt-design.md` — 语音合成/识别设计
- `chat-implementation.md` — 聊天功能实现
- `video-call-implementation.md` — 视频通话实现
- `sse-implementation.md` — 消息推送实现

后端 API 接口文档：`backend/src/main/java/com/interview/API文档.md`

## License

私有项目，仅供学习和内部使用。
