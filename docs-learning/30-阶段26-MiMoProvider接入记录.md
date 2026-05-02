# 阶段 26：MiMo Provider 接入记录

## 阶段目标

把 MiMo（mimo-v2.5-pro）作为新的可选 AI Provider 接入现有百炼 AI 来源整理流程，让用户可以在后台 API 设置页配置 MiMo Key，在 AI 来源整理时选择使用百炼或 MiMo。

## 背景

项目当前的 AI 总结层只支持阿里百炼 / DashScope 一个 Provider。MiMo 是小米推出的 AI 模型，使用 OpenAI 兼容 API 格式。由于百炼和 MiMo 都走 `/chat/completions` 接口，可以在不改变核心业务流程的前提下，通过路由不同 Provider 的 base URL、model 和 API Key 来实现多 Provider 支持。

MiMo Token Plan 中国区 Standard 套餐为临时额度，有效期到 2026-06-01 UTC。项目设计不强依赖 MiMo，后续套餐到期后可能替换或移除。

## 设计决策

### 1. 不引入复杂 Provider 工厂模式

当前只有百炼和 MiMo 两个 Provider，直接在 `AiSummaryServiceImpl` 中用 if-else 路由即可。如果后续接入更多 Provider（如 OpenAI、Claude），再考虑抽象为策略模式。

### 2. MiMo 和百炼共用同一套调用逻辑

两个 Provider 都是 OpenAI 兼容格式（`POST /chat/completions`），区别只在：
- base URL
- model 名称
- API Key

因此提取了公共的请求构建和响应解析逻辑。

### 3. Provider 选择放在请求层而非配置层

`AiSourceSummaryRequest` 新增 `provider` 字段，由前端在发起 AI 来源整理时指定。这比在环境变量中切换默认 Provider 更灵活——用户可以对同一来源尝试不同模型。

### 4. MiMo 配置同样存内存

和百炼一样，MiMo API Key 只保存在后端运行内存中，不落库、不回显完整密钥。这是课程阶段的刻意设计。

### 5. 默认 Provider 仍为百炼

如果请求中不传 `provider` 字段，默认使用百炼。MiMo 作为可选路径，不影响已有流程。

## 涉及文件

### 后端

| 文件 | 改动 |
|---|---|
| `backend/src/main/resources/application.yml` | 新增 `app.mimo` 配置块（api-key、base-url、model） |
| `backend/.../service/ApiCredentialService.java` | 新增 `PROVIDER_MIMO` 常量和 `resolveMimoApiKey()` 方法 |
| `backend/.../service/impl/ApiCredentialServiceImpl.java` | 新增 MiMo 逻辑、`mimoFallbackKey` 字段、`validateProvider` 白名单扩展 |
| `backend/.../vo/ApiSettingsStatusVO.java` | 新增 `mimo` 字段 |
| `backend/.../dto/request/AiSourceSummaryRequest.java` | 新增 `provider` 字段 |
| `backend/.../service/impl/AiSummaryServiceImpl.java` | 重构为多 Provider 路由，提取公共调用逻辑 |
| `backend/.../controller/admin/ApiSettingsAdminController.java` | 新增 `PUT/DELETE /api/v1/admin/api-settings/mimo` |

### 前端

| 文件 | 改动 |
|---|---|
| `frontend-admin/src/views/ApiSettingsView.vue` | 新增 MiMo 卡片（status、forms、providers） |
| `frontend-admin/src/views/ContentManageView.vue` | AI 来源整理新增 Provider 选择器（el-radio-group） |
| `frontend-admin/src/styles/admin.css` | 新增 `.ai-provider-strip` 样式 |

### 配置与文档

| 文件 | 改动 |
|---|---|
| `.env.example` | 新增 `MIMO_API_KEY` |
| `docs-learning/30-阶段26-MiMoProvider接入记录.md` | 本文件 |
| `docs-learning/00-项目总控与长期进度.md` | 更新阶段进度 |
| `AGENTS.md` | 更新技术栈和后续优先级 |

## 验证结果

- 后端 `mvn -q -DskipTests compile`：通过
- 前台 `npm run build`：通过（238KB 主 JS）
- 后台 `npm run build`：通过（340KB element-plus chunk，无 500KB 警告）

## API 接口变化

### 新增接口

- `PUT /api/v1/admin/api-settings/mimo` — 保存 MiMo API Key
- `DELETE /api/v1/admin/api-settings/mimo` — 清除 MiMo API Key

### 修改接口

- `POST /api/v1/admin/ai/source-summary` — 请求体新增可选字段 `provider`（`"bailian"` 或 `"mimo"`，默认 `"bailian"`）

### 返回变化

- `GET /api/v1/admin/api-settings/status` — 返回体新增 `mimo` 字段
- AI 总结结果的 `extraJson` 中 `aiProvider` 字段会根据实际使用的 Provider 写入 `"MiMo"` 或 `"Alibaba Bailian / DashScope"`

## 本地验证方法

1. 设置环境变量 `MIMO_API_KEY`（或在后台 API 设置页配置）
2. 启动后端 `mvn spring-boot:run`
3. 启动后台 `npm run dev`
4. 打开 API 设置页，确认 MiMo 卡片显示"已启用"
5. 进入内容管理 → AI 来源整理 → 选择 MiMo Provider → 填写来源信息 → 生成 AI 导读
6. 确认生成结果中 `aiProvider` 为 `"MiMo"`

## 不做的事

- 不改数据库 schema
- 不引入 Provider 工厂/策略模式
- 不让 MiMo 成为默认 Provider
- 不自动切换 Provider 或做 fallback
- 不提交真实 API Key

## 后续扩展方向

- 如果后续接入更多 Provider（OpenAI、Claude），可将当前 if-else 重构为策略模式
- MiMo Token Plan 到期后，可替换为其他 OpenAI-compatible API 或移除
- 可考虑在 AI 来源整理结果中展示 Provider 名称，方便对比不同模型效果
