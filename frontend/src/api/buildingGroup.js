import request from './request'

export function getBuildingGroupTree() {
  return request({
    url: '/building-group/tree',
    method: 'get'
  })
}

export function getBuildingGroupLeafList() {
  return request({
    url: '/building-group/leaf-list',
    method: 'get'
  })
}

export function getBuildingGroup(id) {
  return request({
    url: `/building-group/${id}`,
    method: 'get'
  })
}

export function saveBuildingGroup(data) {
  return request({
    url: '/building-group',
    method: 'post',
    data
  })
}

export function updateBuildingGroup(data) {
  return request({
    url: '/building-group',
    method: 'put',
    data
  })
}

export function deleteBuildingGroup(id) {
  return request({
    url: `/building-group/${id}`,
    method: 'delete'
  })
}
