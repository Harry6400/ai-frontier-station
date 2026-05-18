# PROJECT_HANDOFF.md

> AI 前沿情报站 - 项目交接文档
> 更新时间：2026-05-04

---

## 一、项目概述

AI 前沿情报站是一个面向 AI 开发者的信息聚合与精选平台，采用前后端分离架构：
- **前台门户**：Vue 3 + Vite + Vue Router + Pinia
- **后台管理**：Vue 3 + Vite + Vue Router + Pinia + Element Plus
- **后端服务**：Spring Boot 3 + MyBatis-Plus + MySQL 8
- **外部集成**：GitHub API、arXiv API、HuggingFace API、阿里百炼 AI

---

## 二、已完成的功能（按阶段）

### 阶段 1-10：基础架构
- ✅ 前后端分离工程搭建
- ✅ MySQL 数据库设计与初始化
- ✅ 内容中心 CRUD（内容、分类、标签、来源）
- ✅ 前台门户首页、内容广场、详情页
- ✅ 后台管理 Dashboard、内容管理、分类/标签/来源管理
- ✅ 浅色/深色双主题系统
- ✅ Element Plus 按需加载与路由懒加载

### 阶段 11-20：功能扩展
- ✅ 外部数据源接入准备（GitHub、arXiv、论文、公司动态）
- ✅ 外部引用关系管理（ai_content_external_ref）
- ✅ GitHub 项目手动导入
- ✅ Markdown 安全清洗（DOMPurify）
- ✅ Dashboard 统计增强
- ✅ 前台内容发现页搜索体验增强（排序、标签筛选、URL 状态保持）

### 阶段 21-26：AI 能力集成
- ✅ 阿里百炼 AI 来源整理（导读、推荐理由、标签建议）
- ✅ MiMo AI Provider 接入
- ✅ 后台 API 设置页（加密存储，重启后保留）
- ✅ GitHub API 真实查询与搜索

### 阶段 27：数据源扩展
- ✅ arXiv 论文搜索与导入
- ✅ HuggingFace 每日热门论文导入
- ✅ API Key 加密存储（AES-256-GCM）

### 阶段 28：安全加固
- ✅ Spring Security + JWT 管理员认证
- ✅ 前端登录页、路由守卫、Token 注入
- ✅ 未认证请求返回 401

### 阶段 29：HuggingFace + 登录页重设计
- ✅ HuggingFace Papers API 集成
- ✅ 登录页改为 GitHub 风格，支持深色模式

### 阶段 30：安全漏洞修复
- ✅ 移除硬编码默认密钥（API_MASTER_KEY、JWT_SECRET）
- ✅ 修复 arXiv XML 解析 XXE 漏洞
- ✅ 启动时未配置密钥则自动生成随机密钥并警告

### 阶段 31：UX 优化
- ✅ 深色模式 CSS 变量覆盖（Element Plus 全套）
- ✅ 项目说明页内容更新（移除答辩相关文案）
- ✅ GitHub 项目热度同步服务（后端 API）

### 阶段 32：核心体验优化
- ✅ 移除所有课程/答辩相关文案（6个文件，18处修改）
- ✅ 统一品牌色为 #2563EB（前台+后台）
- ✅ 前台导航栏添加全局搜索框
- ✅ 后台工具栏按钮分组（筛选+导入下拉+新建）
- ✅ 前台详情页移除开发者面向内容
- ✅ 后台编辑对话框宽度自适应（min(90vw, 1100px)）

---

## 三、修改过的关键文件

### 后端 Java 文件

| 文件路径 | 说明 |
|----------|------|
| `backend/src/main/java/com/harry/aifrontier/util/CryptoUtil.java` | AES-256-GCM 加密工具 |
| `backend/src/main/java/com/harry/aifrontier/security/JwtUtil.java` | JWT Token 生成/验证 |
| `backend/src/main/java/com/harry/aifrontier/security/JwtAuthenticationFilter.java` | JWT 认证过滤器 |
| `backend/src/main/java/com/harry/aifrontier/config/SecurityConfig.java` | Spring Security 配置 |
| `backend/src/main/java/com/harry/aifrontier/service/impl/ApiCredentialServiceImpl.java` | API 密钥加密存储 |
| `backend/src/main/java/com/harry/aifrontier/service/impl/ArxivServiceImpl.java` | arXiv API 集成 |
| `backend/src/main/java/com/harry/aifrontier/service/impl/HuggingFaceServiceImpl.java` | HuggingFace API 集成 |
| `backend/src/main/java/com/harry/aifrontier/service/impl/GitHubSyncServiceImpl.java` | GitHub 热度同步 |
| `backend/src/main/java/com/harry/aifrontier/service/impl/ContentServiceImpl.java` | 内容服务（含导入逻辑） |
| `backend/src/main/java/com/harry/aifrontier/service/impl/AuthServiceImpl.java` | 认证服务 |
| `backend/src/main/java/com/harry/aifrontier/controller/admin/AuthController.java` | 登录/登出接口 |
| `backend/src/main/java/com/harry/aifrontier/controller/admin/ArxivAdminController.java` | arXiv 接口 |
| `backend/src/main/java/com/harry/aifrontier/controller/admin/HuggingFaceAdminController.java` | HuggingFace 接口 |
| `backend/src/main/java/com/harry/aifrontier/controller/admin/GitHubSyncController.java` | GitHub 同步接口 |
| `backend/src/main/java/com/harry/aifrontier/entity/ApiCredential.java` | API 密钥实体 |
| `backend/src/main/java/com/harry/aifrontier/entity/ContentExternalRef.java` | 外部引用实体（含同步字段） |

