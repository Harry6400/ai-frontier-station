# Phase A：定时采集引擎 — 第一波并行实施计划

> 目标：把"人去后台点按钮搜索→导入"变成"系统每天自动跑"。
> 执行方式：delegate_task 并行派 2-3 个子 agent。

---

## 现有代码关键路径

- `ArxivServiceImpl.searchPapers(query, maxResults, start)` — 搜索 arXiv API，返回候选列表
- `HuggingFaceServiceImpl.getDailyPapers()` — 获取 HF Daily Papers
- `ContentServiceImpl.importArxivPaper(request)` — 导入单篇 arXiv 论文到 ai_content + ai_content_external_ref
- `ContentServiceImpl.importHuggingFacePaper(request)` — 导入单篇 HF 论文
- `ContentExternalRef.externalId` — 存 arxiv_id 或 paper_id，用于去重
- `Content.publishStatus` — 已有 DRAFT / PUBLISHED 状态
- 数据库 `ai_content_external_ref` 表已有 `ref_type` + `external_id` 字段

---

## 并行分工（3 个子 Agent）

### Agent 1：arXiv 定时采集器

**职责**：新建定时任务，每天自动拉取 arXiv 论文并入库为草稿。

**要做的事**：

1. `AiFrontierApplication.java` 加 `@EnableScheduling`
2. 新建 `backend/src/main/java/com/harry/aifrontier/scheduler/ArxivFetchScheduler.java`
   - `@Component` + `@Slf4j`
   - 注入 `ArxivService`、`ContentService`、`ContentExternalRefMapper`
   - 核心方法 `@Scheduled(cron = "0 0 3 * * ?")`（每天凌晨3点）
   - 遍历关键词列表：
     - `low-dose CT`, `CT denoising`, `medical image denoising`, `diffusion model for CT`, `sparse-view CT`, `3D CT reconstruction`, `CT artifact reduction`, `noise reduction medical imaging`
   - 每个关键词调 `arxivService.searchPapers(keyword, 10, 0)`
   - 对每个结果，查 `ai_content_external_ref` 表是否存在 `ref_type='arxiv_paper' AND external_id=arxivId`
   - 不存在则构建 `ArxivPaperImportRequest`（publishStatus="DRAFT"），调 `contentService.importArxivPaper()`
   - 记录日志到 ai_fetch_log 表
3. 新建 `backend/src/main/java/com/harry/aifrontier/scheduler/FetchLogService.java`
   - 提供 `log(String source, String keyword, int found, int imported, String status, String message)` 方法
4. 新建 `backend/src/main/java/com/harry/aifrontier/entity/FetchLog.java`（MyBatis-Plus Entity）
5. 新建 `backend/src/main/java/com/harry/aifrontier/mapper/FetchLogMapper.java`
6. 新建 SQL 迁移文件 `database/add-fetch-log-table.sql`：
   ```sql
   CREATE TABLE IF NOT EXISTS ai_fetch_log (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     source VARCHAR(50) NOT NULL COMMENT '来源: arxiv/huggingface/github/rss',
     keyword VARCHAR(200) DEFAULT NULL COMMENT '搜索关键词',
     papers_found INT DEFAULT 0 COMMENT '抓到条数',
     papers_imported INT DEFAULT 0 COMMENT '新入库条数',
     status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
     error_message TEXT DEFAULT NULL COMMENT '失败原因',
     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     KEY idx_fetch_log_source (source),
     KEY idx_fetch_log_created (created_at)
   ) COMMENT='采集日志表';
   ```
7. 验证：`mvn -q -DskipTests compile`

**关键注意**：
- 去重逻辑：先查 ai_content_external_ref 是否已有该 arxiv_id
- 每个关键词搜索间隔加 `Thread.sleep(3000)` 避免 arXiv API 限流
- 入库状态必须是 DRAFT，需要人工审核后才发布
- categoryId 需要硬编码一个默认值（或从数据库查"论文"分类的 ID）
- 失败不能影响其他关键词的采集，try-catch 包住每个关键词

---

### Agent 2：HuggingFace Daily Papers 定时采集器

**职责**：新建定时任务，每天自动拉取 HF Daily Papers 并入库。

**要做的事**：

