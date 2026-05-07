const CONTENT_TYPE_META = {
  news: { label: 'AI资讯', english: 'News Pulse' },
  paper: { label: '论文速递', english: 'Research Digest' },
  project: { label: '热门项目', english: 'Project Watch' },
  company_update: { label: '公司动态', english: 'Company Signal' },
  practice: { label: '技术实践', english: 'Practice Note' }
}

const SOURCE_TYPE_META = {
  github: { label: 'GitHub', description: '适合映射仓库、Star 趋势、项目语言和 README 摘要。' },
  paper: { label: '论文站点', description: '适合映射 arXiv ID、会议、作者、摘要和项目主页。' },
  official_blog: { label: '官方博客', description: '适合映射公司、公告链接、发布时间和产品方向。' },
  community: { label: '技术社区', description: '适合映射工程实践、讨论热度和作者链接。' },
  leaderboard: { label: '结构化榜单', description: '适合映射 Arena、HELM、MTEB、SWE-bench 等榜单条目和指标。' },
  manual: { label: '人工录入', description: '适合人工精选、临时记录和自定义来源内容。' }
}

const EXTRA_FIELD_META = {
  externalType: { label: '外部内容类型', note: '后续采集模块可用它判断 GitHub 仓库、论文或官方公告。' },
  githubRepo: { label: 'GitHub 仓库', note: '用于保存 owner/repo 形式的仓库标识。' },
  starTrend: { label: 'Star 趋势', note: '用于未来热度排序或项目榜单。' },
  language: { label: '主要语言', note: '用于 GitHub 项目筛选。' },
  paperVenue: { label: '论文来源', note: '用于论文栏目展示会议、期刊或预印本站点。' },
  arxivId: { label: 'arXiv ID', note: '用于后续同步论文元数据。' },
  companyFocus: { label: '公司/产品焦点', note: '用于公司动态栏目聚合。' },
  practiceScope: { label: '实践范围', note: '用于技术实践内容分类。' },
  difficulty: { label: '实践难度', note: '用于未来给开发者做筛选。' }
}

const EXTERNAL_REF_META = {
  github_repo: { label: 'GitHub 仓库', note: '后续可同步 Star、语言、README 和更新趋势。' },
  arxiv_paper: { label: 'arXiv 论文', note: '后续可同步作者、摘要、版本和项目主页。' },
  official_post: { label: '官方公告', note: '后续可同步公司博客、产品公告和发布时间。' },
  community_practice: { label: '社区实践', note: '后续可同步工程经验、讨论热度和作者链接。' },
  leaderboard_item: { label: '结构化榜单', note: '后续可同步模型排名、评测指标和榜单更新时间。' },
  manual_source: { label: '人工来源', note: '人工录入并经过 AI 总结的原始来源。' }
}

export function getContentTypeMeta(type) {
  return CONTENT_TYPE_META[type] || { label: type || '内容', english: 'Content' }
}

export function getSourceTypeMeta(type) {
  return SOURCE_TYPE_META[type] || { label: type || '未设置来源类型', description: '自定义来源类型，可在后台继续维护。' }
}

export function getExternalRefMeta(type) {
  return EXTERNAL_REF_META[type] || { label: type || '外部引用', note: '外部系统中的原始记录，可用于后续同步和追踪。' }
}

export function formatDate(value) {
  if (!value) {
    return '未设置日期'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date)
}

export function formatDateTime(value) {
  if (!value) {
    return '未设置时间'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date)
}

export function parseExtraJson(raw) {
  if (!raw) {
    return []
  }
  try {
    const parsed = JSON.parse(raw)
    return Object.entries(parsed).map(([key, value]) => ({
      key,
      value,
      label: EXTRA_FIELD_META[key]?.label || key,
      note: EXTRA_FIELD_META[key]?.note || '自定义扩展字段，后续可继续映射到采集或推荐模块。'
    }))
  } catch (error) {
    return [{
      key: 'extraJson',
      value: raw,
      label: '扩展字段原文',
      note: '当前 JSON 格式无法解析，前台按原文展示。'
    }]
  }
}
