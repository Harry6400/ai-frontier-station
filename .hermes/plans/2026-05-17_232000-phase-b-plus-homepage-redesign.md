# Phase B + 前台首页大改 — 并行实施计划

> 更新时间：2026-05-17
> 执行方式：delegate_task 并行派 2 个子 agent

---

## 一、需求总结

### Phase B：AI 内化管线（中文摘要）
- 新论文入库后自动调 AI 生成中文摘要，替换英文原文摘要
- 内容以中文为主，仅保留不可替代的英文专有名词
- 对已有的 71 篇内容也要做一次批量中文摘要

### 前台首页大改
- 删除「栏目地图」和「这版首页为什么更像系统」两个模块
- 首页顶部加 6 个板块入口框（类似现有的「内容广场」「项目说明」样式）：
  1. GitHub 开源项目 → `/contents?contentType=project`
  2. 论文速递 → `/contents?contentType=paper`
  3. AI 公司动态 → `/contents?contentType=company_update`
  4. AI 资讯 → `/contents?contentType=news`
  5. Arena 模型评测 → `/contents?contentType=arena`（新增类型）
  6. 工具与最佳实践 → `/contents?contentType=practice`
- 每个入口框点击后跳转到已有的 ContentListView，按 contentType 筛选
- 顶部导航栏也更新（去掉「项目说明」，加各板块快捷入口）

---

## 二、并行分工

### Agent 1：Phase B — AI 中文摘要管线（后端）

**职责**：让新论文入库后自动生成中文摘要，已有论文批量补摘要。

#### 要做的事：

1. **读取现有代码理解模式**
   - `backend/src/main/java/com/harry/aifrontier/service/impl/AiSummaryServiceImpl.java`（重点看 buildChatRequest 和 buildPrompt 方法，理解现有 AI 调用模式）
   - `backend/src/main/java/com/harry/aifrontier/service/impl/ContentServiceImpl.java`（看 importArxivPaper 和 importHuggingFacePaper 方法）
   - `backend/src/main/java/com/harry/aifrontier/scheduler/ArxivFetchScheduler.java`（看 runOnce 方法）
   - `backend/src/main/java/com/harry/aifrontier/scheduler/HuggingFaceFetchScheduler.java`
   - `backend/src/main/java/com/harry/aifrontier/service/ApiCredentialService.java`
   - `backend/src/main/java/com/harry/aifrontier/entity/Content.java`
   - `backend/src/main/java/com/harry/aifrontier/mapper/ContentMapper.java`

2. **新建 PaperSummaryService**
   - 文件：`backend/src/main/java/com/harry/aifrontier/service/PaperSummaryService.java`（接口）
   - 文件：`backend/src/main/java/com/harry/aifrontier/service/impl/PaperSummaryServiceImpl.java`
   - 核心方法：`String generateChineseSummary(String title, String abstractText, String provider)`
   - 调用百炼或 MiMo API（复用 AiSummaryServiceImpl 的调用模式）
   - **Prompt 要求**：
     - 用中文撰写论文摘要，面向本科生水平
     - 保留不可替代的英文专有名词（如 CT、diffusion model、U-Net 等）
     - 包含：一句话核心贡献、方法简述（3-5句）、与 CT 去噪的关联度（1-10）、推荐理由
     - 输出纯文本，不要 JSON
   - 提供 `String generateChineseSummaryForExisting(String title, String bodyMarkdown)` 方法（用于批量处理已有内容）

3. **修改 ArxivFetchScheduler**
   - 在 `runOnce()` 中，论文导入成功后立即调用 PaperSummaryService 生成中文摘要
   - 更新该 Content 的 summary 字段为中文摘要
   - 失败不影响其他论文的采集（try-catch 包住）
   - 注意：不要阻塞采集流程，可以异步或在最后统一处理

4. **修改 HuggingFaceFetchScheduler**
   - 同上，导入后自动生成中文摘要

5. **新建批量摘要 API**
   - 在 `FetchAdminController.java` 加一个接口：
     - `POST /api/v1/admin/fetch/batch-summary` — 对所有 summary 为英文（或为空）的内容批量生成中文摘要
     - `POST /api/v1/admin/fetch/batch-summary?provider=mimo` — 指定 provider
   - 这个接口会遍历所有内容，调用 PaperSummaryService 生成中文摘要并更新

6. **数据库注意**
   - summary 字段已改为 TEXT，可以直接存中文长文本
   - 不需要新建表

7. **编译验证**
   - `cd backend && mvn -q -DskipTests compile`

**关键注意**：
- API Key 从 ApiCredentialService 读取（和 AiSummaryServiceImpl 一样）
- 用户说已经填了 3 个 API Key（百炼、MiMo、GitHub）
- 默认用 MiMo（便宜），支持传参切换
- prompt 必须强调"用中文撰写"
- 生成的摘要直接存入 Content.summary 字段，替换英文原文

---

### Agent 2：前台首页大改 + 导航更新（前端）

**职责**：重做首页布局，加 6 个板块入口，删除不需要的模块。

#### 要做的事：

1. **读取现有代码理解结构**
   - `frontend-portal/src/views/HomeView.vue`（重点看完整模板）
   - `frontend-portal/src/components/PortalTopbar.vue`（导航栏）
   - `frontend-portal/src/views/ContentListView.vue`（内容列表页，看 typeOptions）
   - `frontend-portal/src/router/index.js`（路由）
   - `frontend-portal/src/stores/usePortalStore.js`
   - `frontend-portal/src/api/portal.js`
   - `frontend-portal/src/assets/main.css`（样式，看 section-shell、hero-grid 等 class）

