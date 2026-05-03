import request from '@/utils/request'

export function pageResource(baseUrl, query) {
  return request({
    url: `${baseUrl}/page`,
    method: 'get',
    params: query
  })
}

export function listResource(baseUrl, query) {
  return request({
    url: `${baseUrl}/list`,
    method: 'get',
    params: query
  })
}

export function getResource(baseUrl, id) {
  return request({
    url: `${baseUrl}/detail`,
    method: 'get',
    params: { id }
  })
}

export function saveResource(baseUrl, data) {
  return request({
    url: `${baseUrl}/save`,
    method: 'post',
    data
  })
}

export function delResource(baseUrl, ids) {
  return request({
    url: `${baseUrl}/delete`,
    method: 'post',
    data: { ids: Array.isArray(ids) ? ids : [ids] }
  })
}

export function dashboardSummary() {
  return request({
    url: '/api/v1/system/dashboard/summary',
    method: 'get'
  })
}
