# 医疗健康智能客服系统

基于 Spring Boot 3.x + Spring AI Alibaba 的 RAG 增强型医疗健康问答系统。

## 功能概览

- **智能对话** — SSE 流式回复，支持多轮对话、模糊问题澄清、用户反馈
- **RAG 检索增强** — 查询重写 → 多路检索（向量 + BM25）→ RRF 融合 → Cross-encoder 重排序 → 父文档上下文
- **知识库管理** — 医学章节感知切分、证据分级、Elasticsearch HNSW 向量索引
- **数据抓取** — 自动爬取 WHO、中国疾控中心、丁香医生等公开健康知识源
- **管理后台** — 仪表盘、用户管理、知识导入/抓取、DFA 敏感词过滤、操作日志

## 技术栈

| 组件 | 用途 |
|------|------|
| Java 17 + Spring Boot 3.3.5 | 后端框架 |
| Spring AI Alibaba 1.0.0.3 | DashScope Chat（qwen-max）+ Embedding（text-embedding-v3, 1024维） |
| MySQL 8.0 | 业务数据持久化 |
| Redis 7.x | 会话缓存、JWT 黑名单、限流计数 |
| Elasticsearch 8.x | HNSW 向量检索 + IK 中文分词 + BM25 关键词检索 |
| Spring Security + JWT | 无状态认证 & 授权 |
| Jsoup 1.17 | HTML 数据清洗 |
| SpringDoc OpenAPI 2.5 | API 文档（Swagger UI） |

## 运行环境

