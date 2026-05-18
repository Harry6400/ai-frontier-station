# 阶段 25：多 Agent 协作入口记录

## 1. 本阶段目标

本阶段目标是让 Codex、OpenCode 和后续可能使用的其他 coding agent 都能通过同一份入口文档接管项目，避免每次换工具都重新解释项目结构、启动命令、安全边界和开发规则。

## 2. 本阶段完成内容

- 已在主仓库根目录新增 `AGENTS.md`。
- 已在 README 的文档入口区提示：OpenCode / Codex / 其他 agent 接管时先读 `AGENTS.md`。
- 已明确 agent 的优先阅读顺序，避免一上来全量扫描 `node_modules`、`dist`、`target` 等无效目录。
- 已把启动命令、构建验证、数据库初始化、API Key 规则、安全边界和后续优先级写入 `AGENTS.md`。
- 本阶段完成后会同步到课程镜像目录，保证主仓库和课堂目录说明一致。

## 3. 为什么需要 AGENTS.md

README 面向 GitHub 访客、老师和申请评审；`docs-learning` 面向学习、答辩和阶段复盘；`AGENTS.md` 面向 coding agent。

它的作用是：

- 快速告诉 agent 项目是什么、怎么运行、哪些文件先读。
- 固化不能踩的坑，比如不要提交 API Key、不要绕过 DOMPurify、不要随便执行 `init-data.sql`。
- 让多个 agent 共同维护项目时遵守同一套边界。
- 节省上下文 token，避免每次都把整套项目背景重新说一遍。

## 4. OpenCode 接管时应该怎么说

可以直接复制这段给 OpenCode：

```text
请先阅读当前项目根目录的 AGENTS.md，再阅读 README.md 和 docs-learning/00-项目总控与长期进度.md。

这是一个 Vue 3 + Spring Boot 3 + MySQL 的 AI 信息聚合平台，当前重点是长期维护和继续扩展。请不要先改代码，先总结你理解到的项目结构、当前完成度、运行命令、风险点和下一步建议。

如果要继续开发，请优先遵守 AGENTS.md 里的协作规则，并在每个阶段结束后更新 docs-learning 文档。不要提交 .env、真实 API Key、Token、node_modules、dist、target 或临时文件。
```

## 5. 后续协作建议

- Codex 更适合持续推进实现、验证、提交和解释。
- OpenCode 可以作为第二维护者，用来做代码阅读、设计复核、局部实现和开源仓库整理。
- 两个工具都应该以主仓库为准：`/Users/harry64/Documents/proj2/AI前沿情报站`。
- 课程镜像目录只作为备份和课堂提交目录，不应该成为长期开发的权威来源。

## 6. 当前遗留问题

- `AGENTS.md` 只能约束工具协作，不能替代真实 CI、测试和权限系统。
- 当前项目仍缺少后台登录鉴权，后续上线前必须补。
- 当前没有自动化 CI，后续可以加 GitHub Actions 运行前后端构建检查。

## 7. 下一阶段建议

下一阶段建议做“MiMo API Provider 适配设计与最小实现”。现在项目已经有百炼 AI 总结层、API 设置页和申请材料，再加入 MiMo Provider，可以让 MiMo 申请从“计划使用”升级为“具备工程接入入口”。
