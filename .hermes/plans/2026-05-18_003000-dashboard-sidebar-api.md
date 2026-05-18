# 计划：Dashboard 精简 + 侧边栏折叠 + API 测试

> 日期：2026-05-18

---

## 任务 1：Dashboard 精简

### 删除"已发布"统计卡片
文件: `frontend-admin/src/views/DashboardView.vue`
- 行 27: 删除 `{ label: '已发布', value: ..., note: ..., tone: 'success' }`

### 删除"发布状态分布"模块
文件: `frontend-admin/src/views/DashboardView.vue`
- 行 138-159: 删除整个"发布状态分布" section（含 dashboard-bar-list）
- 相关的 `publishStatusStats` 数据和 `findRatio` 调用也可清理

---

## 任务 2：侧边栏可折叠

### 现状
- `App.vue` 行 85: `<aside class="admin-sidebar">` 固定 276px 宽度
- `admin.css` 行 162: `grid-template-columns: 276px minmax(0, 1fr);`
- 无折叠状态管理

### 方案
1. `App.vue` 加 `isSidebarCollapsed` ref
2. 侧边栏顶部加折叠按钮（汉堡图标 / 箭头）
3. 折叠时：
   - 侧边栏宽度从 276px → 64px（只显示图标）
   - 文字隐藏，只保留导航图标点
   - 品牌标题/描述隐藏
   - 底部 footnote 隐藏
4. CSS 用 `transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1)` 实现流畅动画
5. `.admin-shell` 的 grid-template-columns 动态绑定

### 改动文件
- `frontend-admin/src/App.vue` — 加折叠逻辑 + 按钮 + 动态 class
- `frontend-admin/src/styles/admin.css` — 加折叠态样式 + 过渡动画

---

## 任务 3：测试 3 个 API

后端已运行在 localhost:8080。直接调接口测试：

1. **百炼 API**: `POST /api/v1/admin/ai/source-summary` 用 provider=bailian
2. **MiMo API**: `POST /api/v1/admin/ai/source-summary` 用 provider=mimo
3. **GitHub API**: `GET /api/v1/admin/github/search?keyword=llm`

每个测试验证返回 200 + 有效数据。

---

## 验证

- Dashboard 不再显示"已发布"卡片和"发布状态分布"
- 侧边栏点击按钮可折叠/展开，动画流畅
- 3 个 API 测试全部通过
- `npm run build` 通过
