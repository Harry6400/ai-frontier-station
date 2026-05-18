# AI Frontier Station — Code Wiki

> AI前沿情报站完整代码百科，涵盖项目架构、模块职责、关键类与函数、依赖关系及运行方式。

---

## 目录

1. [项目概览](#1-项目概览)
2. [整体架构](#2-整体架构)
3. [后端服务（backend）](#3-后端服务backend)
   - 3.1 [技术栈与依赖](#31-技术栈与依赖)
   - 3.2 [应用配置](#32-应用配置)
   - 3.3 [包结构总览](#33-包结构总览)
   - 3.4 [通用层（common）](#34-通用层common)
   - 3.5 [配置层（config）](#35-配置层config)
   - 3.6 [安全层（security）](#36-安全层security)
   - 3.7 [实体层（entity）](#37-实体层entity)
   - 3.8 [数据访问层（mapper）](#38-数据访问层mapper)
   - 3.9 [数据传输对象（dto / request）](#39-数据传输对象dto--request)
   - 3.10 [视图对象（vo）](#310-视图对象vo)
   - 3.11 [服务层（service）](#311-服务层service)
   - 3.12 [控制器层（controller）](#312-控制器层controller)
   - 3.13 [工具类（util）](#313-工具类util)
4. [前台门户（frontend-portal）](#4-前台门户frontend-portal)
   - 4.1 [技术栈与依赖](#41-技术栈与依赖)
   - 4.2 [目录结构](#42-目录结构)
   - 4.3 [路由定义](#43-路由定义)
   - 4.4 [状态管理](#44-状态管理)
   - 4.5 [API 调用层](#45-api-调用层)
   - 4.6 [工具函数](#46-工具函数)
   - 4.7 [组件说明](#47-组件说明)
5. [后台管理（frontend-admin）](#5-后台管理frontend-admin)
   - 5.1 [技术栈与依赖](#51-技术栈与依赖)
   - 5.2 [目录结构](#52-目录结构)
   - 5.3 [路由与导航守卫](#53-路由与导航守卫)
   - 5.4 [状态管理](#54-状态管理)
   - 5.5 [API 调用层](#55-api-调用层)
   - 5.6 [组件与页面说明](#56-组件与页面说明)
6. [数据库设计（database）](#6-数据库设计database)
   - 6.1 [ER 关系概览](#61-er-关系概览)
   - 6.2 [表结构详述](#62-表结构详述)
7. [依赖关系图](#7-依赖关系图)
8. [项目运行方式](#8-项目运行方式)
9. [环境变量说明](#9-环境变量说明)
10. [API 接口总览](#10-api-接口总览)

---

## 1. 项目概览

**AI Frontier Station（AI前沿情报站）** 是一个面向 AI 开发者的前沿信息聚合与精选平台。项目采用前后端分离架构，包含三个独立子项目：

| 子项目 | 职责 | 端口 |
|--------|------|------|
| `backend` | Spring Boot 3 后端 API 服务 | 8080 |
| `frontend-portal` | Vue 3 前台门户（面向读者） | 5173 |
| `frontend-admin` | Vue 3 + Element Plus 后台管理端（面向运营） | 5174 |

核心数据流：

```
GitHub / arXiv / HuggingFace / 官方博客 / 社区来源
        ↓
后台人工确认或 API 查询导入
        ↓
AI 总结层（百炼 DashScope / MiMo，可选）
        ↓
ai_content + ai_content_external_ref + extra_json
        ↓
前台门户展示 & 后台运营管理
```

---

## 2. 整体架构

```
AI前沿情报站/
├── backend/                Spring Boot 3 后端 API 服务
│   └── src/main/java/com/harry/aifrontier/
│       ├── common/         通用返回结构与全局异常处理
│       ├── config/         Spring Security 与 MyBatis-Plus 配置
│       ├── controller/     REST 控制器（admin + portal + system）
│       ├── dto/request/    请求体 DTO
│       ├── entity/         MyBatis-Plus 实体类
│       ├── mapper/         MyBatis-Plus Mapper 接口
│       ├── security/       JWT 工具与认证过滤器
│       ├── service/        业务接口
│       ├── service/impl/   业务实现
│       ├── util/           加密与 Slug 工具
│       └── vo/             视图对象
├── frontend-portal/        Vue 3 前台门户
│   └── src/
│       ├── api/            Axios 封装与门户 API
│       ├── components/     通用组件
│       ├── router/         路由定义
│       ├── stores/         Pinia 状态管理
│       ├── styles/         全局样式
│       ├── utils/          工具函数
│       └── views/          页面视图
├── frontend-admin/         Vue 3 + Element Plus 后台管理端
│   └── src/
│       ├── api/            Axios 封装与管理 API
│       ├── router/         路由与导航守卫
│       ├── stores/         Pinia 状态管理
│       ├── styles/         后台样式
│       ├── utils/          工具函数
│       └── views/          管理页面
├── database/               MySQL 建表与初始化脚本
├── docs-learning/          中文阶段记录与架构文档
└── .env.example            环境变量参考模板
```

三个子项目相互独立，没有 monorepo 工作区管理工具。进入对应目录后再执行命令。

---

## 3. 后端服务（backend）

### 3.1 技术栈与依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | Web 框架与自动配置 |
| Spring Boot Starter Security | — | JWT 认证与接口保护 |
| Spring Boot Starter Validation | — | 请求参数校验（@Valid） |
| MyBatis-Plus | 3.5.7 | ORM 与分页插件 |
| MySQL Connector/J | — | MySQL 驱动 |
| JJWT | 0.12.6 | JWT Token 生成与解析 |
| Lombok | 1.18.38 | 编译期代码生成（@Data, @RequiredArgsConstructor 等） |

Java 版本：**17**

### 3.2 应用配置

配置文件：`backend/src/main/resources/application.yml`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/ai_frontier_station` | 数据库连接 |
| `mybatis-plus.configuration.map-underscore-to-camel-case` | true | 下划线转驼峰 |
| `app.jwt.secret` | 环境变量 `JWT_SECRET` | JWT 签名密钥 |
| `app.jwt.expiration` | 86400000（24h） | Token 有效期 |
| `app.crypto.master-key` | 环境变量 `API_MASTER_KEY` | API Key 加密主密钥 |
| `app.bailian.api-key` | 环境变量 `DASHSCOPE_API_KEY` | 百炼 API Key |
| `app.bailian.base-url` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 百炼 API 地址 |
| `app.bailian.model` | qwen-plus | 百炼模型 |
| `app.github.token` | 环境变量 `GITHUB_TOKEN` | GitHub Token |
| `app.mimo.api-key` | 环境变量 `MIMO_API_KEY` | MiMo API Key |
| `app.mimo.base-url` | `https://token-plan-cn.xiaomimimo.com/v1` | MiMo API 地址 |
| `app.mimo.model` | mimo-v2.5-pro | MiMo 模型 |

### 3.3 包结构总览

```
com.harry.aifrontier/
├── AiFrontierApplication.java    Spring Boot 启动类
├── common/
│   ├── api/ApiResponse.java      统一返回结构
│   ├── api/PageResult.java       分页返回结构
│   └── exception/GlobalExceptionHandler.java  全局异常处理
├── config/
│   ├── MybatisPlusConfig.java    分页插件配置
│   └── SecurityConfig.java       Spring Security 配置
├── controller/
│   ├── admin/                    后台管理接口（需认证）
│   ├── portal/                   前台门户接口（公开）
│   └── SystemController.java     系统健康检查
├── dto/request/                  请求体 DTO
├── entity/                       数据库实体
├── mapper/                       MyBatis-Plus Mapper
├── security/
│   ├── JwtAuthenticationFilter.java  JWT 认证过滤器
│   └── JwtUtil.java              JWT 工具类
├── service/                      业务接口
├── service/impl/                 业务实现
├── util/
│   ├── CryptoUtil.java           AES-256-GCM 加解密
│   └── SlugUtil.java             URL Slug 生成
└── vo/                           视图对象
```

### 3.4 通用层（common）

#### ApiResponse\<T\>

统一 API 返回结构，所有接口均通过此类包装响应。

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 状态码（200=成功） |
| `message` | String | 消息 |
| `data` | T | 数据载荷 |

| 方法 | 说明 |
|------|------|
| `success(T data)` | 成功返回（code=200, message="success"） |
| `success(String message, T data)` | 成功返回（自定义消息） |
| `fail(Integer code, String message)` | 失败返回 |

#### PageResult\<T\>

分页数据结构。

| 字段 | 类型 | 说明 |
|------|------|------|
| `total` | Long | 总记录数 |
| `pageNum` | Long | 当前页码 |
| `pageSize` | Long | 每页条数 |
| `records` | List\<T\> | 数据列表 |

| 方法 | 说明 |
|------|------|
| `of(total, pageNum, pageSize, records)` | 构造分页结果 |
| `empty(pageNum, pageSize)` | 空分页结果 |

#### GlobalExceptionHandler

全局异常处理器，使用 `@RestControllerAdvice` 捕获异常并转为 `ApiResponse`。

| 异常类型 | 返回码 | 处理逻辑 |
|----------|--------|----------|
| `MethodArgumentNotValidException` | 400 | 提取第一个字段校验错误消息 |
| `ConstraintViolationException` | 400 | 返回约束违反消息 |
| `IllegalArgumentException` | 400 | 返回非法参数消息 |
| `DuplicateKeyException` | 409 | 返回"数据已存在"提示 |
| `Exception` | 500 | 兜底处理 |

### 3.5 配置层（config）

#### SecurityConfig

Spring Security 配置类，定义认证授权规则：

- **CSRF**：禁用（前后端分离无状态架构）
- **Session**：`STATELESS`（无状态，使用 JWT）
- **公开接口**：`/api/v1/system/**`、`/api/v1/portal/**`、`/api/v1/auth/login`
- **受保护接口**：`/api/v1/admin/**` 需要认证
- **认证入口**：返回 401 JSON（"未登录或登录已过期"）
- **权限拒绝**：返回 403 JSON（"权限不足"）
- **过滤器链**：`JwtAuthenticationFilter` 插入在 `UsernamePasswordAuthenticationFilter` 之前
- **密码编码器**：`BCryptPasswordEncoder`

#### MybatisPlusConfig

注册 MyBatis-Plus 分页拦截器（`PaginationInnerInterceptor`，数据库类型 MySQL）。

### 3.6 安全层（security）

#### JwtUtil

JWT Token 生成与解析工具。

| 方法 | 签名 | 说明 |
|------|------|------|
| `generateToken` | `(String username, String role) → String` | 生成 JWT，包含 username（subject）和 role（claim） |
| `parseToken` | `(String token) → Claims` | 解析 Token 获取 Claims |
| `validateToken` | `(String token) → boolean` | 校验 Token 是否有效 |
| `getUsernameFromToken` | `(String token) → String` | 从 Token 提取用户名 |
| `getRoleFromToken` | `(String token) → String` | 从 Token 提取角色 |

密钥派生流程：`secret → SHA-256 → HMAC-SHA 密钥`。若 `JWT_SECRET` 未配置，启动时自动生成随机密钥并打印警告。

#### JwtAuthenticationFilter

继承 `OncePerRequestFilter`，从请求头 `Authorization: Bearer <token>` 提取 JWT，验证后将认证信息写入 `SecurityContextHolder`。

### 3.7 实体层（entity）

所有实体使用 Lombok `@Data` 和 MyBatis-Plus `@TableName` 注解，主键使用 `@TableId(type = IdType.AUTO)` 自增。

#### Content（核心内容表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题 |
| slug | String | URL 标识 |
| contentType | String | 内容类型（news/paper/project/company_update/practice） |
| summary | String | 摘要 |
| coverImage | String | 封面图地址 |
| categoryId | Long | 分类 ID（外键） |
| sourceId | Long | 来源 ID（外键） |
| sourceUrl | String | 原始链接 |
| authorName | String | 作者 |
| publishStatus | String | 发布状态（DRAFT/PUBLISHED/ARCHIVED） |
| featuredLevel | Integer | 精选级别 |
| viewCount | Integer | 浏览量 |
| readingTime | Integer | 预计阅读分钟 |
| publishedAt | LocalDateTime | 发布时间 |
| bodyMarkdown | String | 正文 Markdown |
| extraJson | String | 扩展 JSON |
| createdAt / updatedAt | LocalDateTime | 时间戳 |

#### ContentExternalRef（外部引用表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| contentId | Long | 内容 ID（外键） |
| refType | String | 引用类型（github_repo/arxiv_paper/official_post 等） |
| externalId | String | 外部系统 ID |
| externalUrl | String | 外部链接 |
| rawPayloadJson | String | 原始抓取数据快照 |
| syncedAt | LocalDateTime | 最近同步时间 |
| stars / forks / watchers | Integer | GitHub 统计字段 |
| language | String | GitHub 主要语言 |
| lastSyncedAt | LocalDateTime | 最近同步时间 |
| syncStatus | String | 同步状态（pending/synced/error） |
| syncError | String | 同步错误信息 |

#### Category（分类表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 分类名称 |
| slug | String | 分类标识 |
| description | String | 分类说明 |
| sortOrder | Integer | 排序值 |
| isEnabled | Integer | 是否启用 |

#### Tag（标签表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 标签名称 |
| slug | String | 标签标识 |
| color | String | 展示色 |
| description | String | 标签说明 |

#### Source（来源表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 来源名称 |
| slug | String | 来源标识 |
| sourceType | String | 来源类型（github/paper/official_blog/community/leaderboard/manual） |
| websiteUrl | String | 官网地址 |
| description | String | 来源说明 |
| isEnabled | Integer | 是否启用 |

#### ContentTag（内容标签关联表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| contentId | Long | 内容 ID（外键） |
| tagId | Long | 标签 ID（外键） |

#### AdminUser（管理员用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| username | String | 用户名（唯一） |
| passwordHash | String | BCrypt 密码哈希 |
| displayName | String | 显示名称 |
| role | String | 角色（admin） |
| isEnabled | Integer | 是否启用 |
| lastLoginAt | LocalDateTime | 最后登录时间 |

#### ApiCredential（API 密钥加密存储表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| provider | String | Provider 标识（bailian/github/mimo，唯一） |
| encryptedKey | String | AES 加密后的密钥 |
| iv | String | AES-GCM 初始化向量（Base64） |
| keySuffix | String | 密钥明文后4位（前端展示） |
| remark | String | 备注 |
| isEnabled | Integer | 是否启用 |

### 3.8 数据访问层（mapper）

所有 Mapper 继承 `BaseMapper<T>`（MyBatis-Plus），无需手写 XML。

| Mapper | 实体 | 说明 |
|--------|------|------|
| `ContentMapper` | Content | 内容数据访问 |
| `ContentExternalRefMapper` | ContentExternalRef | 外部引用数据访问 |
| `ContentTagMapper` | ContentTag | 内容标签关联数据访问 |
| `CategoryMapper` | Category | 分类数据访问 |
| `TagMapper` | Tag | 标签数据访问 |
| `SourceMapper` | Source | 来源数据访问 |
| `AdminUserMapper` | AdminUser | 管理员数据访问 |
| `ApiCredentialMapper` | ApiCredential | API 密钥数据访问 |

### 3.9 数据传输对象（dto / request）

| DTO 类 | 字段 | 用途 |
|--------|------|------|
| `LoginRequest` | username, password | 管理员登录 |
| `LoginResponse` | token, username, displayName, role | 登录响应 |
| `ContentSaveRequest` | title, contentType, summary, coverImage, categoryId, sourceId, sourceUrl, authorName, publishStatus, featuredLevel, readingTime, publishedAt, bodyMarkdown, extraJson, tagIds, externalRefs | 内容创建/更新 |
| `ContentQueryRequest` | keyword, contentType, categoryId, sourceId, publishStatus, pageNum, pageSize | 后台内容查询 |
| `ContentStatusUpdateRequest` | publishStatus | 内容状态更新 |
| `PortalContentQueryRequest` | keyword, contentType, categoryId, tagId, sort, pageNum, pageSize | 前台内容查询 |
| `ContentExternalRefSaveRequest` | refType, externalId, externalUrl, rawPayloadJson | 外部引用创建/更新 |
| `CategorySaveRequest` | name, description, sortOrder, isEnabled | 分类创建/更新 |
| `TagSaveRequest` | name, color, description | 标签创建/更新 |
| `SourceSaveRequest` | name, sourceType, websiteUrl, description, isEnabled | 来源创建/更新 |
| `AiSourceSummaryRequest` | sourceUrl, provider | AI 来源总结请求 |
| `AiSourceImportRequest` | title, sourceUrl, provider, categoryId, sourceId, tagIds | AI 来源导入 |
| `GitHubRepoImportRequest` | fullName, categoryId, sourceId, tagIds | GitHub 仓库导入 |
| `ArxivPaperImportRequest` | arxivId, categoryId, sourceId, tagIds | arXiv 论文导入 |
| `HuggingFacePaperImportRequest` | paperId, categoryId, sourceId, tagIds | HuggingFace 论文导入 |
| `ApiCredentialSaveRequest` | apiKey, remark | API 密钥保存 |

### 3.10 视图对象（vo）

| VO 类 | 主要字段 | 用途 |
|--------|----------|------|
| `HomeOverviewVO` | totalContents, totalCategories, totalTags, totalSources, recentContents, featuredContents | 前台首页概览 |
| `ContentAdminListItemVO` | id, title, slug, contentType, summary, categoryName, sourceName, publishStatus, featuredLevel, viewCount, publishedAt, createdAt | 后台内容列表项 |
| `ContentDetailVO` | 全部内容字段 + categoryName, sourceName, tags, externalRefs | 内容详情 |
| `ContentExternalRefVO` | id, refType, externalId, externalUrl, stars, forks, watchers, language, syncStatus | 外部引用视图 |
| `ContentOptionsVO` | categories(List\<OptionVO\>), sources(List\<OptionVO\>), tags(List\<OptionVO\>) | 内容表单下拉选项 |
| `OptionVO` | value, label | 通用选项 |
| `CategoryVO` | id, name, slug, description, sortOrder, isEnabled | 分类视图 |
| `TagVO` | id, name, slug, color, description | 标签视图 |
| `SourceVO` | id, name, slug, sourceType, websiteUrl, description, isEnabled | 来源视图 |
| `DashboardOverviewVO` | totals(TotalsVO), recentContents, recentExternalRefs, stats | 后台仪表盘概览 |
| `AdminUserVO` | username, displayName, role | 管理员信息 |
| `AiSourceSummaryVO` | title, summary, bodyMarkdown, suggestedTags, importanceScore | AI 总结结果 |
| `ApiSettingsStatusVO` | bailian, github, mimo（各含 ApiCredentialStatusVO） | API 设置状态 |
| `ApiCredentialStatusVO` | enabled, keySuffix, remark | 单个 Provider 凭据状态 |
| `GitHubRepoCandidateVO` | fullName, description, stars, forks, language, url, updatedAt | GitHub 仓库候选 |
| `GitHubSyncResultVO` | externalRefId, success, message, stars, forks, language | GitHub 同步结果 |
| `ArxivPaperCandidateVO` | arxivId, title, authors, summary, published, link | arXiv 论文候选 |
| `HuggingFacePaperCandidateVO` | paperId, title, authors, summary, publishedAt, url | HuggingFace 论文候选 |

### 3.11 服务层（service）

#### ContentService（核心内容服务）

| 方法 | 说明 |
|------|------|
| `adminPage(ContentQueryRequest)` | 后台分页查询内容 |
| `adminDetail(Long id)` | 后台内容详情 |
| `create(ContentSaveRequest)` | 创建内容 |
| `update(Long id, ContentSaveRequest)` | 更新内容 |
| `delete(Long id)` | 删除内容 |
| `updateStatus(Long id, String publishStatus)` | 更新发布状态 |
| `createExternalRef(Long contentId, ContentExternalRefSaveRequest)` | 创建外部引用 |
| `updateExternalRef(Long contentId, Long refId, ContentExternalRefSaveRequest)` | 更新外部引用 |
| `deleteExternalRef(Long contentId, Long refId)` | 删除外部引用 |
| `importGitHubRepo(GitHubRepoImportRequest)` | 导入 GitHub 仓库 |
| `importAiSource(AiSourceImportRequest)` | 导入 AI 来源整理内容 |
| `importArxivPaper(ArxivPaperImportRequest)` | 导入 arXiv 论文 |
| `importHuggingFacePaper(HuggingFacePaperImportRequest)` | 导入 HuggingFace 论文 |
| `options()` | 获取内容表单选项 |
| `portalPage(PortalContentQueryRequest)` | 前台分页查询 |
| `portalDetail(Long id)` | 前台内容详情 |
| `portalHome()` | 前台首页概览 |

#### AiSummaryService（AI 总结服务）

| 方法 | 说明 |
|------|------|
| `summarizeSource(AiSourceSummaryRequest)` | 调用百炼/MiMo 生成来源导读 |

#### GitHubService（GitHub API 服务）

| 方法 | 说明 |
|------|------|
| `findRepo(String fullName)` | 查询单个仓库信息 |
| `searchRepos(String keyword, String sort, Long pageNum, Long pageSize)` | 搜索仓库 |

#### GitHubSyncService（GitHub 同步服务）

| 方法 | 说明 |
|------|------|
| `syncRepo(Long externalRefId)` | 同步单个仓库热度数据 |
| `syncAllGitHubRepos()` | 批量同步所有 GitHub 仓库 |

#### ArxivService（arXiv 论文服务）

| 方法 | 说明 |
|------|------|
| `searchPapers(String query, int maxResults, int start)` | 搜索 arXiv 论文 |
| `findPaper(String arxivId)` | 查询单篇论文 |

#### HuggingFaceService（HuggingFace 论文服务）

| 方法 | 说明 |
|------|------|
| `getDailyPapers()` | 获取每日热门论文 |
| `findPaper(String paperId)` | 查询单篇论文 |

#### ApiCredentialService（API 密钥服务）

| 常量/方法 | 说明 |
|-----------|------|
| `PROVIDER_BAILIAN` | "bailian" |
| `PROVIDER_GITHUB` | "github" |
| `PROVIDER_MIMO` | "mimo" |
| `status()` | 获取所有 Provider 状态 |
| `save(String provider, ApiCredentialSaveRequest)` | 保存加密密钥 |
| `clear(String provider)` | 清除密钥 |
| `resolveBailianApiKey(String fallback)` | 解析百炼 Key（优先数据库，回退环境变量） |
| `resolveGitHubToken(String fallback)` | 解析 GitHub Token |
| `resolveMimoApiKey(String fallback)` | 解析 MiMo Key |

#### CategoryService / TagService / SourceService

均提供标准 CRUD：`listAll()`、`create()`、`update()`、`delete()`。

#### DashboardService

| 方法 | 说明 |
|------|------|
| `overview()` | 获取仪表盘概览数据 |

#### AuthService

| 方法 | 说明 |
|------|------|
| `login(LoginRequest)` | 管理员登录（返回 JWT） |
| `getCurrentUser(String username)` | 获取当前用户信息 |

### 3.12 控制器层（controller）

#### 后台管理接口（需 JWT 认证）

| 控制器 | 路径前缀 | 主要端点 |
|--------|----------|----------|
| `AuthController` | `/api/v1/auth` | POST `/login`, GET `/me`, POST `/logout` |
| `ContentAdminController` | `/api/v1/admin/contents` | GET 分页, GET `/{id}`, POST 创建, PUT `/{id}`, DELETE `/{id}`, PUT `/{id}/status`, 外部引用 CRUD |
| `CategoryAdminController` | `/api/v1/admin/categories` | GET 列表, POST 创建, PUT `/{id}`, DELETE `/{id}` |
| `TagAdminController` | `/api/v1/admin/tags` | GET 列表, POST 创建, PUT `/{id}`, DELETE `/{id}` |
| `SourceAdminController` | `/api/v1/admin/sources` | GET 列表, POST 创建, PUT `/{id}`, DELETE `/{id}` |
| `DashboardAdminController` | `/api/v1/admin/dashboard` | GET `/overview` |
| `AiAdminController` | `/api/v1/admin/ai` | POST `/source-summary` |
| `GitHubAdminController` | `/api/v1/admin/github` | GET `/repo`, GET `/search` |
| `GitHubSyncController` | `/api/v1/admin/github-sync` | POST `/repo/{id}`, POST `/all` |
| `ArxivAdminController` | `/api/v1/admin/arxiv` | GET `/search`, GET `/paper` |
| `HuggingFaceAdminController` | `/api/v1/admin/huggingface` | GET `/papers`, GET `/papers/{paperId}` |
| `ImportAdminController` | `/api/v1/admin/import` | POST `/github-repo`, POST `/ai-source`, POST `/arxiv-paper`, POST `/huggingface-paper` |
| `ApiSettingsAdminController` | `/api/v1/admin/api-settings` | GET `/status`, PUT/DELETE `/bailian`, PUT/DELETE `/github`, PUT/DELETE `/mimo` |

#### 前台门户接口（公开访问）

| 控制器 | 路径前缀 | 端点 |
|--------|----------|------|
| `PortalContentController` | `/api/v1/portal` | GET `/home`, GET `/contents`, GET `/contents/{id}` |

#### 系统接口（公开访问）

| 控制器 | 路径前缀 | 端点 |
|--------|----------|------|
| `SystemController` | `/api/v1/system` | GET `/health`, GET `/profile` |

### 3.13 工具类（util）

#### CryptoUtil

AES-256-GCM 加解密工具，用于 API Key 安全存储。

| 方法 | 签名 | 说明 |
|------|------|------|
| `validateOrGenerateMasterKey` | `(String masterKey) → String` | 校验主密钥，为空则生成随机密钥并警告 |
| `generateRandomKey` | `() → String` | 生成 32 字节随机 Base64 密钥 |
| `deriveKey` | `(String masterKey) → byte[]` | SHA-256 派生 32 字节 AES 密钥 |
| `encrypt` | `(String plaintext, String masterKey) → EncryptedResult` | AES-256-GCM 加密，返回密文和 IV（均为 Base64） |
| `decrypt` | `(String encryptedBase64, String ivBase64, String masterKey) → String` | AES-256-GCM 解密 |
| `mask` | `(String value) → String` | 掩码显示（仅保留后4位） |

内部类 `EncryptedResult`（record）：`encrypted`（密文 Base64）、`iv`（IV Base64）。

#### SlugUtil

URL Slug 生成工具，将中文/特殊字符转为 URL 安全标识。

| 方法 | 签名 | 说明 |
|------|------|------|
| `resolveSlug` | `(String raw, String fallbackText, String prefix) → String` | 优先使用 raw，其次 fallbackText，最后生成 `prefix-yyyyMMddHHmmss` 格式 |

---

## 4. 前台门户（frontend-portal）

### 4.1 技术栈与依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Vue | ^3.5.13 | 前端框架 |
| Vue Router | ^4.5.0 | 路由管理 |
| Pinia | ^3.0.3 | 状态管理 |
| Axios | ^1.8.4 | HTTP 客户端 |
| marked | ^18.0.2 | Markdown 解析 |
| DOMPurify | ^3.4.1 | HTML 清洗（XSS 防护） |
| Vite | ^6.2.0 | 构建工具 |

### 4.2 目录结构

```
frontend-portal/src/
├── api/
│   ├── http.js          Axios 实例（baseURL: /api/v1, timeout: 10s）
│   └── portal.js        门户 API（home, contents, detail）
├── components/
│   ├── ContentCard.vue   内容卡片组件
│   └── PortalTopbar.vue  顶部导航栏
├── router/
│   └── index.js          路由定义
├── stores/
│   ├── usePortalStore.js 门户全局状态
│   └── useThemeStore.js  主题切换状态
├── styles/
│   └── global.css        全局样式（含浅色/深色主题变量）
├── utils/
│   ├── content.js        内容类型元数据与日期格式化
│   └── safeMarkdown.js   Markdown 安全渲染
├── views/
│   ├── HomeView.vue      首页
│   ├── ContentListView.vue 内容发现页
│   ├── ContentDetailView.vue 内容详情页
│   └── AboutView.vue     关于页
├── App.vue               根组件（仅 <router-view />）
└── main.js               入口文件
```

### 4.3 路由定义

| 路径 | 名称 | 组件 | 说明 |
|------|------|------|------|
| `/` | home | HomeView | 首页概览 |
| `/contents` | content-list | ContentListView | 内容发现页 |
| `/contents/:id` | content-detail | ContentDetailView | 内容详情页 |
| `/about` | about | AboutView | 关于页 |

### 4.4 状态管理

#### usePortalStore

| 字段 | 类型 | 说明 |
|------|------|------|
| projectName | String | 项目名称 |
| positioning | String | 项目定位描述 |
| contentTypes | String[] | 内容类型列表 |

#### useThemeStore

| 字段/方法 | 说明 |
|-----------|------|
| `theme` | 当前主题（'light' / 'dark'） |
| `initialized` | 是否已初始化 |
| `followingSystem` | 是否跟随系统主题 |
| `isDark`（getter） | 是否深色模式 |
| `modeLabel`（getter） | 当前模式中文标签 |
| `initTheme()` | 初始化主题（读取 localStorage + 系统偏好） |
| `setTheme(theme)` | 设置主题并持久化 |
| `toggleTheme()` | 切换主题 |

主题持久化键：`ai-frontier-portal-theme`，通过 `document.documentElement.dataset.theme` 应用。

### 4.5 API 调用层

#### http.js

Axios 实例配置：
- `baseURL`: `/api/v1`
- `timeout`: 10000ms
- 响应拦截器：自动解包 `response.data`，错误时提取 `message`

#### portal.js

| 函数 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getPortalHome()` | GET | `/portal/home` | 获取首页概览 |
| `getPortalContents(params)` | GET | `/portal/contents` | 分页查询内容 |
| `getPortalContentDetail(id)` | GET | `/portal/contents/{id}` | 获取内容详情 |

### 4.6 工具函数

#### safeMarkdown.js

Markdown 安全渲染工具，防止 XSS 攻击。

| 函数 | 说明 |
|------|------|
| `sanitizeHtml(html)` | DOMPurify 清洗 HTML，白名单标签：h1-h4, p, br, strong, em, code, pre, ul, ol, li, blockquote, a, table 系列 |
| `renderSafeMarkdown(markdown)` | 完整流程：marked 解析 → DOMPurify 清洗 → 链接加固（target=_blank, rel=noopener noreferrer） |

#### content.js

| 函数 | 说明 |
|------|------|
| `getContentTypeMeta(type)` | 获取内容类型元数据（中文标签 + 英文副标题） |
| `getSourceTypeMeta(type)` | 获取来源类型元数据 |
| `getExternalRefMeta(type)` | 获取外部引用类型元数据 |
| `formatDate(value)` | 格式化日期（yyyy/MM/dd） |
| `formatDateTime(value)` | 格式化日期时间（yyyy/MM/dd HH:mm） |
| `parseExtraJson(raw)` | 解析 extraJson 为可展示的键值对数组 |

内容类型映射：`news→AI资讯`、`paper→论文速递`、`project→热门项目`、`company_update→公司动态`、`practice→技术实践`

### 4.7 组件说明

| 组件 | 说明 |
|------|------|
| `ContentCard.vue` | 内容卡片，展示标题、摘要、发布时间、类型标签，点击跳转详情 |
| `PortalTopbar.vue` | 顶部导航栏，包含项目名称、导航链接、主题切换按钮 |

---

## 5. 后台管理（frontend-admin）

### 5.1 技术栈与依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Vue | ^3.5.13 | 前端框架 |
| Vue Router | ^4.5.0 | 路由管理 |
| Pinia | ^3.0.3 | 状态管理 |
| Element Plus | ^2.9.7 | UI 组件库 |
| @element-plus/icons-vue | ^2.3.1 | Element Plus 图标 |
| Axios | ^1.8.4 | HTTP 客户端 |
| DOMPurify | ^3.4.1 | HTML 清洗 |
| Vite | ^6.2.0 | 构建工具 |

### 5.2 目录结构

```
frontend-admin/src/
├── api/
│   ├── http.js          Axios 实例（含 JWT 拦截器）
│   ├── auth.js          认证 API
│   └── admin.js         管理功能 API
├── router/
│   └── index.js          路由与导航守卫
├── stores/
│   ├── useAuthStore.js   认证状态
│   ├── useAdminStore.js  管理后台状态
│   └── useAdminThemeStore.js 主题状态
├── styles/
│   └── admin.css         后台样式
├── utils/
│   └── safeMarkdown.js   Markdown 安全渲染
├── views/
│   ├── LoginView.vue         登录页
│   ├── DashboardView.vue     仪表盘
│   ├── ContentManageView.vue 内容管理
│   ├── CategoryManageView.vue 分类管理
│   ├── TagManageView.vue     标签管理
│   ├── SourceManageView.vue  来源管理
│   └── ApiSettingsView.vue   API 设置
├── App.vue               根组件（侧边栏 + 顶栏 + 路由出口）
└── main.js               入口文件（按需引入 Element Plus 组件）
```

### 5.3 路由与导航守卫

| 路径 | 名称 | 组件 | 说明 |
|------|------|------|------|
| `/login` | login | LoginView | 登录页 |
| `/` | dashboard | DashboardView | 仪表盘 |
| `/contents` | contents | ContentManageView | 内容管理 |
| `/categories` | categories | CategoryManageView | 分类管理 |
| `/tags` | tags | TagManageView | 标签管理 |
| `/sources` | sources | SourceManageView | 来源管理 |
| `/api-settings` | api-settings | ApiSettingsView | API 设置 |

**导航守卫**：
- 未登录（`localStorage` 无 `admin_token`）→ 重定向到 `/login`
- 已登录访问 `/login` → 重定向到 `/`

### 5.4 状态管理

#### useAuthStore

Composition API 风格的认证 Store。

| 字段/方法 | 说明 |
|-----------|------|
| `token` | JWT Token（ref，持久化到 localStorage） |
| `user` | 当前用户信息 |
| `isLoggedIn`（computed） | 是否已登录 |
| `username`（computed） | 用户名 |
| `displayName`（computed） | 显示名称 |
| `login(username, password)` | 调用登录 API 并保存 Token |
| `fetchCurrentUser()` | 获取当前用户信息 |
| `logout()` | 清除 Token 和用户信息 |

#### useAdminStore

| 字段 | 说明 |
|------|------|
| `dashboardStats` | 仪表盘统计项列表 |

#### useAdminThemeStore

与前台 `useThemeStore` 结构相同，持久化键为 `ai-frontier-admin-theme`，通过 `document.documentElement.dataset.adminTheme` 应用。

### 5.5 API 调用层

#### http.js

Axios 实例配置：
- `baseURL`: `/api/v1`
- `timeout`: 10000ms
- **请求拦截器**：自动附加 `Authorization: Bearer <token>` 头
- **响应拦截器**：自动解包 `response.data`；401 时清除 Token 并跳转登录页

#### auth.js

| 函数 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `login(payload)` | POST | `/auth/login` | 管理员登录 |
| `getCurrentUser()` | GET | `/auth/me` | 获取当前用户 |
| `logout()` | POST | `/auth/logout` | 登出 |

#### admin.js

| 函数 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `fetchDashboardSnapshot()` | GET | `/admin/dashboard/overview` | 仪表盘数据 |
| `getApiSettingsStatus()` | GET | `/admin/api-settings/status` | API 设置状态 |
| `saveApiCredential(provider, payload)` | PUT | `/admin/api-settings/{provider}` | 保存 API 密钥 |
| `clearApiCredential(provider)` | DELETE | `/admin/api-settings/{provider}` | 清除 API 密钥 |
| `getCategories()` | GET | `/admin/categories` | 分类列表 |
| `createCategory(payload)` | POST | `/admin/categories` | 创建分类 |
| `updateCategory(id, payload)` | PUT | `/admin/categories/{id}` | 更新分类 |
| `removeCategory(id)` | DELETE | `/admin/categories/{id}` | 删除分类 |
| `getTags()` | GET | `/admin/tags` | 标签列表 |
| `createTag(payload)` | POST | `/admin/tags` | 创建标签 |
| `updateTag(id, payload)` | PUT | `/admin/tags/{id}` | 更新标签 |
| `removeTag(id)` | DELETE | `/admin/tags/{id}` | 删除标签 |
| `getSources()` | GET | `/admin/sources` | 来源列表 |
| `createSource(payload)` | POST | `/admin/sources` | 创建来源 |
| `updateSource(id, payload)` | PUT | `/admin/sources/{id}` | 更新来源 |
| `removeSource(id)` | DELETE | `/admin/sources/{id}` | 删除来源 |
| `getContentOptions()` | GET | `/admin/contents/options` | 内容表单选项 |
| `getContents(params)` | GET | `/admin/contents` | 内容列表 |
| `getContentDetail(id)` | GET | `/admin/contents/{id}` | 内容详情 |
| `createContent(payload)` | POST | `/admin/contents` | 创建内容 |
| `updateContent(id, payload)` | PUT | `/admin/contents/{id}` | 更新内容 |
| `updateContentStatus(id, publishStatus)` | PUT | `/admin/contents/{id}/status` | 更新状态 |
| `removeContent(id)` | DELETE | `/admin/contents/{id}` | 删除内容 |
| `importGithubRepo(payload)` | POST | `/admin/import/github-repo` | 导入 GitHub 仓库 |
| `queryGithubRepo(fullName)` | GET | `/admin/github/repo` | 查询 GitHub 仓库 |
| `searchGithubRepos(params)` | GET | `/admin/github/search` | 搜索 GitHub 仓库 |
| `summarizeAiSource(payload)` | POST | `/admin/ai/source-summary` | AI 来源总结 |
| `importAiSource(payload)` | POST | `/admin/import/ai-source` | 导入 AI 来源 |
| `searchArxivPapers(params)` | GET | `/admin/arxiv/search` | 搜索 arXiv 论文 |
| `findArxivPaper(arxivId)` | GET | `/admin/arxiv/paper` | 查询 arXiv 论文 |
| `importArxivPaper(payload)` | POST | `/admin/import/arxiv-paper` | 导入 arXiv 论文 |
| `getHuggingFaceDailyPapers()` | GET | `/admin/huggingface/papers` | 获取 HF 每日论文 |
| `importHuggingFacePaper(payload)` | POST | `/admin/import/huggingface-paper` | 导入 HF 论文 |
| `createContentExternalRef(contentId, payload)` | POST | `/admin/contents/{id}/external-refs` | 创建外部引用 |
| `updateContentExternalRef(contentId, refId, payload)` | PUT | `/admin/contents/{id}/external-refs/{refId}` | 更新外部引用 |
| `removeContentExternalRef(contentId, refId)` | DELETE | `/admin/contents/{id}/external-refs/{refId}` | 删除外部引用 |
| `syncGitHubRepo(refId)` | POST | `/admin/github-sync/repo/{refId}` | 同步 GitHub 仓库 |
| `syncAllGitHubRepos()` | POST | `/admin/github-sync/all` | 批量同步 |

### 5.6 组件与页面说明

#### App.vue（根组件）

布局结构：
- 登录页：全屏 `<router-view />`
- 其他页面：左侧边栏（项目名 + 导航 + 设计说明） + 右侧主区域（顶栏 + `<router-view />`）

顶栏包含：页面标题区、用户信息与退出按钮、主题切换、前台门户入口链接。

#### 页面组件

| 页面 | 说明 |
|------|------|
| `LoginView.vue` | 管理员登录表单（用户名 + 密码） |
| `DashboardView.vue` | 仪表盘，展示内容/分类/标签/来源统计数据 |
| `ContentManageView.vue` | 内容管理，支持 CRUD、状态切换、GitHub/arXiv/HuggingFace/AI 来源导入、外部引用管理 |
| `CategoryManageView.vue` | 分类管理，支持 CRUD |
| `TagManageView.vue` | 标签管理，支持 CRUD |
| `SourceManageView.vue` | 来源管理，支持 CRUD |
| `ApiSettingsView.vue` | API 密钥配置，支持百炼/GitHub/MiMo 密钥的保存、启用、清除 |

#### Element Plus 按需引入

`main.js` 中按需注册以下组件：ElAlert, ElButton, ElDatePicker, ElDialog, ElForm/ElFormItem, ElInput, ElInputNumber, ElPagination, ElRadio/ElRadioGroup, ElSelect/ElOption, ElTable/ElTableColumn, ElTag。同时按需引入对应 CSS。

#### 构建优化

`vite.config.js` 中配置 `manualChunks` 分包策略：
- `element-plus` → 独立 chunk
- `vue` / `vue-router` / `pinia` → `vue-vendor` chunk
- `axios` → `admin-vendor` chunk
- 其他 `node_modules` → `vendor` chunk

---

## 6. 数据库设计（database）

### 6.1 ER 关系概览

```
ai_category  1──N  ai_content  N──1  ai_source
                     │
                     ├── N──M ── ai_tag  (通过 ai_content_tag)
                     │
                     └── 1──N  ai_content_external_ref

ai_admin_user          独立表（管理员认证）
ai_api_credential      独立表（API 密钥加密存储）
```

### 6.2 表结构详述

#### ai_category（内容分类表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NOT NULL | 分类名称 |
| slug | VARCHAR(120) | NOT NULL, UNIQUE | 分类标识 |
| description | VARCHAR(255) | | 分类说明 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 排序值 |
| is_enabled | TINYINT | NOT NULL, DEFAULT 1 | 是否启用 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL, AUTO UPDATE | 更新时间 |

#### ai_tag（内容标签表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NOT NULL | 标签名称 |
| slug | VARCHAR(120) | NOT NULL, UNIQUE | 标签标识 |
| color | VARCHAR(30) | | 展示色 |
| description | VARCHAR(255) | | 标签说明 |
| created_at / updated_at | DATETIME | | 时间戳 |

#### ai_source（内容来源表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(120) | NOT NULL | 来源名称 |
| slug | VARCHAR(120) | NOT NULL, UNIQUE | 来源标识 |
| source_type | VARCHAR(50) | NOT NULL | 来源类型 |
| website_url | VARCHAR(255) | | 官网地址 |
| description | VARCHAR(255) | | 来源说明 |
| is_enabled | TINYINT | NOT NULL, DEFAULT 1 | 是否启用 |
| created_at / updated_at | DATETIME | | 时间戳 |

#### ai_content（核心内容表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| title | VARCHAR(200) | NOT NULL | 标题 |
| slug | VARCHAR(220) | NOT NULL, UNIQUE | 内容标识 |
| content_type | VARCHAR(50) | NOT NULL, INDEX | 内容类型 |
| summary | VARCHAR(500) | | 摘要 |
| cover_image | VARCHAR(255) | | 封面图 |
| category_id | BIGINT | NOT NULL, FK → ai_category, INDEX | 分类 |
| source_id | BIGINT | FK → ai_source, INDEX | 来源 |
| source_url | VARCHAR(500) | | 原始链接 |
| author_name | VARCHAR(100) | | 作者 |
| publish_status | VARCHAR(30) | NOT NULL, DEFAULT 'DRAFT', INDEX | 发布状态 |
| featured_level | INT | NOT NULL, DEFAULT 0 | 精选级别 |
| view_count | INT | NOT NULL, DEFAULT 0 | 浏览量 |
| reading_time | INT | | 阅读分钟 |
| published_at | DATETIME | INDEX | 发布时间 |
| body_markdown | LONGTEXT | NOT NULL | 正文 Markdown |
| extra_json | JSON | | 扩展信息 |
| created_at / updated_at | DATETIME | | 时间戳 |

#### ai_content_tag（内容标签关联表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| content_id | BIGINT | NOT NULL, FK → ai_content, UNIQUE(content_id, tag_id) | 内容 ID |
| tag_id | BIGINT | NOT NULL, FK → ai_tag, INDEX | 标签 ID |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### ai_content_external_ref（外部引用表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| content_id | BIGINT | NOT NULL, FK → ai_content, INDEX | 内容 ID |
| ref_type | VARCHAR(50) | NOT NULL, INDEX | 引用类型 |
| external_id | VARCHAR(120) | | 外部系统 ID |
| external_url | VARCHAR(500) | | 外部链接 |
| raw_payload_json | JSON | | 原始数据快照 |
| synced_at | DATETIME | | 同步时间 |
| stars | INT | | GitHub Star 数 |
| forks | INT | | GitHub Fork 数 |
| watchers | INT | | GitHub Watcher 数 |
| language | VARCHAR(50) | | GitHub 主要语言 |
| last_synced_at | DATETIME | | 最近同步时间 |
| sync_status | VARCHAR(20) | DEFAULT 'pending' | 同步状态 |
| sync_error | VARCHAR(255) | | 同步错误 |
| created_at / updated_at | DATETIME | | 时间戳 |

#### ai_api_credential（API 密钥加密存储表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| provider | VARCHAR(50) | NOT NULL, UNIQUE | Provider 标识 |
| encrypted_key | TEXT | NOT NULL | AES 加密后的密钥 |
| iv | VARCHAR(64) | NOT NULL | AES-GCM IV（Base64） |
| key_suffix | VARCHAR(10) | NOT NULL | 密钥后4位（展示用） |
| remark | VARCHAR(255) | | 备注 |
| is_enabled | TINYINT | NOT NULL, DEFAULT 1 | 是否启用 |
| created_at / updated_at | DATETIME | | 时间戳 |

#### ai_admin_user（管理员用户表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt 密码哈希 |
| display_name | VARCHAR(100) | | 显示名称 |
| role | VARCHAR(50) | NOT NULL, DEFAULT 'admin' | 角色 |
| is_enabled | TINYINT | NOT NULL, DEFAULT 1 | 是否启用 |
| last_login_at | DATETIME | | 最后登录时间 |
| created_at / updated_at | DATETIME | | 时间戳 |

---

## 7. 依赖关系图

### 后端模块依赖

```
Controller ──→ Service(Interface) ──→ ServiceImpl ──→ Mapper
    │                │                                      │
    │                │                                      ↓
    ├── DTO/Request  ├── VO                    MyBatis-Plus BaseMapper
    │                │                                      │
    │                ├── CryptoUtil                          ↓
    │                ├── SlugUtil                       MySQL Database
    │                ├── JwtUtil
    │                └── ApiCredentialService
    │
    └── ApiResponse / PageResult
```

### 前端依赖关系

```
Views ──→ API Layer ──→ http.js (Axios) ──→ Vite Proxy ──→ Backend
  │
  ├── Stores (Pinia)
  ├── Components
  └── Utils (safeMarkdown, content)
```

### 子项目间通信

```
frontend-portal  ──Vite Proxy(/api)──→  backend(:8080)
frontend-admin   ──Vite Proxy(/api/v1)──→  backend(:8080)
backend          ──HTTP──→  GitHub API / DashScope / MiMo / arXiv / HuggingFace
```

### 外部服务依赖

| 外部服务 | 用途 | 配置方式 |
|----------|------|----------|
| 阿里百炼 DashScope | AI 来源总结 | 后台 API 设置页 / 环境变量 |
| MiMo | AI 来源总结（备选 Provider） | 后台 API 设置页 / 环境变量 |
| GitHub REST API | 仓库查询与搜索 | 后台 API 设置页 / 环境变量 |
| arXiv API | 论文搜索 | 无需密钥 |
| HuggingFace Papers API | 每日论文导入 | 无需密钥 |

---

## 8. 项目运行方式

### 前置条件

- Java 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.0+

### 步骤一：初始化数据库

```bash
mysql -uroot -p < database/schema.sql
mysql -uroot -p ai_frontier_station < database/init-data.sql
```

> ⚠️ `init-data.sql` 会清空并重置示例数据，仅用于首次初始化。

### 步骤二：启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：`http://localhost:8080`

### 步骤三：启动前台门户

```bash
cd frontend-portal
npm install
npm run dev
```

前台地址：`http://localhost:5173`

### 步骤四：启动后台管理

```bash
cd frontend-admin
npm install
npm run dev
```

后台地址：`http://localhost:5174`

### 构建验证

```bash
cd backend && mvn -q -DskipTests compile
cd frontend-portal && npm run build
cd frontend-admin && npm run build
```

### 默认演示账号

- 用户名：`admin`
- 密码：`admin123`

> 仅用于本地演示，生产环境必须修改。

---

## 9. 环境变量说明

| 变量名 | 必需 | 说明 |
|--------|------|------|
| `DB_URL` | 否 | MySQL 连接地址（默认 `jdbc:mysql://localhost:3306/ai_frontier_station?...`） |
| `DB_USERNAME` | 否 | 数据库用户名（默认 root） |
| `DB_PASSWORD` | 否 | 数据库密码（默认空） |
| `JWT_SECRET` | 否 | JWT 签名密钥（未配置则启动时自动生成，重启后失效） |
| `API_MASTER_KEY` | 否 | API Key 加密主密钥（未配置则启动时自动生成，重启后已加密密钥无法解密） |
| `DASHSCOPE_API_KEY` | 否 | 百炼 API Key（也可通过后台 API 设置页配置） |
| `MIMO_API_KEY` | 否 | MiMo API Key（也可通过后台 API 设置页配置） |
| `GITHUB_TOKEN` | 否 | GitHub Token（也可通过后台 API 设置页配置） |

> API Key 优先级：数据库加密记录 > 环境变量。

---

## 10. API 接口总览

所有接口统一前缀 `/api/v1`，统一返回 `ApiResponse<T>` 结构。

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/health` | 系统健康检查 |
| GET | `/system/profile` | 系统信息 |
| GET | `/portal/home` | 前台首页概览 |
| GET | `/portal/contents` | 前台内容列表 |
| GET | `/portal/contents/{id}` | 前台内容详情 |
| POST | `/auth/login` | 管理员登录 |

### 认证接口（需 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/auth/me` | 获取当前用户 |
| POST | `/auth/logout` | 登出 |
| GET | `/admin/dashboard/overview` | 仪表盘概览 |
| GET | `/admin/contents` | 内容分页 |
| GET | `/admin/contents/options` | 内容选项 |
| GET | `/admin/contents/{id}` | 内容详情 |
| POST | `/admin/contents` | 创建内容 |
| PUT | `/admin/contents/{id}` | 更新内容 |
| PUT | `/admin/contents/{id}/status` | 更新状态 |
| DELETE | `/admin/contents/{id}` | 删除内容 |
| POST | `/admin/contents/{id}/external-refs` | 创建外部引用 |
| PUT | `/admin/contents/{id}/external-refs/{refId}` | 更新外部引用 |
| DELETE | `/admin/contents/{id}/external-refs/{refId}` | 删除外部引用 |
| GET | `/admin/categories` | 分类列表 |
| POST | `/admin/categories` | 创建分类 |
| PUT | `/admin/categories/{id}` | 更新分类 |
| DELETE | `/admin/categories/{id}` | 删除分类 |
| GET | `/admin/tags` | 标签列表 |
| POST | `/admin/tags` | 创建标签 |
| PUT | `/admin/tags/{id}` | 更新标签 |
| DELETE | `/admin/tags/{id}` | 删除标签 |
| GET | `/admin/sources` | 来源列表 |
| POST | `/admin/sources` | 创建来源 |
| PUT | `/admin/sources/{id}` | 更新来源 |
| DELETE | `/admin/sources/{id}` | 删除来源 |
| GET | `/admin/api-settings/status` | API 设置状态 |
| PUT | `/admin/api-settings/bailian` | 保存百炼密钥 |
| DELETE | `/admin/api-settings/bailian` | 清除百炼密钥 |
| PUT | `/admin/api-settings/github` | 保存 GitHub Token |
| DELETE | `/admin/api-settings/github` | 清除 GitHub Token |
| PUT | `/admin/api-settings/mimo` | 保存 MiMo 密钥 |
| DELETE | `/admin/api-settings/mimo` | 清除 MiMo 密钥 |
| POST | `/admin/ai/source-summary` | AI 来源总结 |
| GET | `/admin/github/repo` | 查询 GitHub 仓库 |
| GET | `/admin/github/search` | 搜索 GitHub 仓库 |
| POST | `/admin/github-sync/repo/{id}` | 同步单个 GitHub 仓库 |
| POST | `/admin/github-sync/all` | 批量同步 GitHub 仓库 |
| GET | `/admin/arxiv/search` | 搜索 arXiv 论文 |
| GET | `/admin/arxiv/paper` | 查询 arXiv 论文 |
| GET | `/admin/huggingface/papers` | 获取 HF 每日论文 |
| GET | `/admin/huggingface/papers/{paperId}` | 查询 HF 论文 |
| POST | `/admin/import/github-repo` | 导入 GitHub 仓库 |
| POST | `/admin/import/ai-source` | 导入 AI 来源 |
| POST | `/admin/import/arxiv-paper` | 导入 arXiv 论文 |
| POST | `/admin/import/huggingface-paper` | 导入 HF 论文 |
