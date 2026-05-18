USE ai_frontier_station;

DELETE FROM ai_content_tag;
DELETE FROM ai_content_external_ref;
DELETE FROM ai_content;
DELETE FROM ai_tag;
DELETE FROM ai_source;
DELETE FROM ai_category;
DELETE FROM ai_admin_user;

ALTER TABLE ai_category AUTO_INCREMENT = 1;
ALTER TABLE ai_tag AUTO_INCREMENT = 1;
ALTER TABLE ai_source AUTO_INCREMENT = 1;
ALTER TABLE ai_content AUTO_INCREMENT = 1;
ALTER TABLE ai_content_tag AUTO_INCREMENT = 1;
ALTER TABLE ai_content_external_ref AUTO_INCREMENT = 1;
ALTER TABLE ai_admin_user AUTO_INCREMENT = 1;

INSERT INTO ai_category (name, slug, description, sort_order, is_enabled) VALUES
('AI资讯', 'ai-news', '面向模型、产品与行业动态的资讯栏目', 1, 1),
('论文速递', 'paper-digest', '用于整理 AI 论文与研究方向动态', 2, 1),
('热门项目', 'hot-projects', '用于展示 GitHub 等平台上的 AI 热门项目', 3, 1),
('公司动态', 'company-updates', '用于展示 OpenAI、DeepSeek 等公司动态', 4, 1),
('技术实践', 'engineering-practice', '用于整理 RAG、Agent、评测等工程实践', 5, 1);

INSERT INTO ai_tag (name, slug, color, description) VALUES
('GitHub', 'github', '#24292f', 'GitHub 相关内容'),
('Agent', 'agent', '#0f7df2', 'Agent 系统与工作流'),
('RAG', 'rag', '#12a6d6', '检索增强生成相关'),
('多模态', 'multimodal', '#7c5cff', '多模态模型相关'),
('OpenAI', 'openai', '#10a37f', 'OpenAI 相关内容'),
('DeepSeek', 'deepseek', '#1f6feb', 'DeepSeek 相关内容'),
('论文', 'paper', '#f59e0b', '论文研究相关'),
('工程优化', 'engineering-opt', '#ef4444', '性能与工程优化');

INSERT INTO ai_source (name, slug, source_type, website_url, description, is_enabled) VALUES
('GitHub Trending', 'github-trending', 'github', 'https://github.com/trending', 'GitHub 热门仓库来源', 1),
('arXiv', 'arxiv', 'paper', 'https://arxiv.org', 'AI 科研论文来源', 1),
('OpenAI Blog', 'openai-blog', 'official_blog', 'https://openai.com/news', 'OpenAI 官方动态来源', 1),
('DeepSeek News', 'deepseek-news', 'official_blog', 'https://www.deepseek.com', 'DeepSeek 官方动态来源', 1),
('人工录入', 'manual-entry', 'manual', NULL, '当前阶段的人工录入内容来源', 1);

