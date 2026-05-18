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
