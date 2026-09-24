import request from './request'

export function getWaterDispenserPage(params) {
  return request({
    url: '/water-dispenser/page',
    method: 'get',
    params
  })
}

export function getWaterDispenser(id) {
  return request({
    url: `/water-dispenser/${id}`,
    method: 'get'
  })
}

export function saveWaterDispenser(data) {
  return request({
    url: '/water-dispenser',
    method: 'post',
    data
  })
}

export function updateWaterDispenser(data) {
  return request({
    url: '/water-dispenser',
    method: 'put',
    data
  })
}

export function deleteWaterDispenser(id) {
  return request({
    url: `/water-dispenser/${id}`,
    method: 'delete'
  })
}

export function getTransferLogPage(params) {
  return request({
    url: '/water-dispenser/transfer-log/page',
    method: 'get',
    params
  })
}
