<template>
  <div class="search-page">
    <!-- 筛选面板 -->
    <div class="panel-card">
      <div class="panel-header">
        <div class="panel-title">
          <el-icon><Search /></el-icon>
          <span>全园设备快速检索</span>
          <el-tag v-if="selectedGroupPath" type="primary" effect="light" style="margin-left: 8px;">
            区域范围：{{ selectedGroupPath }}（含全部下级）
          </el-tag>
        </div>
        <div style="display:flex; gap:8px;">
          <el-button :icon="Refresh" size="small" @click="reload">刷新结果</el-button>
        </div>
      </div>
      <div class="panel-body">
        <el-form :inline="true" class="search-form" @submit.prevent>
          <el-form-item label="区域范围">
            <el-tree-select
              v-model="filters.groupId"
              :data="treeData"
              :props="{ label: 'label', children: 'children', value: 'id' }"
              node-key="id"
              check-strictly
              :render-after-expand="false"
              clearable
              filterable
              placeholder="全部区域（含所有园区/楼栋/楼层）"
              style="width: 300px;"
            />
          </el-form-item>
          <el-form-item label="设备编号">
            <el-input
              v-model="filters.deviceNo"
              placeholder="按编号前缀，如 WD2024"
              clearable
              style="width: 190px;"
              @keyup.enter="applyFilters"
            />
          </el-form-item>
          <el-form-item label="型号">
            <el-select v-model="filters.model" clearable filterable placeholder="全部型号"
                       style="width: 170px;">
              <el-option v-for="m in facets.models" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="安装规格">
            <el-select v-model="filters.spec" clearable filterable placeholder="全部规格"
                       style="width: 150px;">
              <el-option v-for="s in facets.specs" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item label="出水类型">
            <el-select v-model="filters.waterType" clearable placeholder="全部出水类型"
                       style="width: 140px;">
              <el-option v-for="w in facets.waterTypes" :key="w" :label="w" :value="w" />
            </el-select>
          </el-form-item>
          <el-form-item label="启用状态">
            <el-select v-model="filters.status" clearable placeholder="全部"
                       style="width: 110px;">
              <el-option label="正常" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="待复检">
            <el-select v-model="filters.pendingRetest" clearable placeholder="全部"
                       style="width: 120px;">
              <el-option label="仅待复检" :value="1" />
              <el-option label="仅正常" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="applyFilters">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 游标过期/失效提示：保留筛选条件，只清空翻页位置 -->
        <el-alert
          v-if="expiredNotice"
          :title="expiredNotice"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 12px;"
        >
          <template #default>
            <div style="display:flex; align-items:center; gap:12px;">
              <span>{{ expiredNotice }} 当前筛选条件已保留。</span>
              <el-button type="primary" size="small" @click="restartFromFirstPage">从第一页重新检索</el-button>
            </div>
          </template>
        </el-alert>

        <div class="result-toolbar">
          <div class="sort-area">
            <span class="sort-label">排序：</span>
            <el-radio-group v-model="filters.sort" size="small" @change="applyFilters">
              <el-radio-button label="createTime">建档时间</el-radio-button>
              <el-radio-button label="deviceNo">设备编号</el-radio-button>
              <el-radio-button label="model">型号</el-radio-button>
              <el-radio-button label="installDate">安装日期</el-radio-button>
            </el-radio-group>
            <el-radio-group v-model="filters.order" size="small" @change="applyFilters"
                            style="margin-left: 8px;">
              <el-radio-button label="desc">降序</el-radio-button>
              <el-radio-button label="asc">升序</el-radio-button>
            </el-radio-group>
          </div>
          <el-select v-model="filters.pageSize" size="small" style="width: 130px;"
                     @change="applyFilters">
            <el-option :value="20" label="20 条/页" />
            <el-option :value="50" label="50 条/页" />
            <el-option :value="100" label="100 条/页" />
            <el-option :value="200" label="200 条/页" />
          </el-select>
        </div>

        <el-table :data="records" v-loading="loading" border stripe style="width: 100%;">
          <el-table-column prop="deviceNo" label="设备编号" width="130" />
          <el-table-column prop="model" label="型号" width="120">
            <template #default="{ row }">
              <el-tag type="warning" effect="light" size="small">{{ row.model }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="安装规格" width="120" />
          <el-table-column prop="waterType" label="出水类型" min-width="150">
            <template #default="{ row }">
              <el-tag v-for="t in (row.waterType || '').split(',').filter(Boolean)"
                      :key="t" size="small" type="info" effect="plain"
                      style="margin: 2px;">{{ t }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="groupPath" label="完整区域路径" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <el-icon color="#1d4ed8" style="vertical-align: -2px;"><Location /></el-icon>
              <span style="margin-left:4px;">{{ row.groupPath }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="dark">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="待复检" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.pendingRetest === 1" type="warning" size="small">待复检</el-tag>
              <span v-else style="color:#94a3b8;">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="installDate" label="安装日期" width="110" />
          <el-table-column label="操作" width="90" fixed="right" align="center">
            <template #default="{ row }">
              <el-button type="primary" link :icon="ZoomIn" size="small" @click="openDetail(row)">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager-bar">
          <div class="page-indicator">
            <el-tooltip v-if="source" :content="sourceTip" placement="top">
              <span class="source-note">数据来源：{{ source === 'cache' ? '缓存' : '数据库' }}</span>
            </el-tooltip>
            <span>第 {{ pageNo }} 页</span>
            <span v-if="!hasMore && records.length > 0" class="end-note">— 已到结果末尾</span>
            <span v-else-if="records.length === 0 && !loading" class="end-note">没有符合条件的设备</span>
          </div>
          <div>
            <el-button size="small" :disabled="!canGoPrev || loading"
                       @click="goPrev">上一页</el-button>
            <el-button size="small" type="primary" :disabled="!hasMore || loading"
                       @click="goNext">下一页</el-button>
          </div>
        </div>
      </div>
    </div>

    <WaterDispenserDetail ref="detailRef" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, RefreshLeft, ZoomIn, Location } from '@element-plus/icons-vue'
import { searchDevices, getDeviceFacets } from '@/api/waterDispenser'
import { getBuildingGroupTree } from '@/api/buildingGroup'
import WaterDispenserDetail from '@/components/WaterDispenserDetail.vue'

/**
 * 地址（route.query）是检索视图唯一状态源：
 * - 筛选条件 / 排序 / pageSize / 当前页游标全部在地址里，刷新、前进后退、复制链接均可恢复；
 * - 任何条件变化都生成新指纹 fp 并从第一页开始，绝不沿用旧翻页位置；
 * - 翻页只改地址，由 route watcher 统一发请求，杜绝双触发与页码错位。
 *
 * 上一页所需的游标栈存 sessionStorage：
 * 同一浏览器连续浏览可逐页回退；复制给同事的深页链接即使本地无栈，当前页也能正常查看。
 */
const STACK_KEY = 'device-search-cursor-stack-v1'

const route = useRoute()
const router = useRouter()
const detailRef = ref(null)

const loading = ref(false)
const records = ref([])
const hasMore = ref(false)
const source = ref('')
const sourceTip = '缓存服务不可用时自动回到数据库查询，过滤规则与排序不变'
const expiredNotice = ref('')

const treeData = ref([])
const treeNodeMap = ref(new Map())

const facets = reactive({ models: [], specs: [], waterTypes: ['冷水', '热水', '温水', '冰水'] })

const FILTER_DEFAULTS = {
  groupId: null,
  deviceNo: '',
  model: '',
  spec: '',
  waterType: '',
  status: null,
  pendingRetest: null,
  sort: 'createTime',
  order: 'desc',
  pageSize: 20
}
/** 表单绑定值；由地址同步而来，用户编辑只改本地，点查询/改排序才写回地址 */
const filters = reactive({ ...FILTER_DEFAULTS })

const stack = () => {
  try { return JSON.parse(sessionStorage.getItem(STACK_KEY) || '{}') } catch { return {} }
}
const saveStack = (s) => sessionStorage.setItem(STACK_KEY, JSON.stringify(s))

// ---------------- 地址 <-> 筛选 ----------------

const readFiltersFromRoute = () => {
  const q = route.query
  return {
    groupId: q.groupId ? Number(q.groupId) : null,
    deviceNo: q.deviceNo || '',
    model: q.model || '',
    spec: q.spec || '',
    waterType: q.waterType || '',
    status: q.status === undefined || q.status === '' ? null : Number(q.status),
    pendingRetest: q.pending === undefined || q.pending === '' ? null : Number(q.pending),
    sort: q.sort || 'createTime',
    order: q.order || 'desc',
    pageSize: Number(q.size) || 20
  }
}

const buildQuery = (overrides = {}) => {
  const q = {}
  if (filters.groupId) q.groupId = filters.groupId
  if (filters.deviceNo) q.deviceNo = filters.deviceNo
  if (filters.model) q.model = filters.model
  if (filters.spec) q.spec = filters.spec
  if (filters.waterType) q.waterType = filters.waterType
  if (filters.status !== null && filters.status !== '') q.status = filters.status
  if (filters.pendingRetest !== null && filters.pendingRetest !== '') q.pending = filters.pendingRetest
  if (filters.sort !== 'createTime') q.sort = filters.sort
  if (filters.order !== 'desc') q.order = filters.order
  if (filters.pageSize !== 20) q.size = filters.pageSize
  for (const [k, v] of Object.entries(overrides)) {
    if (v === null || v === undefined || v === '') delete q[k]
    else q[k] = v
  }
  return q
}

/** 条件指纹内容：只含筛选/排序/页大小，不含游标和页码 */
const fingerprintOf = () => JSON.stringify(buildQuery())

const fp = computed(() => route.query.fp || '')
const pageNo = computed(() => Number(route.query.page) || 1)
const currentCursor = computed(() => route.query.cursor || '')

const selectedGroupPath = computed(() => {
  if (!filters.groupId) return ''
  const node = treeNodeMap.value.get(Number(filters.groupId))
  return node ? node.path : ''
})

/** 上一页依赖本地游标栈；复制来的深页链接没有栈时禁用 */
const canGoPrev = computed(() => {
  if (pageNo.value <= 1) return false
  const entry = stack()[fp.value]
  return !!entry && Object.prototype.hasOwnProperty.call(entry, String(pageNo.value - 1))
})

// ---------------- 用户动作（只改地址） ----------------

/** 任一筛选条件/排序/页大小变化：新指纹、第一页、无游标 */
const applyFilters = () => {
  expiredNotice.value = ''
  const newFp = fingerprintOf()
  const s = stack()
  s[newFp] = { 1: '' }
  saveStack(s)
  router.push({ query: buildQuery({ fp: newFp }) })
}

const resetFilters = () => {
  Object.assign(filters, FILTER_DEFAULTS)
  applyFilters()
}

/** 游标过期/失效：保留筛选条件，从起点重查 */
const restartFromFirstPage = () => {
  const s = stack()
  s[fp.value] = { 1: '' }
  saveStack(s)
  router.replace({ query: buildQuery({ fp: fp.value }) })
}

const goNext = () => {
  if (!hasMore.value) return
  const entry = stack()[fp.value] || {}
  const nextCursor = entry[String(pageNo.value + 1)]
  if (!nextCursor) {
    ElMessage.warning('请先从当前页重新获取翻页位置')
    return
  }
  router.push({ query: buildQuery({ fp: fp.value, page: pageNo.value + 1, cursor: nextCursor }) })
}

const goPrev = () => {
  if (!canGoPrev.value) return
  const prev = pageNo.value - 1
  const entry = stack()[fp.value] || {}
  const cursor = entry[String(prev)] || ''
  router.push({
    query: buildQuery({
      fp: fp.value,
      page: prev > 1 ? prev : null,
      cursor: cursor || null
    })
  })
}

const reload = () => loadPage()

const openDetail = (row) => {
  // 弹窗不改变地址也不卸载列表：关闭后筛选条件、页码、滚动位置原样保留
  detailRef.value.open(row.id)
}

// ---------------- 数据加载（由地址驱动） ----------------

const loadPage = async () => {
  Object.assign(filters, readFiltersFromRoute())
  loading.value = true
  try {
    const res = await searchDevices({
      groupId: filters.groupId || undefined,
      deviceNo: filters.deviceNo || undefined,
      model: filters.model || undefined,
      spec: filters.spec || undefined,
      waterType: filters.waterType || undefined,
      status: filters.status === null || filters.status === '' ? undefined : filters.status,
      pendingRetest: filters.pendingRetest === null || filters.pendingRetest === ''
        ? undefined : filters.pendingRetest,
      sort: filters.sort,
      order: filters.order,
      pageSize: filters.pageSize,
      cursor: currentCursor.value || undefined
    })
    if (res.code === 200 && res.data) {
      records.value = res.data.records || []
      hasMore.value = !!res.data.hasMore
      source.value = res.data.source || ''
      expiredNotice.value = ''

      // 记录“进入下一页”所需游标，供下一页与浏览器后退恢复
      if (res.data.nextCursor) {
        const s = stack()
        const entry = s[fp.value] || { 1: '' }
        entry[String(pageNo.value + 1)] = res.data.nextCursor
        s[fp.value] = entry
        saveStack(s)
      }
    }
  } catch (e) {
    if (e.code === 410) {
      // 过期/篡改/换条件：筛选条件仍在地址里保留，仅提示并引导回起点
      expiredNotice.value = e.message || '翻页位置已过期'
      hasMore.value = false
      records.value = []
    } else {
      ElMessage.error(e.message || '检索失败')
    }
  } finally {
    loading.value = false
  }
}

watch(() => route.fullPath, (full) => {
  // 地址变化（查询/翻页/前进后退/首次规范化）一律按地址加载，绝不沿用旧翻页位置
  lastUrlSignature = full
  loadPage()
})

// ---------------- 初始化 ----------------

const flattenTree = (nodes, parents = []) => {
  for (const n of nodes) {
    const path = [...parents, n.label].join(' / ')
    treeNodeMap.value.set(n.id, { ...n, path })
    if (n.children && n.children.length) {
      flattenTree(n.children, [...parents, n.label])
    }
  }
}

const loadTree = async () => {
  try {
    const res = await getBuildingGroupTree()
    treeData.value = res.data || []
    treeNodeMap.value = new Map()
    flattenTree(treeData.value)
  } catch (e) {
    console.error(e)
  }
}

const loadFacets = async () => {
  try {
    const res = await getDeviceFacets()
    if (res.code === 200 && res.data) {
      facets.models = res.data.models || []
      facets.specs = res.data.specs || []
      facets.waterTypes = res.data.waterTypes || facets.waterTypes
    }
  } catch (e) {
    console.error(e)
  }
}

onMounted(async () => {
  Object.assign(filters, readFiltersFromRoute())
  await Promise.all([loadTree(), loadFacets()])

  // 地址缺少指纹时（旧链接/首次进入）规范化地址；watcher 随后触发首页加载
  if (!fp.value) {
    const newFp = fingerprintOf()
    const s = stack()
    if (!s[newFp]) s[newFp] = { 1: '' }
    saveStack(s)
    await router.replace({ query: buildQuery({ fp: newFp }) })
  } else {
    // 深页链接：把携带的游标登记进栈，使“上一页”边界正确（再上一页需要重新翻入）
    const s = stack()
    const entry = s[fp.value] || { 1: '' }
    if (pageNo.value > 1 && currentCursor.value) {
      entry[String(pageNo.value)] = currentCursor.value
      s[fp.value] = entry
      saveStack(s)
    }
    loadPage()
  }
})
</script>

<style lang="scss" scoped>
.search-page {
  .panel-card { margin-bottom: 0; }
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 4px;

  :deep(.el-form-item) {
    margin-bottom: 12px;
    margin-right: 12px;
  }
}

.result-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;

  .sort-area {
    display: flex;
    align-items: center;
    .sort-label {
      font-size: 13px;
      color: #475569;
      margin-right: 4px;
    }
  }
}

.pager-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;

  .page-indicator {
    font-size: 13px;
    color: #475569;
    display: flex;
    gap: 12px;
    align-items: center;

    .source-note {
      color: #0e7490;
      background: #ecfeff;
      border: 1px solid #a5f3fc;
      padding: 2px 8px;
      border-radius: 10px;
      cursor: help;
    }
    .end-note { color: #94a3b8; }
  }
}
</style>
