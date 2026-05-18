# 计划：去掉审核，采集内容直接发布

> 目标：定时采集的论文自动以 PUBLISHED 状态入库，前台立即可见。
> 背景：这是个人网站，不需要人工审核环节。

---

## 现状分析

当前数据流：
  定时采集器 → setPublishStatus("DRAFT") → ContentServiceImpl.importXxx()
  → 入库为 DRAFT → 前台不可见 → 需要手动在后台改状态为 PUBLISHED

目标数据流：
  定时采集器 → setPublishStatus("PUBLISHED") → ContentServiceImpl.importXxx()
  → 入库为 PUBLISHED → 前台立即可见

关键发现：
  - `ContentServiceImpl.resolvePublishedAt()` 已有逻辑：status=PUBLISHED 且 publishedAt=null 时自动设为当前时间
  - Portal 查询全部过滤 `publish_status = 'PUBLISHED'`，不需要改
  - 前台、后台前端都不需要改
  - 审核队列功能尚未实现，不需要删除

---

## 需要改的地方（共 3 个文件，3 行代码）

### 1. ArxivFetchScheduler.java
文件: `backend/src/main/java/com/harry/aifrontier/scheduler/ArxivFetchScheduler.java`
行 151: `request.setPublishStatus("DRAFT")` → `request.setPublishStatus("PUBLISHED")`

### 2. HuggingFaceFetchScheduler.java
文件: `backend/src/main/java/com/harry/aifrontier/scheduler/HuggingFaceFetchScheduler.java`
行 156: `request.setPublishStatus("DRAFT")` → `request.setPublishStatus("PUBLISHED")`

### 3. ContentServiceImpl.java（AI 来源整理导入）
文件: `backend/src/main/java/com/harry/aifrontier/service/impl/ContentServiceImpl.java`
行 693: `contentRequest.setPublishStatus("DRAFT")` → `contentRequest.setPublishStatus("PUBLISHED")`

### 不需要改的地方
- PortalContentController — 已经只查 PUBLISHED
- ContentServiceImpl.portalDetail/portalHome/portalPage — 已正确过滤
- resolvePublishedAt() — 已自动处理 publishedAt
- 前台前端 — 不涉及状态逻辑
- 后台前端 — 手动创建内容时的 DRAFT 默认值保留（手动创建仍可选状态）
- 数据库 — 无 schema 变更

---

## 验证

1. `mvn -q -DskipTests compile` 编译通过
2. 手动触发 `POST /api/v1/admin/fetch/trigger/huggingface`
3. 查数据库：`SELECT publish_status, published_at FROM ai_content ORDER BY id DESC LIMIT 5` 确认新入库内容为 PUBLISHED 且 published_at 不为 NULL
4. 刷新前台 http://localhost:5173，新论文应立即可见

---

## 风险

- **低风险**：改动极小（3 行），不涉及数据库 schema 变更
- **已知取舍**：AI 生成的摘要质量未经人工把关就直接展示。后续可通过 Phase B 的 AI 总结管线提升质量
- **不影响手动创建**：后台手动创建内容时仍可选择 DRAFT/PUBLISHED 状态