1. 新建 `backend/src/main/java/com/harry/aifrontier/scheduler/HuggingFaceFetchScheduler.java`
   - `@Component` + `@Slf4j`
   - 注入 `HuggingFaceService`、`ContentService`、`ContentExternalRefMapper`、`FetchLogService`
   - `@Scheduled(cron = "0 0 4 * * ?")`（每天凌晨4点，错开 arXiv 的3点）
   - 调 `huggingFaceService.getDailyPapers()`
   - 过滤：只保留 title 或 abstract 包含 CV / medical / imaging / denoising / CT 等关键词的论文
   - 去重：查 `ai_content_external_ref` 的 `ref_type='huggingface_paper' AND external_id=paperId`
   - 构建 `HuggingFacePaperImportRequest`（publishStatus="DRAFT"），调 `contentService.importHuggingFacePaper()`
   - 记录日志到 ai_fetch_log
2. HF 关键词过滤列表（作为配置常量）：
   - `medical`, `imaging`, `CT`, `denoising`, `segmentation`, `reconstruction`, `diffusion`, `radiology`, `pathology`, `3D`
3. 验证：`mvn -q -DskipTests compile`

**关键注意**：
- HF API 不需要 key，但要注意请求频率
- HF Daily Papers 可能返回几十篇，只过滤相关领域
- 去重同理，查 external_ref 表
- 与 Agent 1 共享 FetchLogService（Agent 1 负责创建，这里直接用）

---

### Agent 3：采集日志表 + Dashboard 展示 + 前端手动触发

**职责**：建表 + 后台 Dashboard 展示采集状态 + 提供手动触发采集的 API。

**要做的事**：

1. 执行 `database/add-fetch-log-table.sql`（如果 Agent 1 已创建，直接用）
2. 如果 Agent 1 还没创建 FetchLog 实体，自己创建（同上定义）
3. DashboardServiceImpl 加采集日志统计：
   - 最近 7 天采集记录
   - 各来源采集成功率
   - 最近一次采集时间和状态
4. DashboardOverviewVO 增加 `fetchLogStats` 字段
5. 新建 `FetchAdminController.java`（或在现有 controller 加接口）：
   - `GET /api/v1/admin/fetch/logs` — 查询采集日志列表（分页）
   - `POST /api/v1/admin/fetch/trigger/arxiv` — 手动触发一次 arXiv 采集
   - `POST /api/v1/admin/fetch/trigger/huggingface` — 手动触发一次 HF 采集
6. 验证：`mvn -q -DskipTests compile`

**关键注意**：
- 手动触发和定时任务调用同一个方法，不重复写逻辑
- Dashboard 展示的数据结构需要和前端对齐
- 注意与 Agent 1 的 FetchLog 实体/表结构保持一致

---

## 文件变更汇总

### 新建文件
- `backend/src/main/java/com/harry/aifrontier/scheduler/ArxivFetchScheduler.java`
- `backend/src/main/java/com/harry/aifrontier/scheduler/HuggingFaceFetchScheduler.java`
- `backend/src/main/java/com/harry/aifrontier/scheduler/FetchLogService.java`
- `backend/src/main/java/com/harry/aifrontier/entity/FetchLog.java`
- `backend/src/main/java/com/harry/aifrontier/mapper/FetchLogMapper.java`
- `backend/src/main/java/com/harry/aifrontier/controller/admin/FetchAdminController.java`
- `database/add-fetch-log-table.sql`

### 修改文件
- `backend/src/main/java/com/harry/aifrontier/AiFrontierApplication.java` — 加 @EnableScheduling
- `backend/src/main/java/com/harry/aifrontier/service/impl/DashboardServiceImpl.java` — 加采集统计
- `backend/src/main/java/com/harry/aifrontier/vo/DashboardOverviewVO.java` — 加 fetchLogStats 字段

---

## 验证清单

- [ ] `mvn -q -DskipTests compile` 编译通过
- [ ] 手动调 `/api/v1/admin/fetch/trigger/arxiv` 能抓到论文
- [ ] 重复抓取同一篇论文不会重复入库（去重生效）
- [ ] 采集日志记录正常
- [ ] Dashboard 能看到采集统计
- [ ] 入库的论文状态为 DRAFT

---

## 风险和对策

| 风险 | 对策 |
|------|------|
| arXiv API 限流 | 每个关键词间隔 3 秒，总关键词 8 个 ≈ 24 秒完成 |
| 关键词搜索结果重复 | 用 HashSet<arxivId> 去重后再入库 |
| categoryId 硬编码 | 从数据库查"论文"分类，找不到就用第一个分类 |
| 三个 Agent 改同一个文件冲突 | Agent 1 创建 FetchLog 实体和表，Agent 2 直接引用，Agent 3 做 Dashboard 集成 |
| Spring @Scheduled 应用启动就跑 | 加 `initialDelay` 或用 `fixedDelayString` 配合配置文件 |
