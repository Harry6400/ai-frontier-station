# 阶段22：后台 API 设置与 GitHub API 接入记录

## 1. 本阶段目标

本阶段解决两个问题：

1. 不再要求每次都在终端输入百炼或 GitHub 的环境变量。
2. 把 GitHub 项目导入从“手动填写 Demo”升级为“真实 GitHub API 查询/搜索后回填导入”。

第一版仍然保持安全边界：API Key / Token 只保存在后端运行内存中，不写数据库、不回显完整密钥、不写入日志。后端重启后需要重新配置，适合课程演示和后续扩展。

## 2. 本阶段涉及的技术点

- Spring Boot 运行时内存配置
- 后端密钥不落库与掩码展示
- Vue 后台设置页
- GitHub REST API 仓库查询与搜索
- 可选 Token 策略
- 外部 API 错误兜底
- 继续复用 `ai_content`、`ai_content_external_ref`、`extra_json`

## 3. 当前代码结构怎么理解

后端新增 API 设置能力：

- `ApiCredentialService`：定义运行时密钥管理能力
- `ApiCredentialServiceImpl`：使用内存 Map 保存百炼 Key 和 GitHub Token
- `ApiSettingsAdminController`：提供后台 API 设置接口
- `ApiCredentialStatusVO` / `ApiSettingsStatusVO`：只返回启用状态、掩码后缀和来源，不返回完整密钥

后端新增 GitHub 能力：

- `GitHubService`：定义仓库查询和搜索能力
- `GitHubServiceImpl`：调用 GitHub REST API
- `GitHubAdminController`：提供后台 GitHub 查询接口
- `GitHubRepoCandidateVO`：前端候选仓库展示与回填使用

后台新增页面：

- `frontend-admin/src/views/ApiSettingsView.vue`
- `frontend-admin/src/router/index.js` 新增 `/api-settings`
- `frontend-admin/src/App.vue` 左侧导航新增“API 设置”

后台 GitHub 导入弹窗也被增强：

- 支持按 `owner/repo` 查询仓库
- 支持关键词搜索仓库候选
- 点击候选仓库后回填已有导入表单
- 导入仍然创建草稿内容和 `github_repo` 外部引用

## 4. 为什么这样设计

API Key 属于敏感信息，不能写死在前端，也不适合第一版明文落库。本阶段选择“后端内存保存”是一个安全与实现复杂度之间的折中：

- 比写死在代码里安全
- 比浏览器本地保存安全
- 不需要新增数据库表
- 重启后失效，适合课堂演示
- 后续可以平滑升级成加密落库和管理员权限

GitHub Token 设计成可选，是因为公开仓库匿名也能查。配置 Token 后额度更高，演示更稳定；不配置 Token 时也不影响基础公开查询。

## 5. 关键接口

API 设置接口：

- `GET /api/v1/admin/api-settings/status`
- `PUT /api/v1/admin/api-settings/bailian`
- `DELETE /api/v1/admin/api-settings/bailian`
- `PUT /api/v1/admin/api-settings/github`
- `DELETE /api/v1/admin/api-settings/github`

GitHub 接口：

- `GET /api/v1/admin/github/repo?fullName=openai/openai-cookbook`
- `GET /api/v1/admin/github/search?keyword=llm agent&sort=stars&pageNum=1&pageSize=10`

这些接口都属于后台管理接口，第一版没有登录鉴权。答辩时需要说明：后续如果做用户系统，API 设置页必须限制为管理员可访问。

## 6. 常见误区

- 不要把 API Key 放到 Vue 前端代码里。
- 不要在接口响应中返回完整密钥。
- 不要把 GitHub API 查询等同于自动采集，当前仍是“查询后人工确认导入”。
- 不要保存 README 全文，第一版只取摘要信息辅助判断。
- 不要把 GitHub Token 作为强依赖，不配置 Token 也应该能查询公开仓库。

## 7. 老师可能会怎么问

**问：密钥为什么不存数据库？**

答：第一版没有登录和权限系统，如果把密钥明文存数据库反而有安全隐患。现在只保存在后端内存，重启后失效，更适合课程演示。后续可以扩展为管理员权限 + 加密存储。

**问：为什么 GitHub Token 是可选的？**

答：GitHub 公开仓库可以匿名查询，但匿名请求额度低。Token 可选能降低使用门槛，同时在演示或后续维护时提高稳定性。

**问：GitHub API 接入后，和之前手动导入有什么区别？**

答：之前是手动模拟仓库数据；现在可以从 GitHub 获取真实仓库信息，再由管理员确认导入，数据来源更真实，也更接近未来自动采集模块。

**问：为什么不自动发布 GitHub 导入内容？**

答：外部数据进入平台后仍需要人工审核，尤其是项目简介、标签、分类和展示标题。默认草稿可以避免未经确认的内容直接出现在前台。

## 8. 后续如何扩展

下一步建议二选一：

1. GitHub 项目热度同步任务：根据 `github_repo` 外部引用定期刷新 Star、语言、更新时间等字段。
2. 百炼联网搜索一手来源工作流：按指定主题搜索官方动态、论文、项目和社区实践，再进入 AI 来源整理工作台。

如果优先课堂稳定演示，建议先做 GitHub 热度同步；如果优先长期愿景，建议规划百炼联网搜索。
