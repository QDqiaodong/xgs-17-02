import request from './request'

export function getWaterDispenserPage(params) {
  return request({
    url: '/water-dispenser/page',
    method: 'get',
    params
  })
}

/**
 * 全园设备快速检索（游标键集分页）
 * params: groupId/deviceNo/model/spec/waterType/status/pendingRetest/sort/order/pageSize/cursor
 */
export function searchDevices(params) {
  return request({
    url: '/water-dispenser/search',
    method: 'get',
    params
  })
}

/** 筛选面板可选项：型号 / 安装规格 / 出水类型（区域树仍用 building-group/tree） */
export function getDeviceFacets() {
  return request({
    url: '/water-dispenser/search/facets',
    method: 'get'
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
