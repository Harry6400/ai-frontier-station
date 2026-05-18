# 计划：全面去掉草稿机制 + 内容直接发布

> 更新时间：2026-05-17
> 用户需求：
> 1. 所有内容直接发布，没有草稿概念
> 2. 已有的 64 篇 DRAFT 内容全部改为 PUBLISHED
> 3. 论文内容应该是中文（重要专有名词保留英文）
> 4. 不需要人工审核

---

## 现状

- ai_content 表有 64 篇 DRAFT、7 篇 PUBLISHED
- 前台只显示 PUBLISHED 内容（Portal 代码已有过滤）
- 后台有"转草稿"按钮、状态筛选、DRAFT 默认值等 UI
- 已入库论文摘要全部是英文（arXiv/HF 原文）

---

## 改动计划

### 第一步：数据库（SQL）

```sql
-- 把所有 DRAFT 内容改为 PUBLISHED，补上 published_at
UPDATE ai_content
SET publish_status = 'PUBLISHED',
    published_at = COALESCE(published_at, NOW())
WHERE publish_status = 'DRAFT';
```

### 第二步：后端 — 采集器默认 PUBLISHED

| 文件 | 行 | 改动 |
|------|-----|------|
| `ArxivFetchScheduler.java` | 151 | `"DRAFT"` → `"PUBLISHED"` |
| `HuggingFaceFetchScheduler.java` | 156 | `"DRAFT"` → `"PUBLISHED"` |
| `ContentServiceImpl.java` | 693 | `"DRAFT"` → `"PUBLISHED"`（AI 来源整理） |

### 第三步：后端 — 去掉 DRAFT 选项

文件: `ContentServiceImpl.java`
- 行 249: 删除 `new OptionVO("草稿", "DRAFT")`，publishStatuses 只保留 PUBLISHED 和 ARCHIVED
- `updateStatus` 方法保持不变（仍可用于归档/恢复）

### 第四步：后台前端 — ContentManageView.vue

改动点：
1. **表单默认值**（行 206, 243, 268, 285, 621, 655, 843, 1136, 1220）：`'DRAFT'` → `'PUBLISHED'`
2. **"转草稿"按钮**（行 1365-1372）：删除整个"转草稿"按钮
3. **"草稿"提示文案**（行 812）：`'请先生成 AI 导读，再创建草稿'` → 改为合适的提示
4. **"已创建为草稿"文案**（行 817）：`'AI 来源整理内容已创建为草稿'` → `'AI 来源整理内容已发布'`
5. **publishStatus 表单字段**：保留但隐藏，不从表单中删除（避免影响后端校验）
6. **状态筛选器**：保留（可用于筛选 PUBLISHED/ARCHIVED）

### 第五步：后台前端 — DashboardView.vue

- 行 27: 删除"已发布"统计卡片中的 `note: '前台可见内容'` 或保留（反正所有内容都是已发布）
- 行 83-85: `getStatusType` 函数简化（永远返回 success）
- 行 232: 状态标签保留但始终显示 PUBLISHED

### 第六步：中文内容要求（记录备忘）

当前已入库的 71 篇内容摘要仍是英文。这需要 Phase B（AI 内化管线）来解决：
- Phase B 实现时，prompt 必须要求"用中文撰写，仅保留不可替代的英文专有名词"
- 对已有内容可以做一次批量重新总结

本次不改已有内容的文本（那是 Phase B 的事）。

---

## 不改的地方

- `publish_status` 数据库列保留（未来可能用 ARCHIVED）
- Portal 代码不变（已经只查 PUBLISHED）
- `updateStatus` 接口保留（可用于归档）
- 前台前端不变
- `resolvePublishedAt()` 逻辑不变

---

## 验证

1. `mvn -q -DskipTests compile` 通过
2. `npm run build`（frontend-admin）通过
3. `npm run build`（frontend-portal）通过
4. `SELECT publish_status, COUNT(*) FROM ai_content GROUP BY publish_status;` 结果只有 PUBLISHED
5. 刷新前台 http://localhost:5173，所有 71 篇内容可见
6. 后台内容列表不再有"转草稿"按钮