INSERT INTO ai_content (
  title, slug, content_type, summary, cover_image, category_id, source_id, source_url,
  author_name, publish_status, featured_level, view_count, reading_time, published_at,
  body_markdown, extra_json
) VALUES
(
  'GitHub 热门 AI 项目观察：从工具堆叠走向平台化',
  'github-hot-ai-projects-platform-shift',
  'project',
  '以热门 AI 开源项目为切口，观察当前工程工具从单点能力转向平台化组合的趋势。',
  NULL,
  3,
  1,
  'https://github.com/trending',
  'Harry',
  'PUBLISHED',
  3,
  128,
  6,
  '2026-04-23 20:30:00',
  '# GitHub 热门 AI 项目观察\n\n这一篇内容用于展示“热门项目”类型的数据结构。\n\n## 这一类内容要承载什么\n\n- 项目定位\n- 核心亮点\n- 适用场景\n- 项目地址\n- 延伸标签\n\n## 为什么第一版先做人工整理\n\n因为当前阶段更重要的是把内容模型、来源模型、标签模型跑通。\n后续接 GitHub 自动采集时，可以直接把仓库数据映射到现有内容结构里。',
  JSON_OBJECT('starTrend', 'up', 'externalType', 'github_repo')
),
(
  '论文速递：多模态 Agent 的研究趋势正在形成新的任务链路',
  'paper-digest-multimodal-agent-trend',
  'paper',
  '以论文摘要和研究方向角度，整理多模态 Agent 方向的新趋势。',
  NULL,
  2,
  2,
  'https://arxiv.org',
  'Harry',
  'PUBLISHED',
  2,
  86,
  8,
  '2026-04-22 18:00:00',
  '# 论文速递\n\n这一条内容用于模拟论文类型。\n\n## 第一版先做什么\n\n- 标题\n- 摘要\n- 来源\n- 标签\n- 正文整理\n\n## 以后扩展什么\n\n- 论文作者\n- 会议期刊\n- 项目主页\n- 引用链接\n- 自动抓取摘要',
  JSON_OBJECT('paperVenue', 'arXiv', 'externalType', 'arxiv_paper')
),
(
  'OpenAI 与 DeepSeek 动态对比：产品节奏和开放策略的差异',
  'company-updates-openai-deepseek-compare',
  'company_update',
  '面向公司动态栏目的一条示例内容，用于展示多来源资讯的统一建模能力。',
  NULL,
  4,
  3,
  'https://openai.com/news',
  'Harry',
  'PUBLISHED',
  1,
  52,
  5,
  '2026-04-21 09:30:00',
  '# 公司动态\n\n这条内容模拟官方博客或新闻稿整理后的发布内容。\n\n统一内容表的价值在这里很明显：\n\n- 项目可以是内容\n- 论文可以是内容\n- 公司动态也可以是内容\n\n这样前台和后台都不用为每种类型重写一整套系统。',
  JSON_OBJECT('companyFocus', 'OpenAI vs DeepSeek')
),
(
  'RAG 与 Agent 工程实践：从 Demo 到稳定系统要补哪些能力',
  'rag-agent-engineering-practice',
  'practice',
  '一条技术实践内容示例，突出内容平台后续向开发者精选站扩展的能力。',
  NULL,
  5,
  5,
  NULL,
  'Harry',
  'PUBLISHED',
  2,
  64,
  7,
  '2026-04-20 15:45:00',
  '# 技术实践\n\n这条内容用于模拟工程经验总结文章。\n\n## 常见问题\n\n- Demo 能跑但结构松散\n- 数据来源和内容组织混在一起\n- 没有标签与来源抽象\n\n## 平台化之后的收益\n\n- 内容可筛选\n- 内容可复用\n- 内容可继续接推荐、订阅、专题功能',
  JSON_OBJECT('practiceScope', 'rag-agent')
);

INSERT INTO ai_content_tag (content_id, tag_id) VALUES
(1, 1),
(1, 2),
(1, 8),
(2, 2),
(2, 4),
(2, 7),
(3, 5),
(3, 6),
(4, 2),
(4, 3),
(4, 8);

INSERT INTO ai_content_external_ref (content_id, ref_type, external_id, external_url, raw_payload_json, synced_at) VALUES
(1, 'github_repo', 'trending-demo-001', 'https://github.com/trending', JSON_OBJECT('source', 'seed'), NOW()),
(2, 'arxiv_paper', 'arxiv-demo-001', 'https://arxiv.org', JSON_OBJECT('source', 'seed'), NOW()),
(3, 'official_post', 'openai-demo-001', 'https://openai.com/news', JSON_OBJECT('source', 'seed'), NOW());

INSERT INTO ai_admin_user (username, password_hash, display_name, role, is_enabled) VALUES
('admin', '$2y$10$.vdOnVZcNImd5eccQwby.O25KEplZbt99cjb7LG5koNYNZBtolPAO', '系统管理员', 'admin', 1);
