-- ============================================================
-- 种子数据：GitHub / AI资讯 / 产品动态 / 模型评测 / 工具实践
-- 执行方式：mysql -uroot -p ai_frontier_station < database/seed-sections.sql
-- ============================================================

USE ai_frontier_station;

-- ====================== GitHub 开源 (5条) ======================
INSERT INTO ai_content (title, slug, content_type, summary, cover_image, category_id, source_id, source_url, author_name, publish_status, featured_level, published_at, body_markdown, extra_json) VALUES

('openai/codex：轻量级终端 AI 编程助手', 'github-openai-codex',
 'project', 'OpenAI 开源的终端AI编程助手，支持代码生成、解释和重构，在终端中直接运行，支持多种LLM后端。',
 NULL, 7, 1, 'https://github.com/openai/codex', 'OpenAI', 'PUBLISHED', 2, '2026-05-17 10:00:00',
 '## 项目简介\n\nCodex 是 OpenAI 开源的轻量级终端AI编程助手。它直接在终端中运行，无需IDE插件，支持代码生成、代码解释、重构和调试。\n\n## 核心功能\n\n- **代码生成**：根据自然语言描述生成代码\n- **代码解释**：选中代码片段获取中文解释\n- **重构建议**：识别代码异味并提供重构方案\n- **多模型支持**：可切换GPT-4o、Claude等模型后端\n\n## 技术实现\n\n- 使用 Rust 编写CLI核心，启动速度快\n- 通过 API 调用LLM，支持流式输出\n- 支持本地文件上下文注入，理解项目结构\n- 内置安全沙箱，限制文件系统访问范围\n\n## 适用人群\n\n适合习惯终端工作流的开发者，特别是后端工程师和DevOps人员。对于不想离开终端但需要AI辅助编程的场景非常合适。',
 '{"starCount": 42100, "weeklyGrowth": 8234, "language": "Rust", "topics": ["cli", "ai-coding", "openai"], "implementationSummary": "Rust编写CLI核心，通过API调用LLM，支持流式输出和本地文件上下文注入。"}'),

('vllm-project/vllm：高吞吐量 LLM 推理引擎', 'github-vllm',
 'project', 'UC Berkeley 开源的高吞吐量LLM推理引擎，基于PagedAttention，已成为大模型部署的事实标准。',
 NULL, 7, 1, 'https://github.com/vllm-project/vllm', 'UC Berkeley', 'PUBLISHED', 2, '2026-05-16 10:00:00',
 '## 项目简介\n\nvLLM 是一个高吞吐量、低延迟的LLM推理和服务引擎。它通过创新的PagedAttention机制显著提升了推理效率。\n\n## 核心功能\n\n- **PagedAttention**：创新的注意力机制内存管理，减少显存浪费\n- **连续批处理**：动态批处理请求，最大化GPU利用率\n- **张量并行**：支持多GPU分布式推理\n- **OpenAI兼容API**：直接替换OpenAI API端点\n\n## 技术实现\n\n- 基于PagedAttention的内存管理，将KV Cache分页存储\n- 连续批处理调度器，动态插入新请求\n- 支持Tensor并行和Pipeline并行\n- 支持量化推理（AWQ/GPTQ/FP8）\n\n## 适用人群\n\n需要部署大模型的团队，特别是对推理吞吐量有要求的生产环境。已成为Llama、Mistral等开源模型部署的事实标准。',
 '{"starCount": 58700, "weeklyGrowth": 5102, "language": "Python", "topics": ["inference", "llm", "gpu"], "implementationSummary": "基于PagedAttention的内存管理，连续批处理调度，支持Tensor并行和Pipeline并行。"}'),