- **JDK** 17+
- **Maven** 3.9+
- **MySQL** 8.0 — 创建数据库 `health_assistant`（UTF-8, utf8mb4）
- **Redis** 7.x（默认端口 6379）
- **Elasticsearch** 8.x（默认端口 9200）
- **DashScope API Key** — [阿里云百炼控制台](https://bailian.console.aliyun.com/) 获取

## 快速启动

### 1. 环境准备

确保已安装并启动以下服务：

- **JDK 17+** & **Maven 3.9+**
- **Node.js 18+**（前端）
- **MySQL 8.0**
- **Redis 7.x**
- **Elasticsearch 8.x**（RAG 阶段需要，可延后配置）

### 2. 创建数据库

```sql
CREATE DATABASE health_assistant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 配置环境变量

**只需一个文件**：复制 `.env.template` 为 `.env`，填入实际值。

```bash
cp .env.template .env
```

编辑 `.env`：

```ini
# 必填
DB_PASSWORD=你的MySQL密码
DASHSCOPE_API_KEY=sk-你的百炼API密钥
JWT_SECRET=你的JWT密钥（可用 openssl rand -base64 64 生成）

# 可选（使用默认值可留空）
DB_USER=root
REDIS_HOST=localhost
REDIS_PORT=6379
ES_URIS=http://localhost:9200
```

> `.env` 已在 `.gitignore` 中，不会被提交到 Git。Spring Boot 启动时自动加载，Python 脚本也可复用同一份配置。

### 4. 启动后端

```bash
mvn spring-boot:run
```

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问：
- **前端页面**: `http://localhost:5173`
- **API 服务**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

### 5. 导入知识库（可选）

启动后知识库为空，可通过两种方式导入：

**方式一：爬虫抓取**

```bash
# 管理员登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 使用返回的 token 触发全量抓取（WHO + CDC + 丁香医生）
curl -X POST http://localhost:8080/api/v1/admin/knowledge/scrape \
  -H "Authorization: Bearer <your_token>"
```

**方式二：后台手动导入**

```bash
curl -X POST http://localhost:8080/api/v1/admin/knowledge/import/single \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"糖尿病饮食指南",
    "content":"糖尿病患者应控制碳水化合物摄入...",
    "documentType":"health_encyclopedia",
    "sourceName":"测试来源",
    "evidenceLevel":3
  }'
```

### 6. 开始对话

```bash
# 注册用户
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"User1234","email":"user@test.com"}'

# 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"User1234"}' \
  | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"//;s/"//')

# 创建会话
SESSION=$(curl -s -X POST http://localhost:8080/api/v1/chat/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"健康咨询","firstMessage":"你好"}' \
  | grep -o '"sessionId":"[^"]*"' | sed 's/"sessionId":"//;s/"//')

# 发送问题（SSE 流式返回）
curl -X POST http://localhost:8080/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"sessionId\":\"$SESSION\",\"content\":\"糖尿病血糖控制不好怎么办\"}"
```

## API 端点总览

### 认证模块 `/api/v1/auth`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/register` | 用户注册 |
| POST | `/login` | 用户登录 |
| POST | `/refresh` | 刷新 Token |
| POST | `/logout` | 退出登录 |

### 用户模块 `/api/v1/user`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/profile` | 获取个人信息 |
| PUT | `/profile` | 更新个人信息 |
| GET | `/health-record` | 获取健康档案 |
| PUT | `/health-record` | 更新健康档案 |

### 对话模块 `/api/v1/chat`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/sessions` | 创建会话 |
| GET | `/sessions` | 会话列表 |
| GET | `/sessions/{id}` | 会话详情 |
| DELETE | `/sessions/{id}` | 删除会话 |
| POST | `/messages` | 发送消息（SSE 流式） |
| GET | `/messages/{sessionId}` | 消息历史 |
| POST | `/clarify` | 提交澄清回答 |
| POST | `/feedback` | 提交反馈 |
| GET | `/suggested-questions` | 推荐问题 |

### 知识库模块 `/api/v1/knowledge`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/items` | 创建知识条目 |
| GET | `/items` | 知识条目列表 |
| GET | `/items/{docId}` | 知识条目详情 |
| DELETE | `/items/{docId}` | 删除知识条目 |
| POST | `/items/{docId}/index` | 索引单个条目 |
| POST | `/import/single` | 导入单篇文档 |
| POST | `/import/batch` | 批量导入 |
| POST | `/reindex` | 全部重建索引 |
| POST | `/scrape` | 触发爬虫抓取 |
| POST | `/scrape/preview` | 预览爬取结果 |
| GET | `/sync-tasks/{taskId}` | 同步任务状态 |

### 管理模块 `/api/v1/admin`（需管理员角色）
**仪表盘**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dashboard/stats` | 核心统计数据 |
| GET | `/dashboard/trends` | 7 日趋势 |
| GET | `/dashboard/source-distribution` | 知识来源分布 |

**用户管理**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users` | 用户列表（支持筛选） |
| GET | `/users/{id}` | 用户详情 |
| PUT | `/users/{id}/status` | 启用/禁用 |
| PUT | `/users/{id}/role` | 修改角色 |
| DELETE | `/users/{id}` | 软删除 |

**知识库管理**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/knowledge/items` | 知识条目列表 |
| GET | `/knowledge/items/{docId}` | 知识条目详情 |
| POST | `/knowledge/items` | 手动创建（自动索引） |
| DELETE | `/knowledge/items/{docId}` | 删除 |
| POST | `/knowledge/items/batch-delete` | 批量删除 |
| POST | `/knowledge/import/single` | 导入单篇 |
| POST | `/knowledge/import/batch` | 批量导入（异步） |
| POST | `/knowledge/scrape` | 全量爬虫抓取 |
| POST | `/knowledge/scrape/{source}` | 指定来源抓取 |
| POST | `/knowledge/reindex` | 全部重建索引 |
| POST | `/knowledge/reindex/{docId}` | 单个重建索引 |
| GET | `/knowledge/import-history` | 导入历史 |
| GET | `/knowledge/scrape-report` | 抓取报告 |

**安全 & 监控**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/sensitive-words` | 敏感词列表 |
| POST | `/sensitive-words` | 添加敏感词 |
| DELETE | `/sensitive-words/{id}` | 删除敏感词 |
| GET | `/operation-logs` | 操作日志 |
| GET | `/health` | 健康检查 |

## 知识来源 & 证据等级

| 来源 | 等级 | 说明 |
|------|------|------|
| WHO（世界卫生组织） | 5 | 最高级别，国际权威指南 |
| 中国疾控中心 | 4 | 国家级卫生机构，临床指南 |
| 丁香医生 | 2-3 | 商业健康媒体，药学审核（JS 渲染，Jsoup 抓取有限） |

## 数据流程

```
爬虫抓取 URL → Jsoup 清洗 HTML → 提取标题/日期/正文
  → KnowledgeService 创建 MySQL 记录
  → TextChunker 医学章节感知切分（200-500 token）
  → MedicalEntityExtractor 标注医学实体
  → EmbeddingPipeline 向量化（text-embedding-v3, 1024维）
  → Elasticsearch HNSW 索引
```

```
用户提问 → SensitiveWordService DFA 过滤
  → QueryRewriter LLM 多查询重写（口语→医学检索词）
  → VectorSearch + BM25 并行检索
  → RRF 融合 → Cross-encoder 重排序
  → 父文档上下文扩展（MySQL 回查完整章节）
  → DashScope qwen-max 流式生成 → SSE 返回
```

## 默认账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员（启动时自动创建） |

## 项目结构

```
health_assistant/
├── src/main/java/com/healthassistant/
│   ├── HealthAssistantApplication.java  # 入口（含 .env 加载）
│   ├── common/          # 公共模块（常量、异常、工具类）
│   ├── config/          # Spring 配置
│   ├── security/        # JWT 认证 & 权限
│   └── module/
│       ├── user/        # 用户模块
│       ├── chat/        # 对话模块（SSE 流式 + 澄清）
│       ├── knowledge/   # 知识库管理 + 爬虫
│       ├── rag/         # RAG 检索增强
│       └── admin/       # 管理后台
├── src/main/resources/
│   ├── application.yml          # 主配置（从环境变量读取）
│   ├── logback-spring.xml
│   ├── es/                      # ES 索引映射
│   └── prompts/                 # LLM 提示词
├── frontend/                    # Vue 3 前端
│   └── src/
│       ├── views/chat/          # 对话页面
│       ├── views/admin/         # 管理后台
│       ├── components/chat/     # 对话组件
│       ├── stores/              # Pinia 状态管理
│       └── api/                 # API 封装
├── scripts/                     # 工具脚本（数据导入等）
├── .env.template                # 环境变量模板（唯一配置入口）
├── .gitignore
├── pom.xml
└── README.md
```
