<template>
  <div class="page-container">
    <div class="tree-panel">
      <BuildingGroupTree
        ref="treeRef"
        @refresh-dispenser="onRefreshDispenser"
        @current-change="onCurrentGroupChange"
      />
    </div>
    <div class="main-panel">
      <div class="stat-row">
        <div class="stat-card">
          <div class="stat-label">园区总数</div>
          <div class="stat-value">{{ stats.parkCount }}</div>
        </div>
        <div class="stat-card success">
          <div class="stat-label">楼栋总数</div>
          <div class="stat-value">{{ stats.buildingCount }}</div>
        </div>
        <div class="stat-card warning">
          <div class="stat-label">楼层总数</div>
          <div class="stat-value">{{ stats.floorCount }}</div>
        </div>
        <div class="stat-card info">
          <div class="stat-label">饮水机设备</div>
          <div class="stat-value">{{ stats.dispenserCount }}</div>
        </div>
      </div>

      <WaterDispenserList
        ref="listRef"
        :current-group-id="currentGroupId"
        :current-group-name="currentGroupName"
        @refresh-tree="refreshTree"
        @stats-change="onStatsChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import BuildingGroupTree from '@/components/BuildingGroupTree.vue'
import WaterDispenserList from '@/components/WaterDispenserList.vue'
import { getBuildingGroupTree } from '@/api/buildingGroup'

const treeRef = ref(null)
const listRef = ref(null)
const currentGroupId = ref(null)
const currentGroupName = ref('全部区域')

const stats = reactive({
  parkCount: 0,
  buildingCount: 0,
  floorCount: 0,
  dispenserCount: 0
})

const countTree = (nodes) => {
  let park = 0, building = 0, floor = 0, dispenser = 0
  const traverse = (list) => {
    for (const node of list) {
      if (node.type === 1) park++
      else if (node.type === 2) building++
      else if (node.type === 3) floor++
      dispenser += node.deviceCount || 0
      if (node.children && node.children.length) {
        traverse(node.children)
      }
    }
  }
  traverse(nodes)
  return { parkCount: park, buildingCount: building, floorCount: floor, dispenserCount: dispenser }
}

const refreshTree = async () => {
  if (treeRef.value) {
    await treeRef.value.loadTree()
    loadStats()
  }
}

const loadStats = async () => {
  try {
    const res = await getBuildingGroupTree()
    const tree = res.data || []
    const counts = countTree(tree)
    Object.assign(stats, counts)
  } catch (e) {
    console.error(e)
  }
}

const onRefreshDispenser = () => {
  if (listRef.value) {
    listRef.value.loadList()
  }
}

const onCurrentGroupChange = (group) => {
  if (group) {
    currentGroupId.value = group.id
    currentGroupName.value = group.label
  } else {
    currentGroupId.value = null
    currentGroupName.value = '全部区域'
  }
}

const onStatsChange = (total) => {
  if (currentGroupId.value === null) {
    stats.dispenserCount = total
  }
}

onMounted(() => {
  loadStats()
})
</script>
