# 阶段 18：Dashboard 统计增强记录

## 1. 本阶段目标

本阶段把后台 Dashboard 从“前端临时拼统计”的早期版本，升级为由后端统一提供统计口径的内容运营控制台。

本阶段不新增数据库表，不引入图表库，不影响内容 CRUD、GitHub 手动导入、Markdown 安全清洗、后台深色模式和包体优化。

## 2. 为什么现在做 Dashboard 统计增强

项目已经具备内容管理、分类、标签、来源、外部引用、GitHub 手动导入和 Markdown 安全清洗。此时后台首页如果仍然只是几个简单数字，就不能很好地体现“这是一个完整系统”。

Dashboard 增强后，答辩时可以先从后台首页讲：

- 当前平台有多少内容
- 哪些内容已发布
- 内容类型如何分布
- 来源类型如何分布
- 最近有哪些内容更新
- 最近有哪些外部引用进入系统

这比直接打开 CRUD 页面更像真实内容运营平台。

## 3. 本阶段涉及的技术点

- Spring Boot 新增统计接口
- Service 层聚合统计数据
- MyBatis-Plus 查询内容、来源、外部引用
- Vue 后台 Dashboard 一次请求加载统计总览
- CSS 进度条和列表卡片实现轻量可视化
- 前后端统一统计口径

## 4. 当前代码结构怎么理解

后端新增：

```text
backend/src/main/java/com/harry/aifrontier/
├─ controller/admin/DashboardAdminController.java
├─ service/DashboardService.java
├─ service/impl/DashboardServiceImpl.java
└─ vo/DashboardOverviewVO.java
```

后台更新：

```text
frontend-admin/src/api/admin.js
frontend-admin/src/views/DashboardView.vue
frontend-admin/src/styles/admin.css
```

## 5. 为什么统计接口放在后端

之前 Dashboard 是前端同时请求分类、标签、来源和内容列表，然后在浏览器里临时计算。这种方式早期方便，但后续会有问题：

- 前端请求次数多
- 统计口径分散
- 后续加权限或数据范围时不好控制
- 内容列表分页后，前端拿到的可能不是全量数据

现在改为后端提供 `GET /api/v1/admin/dashboard/overview`，由后端统一计算统计数据。这样口径更稳定，也更像真实系统。

## 6. 接口返回什么

新增接口：

```text
GET /api/v1/admin/dashboard/overview
```

返回内容包括：

- `totals`：内容、已发布、草稿、归档、分类、标签、来源、外部引用数量
- `publishStatusStats`：发布状态分布
- `contentTypeStats`：内容类型分布
- `sourceTypeStats`：来源类型分布
- `recentContents`：最近 6 条内容
- `recentExternalRefs`：最近 5 条外部引用

## 7. Dashboard 页面怎么展示

后台首页现在展示：

- 顶部关键指标卡
- 发布状态分布进度条
- 运营信号卡片
- 内容类型分布
- 来源类型分布
- 近期内容列表
- 外部引用动态列表

没有引入 ECharts 等图表库，而是用 CSS 进度条和卡片完成轻量可视化。这样能保持后台包体稳定，也更容易讲清楚实现。

## 8. 老师可能会怎么问

### 问：为什么不继续在前端统计？

可以回答：前端统计适合早期 Demo，但真实系统应该由后端统一统计口径。特别是内容列表有分页后，前端拿到的不是全量数据，统计会不准确。

### 问：为什么不加图表库？

可以回答：当前阶段数据量和展示需求不复杂，用 CSS 进度条和卡片就能表达清楚。引入图表库会增加包体和学习成本，后续真正需要复杂图表时再加。

### 问：Dashboard 体现了哪些系统能力？

可以回答：它把内容、发布状态、来源、外部引用这些核心业务对象聚合在一起，说明后台不是孤立 CRUD，而是内容运营控制台。

### 问：后续怎么扩展？

可以回答：后续可以增加阅读趋势、热门内容排行、GitHub 导入次数、来源质量评估、最近采集任务状态等统计。

## 9. 本阶段验收结果

- 后端新增 Dashboard 统计接口
- 后台 Dashboard 已接入新接口
- 接口真实返回统计数据
- 后台首页已展示分布、近期内容和外部引用动态
- 前台构建通过
- 后台构建通过，仍无 500KB chunk 警告
- 后端编译通过

## 10. 下一阶段建议

下一阶段建议做“前台内容发现页搜索体验增强”。

原因是后台运营能力已经更完整，接下来可以回到用户侧，把内容广场的搜索、筛选、排序、结果反馈做得更像真正的信息发现产品。
