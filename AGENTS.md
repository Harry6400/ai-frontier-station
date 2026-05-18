# AGENTS.md

AI Frontier Station（AI前沿情报站）是一个面向 AI 开发者的前沿信息聚合与精选平台。当前是课程 MVP + 开源原型，长期目标是接入 GitHub、论文、官方动态、技术社区和大模型 API，总结并精选 AI 一手信息。

本文件是 Codex、OpenCode 和其他 coding agent 的项目接管入口。开始开发前请先读本文件，再读下方文档入口。

## 项目结构

```text
AI前沿情报站/
├── backend/            Spring Boot 3 后端 API 服务
├── frontend-portal/    Vue 3 前台门户
├── frontend-admin/     Vue 3 + Element Plus 后台管理端
├── database/           MySQL 建表与初始化脚本
├── docs-learning/      中文阶段记录、架构决策、答辩与申请材料
├── .env.example        环境变量参考模板
└── README.md           GitHub 开源展示入口
```

三个子项目相互独立，没有 monorepo 工作区管理工具。进入对应目录后再执行命令。

## 优先阅读顺序

为了节省 token，请不要一上来全量扫描项目。优先读：

1. `README.md`
2. `docs-learning/00-项目总控与长期进度.md`
3. `docs-learning/02-技术选型与目录结构说明.md`
4. `docs-learning/26-MiMo申请材料.md`
5. `docs-learning/33-阶段29-HuggingFace数据源与登录页重设计记录.md`

如果要继续某个具体阶段，再读对应 `docs-learning/xx-阶段...md`。

## 技术栈

- 前台门户：Vue 3 / Vite / Vue Router / Pinia / marked / DOMPurify
- 后台管理：Vue 3 / Vite / Vue Router / Pinia / Element Plus
- 后端服务：Spring Boot 3 / MyBatis-Plus / Lombok / Java 17
- 数据库：MySQL 8
- 外部能力：阿里百炼 DashScope / MiMo / GitHub REST API / arXiv / HuggingFace Papers

## 启动命令

后端必须先启动，否则两个前端的 API 调用会失败。

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend-portal
npm run dev
```

```bash
cd frontend-admin
npm run dev
```

默认端口：

- 后端：`http://localhost:8080`
- 前台：`http://localhost:5173`
- 后台：`http://localhost:5174`

## 构建验证

阶段任务完成后，至少按影响范围执行：

```bash
cd backend
mvn -q -DskipTests compile
```

```bash
cd frontend-portal
npm run build
```

```bash
cd frontend-admin
npm run build
```

后台之前做过包体优化，正常情况下不应再出现 500KB chunk 警告。

## 数据库初始化

```bash
mysql -uroot -p < database/schema.sql
mysql -uroot -p ai_frontier_station < database/init-data.sql
```

注意：`database/init-data.sql` 会清空并重置示例数据，只适合首次初始化或明确要重置演示库时执行。日常开发不要随便运行。

## 环境变量和 API Key

参考 `.env.example`：

- `DB_URL`：MySQL 连接地址
- `DB_USERNAME`：数据库用户名
- `DB_PASSWORD`：数据库密码
- `DASHSCOPE_API_KEY`：阿里百炼 API Key
- `MIMO_API_KEY`：MiMo API Key
- `GITHUB_TOKEN`：GitHub Token
- `JWT_SECRET`：后台登录 JWT 签名密钥
- `API_MASTER_KEY`：API Key 加密存储主密钥

后台“API 设置”页支持配置百炼、MiMo 和 GitHub Token。当前版本会优先使用数据库中的 AES-256-GCM 加密记录；未配置时再回退读取环境变量。接口只返回启用状态和掩码，不回显完整密钥。

严禁提交 `.env`、真实 API Key、GitHub Token 或任何本机隐私配置。

## API 架构

所有后端接口统一前缀为 `/api/v1`。

- `controller/admin/`：后台管理接口，包括内容 CRUD、分类、标签、来源、Dashboard、外部引用、GitHub 导入、AI 来源整理、API 设置。
- `controller/portal/`：前台门户接口，包括首页概览、内容列表、内容详情。
- 统一返回结构：`ApiResponse<T>`，包含 `code`、`message`、`data`。

两个前端通过 Vite 代理访问后端。后台代理应保持 `/api/v1`，不要改回宽泛的 `/api`，否则 `/api-settings` 前端路由会被错误代理到后端。

## 安全和边界

- 当前后台已有管理员 JWT 登录，默认演示账号为 `admin / admin123`。该账号只适合本地演示，生产化前必须修改默认账号、强密钥和权限策略。
- Markdown 渲染必须经过 DOMPurify 清洗，不要绕过 `safeMarkdown` 工具直接 `v-html` 外部内容。
- API Key 只能通过环境变量或后台 API 设置页配置。数据库存储必须加密，严禁明文落库、日志打印或接口返回完整 Key。
- 不要把 AI 建议标签自动写入标签库，第一版应由管理员确认，避免污染数据。
- 不做无关重构，不删除用户已有改动，不为了“顺手优化”扩大范围。

## 协作规则

- 每次阶段任务结束后，更新 `docs-learning/00-项目总控与长期进度.md`。
- 重要功能新增或关键架构调整时，新增一份 `docs-learning/xx-阶段...记录.md`。
- 修改前先理解当前代码分层，不要跳过 Controller / Service / Mapper / DTO / VO 的边界。
- 优先小步实现、小步验证，再提交。
- 提交前检查 `git status --short`，确认没有提交 `node_modules`、`dist`、`target`、`.env`、临时文件。
- 如果发现真实密钥、隐私路径或不该提交的构建产物，先停止并处理，不要继续提交。

## 当前完成度摘要

- 已完成 Vue 前台门户、Vue 后台管理、Spring Boot 后端和 MySQL 数据库。
- 已完成内容、分类、标签、来源、外部引用等核心数据模型。
- 已完成前台首页、内容发现页、详情页、搜索筛选、排序和双主题。
- 已完成后台登录、Dashboard、内容管理、分类管理、标签管理、来源管理、API 设置页。
- 已接入 GitHub REST API 查询与仓库导入。
- 已接入百炼 / MiMo 可选 AI Provider 的来源整理工作流。
- 已接入 arXiv 论文搜索导入和 HuggingFace Daily Papers 导入。
- 已完成 API Key 加密存储，支持后端重启后继续读取已保存配置。
- 已完成 Markdown 安全清洗、后台深色模式、后台包体优化、MiMo 申请截图和开源 README。

## 后续优先级

推荐下一阶段优先级：

1. GitHub 项目热度同步：定期刷新 star、fork、语言、更新时间等结构化字段。
2. CI/CD 与开源质量：补 GitHub Actions，自动跑后端编译和前后台构建。
3. 部署上线：前端可考虑 Cloudflare Pages，后端可考虑 Railway、Render 或其他 Java 服务托管。
4. 数据源扩展：官方博客 RSS、技术社区来源、榜单源快照。
5. 权限增强：把单管理员登录升级为角色权限、操作审计和更安全的密钥轮换。