('browser-use/browser-use：让 AI Agent 控制浏览器', 'github-browser-use',
 'project', '开源浏览器自动化Agent框架，支持网页导航、表单填写、数据提取等操作。',
 NULL, 7, 1, 'https://github.com/browser-use/browser-use', 'browser-use', 'PUBLISHED', 1, '2026-05-15 10:00:00',
 '## 项目简介\n\nbrowser-use 是一个让AI Agent控制浏览器的开源框架。它可以自动完成网页导航、表单填写、数据提取等操作。\n\n## 核心功能\n\n- **浏览器控制**：AI Agent可以直接操作浏览器页面\n- **视觉理解**：结合截图理解页面内容\n- **表单交互**：自动填写表单、点击按钮\n- **数据提取**：从网页中提取结构化数据\n\n## 技术实现\n\n- 基于 Playwright 驱动浏览器\n- LLM 通过结构化指令控制页面元素\n- 支持视觉理解模式（截图+分析）\n- 支持多种LLM后端（OpenAI/Anthropic/本地模型）\n\n## 适用人群\n\n需要自动化Web操作的开发者，如数据采集、自动化测试、RPA等场景。',
 '{"starCount": 31200, "weeklyGrowth": 4876, "language": "Python", "topics": ["browser", "agent", "automation"], "implementationSummary": "基于Playwright驱动浏览器，LLM通过结构化指令控制页面元素，支持视觉理解。"}'),

('ollama/ollama：本地运行大模型的最简方案', 'github-ollama',
 'project', '一行命令在本地启动Llama 3、Mistral等模型，支持GPU加速，已成为本地部署首选工具。',
 NULL, 7, 1, 'https://github.com/ollama/ollama', 'Ollama', 'PUBLISHED', 2, '2026-05-14 10:00:00',
 '## 项目简介\n\nOllama 让你在本地一行命令运行大模型。支持Llama 3、Mistral、Gemma等主流开源模型。\n\n## 核心功能\n\n- **一键部署**：`ollama run llama3` 即可启动\n- **模型管理**：内置模型仓库，自动下载和管理\n- **API服务**：启动后自动提供REST API\n- **GPU加速**：自动检测并使用GPU\n\n## 技术实现\n\n- Go 语言编写核心运行时\n- 使用 GGUF 格式进行量化推理\n- 内置模型仓库和版本管理\n- 提供 REST API 和命令行交互\n\n## 适用人群\n\n想在本地体验大模型的个人开发者，以及需要在边缘设备部署模型的团队。',
 '{"starCount": 128000, "weeklyGrowth": 3541, "language": "Go", "topics": ["local-llm", "inference", "quantization"], "implementationSummary": "Go编写核心运行时，GGUF格式量化推理，内置模型仓库和API服务。"}'),

('langgenius/dify：开源 LLM 应用开发平台', 'github-dify',
 'project', '提供RAG引擎、Agent编排、模型管理和Prompt IDE的低代码AI应用开发平台。',
 NULL, 7, 1, 'https://github.com/langgenius/dify', 'LangGenius', 'PUBLISHED', 1, '2026-05-13 10:00:00',
 '## 项目简介\n\nDify 是一个开源的LLM应用开发平台，提供从Prompt工程到Agent编排的完整工具链。\n\n## 核心功能\n\n- **RAG引擎**：内置文档检索增强生成\n- **Agent编排**：可视化编排多步骤Agent\n- **Prompt IDE**：在线调试和优化Prompt\n- **模型管理**：统一管理多个LLM提供商\n\n## 技术实现\n\n- Flask 后端 + React 前端\n- 插件化架构，支持自定义工具\n- 支持向量数据库集成（Weaviate/Qdrant等）\n- 提供REST API和SDK\n\n## 适用人群\n\n需要快速构建AI应用的团队，特别是不熟悉底层技术但想用AI解决问题的产品经理和业务人员。',
 '{"starCount": 67300, "weeklyGrowth": 2890, "language": "Python", "topics": ["llm-platform", "rag", "agent"], "implementationSummary": "Flask后端+React前端，插件化架构，支持自定义工具和外部API集成。"}');


-- ====================== AI资讯 (5条) ======================
INSERT INTO ai_content (title, slug, content_type, summary, cover_image, category_id, source_id, source_url, author_name, publish_status, featured_level, published_at, body_markdown, extra_json) VALUES

