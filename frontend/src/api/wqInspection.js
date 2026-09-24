import request from './request'

// ===================== 抽检批次 =====================

export function getWqBatchPage(params) {
  return request({ url: '/wq/batch/page', method: 'get', params })
}

export function getWqBatchDetail(id) {
  return request({ url: `/wq/batch/${id}`, method: 'get' })
}

export function saveWqDraft(data) {
  return request({ url: '/wq/batch/draft', method: 'post', data })
}

export function deleteWqDraft(id) {
  return request({ url: `/wq/batch/${id}/draft`, method: 'delete' })
}

export function submitWqBatch(data) {
  return request({ url: '/wq/batch/submit', method: 'post', data })
}

export function reviewWqBatch(data) {
  return request({ url: '/wq/batch/review', method: 'post', data })
}

export function submitWqRetest(data) {
  return request({ url: '/wq/batch/retest', method: 'post', data })
}

// ===================== 生效阈值 =====================

export function getWqThresholdList() {
  return request({ url: '/wq/threshold/list', method: 'get' })
}

export function updateWqThreshold(data) {
  return request({ url: '/wq/threshold', method: 'put', data })
}