### 前端 Vue 文件

| 文件路径 | 说明 |
|----------|------|
| `frontend-portal/src/views/HomeView.vue` | 首页（移除课程痕迹） |
| `frontend-portal/src/views/ContentListView.vue` | 内容广场 |
| `frontend-portal/src/views/ContentDetailView.vue` | 内容详情（移除开发者内容） |
| `frontend-portal/src/views/AboutView.vue` | 项目说明（重写） |
| `frontend-portal/src/components/PortalTopbar.vue` | 顶部导航（新增搜索框） |
| `frontend-admin/src/App.vue` | 后台主布局 |
| `frontend-admin/src/views/LoginView.vue` | 登录页（GitHub 风格） |
| `frontend-admin/src/views/DashboardView.vue` | Dashboard |
| `frontend-admin/src/views/ContentManageView.vue` | 内容管理（工具栏分组） |
| `frontend-admin/src/views/ApiSettingsView.vue` | API 设置 |
| `frontend-admin/src/stores/useAuthStore.js` | 认证状态管理 |
| `frontend-admin/src/api/admin.js` | 后台 API 函数 |
| `frontend-admin/src/api/auth.js` | 认证 API |
| `frontend-admin/src/api/http.js` | Axios 配置（Token 注入） |
| `frontend-admin/src/router/index.js` | 路由（含登录守卫） |

### CSS 文件

| 文件路径 | 说明 |
|----------|------|
| `frontend-portal/src/styles/global.css` | 前台样式（统一品牌色+骨架屏） |
| `frontend-admin/src/styles/admin.css` | 后台样式（深色模式变量覆盖） |

### 配置文件

| 文件路径 | 说明 |
|----------|------|
| `backend/src/main/resources/application.yml` | 后端配置（移除默认密钥） |
| `.env.example` | 环境变量模板 |
| `database/schema.sql` | 数据库表结构 |
| `database/add-api-credential-table.sql` | API 密钥表迁移 |
| `database/add-github-sync-fields.sql` | GitHub 同步字段迁移 |

---

## 四、启动方式

### 1. 数据库初始化（首次）

```bash
# 创建数据库并导入表结构
mysql -uroot -p123456 < database/schema.sql

# 初始化示例数据（可选）
mysql -uroot -p123456 ai_frontier_station < database/init-data.sql

# 添加 API 密钥表（如未执行）
mysql -uroot -p123456 < database/add-api-credential-table.sql

# 添加 GitHub 同步字段（如未执行）
mysql -uroot -p123456 < database/add-github-sync-fields.sql
```

### 2. 启动后端

```bash
cd backend
export DB_PASSWORD=123456
mvn spring-boot:run
```

### 3. 启动前台门户

```bash
cd frontend-portal
npm install  # 首次需要
npm run dev
```

### 4. 启动后台管理

```bash
cd frontend-admin
npm install  # 首次需要
npm run dev
```

---

## 五、服务端口

| 服务 | 端口 | 地址 |
|------|------|------|
| 后端 API | 8080 | http://localhost:8080 |
| 前台门户 | 5173 | http://localhost:5173 |
| 后台管理 | 5174 | http://localhost:5174 |

---

## 六、测试账号

### 后台管理员

| 字段 | 值 |
|------|------|
| 用户名 | `admin` |
| 密码 | `admin123` |
| 登录接口 | `POST /api/v1/auth/login` |

### JWT Token

- 登录成功后返回 Token
- 前端自动存储在 `localStorage` 的 `admin_token` 键中
- 有效期：24 小时
- 过期后自动跳转登录页

---

## 七、数据库配置

### 连接信息

| 字段 | 值 |
|------|------|
| 主机 | `localhost` |
| 端口 | `3306` |
| 数据库名 | `ai_frontier_station` |
| 用户名 | `root` |
| 密码 | `123456` |

### 环境变量（可选）