('欧盟AI法案正式执行：高风险AI系统必须在6个月内完成合规审查', 'news-eu-ai-act-enforcement',
 'news', '欧盟委员会宣布AI法案高风险条款正式生效，违规企业最高可处全球营业额7%的罚款。',
 NULL, 6, 5, 'https://ec.europa.eu/ai-act', 'Reuters', 'PUBLISHED', 2, '2026-05-18 14:30:00',
 '## 核心要点\n\n欧盟AI法案（AI Act）高风险条款于本周正式生效，标志着全球最严格的AI监管框架进入实质性执行阶段。\n\n## 关键内容\n\n- **执行时间**：所有在欧盟运营的高风险AI系统需在6个月内完成合规审查\n- **处罚力度**：违规企业最高可处全球营业额7%的罚款\n- **高风险定义**：涉及医疗、教育、招聘、司法等领域的AI系统\n- **合规要求**：需要进行风险评估、数据治理、透明度报告\n\n## 影响分析\n\n这一法案将直接影响所有在欧洲市场部署AI系统的公司。OpenAI、Google、Anthropic等公司已经开始调整其欧洲业务策略。对于中国出海企业，也需要关注合规要求。\n\n## 开发者关注点\n\n- AI系统的训练数据需要可追溯\n- 需要建立人工监督机制\n- 高风险系统需要定期审计\n- 用户有权知道他们正在与AI交互',
 '{"region": "欧洲", "importance": "high", "category": "AI政策"}'),

('Google DeepMind 发布 Gemini 2.5：原生多模态推理能力大幅提升', 'news-gemini-25-launch',
 'news', 'Gemini 2.5 Pro在数学推理、代码生成和多模态理解三项基准上均超越GPT-4o。',
 NULL, 6, 5, 'https://blog.google/technology/ai/gemini-2-5/', 'TechCrunch', 'PUBLISHED', 2, '2026-05-17 12:00:00',
 '## 核心要点\n\nGoogle DeepMind 正式发布 Gemini 2.5 Pro，这是其旗舰多模态模型的重大升级版本。\n\n## 性能提升\n\n- **数学推理**：在MATH基准上超越GPT-4o 3.2个百分点\n- **代码生成**：在HumanEval上达到92.1%，创历史新高\n- **多模态理解**：图像理解和视频分析能力显著提升\n- **上下文窗口**：首次实现百万token上下文的稳定输出\n\n## 技术亮点\n\n- 原生多模态架构，非后融合方式\n- 改进的推理链（Chain-of-Thought）机制\n- 更高效的注意力计算，降低长文本推理成本\n- 支持工具调用和代码执行\n\n## 开发者影响\n\n- API价格与GPT-4o竞争，输入$2.5/M tokens\n- 百万token上下文适合处理长文档和代码库\n- 多模态API支持图像、视频、音频输入',
 '{"region": "美国", "importance": "high", "category": "行业动态"}'),

('FDA批准首个AI驱动的实时手术辅助系统', 'news-fda-ai-surgery',
 'news', '该系统利用计算机视觉实时分析手术画面，标记关键血管和神经，并发症发生率降低23%。',
 NULL, 6, 5, 'https://www.fda.gov/ai-surgery-approval', 'Nature Medicine', 'PUBLISHED', 1, '2026-05-16 08:00:00',
 '## 核心要点\n\nFDA正式批准了首个AI驱动的实时手术辅助系统，标志着AI在临床手术领域的重大突破。\n\n## 系统功能\n\n- **实时分析**：在手术过程中实时分析视频画面\n- **结构识别**：自动标记关键血管、神经和器官边界\n- **风险预警**：在接近危险区域时发出警告\n- **决策支持**：基于术中情况推荐最佳操作路径\n\n## 临床数据\n\n- 并发症发生率降低23%\n- 手术时间平均缩短15%\n- 关键结构识别准确率达98.5%\n- 已在12家医院完成临床验证\n\n## 行业意义\n\n这是AI从辅助诊断走向辅助治疗的关键一步。预计未来3-5年内，AI手术辅助将成为标准配置。',
 '{"region": "美国", "importance": "high", "category": "行业实践"}'),

('中国发布《生成式AI服务管理办法》修订版：放宽训练数据要求', 'news-china-ai-regulation-update',
 'news', '修订版取消训练数据必须全部来自国内的要求，改为"关键数据不出境"原则，简化备案流程。',
 NULL, 6, 5, 'https://www.xinhuanet.com/ai-regulation', '新华社', 'PUBLISHED', 2, '2026-05-15 10:00:00',
 '## 核心要点\n\n中国网信办发布《生成式AI服务管理办法》修订版，对训练数据和备案流程做出重要调整。\n\n## 主要变化\n\n- **数据要求**：取消"训练数据必须全部来自国内"的限制\n- **新原则**：改为"关键数据不出境"，普通数据可跨境使用\n- **备案流程**：简化审批环节，从60天缩短至30天\n- **创新激励**：对中小企业和研究机构提供绿色通道\n\n## 行业影响\n\n- 国产大模型迭代速度将加快\n- 中外合作研发更加便利\n- 降低了创业公司的合规成本\n- 仍需注意关键数据的定义和边界\n\n## 开发者关注点\n\n- 可以使用更多国际开源数据集\n- API服务备案流程更简化\n- 需要建立数据分类分级机制',
 '{"region": "中国", "importance": "high", "category": "AI政策"}'),

