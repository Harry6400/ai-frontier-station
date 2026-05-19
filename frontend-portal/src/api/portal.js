import http from './http'

export function getPortalHome() {
  return http.get('/portal/home')
}

export function getPortalContents(params) {
  return http.get('/portal/contents', { params })
}

export function getPortalContentDetail(id) {
  return http.get(`/portal/contents/${id}`)
}

// 论文列表（支持子分类过滤）
export function getPapers(params) {
  return http.get('/portal/contents', {
    params: { contentType: 'paper', ...params }
  })
}

// 通用内容列表（按类型过滤）
export function getContentByType(contentType, params = {}) {
  return http.get('/portal/contents', {
    params: { contentType, ...params }
  })
}