```bash
export DB_URL='jdbc:mysql://localhost:3306/ai_frontier_station?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='root'
export DB_PASSWORD='123456'
```

### 数据库表清单

| 表名 | 说明 |
|------|------|
| `ai_content` | 内容主表 |
| `ai_category` | 分类表 |
| `ai_tag` | 标签表 |
| `ai_source` | 来源表 |
| `ai_content_tag` | 内容-标签关联表 |
| `ai_content_external_ref` | 外部引用表（含同步字段） |
| `ai_api_credential` | API 密钥加密存储表 |
| `ai_admin_user` | 管理员用户表 |

---

## 八、当前报错与警告

### 后端警告（可忽略）

```
HikariPool-1 - Thread starvation or clock leap detected
```

**原因**：HikariCP 连接池的 housekeeper 线程检测到时钟跳变，通常是因为电脑休眠/唤醒。

**影响**：不影响功能，可忽略。

### API 密钥解密警告（首次启动）

```
解密 bailian 密钥失败: 解密失败
解密 github 密钥失败: 解密失败
解密 mimo 密钥失败: 解密失败
```

**原因**：之前用旧的加密密钥存储的 API Key，重启后主密钥变更导致解密失败。

**解决**：在后台 API 设置页重新输入密钥即可。

### JWT/API 密钥警告（开发环境）

```
WARNING: JWT_SECRET 未配置，已生成随机密钥
WARNING: API_MASTER_KEY 未配置，已生成随机密钥
```

**原因**：开发环境未设置环境变量，系统自动生成随机密钥。

**影响**：重启后密钥变更，已存储的 Token 和 API Key 失效。

**生产环境解决方案**：配置环境变量：
```bash
export JWT_SECRET=your-strong-secret-key-here
export API_MASTER_KEY=your-strong-master-key-here
```

---

## 九、下一步任务

### P1：一致性优化（建议优先）

| 任务 | 预计工作量 | 说明 |
|------|------------|------|
| 前台筛选改用 Element Plus | 2h | 原生 `<select>` 改为 `el-select` |
| 卡片 hover 反馈和整体可点击 | 1h | 添加动画效果，整卡可跳转 |
| 后台表格数据格式化 | 1h | contentType 显示中文、时间格式化 |
| 操作确认对话框增强 | 1h | 发布/转草稿添加确认，消息包含对象名 |

### P2：细节打磨

| 任务 | 预计工作量 | 说明 |
|------|------------|------|
| 前台骨架屏应用 | 2h | CSS 已定义，组件中使用 |
| 分页增强 | 1.5h | 页码列表、每页条数选择 |
| 颜色对比度优化 | 0.5h | `--text-tertiary` 提高到 WCAG AA |
| 字体回退链补全 | 0.5h | 添加 `sans-serif` 回退 |
| 相邻内容导航 | 1h | 详情页上一篇/下一篇 |

### P3：功能扩展（长期）

| 任务 | 说明 |
|------|------|
| RSS 订阅源 | 接入 OpenAI Blog、Google AI Blog |
| 定时任务 | GitHub 热度同步、每日论文获取 |
| Docker 容器化 | 简化部署流程 |
| 响应式设计 | 移动端适配 |

---

## 十、技术栈速查

### 后端

- Java 17 + Spring Boot 3.2.5
- MyBatis-Plus 3.5.7
- Spring Security 6 + JWT (jjwt 0.12.6)
- MySQL 8

### 前端

- Vue 3.5 + Vite 6.2
- Vue Router 4.5
- Pinia 3.0
- Element Plus（后台）
- marked + DOMPurify（Markdown 渲染）
- axios（HTTP 请求）

### 外部 API

- GitHub REST API（仓库查询/搜索）
- arXiv API（论文搜索）
- HuggingFace API（每日论文）
- 阿里百炼 / DashScope（AI 生成）
- MiMo API（AI 生成）

---

## 十一、注意事项

1. **密钥安全**：不要提交 `.env` 文件，不要硬编码 API Key
2. **数据库密码**：当前使用 `123456`，生产环境需更改
3. **JWT 密钥**：重启后随机生成的密钥会导致 Token 失效
4. **前端端口**：默认 5173/5174，如被占用会自动递增
5. **后端端口**：默认 8080，如被占用需手动停止原进程

---

## 十二、快速验证命令

```bash
# 检查服务状态
curl -s http://localhost:8080/api/v1/system/health
curl -s -o /dev/null -w "%{http_code}" http://localhost:5173
curl -s -o /dev/null -w "%{http_code}" http://localhost:5174

# 测试登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 后端编译验证
cd backend && mvn -q -DskipTests compile

# 前端构建验证
cd frontend-portal && npm run build
cd frontend-admin && npm run build
```