('AI辅助蛋白质设计首次进入临床：Insilico Medicine抗纤维化药物完成II期试验', 'news-ai-drug-clinical',
 'news', '由AI从头设计的小分子药物在特发性肺纤维化II期临床中达到主要终点。',
 NULL, 6, 5, 'https://www.statnews.com/ai-drug', 'STAT News', 'PUBLISHED', 1, '2026-05-14 06:00:00',
 '## 核心要点\n\nInsilico Medicine宣布其AI设计的抗纤维化药物INS018_055完成II期临床试验，达到主要终点。\n\n## 关键数据\n\n- **适应症**：特发性肺纤维化（IPF）\n- **主要终点**：FVC（用力肺活量）改善显著优于安慰组\n- **安全性**：不良事件发生率与安慰剂相当\n- **设计方式**：完全由AI从靶点发现到分子设计\n\n## 技术路线\n\n1. AI靶点发现：使用PandaOmics平台识别新靶点\n2. 分子设计：使用Chemistry42生成候选分子\n3. 临床预测：AI预测临床试验成功率\n4. 实验验证：合成并进行体内外实验\n\n## 行业意义\n\n这是AI药物设计从概念验证走向临床验证的里程碑事件。传统药物研发需要10-15年，AI有望将这个周期缩短至3-5年。',
 '{"region": "全球", "importance": "medium", "category": "行业实践"}');


-- ====================== 产品动态 (5条) ======================
INSERT INTO ai_content (title, slug, content_type, summary, cover_image, category_id, source_id, source_url, author_name, publish_status, featured_level, published_at, body_markdown, extra_json) VALUES

('GPT-4o API 价格下调30%：输入$2.5/M tokens', 'product-chatgpt-price-cut',
 'company_update', '面对DeepSeek和Qwen的价格竞争，OpenAI大幅下调GPT-4o API定价。',
 NULL, 7, 3, 'https://openai.com/api/pricing', 'OpenAI', 'PUBLISHED', 2, '2026-05-16 16:00:00',
 '## 更新内容\n\nOpenAI宣布GPT-4o API价格下调30%，立即生效。\n\n## 新价格\n\n| 模型 | 输入价格 | 输出价格 |\n|------|---------|----------|\n| GPT-4o | $2.5/M tokens | $10/M tokens |\n| GPT-4o-mini | $0.15/M tokens | $0.60/M tokens |\n| o1 | $15/M tokens | $60/M tokens |\n\n## 市场分析\n\n这是OpenAI应对DeepSeek（$0.14/M）和Qwen（¥0.02/千tokens）价格竞争的举措。大模型API价格战持续升级，对开发者来说是利好。\n\n## 开发者建议\n\n- 对于大多数应用场景，GPT-4o-mini性价比最高\n- 需要强推理能力时使用o1\n- 可以考虑混合使用多家API降低成本',
 '{"product": "ChatGPT", "updateType": "price", "company": "OpenAI"}'),

('Claude Code 正式支持 MCP 协议：可连接外部工具和数据源', 'product-claude-code-mcp',
 'company_update', '通过MCP扩展工具调用能力，支持连接数据库、API和自定义工具。',
 NULL, 7, 5, 'https://docs.anthropic.com/claude-code/mcp', 'Anthropic', 'PUBLISHED', 2, '2026-05-17 11:00:00',
 '## 更新内容\n\nClaude Code正式支持MCP（Model Context Protocol）协议，开发者可以通过MCP连接器扩展Claude Code的能力。\n\n## 新能力\n\n- **数据库连接**：直接查询PostgreSQL、MySQL等数据库\n- **API调用**：连接外部REST API和GraphQL服务\n- **文件系统**：访问本地和远程文件系统\n- **自定义工具**：注册自定义MCP工具供Claude调用\n\n## 使用方式\n\n在Claude Code配置文件中添加MCP服务器配置，Claude会自动发现并使用可用的工具。支持stdio和HTTP两种传输方式。\n\n## 生态影响\n\nMCP协议正在成为AI工具连接的标准。社区已贡献了Notion、Slack、GitHub等20+连接器。这标志着AI编程从"生成代码"向"操作一切"的进化。',
 '{"product": "Claude Code", "updateType": "feature", "company": "Anthropic"}'),

