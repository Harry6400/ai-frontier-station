# AGENTS.md

AI Frontier Station（AI前沿情报站）是一个 AI 趋势智能平台。从海量 AI 信息中筛选"真正值得关注"的趋势、项目和事件。不是 RSS 新闻站，是 AI Trend Intelligence Platform。

本文件是 Codex、OpenCode 和其他 coding agent 的项目接管入口。开始开发前请先读本文件，再读下方文档入口。

## 项目结构

```text
AI前沿情报站/
├── backend/            Spring Boot 3 后端 API 服务
├── frontend-portal/    Vue 3 前台门户
├── frontend-admin/     Vue 3 + Element Plus 后台管理端
├── database/           MySQL 建表与初始化脚本
├── docs-learning/      中文阶段记录、架构决策、答辩与申请材料
├── designs/            设计稿 HTML 文件
├── .env.example        环境变量参考模板
└── README.md           GitHub 开源展示入口
```

三个子项目相互独立，没有 monorepo 工作区管理工具。进入对应目录后再执行命令。

## 优先阅读顺序

为了节省 token，请不要一上来全量扫描项目。优先读：

1. `README.md`
2. `AGENTS.md`（本文件）
3. `docs-learning/开发踩坑记录.md` — 必读，防止重复犯错
4. `docs-learning/00-项目总控与长期进度.md`

如果要继续某个具体阶段，再读对应 `docs-learning/xx-阶段...md`。

## 技术栈

- 前台门户：Vue 3 / Vite / Vue Router / Pinia / marked / DOMPurify
- 后台管理：Vue 3 / Vite / Vue Router / Pinia / Element Plus（按需导入）
- 后端服务：Spring Boot 3 / MyBatis-Plus / Lombok / Java 17
- 数据库：MySQL 8
- 外部能力：阿里百炼 DashScope / MiMo / GitHub REST API / arXiv / HuggingFace Papers

## 系统架构：TrendRadar 七层管道

```
数据源层 → 清洗层 → 评分层 → 聚类层 → AI理解层 → 排序层 → 展示层
```

当前实现状态：
- ✅ 数据源层：GitHub API、arXiv、HuggingFace、TechCrunch RSS、Hacker News API、Reddit API
- ✅ 清洗层：去重（按external_id）、标准化
- ✅ 评分层：Trend Score 算法（star增长35% + 更新时间15% + fork比率10% + AI相关性10%）× 时间衰减
- ⬜ 聚类层：事件聚类（后续）
- ✅ AI理解层：百炼/MiMo 生成中文摘要、技术标签、方向分类、难度评级、适合人群、学习路线
- ✅ 排序层：按 trend_score 排序
- ✅ 展示层：Trend Board + 详情页

## 数据管道：两层架构

```
真实数据源 → ai_content_candidate(原始候选) → 管理员审核/AI处理 → ai_content(发布内容)
```

- **候选表**：存储从 API/网站采集的原始数据，管理员可编辑
- **内容表**：存储经过 AI 处理和管理员审核的最终内容
- **AI处理**：调用百炼/MiMo API，用可编辑的提示词生成结构化中文内容
- 每一步都可编辑，管理员能看到原始数据 → AI处理过程 → 最终展示

## 前台六大板块

| 板块 | 路由 | 数据来源 | 特点 |
|------|------|---------|------|
| 论文 | /papers | arXiv + HuggingFace | 子分类：3D CT去噪/医学影像/大模型 |
| GitHub | /github | GitHub Trending API | Trend Board，按趋势分排序 |
| AI资讯 | /news | TechCrunch RSS + HN | 国际AI政策和行业动态 |
| 产品动态 | /company | 官方博客RSS | 按产品归属组织 |
| 模型评测 | /arena | LMSYS Arena | AI解读板块 |
| 工具实践 | /tools | Reddit | 工具和实践分享 |

## 后台管理

- **内容管理**：横向Tab切换内容类型（无"全部"），论文有子分类chips
- **数据采集**：候选审核页面，左侧Tab导航（AI内容/提示词/原始内容），支持AI处理
- **来源管理**：按类型分组显示
- **API设置**：百炼/MiMo/GitHub Token，加密持久化
- ~~分类管理~~：已删除

## 启动命令

后端必须先启动，否则两个前端的 API 调用会失败。

```bash
cd backend
DB_URL="jdbc:mysql://localhost:3306/ai_frontier_station?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai" \
DB_USERNAME=root DB_PASSWORD=123456 \
API_MASTER_KEY=xK9mP2vL8nQ4wR7tY5zA \
JWT_SECRET=fJ3kD7gH9pL2sV5xB8nM4qW6yT0rE1uC \
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
cd backend && mvn -q -DskipTests compile
cd frontend-portal && npm run build
cd frontend-admin && npm run build
```

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

后台"API 设置"页支持配置百炼、MiMo 和 GitHub Token。当前版本会优先使用数据库中的 AES-256-GCM 加密记录；未配置时再回退读取环境变量。接口只返回启用状态和掩码，不回显完整密钥。

**重要**：`API_MASTER_KEY` 必须每次启动保持一致，否则已保存的 API Key 无法解密。

严禁提交 `.env`、真实 API Key、GitHub Token 或任何本机隐私配置。

## API 架构

所有后端接口统一前缀为 `/api/v1`。

- `controller/admin/`：后台管理接口，包括内容 CRUD、来源、Dashboard、候选管理、AI处理、API 设置
- `controller/portal/`：前台门户接口，包括首页概览、内容列表、内容详情
- 统一返回结构：`ApiResponse<T>`，包含 `code`、`message`、`data`

**重要**：前台 axios 拦截器已做 `response.data` 剥离，前端拿到的 `res` 就是 `{code, message, data}`，`res.data` 就是业务数据，不要再做 `res.data.data`。

两个前端通过 Vite 代理访问后端。后台代理应保持 `/api/v1`，不要改回宽泛的 `/api`。

## 安全和边界

- 当前后台已有管理员 JWT 登录，默认演示账号为 `admin / admin123`。
- Markdown 渲染必须经过 DOMPurify 清洗。
- API Key 只能通过环境变量或后台 API 设置页配置。数据库存储必须加密。
- 不做无关重构，不删除用户已有改动，不为了"顺手优化"扩大范围。
- **永远不要编造数据**。宁可显示"暂无数据"也不要造假。用户明确拒绝过假数据。

## 前端开发注意事项

- Element Plus 按需导入时，**指令不会自动注册**（v-loading 等），不要使用未注册的指令
- 前端字段统一用 **camelCase**（与 Java 后端一致），不要用 snake_case
- 使用 `<el-input type="textarea">` 而不是 native `<textarea>`
- 后台侧边栏：概览 / 内容管理 / 数据采集 / 来源管理 / API 设置

## 协作规则

- 每次阶段任务结束后，更新 `docs-learning/00-项目总控与长期进度.md`。
- 重要功能新增或关键架构调整时，新增一份 `docs-learning/xx-阶段...记录.md`。
- 修改前先理解当前代码分层，不要跳过 Controller / Service / Mapper / DTO / VO 的边界。
- 优先小步实现、小步验证，再提交。
- 提交前检查 `git status --short`，确认没有提交 `node_modules`、`dist`、`target`、`.env`、临时文件。

## 踩坑记录

详见 `docs-learning/开发踩坑记录.md`，包含所有已知问题和解决方案。开发前必读。
