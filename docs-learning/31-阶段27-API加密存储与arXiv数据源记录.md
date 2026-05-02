# 阶段 27：API Key 加密存储与 arXiv 数据源接入

## 阶段目标

- 将 API Key 从纯内存存储升级为 AES-256-GCM 加密存储到 MySQL 数据库
- 接入 arXiv 论文数据源，支持搜索和导入 arXiv 论文为平台内容
- 前端 API 设置页支持密钥持久化，重启后自动加载

## 本阶段完成情况

### API Key 加密存储

- [x] 新增 `ai_api_credential` 数据库表，存储加密后的密钥、IV、密钥后缀
- [x] 新建 `CryptoUtil.java` 加密工具类，实现 AES-256-GCM 加密/解密
- [x] 新建 `ApiCredential` 实体类和 `ApiCredentialMapper`
- [x] 改造 `ApiCredentialServiceImpl`，启动时从数据库加载密钥，保存时加密写入数据库
- [x] 主密钥通过环境变量 `API_MASTER_KEY` 配置，未配置时使用默认开发密钥
- [x] 前端 API 设置页更新说明文案，反映加密存储特性
- [x] 保存位置从"后端内存"改为"加密存储"
- [x] 清除确认对话框更新为数据库删除提示

### arXiv 数据源接入

- [x] 新建 `ArxivService` 接口和 `ArxivServiceImpl` 实现
- [x] 实现 arXiv API 调用和 XML 解析（使用 Java 内置 DocumentBuilder）
- [x] 支持按关键词、分类、作者等条件搜索 arXiv 论文
- [x] 支持单篇论文查询（通过 arXiv ID）
- [x] 新建 `ArxivAdminController`，提供 `/admin/arxiv/search` 和 `/admin/arxiv/paper` 端点
- [x] 新建 `ArxivPaperCandidateVO` 和 `ArxivPaperImportRequest` DTO
- [x] 扩展 `ContentService` 和 `ContentServiceImpl`，新增 `importArxivPaper()` 方法
- [x] 扩展 `ImportAdminController`，新增 `/admin/import/arxiv-paper` 端点
- [x] 前端 `admin.js` 新增 arXiv 相关 API 函数
- [x] 前端内容管理页新增"arXiv 论文导入"按钮和弹窗
- [x] 弹窗支持关键词搜索、结果列表、回填表单、确认导入

## 技术实现细节

### 加密方案

```java
// AES-256-GCM 加密
算法：AES/GCM/NoPadding
密钥派生：SHA-256(masterKey) -> 32字节
IV：12字节随机数
标签长度：128位
```

### arXiv API 集成

```text
请求：GET https://export.arxiv.org/api/query
参数：search_query, start, max_results, sortBy, sortOrder
响应：Atom XML 格式
解析：Java DocumentBuilder DOM 解析
```

### 数据库表结构

```sql
CREATE TABLE ai_api_credential (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider VARCHAR(50) NOT NULL UNIQUE,
  encrypted_key TEXT NOT NULL,
  iv VARCHAR(64) NOT NULL,
  key_suffix VARCHAR(10) NOT NULL,
  remark VARCHAR(255),
  is_enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 本阶段新增或修改的文件

### 新建文件（8个）

| 文件 | 说明 |
|------|------|
| `database/add-api-credential-table.sql` | 新增表 SQL |
| `backend/.../util/CryptoUtil.java` | AES-256-GCM 加密工具 |
| `backend/.../entity/ApiCredential.java` | API 密钥实体 |
| `backend/.../mapper/ApiCredentialMapper.java` | MyBatis-Plus Mapper |
| `backend/.../service/ArxivService.java` | arXiv 服务接口 |
| `backend/.../service/impl/ArxivServiceImpl.java` | arXiv 服务实现 |
| `backend/.../controller/admin/ArxivAdminController.java` | arXiv 后台接口 |
| `backend/.../vo/ArxivPaperCandidateVO.java` | arXiv 论文 VO |
| `backend/.../dto/request/ArxivPaperImportRequest.java` | arXiv 导入请求 DTO |

### 修改文件（7个）

| 文件 | 修改内容 |
|------|----------|
| `database/schema.sql` | 追加 `ai_api_credential` 表 |
| `backend/.../application.yml` | 新增 `app.crypto.master-key` 配置 |
| `backend/.../service/impl/ApiCredentialServiceImpl.java` | 从内存改为数据库+缓存 |
| `backend/.../service/ContentService.java` | 新增 `importArxivPaper()` |
| `backend/.../service/impl/ContentServiceImpl.java` | 实现 arXiv 导入 |
| `backend/.../controller/admin/ImportAdminController.java` | 新增 arXiv 导入端点 |
| `frontend-admin/src/api/admin.js` | 新增 arXiv API 函数 |
| `frontend-admin/src/views/ApiSettingsView.vue` | 更新说明和状态文案 |
| `frontend-admin/src/views/ContentManageView.vue` | 新增 arXiv 导入 UI |
| `.env.example` | 新增 `API_MASTER_KEY` |

## 验证结果

- [x] 后端编译通过：`mvn -q -DskipTests compile`
- [x] 前台构建通过：`npm run build`
- [x] 后台构建通过：`npm run build`（无 500KB chunk 警告）

## 答辩时可以怎么讲

### API Key 加密存储

> 之前 API 密钥只存在后端内存里，重启就丢失，每次都要重新输入。现在改成了 AES-256-GCM 加密后存储到 MySQL 数据库，主密钥通过环境变量配置。前端填写密钥后，后端会验证并加密存储，重启后自动加载，无需重复输入。这比纯内存存储更安全，也比明文落库更符合安全规范。

### arXiv 数据源

> 为了扩展平台的数据来源，我接入了 arXiv 论文 API。用户可以在后台搜索 arXiv 上的 AI 相关论文，选择后导入为平台内容。系统会自动提取论文的标题、作者、摘要、PDF 链接等信息，并生成结构化的内容页面。这是平台从"人工录入"向"API 数据源接入"迈出的重要一步。

### 技术亮点

1. **加密方案**：使用 AES-256-GCM，这是目前最安全的对称加密算法之一，GCM 模式同时提供加密和认证
2. **XML 解析**：arXiv API 返回 XML 格式，使用 Java 内置 DocumentBuilder 解析，无需额外依赖
3. **代码复用**：完全复用现有的内容导入流程（create + createExternalRef），保持架构一致性

## 后续扩展方向

1. **更多数据源**：HuggingFace Papers、Papers with Code、技术博客 RSS
2. **定时同步**：定期刷新 arXiv 论文引用数、下载量等指标
3. **AI 增强**：对接百炼/MiMo 对导入的论文自动生成导读和推荐理由
4. **批量导入**：支持按分类批量导入最新论文