('DeepSeek-V3.1 发布：数学推理提升4%，HumanEval达91.2%', 'product-deepseek-v31',
 'company_update', '小版本更新但性能提升明显，在代码生成基准上首次突破91%。',
 NULL, 7, 4, 'https://github.com/deepseek-ai/DeepSeek-V3', 'DeepSeek', 'PUBLISHED', 1, '2026-05-16 15:00:00',
 '## 更新内容\n\nDeepSeek发布V3.1版本，虽然是小版本更新但性能提升明显。\n\n## 性能提升\n\n- **数学推理**：MATH基准提升4个百分点\n- **代码生成**：HumanEval首次突破91%，达91.2%\n- **中文理解**：C-Eval提升2.1个百分点\n- **推理速度**：优化后推理延迟降低15%\n\n## 技术改进\n\n- 改进了MoE（混合专家）路由策略\n- 优化了长上下文处理能力\n- 增强了工具调用的准确性\n- 降低了幻觉率\n\n## 价格优势\n\nAPI定价保持不变：输入$0.14/M tokens，输出$0.28/M tokens。相比GPT-4o便宜约18倍，是目前性价比最高的选择之一。',
 '{"product": "DeepSeek", "updateType": "model", "company": "DeepSeek"}'),

('Trae Solo 模式更新：支持多文件并行编辑 + 自动Git提交', 'product-trae-solo-update',
 'company_update', 'Solo模式新增多文件并行编辑能力，Agent可同时修改多个文件并自动创建Git提交。',
 NULL, 7, 5, 'https://trae.ai/changelog', '字节跳动', 'PUBLISHED', 1, '2026-05-18 09:00:00',
 '## 更新内容\n\nTrae的Solo模式（AI Agent自主编程模式）迎来重大更新。\n\n## 新功能\n\n- **多文件并行编辑**：Agent可以同时修改多个相关文件\n- **自动Git提交**：完成任务后自动创建有意义的commit\n- **任务规划**：先展示执行计划，确认后再执行\n- **错误恢复**：遇到错误自动回滚并尝试其他方案\n\n## 使用体验\n\n用户只需描述需求，Trae Solo会自动：\n1. 分析项目结构\n2. 制定修改计划\n3. 并行编辑多个文件\n4. 运行测试验证\n5. 自动Git提交\n\n## 与Cursor对比\n\nTrae Solo的多文件并行编辑是Cursor Composer尚未支持的功能。在复杂重构场景下效率提升明显。',
 '{"product": "Trae", "updateType": "feature", "company": "字节跳动"}'),

('Qwen-2.5-VL 发布：视觉语言模型支持视频理解和OCR', 'product-qwen25-vl',
 'company_update', '通义千问团队发布视觉语言模型，支持图像理解、视频分析和高精度OCR识别。',
 NULL, 7, 5, 'https://huggingface.co/Qwen/Qwen2.5-VL', '阿里云', 'PUBLISHED', 1, '2026-05-15 09:00:00',
 '## 更新内容\n\n阿里云通义千问团队发布Qwen-2.5-VL视觉语言模型。\n\n## 核心能力\n\n- **图像理解**：支持复杂场景描述、物体识别、空间关系推理\n- **视频分析**：支持长视频理解，可回答视频内容相关问题\n- **OCR识别**：高精度文字识别，支持多语言混合排版\n- **图表解读**：理解图表、表格、公式等结构化信息\n\n## 模型规格\n\n- 参数量：3B / 7B / 72B 三个版本\n- 支持最大图像分辨率：4096x4096\n- 视频理解：最长支持2分钟视频\n- 上下文窗口：128K tokens\n\n## API使用\n\n通过阿里百炼DashScope平台调用，72B版本价格约¥0.04/千tokens。',
 '{"product": "Qwen", "updateType": "model", "company": "阿里云"}');


