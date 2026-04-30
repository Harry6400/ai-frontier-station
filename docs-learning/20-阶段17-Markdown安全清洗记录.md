# 阶段 17：Markdown 安全清洗记录

## 1. 本阶段目标

本阶段给前台详情页和后台正文预览补充 Markdown 安全清洗，避免未来接入 GitHub、论文、官方博客和社区内容时，外部 Markdown 中夹带危险 HTML 或脚本。

本阶段不改数据库，不改后端接口，不清洗入库内容。数据库继续保存原始 Markdown，安全边界放在前端展示层。

## 2. 为什么现在做安全清洗

项目已经完成 GitHub 项目手动导入 Demo，说明外部内容源开始进入平台。

如果后续接入 GitHub README、论文摘要或社区文章，这些内容不应该直接通过 `v-html` 渲染。否则可能出现 XSS 风险，例如：

```html
<script>alert(1)</script>
<img src=x onerror=alert(1)>
```

所以现在先补安全清洗，比继续加采集功能更稳。

## 3. 本阶段涉及的技术点

- Markdown 渲染
- DOMPurify HTML 清洗
- Vue `v-html` 的安全边界
- 白名单标签和属性
- 外部链接安全属性
- 前台正式展示和后台预览的差异化处理

## 4. 当前代码结构怎么理解

前台新增：

```text
frontend-portal/src/utils/safeMarkdown.js
```

后台新增：

```text
frontend-admin/src/utils/safeMarkdown.js
```

前台详情页：

```text
frontend-portal/src/views/ContentDetailView.vue
```

后台内容管理页：

```text
frontend-admin/src/views/ContentManageView.vue
```

## 5. 为什么安全边界放在前端展示层

数据库保存的是内容原文，也就是原始 Markdown。这样做有三个好处：

- 后续如果要重新渲染或换渲染器，不会丢失原始内容。
- 如果接入采集模块，可以保留外部来源原文快照。
- 安全策略可以随着前端展示需求调整，不需要反复迁移数据库。

真正危险的是把 Markdown 渲染成 HTML 后再塞进 `v-html`。所以本阶段在渲染前做 DOMPurify 清洗。

## 6. 前台怎么处理

前台详情页之前是：

```js
marked.parse(detail.value?.bodyMarkdown || '')
```

现在改为：

```js
renderSafeMarkdown(detail.value?.bodyMarkdown || '')
```

`renderSafeMarkdown()` 内部流程是：

1. 用 `marked` 把 Markdown 转成 HTML。
2. 用 DOMPurify 按白名单清洗 HTML。
3. 给外部链接补 `target="_blank"` 和 `rel="noopener noreferrer"`。
4. 返回安全 HTML 给 `v-html`。

## 7. 后台怎么处理

后台正文预览原本是轻量 Markdown 渲染器，并且已经对用户输入做了 HTML 转义。

本阶段没有给后台引入 `marked`，避免扩大后台包体和复杂度。后台做法是：

1. 保留现有轻量预览逻辑。
2. 把生成的预览 HTML 再交给 DOMPurify 清洗。
3. 作为后台预览的最后一道安全兜底。

这样后台既保持原有预览效果，也不会因为预览区使用 `v-html` 而留下风险。

## 8. 白名单规则

前台允许常用 Markdown 标签：

```text
h1-h4、p、br、strong、em、code、pre、ul、ol、li、blockquote、a、table、thead、tbody、tr、th、td
```

允许属性：

```text
href、title、target、rel
```

禁止：

```text
script、iframe、style、form、input、button、textarea、select、option
style、class、id、onerror、onclick 等事件属性
```

## 9. 老师可能会怎么问

### 问：为什么不能直接渲染 Markdown？

可以回答：Markdown 渲染后本质上会变成 HTML，而 `v-html` 会把 HTML 插入页面。如果外部内容包含脚本或事件属性，就可能造成 XSS，所以必须先清洗。

### 问：为什么不在后端入库前清洗？

可以回答：我们希望保留原始 Markdown，后续可能用于重新渲染、采集溯源或调试。安全风险发生在展示层，所以当前阶段选择展示前清洗。

### 问：为什么用 DOMPurify？

可以回答：DOMPurify 是成熟的 HTML 清洗库，适合处理用户生成内容和外部 Markdown。比自己手写正则更可靠，也更符合真实项目实践。

### 问：后台为什么不直接用 marked？

可以回答：后台预览目前只需要轻量效果，而且已经有简单渲染函数。为了不扩大后台包体，本阶段只给后台加 DOMPurify 清洗兜底。

## 10. 本阶段验收结果

- 前台已新增 `dompurify`
- 后台已新增 `dompurify`
- 前台详情页已改为安全 Markdown 渲染
- 后台正文预览已增加 DOMPurify 清洗兜底
- 前台构建通过
- 后台构建通过，仍无 500KB chunk 警告
- 后端编译通过

## 11. 包体变化说明

DOMPurify 属于安全能力依赖，会带来少量包体增加：

- 前台主 JS 从约 207KB 增加到约 233KB
- 后台 `vendor` chunk 从约 180KB 增加到约 205KB
- 后台最大 chunk 仍然是 Element Plus 约 341KB，没有重新出现 500KB 警告

这个变化属于合理安全成本。

## 12. 下一阶段建议

下一阶段建议做 Dashboard 统计增强。

原因是当前内容、来源、标签、外部引用、GitHub 手动导入和安全清洗都已经具备，后台首页最适合升级成真正的“内容运营控制台”，展示内容数量、发布状态、来源分布、热门内容和近期导入记录。
