# 阶段 16：GitHub 项目手动导入 Demo 记录

## 1. 本阶段目标

本阶段把“AI 信息聚合平台”的长期方向落成一个可演示的小闭环：在后台手动填写 GitHub 仓库信息，然后自动生成平台内容和外部引用记录。

本阶段不接入真实 GitHub API，不需要 GitHub Token，也不做自动采集。它是为未来采集模块预留结构，同时保证当前课程展示稳定可讲。

## 2. 为什么现在做这个功能

项目已经具备三类核心能力：

- `ai_content`：统一内容表
- `ai_source`：来源表
- `ai_content_external_ref`：外部引用关系表

GitHub 项目导入正好能把这三者串起来。这样答辩时不只是说“以后可以接 GitHub”，而是可以演示“GitHub 项目信息如何进入平台内容中心”。

## 3. 本阶段涉及的技术点

- Spring Boot 新增后台导入接口
- DTO 请求参数校验
- Service 层复用内容创建逻辑
- 同一事务中写入内容和外部引用
- Vue 后台弹窗表单
- 后台接口调用与列表刷新
- `extra_json` 扩展字段设计

## 4. 当前代码结构怎么理解

后端新增：

```text
backend/src/main/java/com/harry/aifrontier/
├─ controller/admin/ImportAdminController.java
├─ dto/request/GitHubRepoImportRequest.java
└─ service/ContentService.java
```

后台新增：

```text
frontend-admin/src/
├─ api/admin.js
└─ views/ContentManageView.vue
```

导入功能没有新增数据库表，而是复用已有的内容表和外部引用表。

## 5. 数据流怎么走

1. 管理员在后台内容管理页点击“GitHub 项目导入”。
2. 后台弹窗收集仓库全名、仓库链接、简介、Star 数、语言、Topics、分类、来源、标签和发布状态。
3. 前端调用 `POST /api/v1/admin/import/github-repo`。
4. 后端把表单数据转换成 `ContentSaveRequest`。
5. 后端创建一条 `content_type = project` 的内容。
6. 后端创建一条 `ref_type = github_repo` 的外部引用记录。
7. 后台刷新内容列表，并默认筛选到“项目”内容。

## 6. 为什么不用真实 GitHub API

真实 GitHub API 会引入更多问题：

- 是否需要 Token
- API 限流
- 网络不稳定
- 字段映射更复杂
- 课堂演示时失败风险更高

当前阶段优先做“可运行、可答辩、可扩展”的最小闭环。后续如果要接真实采集，只需要把手动表单换成自动获取数据，再复用当前写入内容和外部引用的逻辑。

## 7. 关键代码应该怎么读

### 7.1 导入请求 DTO

`GitHubRepoImportRequest` 描述后台表单提交的数据，包括：

- `repoFullName`
- `repoUrl`
- `description`
- `stars`
- `language`
- `topics`
- `homepage`
- `categoryId`
- `sourceId`
- `tagIds`
- `publishStatus`

这些字段不是数据库表的直接复制，而是“外部数据源输入模型”。

### 7.2 导入接口

`ImportAdminController` 暴露：

```text
POST /api/v1/admin/import/github-repo
```

它调用 `ContentService.importGitHubRepo()`，返回创建后的内容详情。

### 7.3 内容生成逻辑

`ContentServiceImpl.importGitHubRepo()` 做两件事：

- 生成一条平台内容，内容类型固定为 `project`
- 生成一条外部引用，引用类型固定为 `github_repo`

这样未来无论是 GitHub、论文、官方博客还是社区文章，都可以走类似的导入模型。

## 8. 生成的 extra_json 有什么用

GitHub 项目导入会把仓库信息写入 `extra_json`：

```json
{
  "externalType": "github_repo",
  "repoFullName": "openai/openai-cookbook",
  "repoUrl": "https://github.com/openai/openai-cookbook",
  "stars": 0,
  "language": "Jupyter Notebook",
  "topics": ["openai", "llm", "cookbook"],
  "homepage": null,
  "importMode": "manual_demo"
}
```

它的意义是：核心内容字段保持统一，外部来源特有字段放在扩展 JSON 中。这样既不会把内容表设计得过宽，也方便未来支持论文、公司动态、技术实践等不同数据源。

## 9. 老师可能会怎么问

### 问：这个导入是真正爬 GitHub 吗？

可以回答：当前不是。第一版做的是手动导入 Demo，用来验证数据结构和业务流程。真实采集会带来 Token、限流和网络稳定性问题，所以先完成稳定闭环。

### 问：为什么导入后还是写入内容表？

可以回答：平台展示的核心对象是内容。GitHub 仓库、论文、官方博客最终都需要转成统一的内容模型，前台才能统一列表、详情、筛选和推荐。

### 问：外部引用表有什么作用？

可以回答：它记录平台内容和外部系统记录之间的映射。例如这条内容来自某个 GitHub 仓库，后续同步 Star、语言、README 时，就能通过 `github_repo + external_id` 找到原始来源。

### 问：为什么不用新表存 GitHub 项目？

可以回答：第一版先不拆太细。GitHub 项目的平台展示信息放进内容表，GitHub 特有字段放进 `extra_json`，外部映射放进 `ai_content_external_ref`。如果未来 GitHub 模块变复杂，再单独拆项目详情表。

## 10. 本阶段验收结果

- 后端新增 GitHub 手动导入接口
- 后台内容管理页新增 GitHub 项目导入按钮
- 导入弹窗支持填写仓库、简介、Star、语言、Topics、主页、分类、来源、标签和状态
- 导入后会生成 `project` 内容
- 导入后会生成 `github_repo` 外部引用记录
- 后端编译通过
- 后台构建通过，且无 500KB chunk 警告

## 11. 固定收尾规范

从本阶段开始，每次完成一个实施计划后，最终回复末尾都要固定补充：

- 下一步推荐方向
- 为什么推荐这个方向
- 下一步可执行计划摘要

这样可以避免长期项目在多轮对话后失去推进节奏。

## 12. 下一阶段建议

下一阶段建议优先做 Markdown 安全清洗。

原因是项目后续会接入 GitHub、论文、官方博客和社区内容，外部内容进入平台后，如果直接渲染 Markdown，可能存在 XSS 风险。现在先补安全清洗，比继续加新功能更稳。