-- ====================== 模型评测 (3条) ======================
INSERT INTO ai_content (title, slug, content_type, summary, cover_image, category_id, source_id, source_url, author_name, publish_status, featured_level, published_at, body_markdown, extra_json) VALUES

('Arena 总榜更新：Gemini 2.5 Pro 首次登顶，超越 GPT-4o', 'arena-gemini-tops-overall',
 'arena', 'Gemini 2.5 Pro以1342分首次登顶Arena总榜，成为首个登顶的Google模型。',
 NULL, 8, 8, 'https://chat.lmsys.org/leaderboard', 'AI前沿情报站', 'PUBLISHED', 2, '2026-05-18 09:00:00',
 '## 排名变化\n\n| 排名 | 模型 | ELO分数 | 变化 |\n|------|------|---------|------|\n| 1 | Gemini 2.5 Pro | 1342 | ↑8 NEW |\n| 2 | GPT-4o | 1338 | ↓2 |\n| 3 | Claude 3.5 Sonnet | 1335 | +1 |\n| 4 | DeepSeek-V3 | 1310 | ↑12 |\n| 5 | Llama 3.1 405B | 1298 | — |\n\n## 关键分析\n\n- **Gemini登顶**：Google首次在Arena总榜超越OpenAI，百万token上下文是关键优势\n- **DeepSeek崛起**：以1310分进入Top 5，成为首个进入前五的中国模型\n- **Claude稳定**：Claude 3.5 Sonnet保持第三，编程子榜仍为第一\n\n## 投票数据\n\n本周总投票数超过25万次，参与用户来自180+个国家和地区。',
 '{"category": "总榜", "topModel": "Gemini 2.5 Pro", "topElo": 1342}'),

('Arena 编程子榜：Claude 3.5 Sonnet 保持领先，Gemini差距缩小至12分', 'arena-coding-claude-leads',
 'arena', 'Claude 3.5 Sonnet以1389分保持编程子榜第一，Gemini 2.5 Pro紧随其后。',
 NULL, 8, 8, 'https://chat.lmsys.org/leaderboard#coding', 'AI前沿情报站', 'PUBLISHED', 1, '2026-05-17 09:00:00',
 '## 编程子榜排名\n\n| 排名 | 模型 | ELO分数 | 变化 |\n|------|------|---------|------|\n| 1 | Claude 3.5 Sonnet | 1389 | — |\n| 2 | Gemini 2.5 Pro | 1377 | ↑5 NEW |\n| 3 | GPT-4o | 1371 | ↓1 |\n| 4 | DeepSeek-V3 | 1345 | ↑3 |\n| 5 | Codestral | 1330 | — |\n\n## 分析\n\n- **Claude优势**：在代码生成准确性和代码理解深度上仍保持优势\n- **Gemini追赶**：差距从上月的28分缩小至12分，进步明显\n- **DeepSeek进步**：在中文编程和算法题上表现突出\n\n## 用户偏好\n\n在编程任务中，67%的用户表示更倾向于使用Claude或Gemini，而非GPT-4o。',
 '{"category": "编程", "topModel": "Claude 3.5 Sonnet", "topElo": 1389}'),

('Arena 趋势：中国模型首次进入 Top 5，DeepSeek 和 Qwen 表现抢眼', 'arena-china-models-rise',
 'arena', 'DeepSeek-V3以1310分进入总榜Top 5，Qwen-2.5 72B在中文子榜排名第二。',
 NULL, 8, 8, 'https://chat.lmsys.org/leaderboard', 'AI前沿情报站', 'PUBLISHED', 1, '2026-05-16 09:00:00',
 '## 中国模型表现\n\n### DeepSeek-V3\n- 总榜第4名（1310分），较上周上升3位\n- 编程子榜第4名（1345分）\n- 数学子榜第3名（1350分）\n- 价格仅为GPT-4o的1/18\n\n### Qwen-2.5 72B\n- 中文子榜第2名（1340分）\n- 总榜第6名（1285分）\n- 在中文理解和生成任务上表现优异\n\n### GLM-4-Plus\n- 中文子榜第5名（1290分）\n- 在长文本理解上有独特优势\n\n## 趋势分析\n\n中国模型正在快速缩小与美国模型的差距。在中文任务上，中国模型已经全面超越。在编程和数学等通用任务上，差距从半年前的50+分缩小至20分以内。',
 '{"category": "趋势", "highlight": "中国模型崛起"}');


