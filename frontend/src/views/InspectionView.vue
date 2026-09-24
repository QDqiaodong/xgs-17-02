<template>
  <div class="page-container" style="display: block;">
    <div class="panel-card">
      <div class="panel-header">
        <div class="panel-title">
          <el-icon><DocumentChecked /></el-icon>
          <span>饮水机水质抽检批次</span>
        </div>
        <div>
          <el-button type="success" :icon="Refresh" size="small" @click="loadList">刷新</el-button>
          <el-button type="primary" :icon="Plus" size="small" @click="handleCreate">发起抽检批次</el-button>
        </div>
      </div>

      <div class="panel-body">
        <div class="filter-bar">
          <el-tree-select
            v-model="filters.groupId"
            :data="groupTree"
            :props="{ label: 'label', value: 'id', children: 'children' }"
            check-strictly
            :render-after-expand="false"
            placeholder="按区域（园区/楼栋/楼层）"
            clearable
            style="width: 240px;"
            @change="onSearch"
          />
          <el-select v-model="filters.status" placeholder="批次状态" clearable style="width: 140px;" @change="onSearch">
            <el-option v-for="(label, val) in statusOptions" :key="val" :label="label" :value="val" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="采样开始日期"
            end-placeholder="采样结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px;"
            @change="onSearch"
          />
          <el-input
            v-model="filters.deviceNo"
            placeholder="设备编号"
            :prefix-icon="Search"
            clearable
            style="width: 180px;"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
          <el-button type="primary" plain :icon="Search" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>

        <el-table :data="tableData" v-loading="loading" border stripe>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="batchNo" label="批次编号" width="190" />
          <el-table-column prop="scopeGroupPath" label="抽检区域" min-width="220" show-overflow-tooltip />
          <el-table-column label="设备/不合格" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.deviceCount }}台</el-tag>
              <el-tag v-if="row.failCount > 0" size="small" type="danger" style="margin-left: 4px;">
                {{ row.failCount }}不合格
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small" effect="dark">
                {{ row.statusName }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submitter" label="提交人" width="100" />
          <el-table-column label="采样/提交时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.submitTime || row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="reviewer" label="复核人" width="100" />
          <el-table-column label="未关闭不合格项" width="130" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.openFailCount > 0" type="warning" size="small">
                {{ row.openFailCount }} 项待复检
              </el-tag>
              <span v-else style="color: #94a3b8;">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-button type="primary" link :icon="ZoomIn" size="small" @click="goDetail(row)">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadList"
            @current-change="loadList"
          />
        </div>
      </div>
    </div>

    <BatchForm ref="formRef" @success="onFormSuccess" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh, Search, ZoomIn, DocumentChecked } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getWqBatchPage } from '@/api/wqInspection'
import { getBuildingGroupTree } from '@/api/buildingGroup'
import BatchForm from '@/components/wq/BatchForm.vue'
import { formatTime } from '@/utils/wqFormat'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const groupTree = ref([])
const dateRange = ref([])

const filters = reactive({ groupId: null, status: '', deviceNo: '' })

const statusOptions = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待复核',
  RETURNED: '退回',
  PENDING_RETEST: '待复检',
  CLOSED: '已关闭'
}

const statusTagType = (status) => ({
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  RETURNED: 'danger',
  PENDING_RETEST: 'warning',
  CLOSED: 'success'
}[status] || 'info')

const loadList = async () => {
  loading.value = true
  try {
    const res = await getWqBatchPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      groupId: filters.groupId || undefined,
      status: filters.status || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      deviceNo: filters.deviceNo || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadTree = async () => {
  try {
    const res = await getBuildingGroupTree()
    groupTree.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const onSearch = () => {
  pageNum.value = 1
  loadList()
}

const onReset = () => {
  filters.groupId = null
  filters.status = ''
  filters.deviceNo = ''
  dateRange.value = []
  onSearch()
}

const handleCreate = () => {
  formRef.value.openCreate(filters.groupId)
}

const onFormSuccess = (id) => {
  loadList()
  if (id) {
    router.push(`/inspection/batch/${id}`)
  }
}

const goDetail = (row) => {
  router.push(`/inspection/batch/${row.id}`)
}

onMounted(() => {
  loadTree()
  loadList()
})
</script>
