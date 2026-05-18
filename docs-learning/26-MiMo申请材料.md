# MiMo Orbit 百万亿 Token 申请材料草稿

## 1. 项目名称

AI Frontier Station（AI前沿情报站）

## 2. GitHub 仓库链接

`https://github.com/Harry6400/ai-frontier-station`

## 2.1 可视化证明截图

以下截图已保存到仓库中，README 也已引用，申请时可以直接打开 GitHub 仓库查看：

- 前台首页：`docs-learning/assets/mimo/portal-home.png`
- 内容发现页：`docs-learning/assets/mimo/portal-discovery.png`
- 内容详情页：`docs-learning/assets/mimo/portal-detail-ai-guide.png`
- 后台 Dashboard：`docs-learning/assets/mimo/admin-dashboard.png`
- 后台 API 设置页：`docs-learning/assets/mimo/admin-api-settings.png`

这些截图都来自本地真实运行页面，不是单独伪造的宣传图；API 设置页没有展示任何真实密钥。

## 3. 项目一句话简介

AI Frontier Station 是一个面向 AI 开发者的前沿信息聚合与精选平台，支持 GitHub 项目发现、AI 来源整理、标签化内容管理、前台内容展示和后台运营管理。

## 4. 项目详细介绍

这个项目最初是我的 Web 开发课程大作业，但我没有把它做成普通的学生管理系统或商品管理系统，而是把它设计成一个可以长期扩展的 AI 信息聚合平台。

当前项目已经具备完整的前后端分离架构：

- 前台门户使用 Vue 3，支持内容首页、内容发现页、详情页、搜索筛选、浅色/深色主题。
- 后台管理端使用 Vue 3 + Element Plus，支持内容、分类、标签、来源、外部引用、Dashboard 统计和 API 设置。
- 后端使用 Spring Boot 3 + MyBatis-Plus，提供统一 API、内容管理、门户展示、GitHub API 查询和百炼 AI 总结接口。
- 数据库使用 MySQL，核心表包括内容表、分类表、标签表、来源表、内容标签关联表和外部引用表。

项目长期目标是把 GitHub、论文、官方博客、技术社区和大模型动态聚合起来，通过大模型 API 做摘要、分类、标签建议、推荐理由生成和内容精选，最终形成面向 AI 开发者的信息发现平台。

## 5. 当前已完成能力

- 内容管理：新增、编辑、删除、发布、归档。
- 分类/标签/来源管理：为后续多来源信息聚合提供基础结构。
- 前台内容展示：支持首页精选、内容发现、详情阅读、搜索筛选和排序。
- 后台 Dashboard：展示内容数量、发布状态、来源类型、近期内容和外部引用动态。
- Markdown 安全清洗：使用 DOMPurify 防止外部内容中的 XSS 风险。
- GitHub API 接入：支持真实仓库查询、关键词搜索和仓库信息回填导入。
- 百炼 API 接入：支持 AI 来源整理，生成 AI 导读、推荐理由、标签建议和重要性评分。
- API 设置页：支持在网页中临时输入百炼 API Key 和 GitHub Token，密钥只保存在后端内存中。
- 阶段学习文档：记录每个阶段的技术设计、答辩问答和后续扩展思路。

## 6. 我如何使用 AI / 大模型能力

当前项目中，大模型不是简单聊天入口，而是作为“AI 总结层”嵌入内容平台流程：

1. 管理员输入一条来源链接、原文摘要或关键摘录。
2. 后端调用百炼 API。
3. 模型生成 AI 导读、推荐理由、标签建议、重要性评分和正文 Markdown。
4. 管理员确认后创建草稿内容。
5. 前台详情页展示 AI 导读和原始来源信息。

这种设计的好处是：数据源和 AI 总结层解耦。GitHub、论文、官方博客、社区帖子都可以作为来源，大模型负责整理和提炼，而不是替代全部数据来源。

## 7. 为什么需要 MiMo Token

如果获得 MiMo Token，我计划把它用于以下方向：

- 批量总结 AI 官方博客、论文摘要、GitHub README 和社区实践内容。
- 为每条内容生成更稳定的标签建议和推荐理由。
- 建立“AI 前沿周报”功能，把一周内的 GitHub 项目、论文和公司动态整理成摘要。
- 对不同来源进行可信度说明和重要性评分。
- 尝试把 MiMo 作为可选 AI Provider 接入现有的 AI 总结层，和百炼一起形成多模型接口适配能力。

项目已经有 API 设置页、AI 总结服务、内容表、来源表和外部引用表，因此获得 Token 后可以比较快地扩展出 MiMo Provider，而不是从零开始。

## 8. 项目亮点

- 不是普通 CRUD 管理系统，而是围绕 AI 信息聚合方向设计。
- 已有真实 GitHub API 接入，不只是静态假数据。
- 已有百炼 API 总结工作流，体现了大模型在内容平台中的真实作用。
- 数据模型为未来扩展预留了 `source`、`external_ref`、`extra_json` 等结构。
- 前台和后台都有产品化设计，支持浅色/深色主题和现代内容平台布局。
- 文档完整，适合长期维护，也适合作为课程答辩和开源项目展示。