-- ====================== 工具实践 (5条) ======================
INSERT INTO ai_content (title, slug, content_type, summary, cover_image, category_id, source_id, source_url, author_name, publish_status, featured_level, published_at, body_markdown, extra_json) VALUES

('Karpathy：AI编程的正确姿势——理解代码比盲目接受AI建议更重要', 'tools-karpathy-ai-coding',
 'practice', 'Karpathy长文分享AI编程方法论：先理解问题本质，再让AI生成方案，最后用判断力筛选修改。',
 NULL, 9, 9, 'https://x.com/karpathy/status/ai-coding', 'Karpathy', 'PUBLISHED', 2, '2026-05-18 12:30:00',
 '## 核心观点\n\nAndrej Karpathy（前OpenAI/Tesla AI负责人）分享了他对AI编程的深度思考。\n\n## 关键方法论\n\n### 1. 先理解，再生成\n不要一上来就让AI写代码。先自己理解问题的本质，明确需求和约束条件。\n\n### 2. AI是加速器，不是替代品\nAI编程的正确姿势是"用AI加速你的思考循环"，而不是让AI替你思考。\n\n### 3. 保持批判性思维\nAI生成的代码不一定正确。你需要用你的判断力筛选和修改，特别是边界条件和安全性。\n\n### 4. 学习AI的输出\n把AI生成的代码当作学习材料，理解它为什么这样写，从中提升自己的能力。\n\n## 实践建议\n\n- 对于熟悉的领域：让AI处理样板代码，自己专注核心逻辑\n- 对于不熟悉的领域：先让AI解释思路，理解后再生成代码\n- 对于关键代码：必须逐行Review，不能盲目信任\n\n## 社区反响\n\n这条推文获得4.2k点赞和1.8k转发，引发广泛讨论。许多开发者表示"这正是我需要听到的"。',
 '{"platform": "X/Twitter", "likes": 4200, "retweets": 1800, "comments": 326, "authorHandle": "@karpathy", "contentType": "opinion"}'),

('Cursor 高效工作流：Plan → Generate → Review，编码效率提升3倍', 'tools-cursor-workflow',
 'practice', 'swyx总结的Cursor三步工作流：先用Chat模式让AI理解需求并生成计划，再用Composer批量生成代码。',
 NULL, 9, 9, 'https://x.com/swyx/status/cursor-workflow', 'swyx', 'PUBLISHED', 1, '2026-05-17 10:00:00',
 '## 工作流概述\n\n开发者swyx（@swyx）分享了一套高效的Cursor使用工作流，实测编码效率提升3倍。\n\n## 三步工作流\n\n### Step 1: Plan（规划）\n使用Cursor Chat模式，让AI理解你的需求并生成执行计划。\n\n```\n提示词：请分析这个项目结构，然后为[需求描述]制定一个实现计划，\n列出需要修改的文件和每个文件的具体改动。\n```\n\n### Step 2: Generate（生成）\n切换到Composer模式，按照计划批量生成代码。\n\n- 一次可以修改多个文件\n- AI会自动理解文件间的依赖关系\n- 支持增量修改\n\n### Step 3: Review（审查）\n使用Diff视图逐行审查AI生成的代码。\n\n- 重点关注边界条件\n- 检查错误处理\n- 验证类型安全\n\n## 关键技巧\n\n- 提供足够的上下文（相关文件、API文档）\n- 分步骤执行，不要一次要求太多\n- 保持对代码的理解，不要盲目接受',
 '{"platform": "X/Twitter", "likes": 2100, "retweets": 890, "comments": 145, "authorHandle": "@swyx", "contentType": "workflow"}'),