2. **重做 HomeView.vue**
   - **删除**：
     - 「栏目地图」section（行 207-226 的 editorial-grid 第一个 section）
     - 「这版首页为什么更像系统」section（行 228-252 的 editorial-grid 第二个 section）
     - `architecturePoints` 数据定义
   - **保留**：
     - 顶部 hero 区域（AI 前沿情报站标题 + 描述）
     - Signal Deck（最新情报面板）
     - Featured Selection（精选内容卡片）
   - **新增**：在 hero 区域下方、Signal Deck 之前，加一个「板块导航」区域：
     ```
     ┌─────────────────────────────────────────────────┐
     │  Section Navigator                               │
     │                                                  │
     │  ┌──────────┐ ┌──────────┐ ┌──────────┐        │
     │  │ GitHub   │ │ 论文速递  │ │ AI公司动态│        │
     │  │ 开源项目 │ │          │ │          │        │
     │  └──────────┘ └──────────┘ └──────────┘        │
     │  ┌──────────┐ ┌──────────┐ ┌──────────┐        │
     │  │ AI 资讯  │ │ Arena    │ │ 工具与   │        │
     │  │          │ │ 模型评测  │ │ 最佳实践 │        │
     │  └──────────┘ └──────────┘ └──────────┘        │
     └─────────────────────────────────────────────────┘
     ```
   - 每个框是一个 `RouterLink`，跳转到 `/contents?contentType=xxx`
   - 样式复用现有的 `section-shell` class，每个框用卡片样式
   - 每个框包含：图标/emoji + 标题 + 简短描述 + 内容数量

3. **更新 PortalTopbar.vue 导航栏**
   - 现有：`首页 | 内容广场 | 项目说明`
   - 改为：`首页 | 论文 | GitHub | AI资讯 | 公司动态 | 工具实践`
   - 每个导航项链接到 `/contents?contentType=xxx`
   - 保留搜索框和主题切换

4. **更新 ContentListView.vue**
   - typeOptions 加一个新的 Arena 类型：
     ```js
     { label: 'Arena 模型评测', value: 'arena' }
     ```
   - 确保从 URL query 参数 `contentType` 能正确预选筛选条件（已有逻辑）

5. **更新 router/index.js**
   - 不需要新增路由（现有 `/contents` 路由已支持 query 参数筛选）
   - 但可以考虑加一些便捷路由别名（可选）

6. **更新 usePortalStore.js**
   - contentTypes 加 'Arena 模型评测'

7. **样式注意**
   - 6 个板块入口框用 3×2 网格布局
   - 复用现有的 `section-shell`、`card-grid` 等 class
   - 保持和现有首页风格一致
   - 深色/浅色主题兼容

8. **构建验证**
   - `cd frontend-portal && npm run build`

---

## 三、新增 contentType：arena

需要在后端也加上这个类型选项：

**Agent 1 负责**：
- `ContentServiceImpl.java` 的 `options()` 方法加 `new OptionVO("Arena 模型评测", "arena")`

**Agent 2 负责**：
- `ContentListView.vue` 的 typeOptions 加 `{ label: 'Arena 模型评测', value: 'arena' }`
- `usePortalStore.js` 的 contentTypes 加 'Arena 模型评测'

---

## 四、文件变更汇总

### Agent 1（后端）新建
- `backend/src/main/java/com/harry/aifrontier/service/PaperSummaryService.java`
- `backend/src/main/java/com/harry/aifrontier/service/impl/PaperSummaryServiceImpl.java`

### Agent 1（后端）修改
- `backend/src/main/java/com/harry/aifrontier/scheduler/ArxivFetchScheduler.java`
- `backend/src/main/java/com/harry/aifrontier/scheduler/HuggingFaceFetchScheduler.java`
- `backend/src/main/java/com/harry/aifrontier/controller/admin/FetchAdminController.java`
- `backend/src/main/java/com/harry/aifrontier/service/impl/ContentServiceImpl.java`（加 arena 类型选项）

### Agent 2（前端）修改
- `frontend-portal/src/views/HomeView.vue`（大改）
- `frontend-portal/src/components/PortalTopbar.vue`（导航更新）
- `frontend-portal/src/views/ContentListView.vue`（加 arena 类型）
- `frontend-portal/src/stores/usePortalStore.js`（加 arena）

---

## 五、验证清单

- [ ] `mvn -q -DskipTests compile` 后端编译通过
- [ ] `npm run build` 前台构建通过
- [ ] 首页显示 6 个板块入口框
- [ ] 点击「论文速递」跳转到 `/contents?contentType=paper`，显示 63 篇论文
- [ ] 点击「GitHub 开源项目」跳转到 `/contents?contentType=project`，显示 3 个项目
- [ ] 导航栏显示新板块入口
- [ ] 「栏目地图」和「这版首页为什么更像系统」已删除
- [ ] 手动触发 `POST /api/v1/admin/fetch/batch-summary` 能生成中文摘要
- [ ] 新采集的论文自动获得中文摘要

---

## 六、风险和注意事项

| 风险 | 对策 |
|------|------|
| AI 生成中文摘要可能很慢（71篇批量） | batch-summary 接口用后台异步，返回"处理中" |
| MiMo/百炼 API 限流 | 批量处理时每篇间隔 2 秒 |
| 前端样式不一致 | 复用现有 CSS class，不新造样式体系 |
| arena 类型没有内容 | 初始为空，后续手动或自动采集填入 |
| Agent 改同一个文件冲突 | Agent 1 改后端，Agent 2 改前端，不重叠 |
