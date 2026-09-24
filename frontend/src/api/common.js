import request from './request'

export function getWaterModels() {
  return request({
    url: '/common/models',
    method: 'get'
  })
}

export function getWaterModelParams(name) {
  return request({
    url: `/common/models/${encodeURIComponent(name)}`,
    method: 'get'
  })
}

export function refreshModelCache() {
  return request({
    url: '/common/models/refresh',
    method: 'post'
  })
}

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/common/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