## 9. 后续路线

短期计划：

- GitHub 项目热度同步：定期刷新 Star、语言、更新时间。
- 百炼联网搜索或 MiMo Provider：按主题搜索并整理一手信息。
- 论文来源接入：支持 arXiv / Hugging Face Papers 等论文信息。

中期计划：

- AI 周报生成。
- 内容推荐与重要性排序。
- 收藏、订阅、专题页面。
- 管理员登录与权限系统。

长期愿景：

把 AI Frontier Station 扩展为一个面向 AI 开发者的高质量信息发现平台，让开发者更快了解 AI 项目、论文、模型动态和工程实践。

## 10. 申请表可复制版

我正在开发一个名为 AI Frontier Station（AI前沿情报站）的开源项目。它是一个 Vue + Spring Boot + MySQL 的 AI 信息聚合与精选平台，当前已经支持前台内容展示、后台内容管理、GitHub API 仓库查询、百炼 API 导读生成、标签与来源管理、Markdown 安全清洗和 Dashboard 统计。

这个项目的目标不是做普通 CRUD 系统，而是持续扩展为面向 AI 开发者的信息发现平台。它会聚合 GitHub 项目、论文、官方博客、公司动态和技术社区内容，并使用大模型生成摘要、推荐理由、标签建议、重要性评分和周报。

如果获得 MiMo Token，我会把它接入现有的 AI 总结层，尝试用 MiMo 对 GitHub README、论文摘要、官方动态和社区实践进行批量总结，并实现 AI 前沿周报、内容推荐和多模型 Provider 适配能力。项目已经有后端 API 设置页和外部数据源结构，因此可以较快落地 MiMo API 实验。

## 11. 短版申请表文案

我正在开发 AI Frontier Station（AI前沿情报站），这是一个开源的 AI 信息聚合与精选平台，技术栈为 Vue 3 + Spring Boot 3 + MySQL。项目已经实现前台门户、后台管理、内容发现、搜索筛选、Dashboard 统计、GitHub API 查询、百炼 AI 导读生成、Markdown 安全清洗和运行时 API 设置页。

我希望获得 MiMo Orbit Token，用于把 MiMo 接入现有 AI 总结层，批量整理 GitHub 项目、论文摘要、官方博客和技术社区内容，生成摘要、标签建议、推荐理由、重要性评分和 AI 前沿周报。这个项目已经公开到 GitHub，并包含真实运行截图和完整阶段文档，后续会持续维护为 AI Builder 项目。

GitHub 仓库：`https://github.com/Harry6400/ai-frontier-station`

## 12. 项目亮点压缩版

- 不是普通课程 CRUD，而是围绕 AI 信息聚合和 AI 内容精选设计。
- 已经有完整前后端分离系统：Vue 前台、Vue 后台、Spring Boot 后端、MySQL 数据库。
- 已接入 GitHub REST API，可查询真实仓库并导入为平台内容。
- 已接入百炼 API 工作流，可生成 AI 导读、推荐理由、标签建议和重要性评分。
- 已有运行时 API 设置页，密钥不落库、不回显完整内容，适合开源演示。
- 已有来源、标签、外部引用和扩展 JSON 结构，方便继续扩展论文、官方动态和社区来源。
- README 和 `docs-learning` 中保留了阶段记录、答辩说明和申请材料，能证明项目是持续演进的。

## 13. Token 使用计划压缩版

- 第一阶段：把 MiMo 作为新的 AI Provider 接入当前 AI 总结层，复用现有 API 设置页和内容导入流程。
- 第二阶段：用 MiMo 总结 GitHub README、论文摘要、官方博客和技术社区文章，生成结构化 AI 导读。
- 第三阶段：基于多来源内容生成 AI 前沿周报，包括热门项目、论文趋势、公司动态和工程实践。
- 第四阶段：加入内容重要性评分、标签推荐和来源可信度说明，提升平台的信息筛选能力。
- 第五阶段：沉淀为课程项目、开源项目和长期 AI 信息平台三者统一的作品。

## 14. 申请时可以强调的证明材料

- GitHub 公开仓库：`https://github.com/Harry6400/ai-frontier-station`
- README 截图区：展示前台、后台、详情页、Dashboard 和 API 设置页。
- `docs-learning/26-MiMo申请材料.md`：当前申请文案。
- `docs-learning/00-项目总控与长期进度.md`：长期阶段记录。
- `docs-learning/25-阶段22-后台API设置与GitHubAPI接入记录.md`：说明 API 设置页和 GitHub API 接入。
- `docs-learning/23-阶段20-数据源分层与百炼总结接入记录.md`：说明为什么百炼/MiMo 是 AI 总结层，而不是唯一数据源。
