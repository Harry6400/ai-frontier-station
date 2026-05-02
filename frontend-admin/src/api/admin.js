import http from './http'

export function fetchDashboardSnapshot() {
  return http.get('/admin/dashboard/overview')
}

export function getApiSettingsStatus() {
  return http.get('/admin/api-settings/status')
}

export function saveApiCredential(provider, payload) {
  return http.put(`/admin/api-settings/${provider}`, payload)
}

export function clearApiCredential(provider) {
  return http.delete(`/admin/api-settings/${provider}`)
}

export function getCategories() {
  return http.get('/admin/categories')
}

export function createCategory(payload) {
  return http.post('/admin/categories', payload)
}

export function updateCategory(id, payload) {
  return http.put(`/admin/categories/${id}`, payload)
}

export function removeCategory(id) {
  return http.delete(`/admin/categories/${id}`)
}

export function getTags() {
  return http.get('/admin/tags')
}

export function createTag(payload) {
  return http.post('/admin/tags', payload)
}

export function updateTag(id, payload) {
  return http.put(`/admin/tags/${id}`, payload)
}

export function removeTag(id) {
  return http.delete(`/admin/tags/${id}`)
}

export function getSources() {
  return http.get('/admin/sources')
}

export function createSource(payload) {
  return http.post('/admin/sources', payload)
}

export function updateSource(id, payload) {
  return http.put(`/admin/sources/${id}`, payload)
}

export function removeSource(id) {
  return http.delete(`/admin/sources/${id}`)
}

export function getContentOptions() {
  return http.get('/admin/contents/options')
}

export function getContents(params) {
  return http.get('/admin/contents', { params })
}

export function getContentDetail(id) {
  return http.get(`/admin/contents/${id}`)
}

export function createContent(payload) {
  return http.post('/admin/contents', payload)
}

export function updateContent(id, payload) {
  return http.put(`/admin/contents/${id}`, payload)
}

export function updateContentStatus(id, publishStatus) {
  return http.put(`/admin/contents/${id}/status`, { publishStatus })
}

export function importGithubRepo(payload) {
  return http.post('/admin/import/github-repo', payload)
}

export function queryGithubRepo(fullName) {
  return http.get('/admin/github/repo', { params: { fullName } })
}

export function searchGithubRepos(params) {
  return http.get('/admin/github/search', { params })
}

export function summarizeAiSource(payload) {
  return http.post('/admin/ai/source-summary', payload)
}

export function importAiSource(payload) {
  return http.post('/admin/import/ai-source', payload)
}

export function searchArxivPapers(params) {
  return http.get('/admin/arxiv/search', { params })
}

export function findArxivPaper(arxivId) {
  return http.get('/admin/arxiv/paper', { params: { arxivId } })
}

export function importArxivPaper(payload) {
  return http.post('/admin/import/arxiv-paper', payload)
}

export function getHuggingFaceDailyPapers() {
  return http.get('/admin/huggingface/papers')
}

export function importHuggingFacePaper(payload) {
  return http.post('/admin/import/huggingface-paper', payload)
}

export function createContentExternalRef(contentId, payload) {
  return http.post(`/admin/contents/${contentId}/external-refs`, payload)
}

export function updateContentExternalRef(contentId, refId, payload) {
  return http.put(`/admin/contents/${contentId}/external-refs/${refId}`, payload)
}

export function removeContentExternalRef(contentId, refId) {
  return http.delete(`/admin/contents/${contentId}/external-refs/${refId}`)
}

export function removeContent(id) {
  return http.delete(`/admin/contents/${id}`)
}