('MCP协议生态爆发：社区贡献了20+新连接器', 'tools-mcp-ecosystem',
 'practice', 'MCP（Model Context Protocol）协议发布3个月后，社区已经贡献了20多个连接器，覆盖数据库、笔记、通讯等场景。',
 NULL, 9, 10, 'https://www.reddit.com/r/MachineLearning/comments/mcp-ecosystem', 'r/MachineLearning', 'PUBLISHED', 1, '2026-05-16 09:00:00',
 '## MCP协议简介\n\nMCP（Model Context Protocol）是Anthropic发布的开放协议，用于标准化AI模型与外部工具的连接方式。\n\n## 社区生态\n\n### 数据库连接器\n- PostgreSQL MCP Server\n- MySQL MCP Server\n- MongoDB MCP Server\n- Redis MCP Server\n\n### 笔记和知识库\n- Notion MCP Server\n- Obsidian MCP Server\n- Confluence MCP Server\n\n### 通讯和协作\n- Slack MCP Server\n- Discord MCP Server\n- Microsoft Teams MCP Server\n\n### 开发工具\n- GitHub MCP Server\n- GitLab MCP Server\n- Jira MCP Server\n\n## 使用方式\n\n只需在AI工具的配置文件中添加MCP服务器地址，AI就能自动发现并使用这些工具。支持stdio和HTTP两种传输方式。\n\n## 发展趋势\n\nMCP正在成为AI工具连接的事实标准。预计到年底将有100+连接器可用。',
 '{"platform": "Reddit", "likes": 1500, "comments": 287, "subreddit": "r/MachineLearning", "contentType": "tool"}'),

('本地部署大模型完全指南：从Ollama到vLLM的选型建议', 'tools-local-llm-guide',
 'practice', 'ggerganov（llama.cpp作者）分享本地部署大模型的完整选型指南。',
 NULL, 9, 9, 'https://x.com/ggerganov/status/local-llm-guide', 'ggerganov', 'PUBLISHED', 1, '2026-05-15 16:00:00',
 '## 选型指南\n\nggerganov（llama.cpp作者）分享了本地部署大模型的选型建议。\n\n## 方案对比\n\n### Ollama — 最简单\n- 适合：个人用户体验本地模型\n- 优势：一行命令启动，内置模型管理\n- 劣势：性能不是最优，定制化有限\n- 命令：`ollama run llama3`\n\n### vLLM — 最高性能\n- 适合：团队部署，生产环境\n- 优势：最高吞吐量，OpenAI兼容API\n- 劣势：需要GPU，配置复杂\n- 场景：API服务、批量推理\n\n### llama.cpp — 最灵活\n- 适合：边缘设备，资源受限环境\n- 优势：CPU推理，跨平台，量化支持\n- 劣势：需要手动管理模型文件\n- 场景：嵌入式设备、离线环境\n\n## 通用建议\n\n1. 先用Ollama体验，确认需求\n2. 需要API服务时切换到vLLM\n3. 资源受限时使用llama.cpp\n4. 量化是关键：4bit量化几乎不损失质量',
 '{"platform": "X/Twitter", "likes": 3800, "retweets": 1200, "comments": 198, "authorHandle": "@ggerganov", "contentType": "guide"}'),

('实测对比：5款AI编程助手在React项目中的代码质量评分', 'tools-ai-coding-benchmark',
 'practice', 'levelsio对比Cursor、Copilot、Codeium、Tabnine、Cody在React项目中的表现。',
 NULL, 9, 9, 'https://x.com/levelsio/status/ai-coding-benchmark', 'levelsio', 'PUBLISHED', 1, '2026-05-14 08:00:00',
 '## 测试方法\n\n知名独立开发者levelsio在一个中等复杂度的React项目中，对比了5款AI编程助手的表现。\n\n## 评分结果\n\n| 工具 | 综合分 | 代码准确率 | 补全速度 | 上下文理解 |\n|------|--------|-----------|---------|------------|\n| Cursor | 92 | 95% | 85ms | 优秀 |\n| Copilot | 88 | 90% | 60ms | 良好 |\n| Codeium | 85 | 88% | 70ms | 良好 |\n| Tabnine | 80 | 82% | 50ms | 一般 |\n| Cody | 78 | 80% | 90ms | 一般 |\n\n## 关键发现\n\n- **Cursor综合最强**：在准确率和上下文理解上领先\n- **Copilot最快**：补全速度最快，但准确率略低\n- **Codeium性价比最高**：免费方案质量不错\n- **本地模型差距明显**：Tabnine和Cody在复杂任务上表现较弱\n\n## 建议\n\n- 个人开发者：Cursor或Codeium（免费）\n- 团队使用：Cursor Business或Copilot Enterprise\n- 预算有限：Codeium免费版',
 '{"platform": "X/Twitter", "likes": 1900, "retweets": 567, "comments": 234, "authorHandle": "@levelsio", "contentType": "benchmark"}');
