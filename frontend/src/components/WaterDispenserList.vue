<template>
  <div class="panel-card">
    <div class="panel-header">
      <div class="panel-title">
        <el-icon><Tools /></el-icon>
        <span>饮水机设备档案</span>
        <el-tag v-if="currentGroupId" type="primary" effect="light" style="margin-left: 12px;">
          当前区域：{{ currentGroupName }}
        </el-tag>
      </div>
      <div style="display: flex; gap: 8px;">
        <el-button type="success" :icon="Refresh" size="small" @click="loadList">
          刷新
        </el-button>
        <el-button type="primary" :icon="Plus" size="small" @click="handleAdd">
          新增设备
        </el-button>
      </div>
    </div>
    <div class="panel-body">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索设备编号/型号"
          :prefix-icon="Search"
          clearable
          size="default"
          style="width: 260px;"
          @keyup.enter="loadList"
          @clear="loadList"
        />
        <el-select
          v-model="statusFilter"
          placeholder="设备状态"
          clearable
          size="default"
          style="width: 140px;"
          @change="loadList"
        >
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" plain :icon="Search" @click="loadList">
          查询
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%;">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="设备图片" width="100" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 6px;"
            />
            <div v-else style="color: #cbd5e1;">
              <el-icon :size="28"><Picture /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deviceNo" label="设备编号" width="130" />
        <el-table-column prop="model" label="型号" width="130">
          <template #default="{ row }">
            <el-tag type="warning" effect="light">{{ row.model }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="spec" label="安装规格" width="130" />
        <el-table-column prop="waterType" label="出水类型" min-width="160">
          <template #default="{ row }">
            <el-tag
              v-for="t in (row.waterType || '').split(',').filter(Boolean)"
              :key="t"
              size="small"
              type="info"
              effect="plain"
              style="margin: 2px;"
            >
              {{ t }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="groupPath" label="所属区域" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
            <el-tag v-if="row.pendingRetest === 1" type="warning" size="small" effect="plain"
              style="margin-top: 2px; display: block; width: fit-content; margin-left: auto; margin-right: auto;">
              待复检
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="installDate" label="安装日期" width="120" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="ZoomIn" size="small" @click="handleView(row)">
              详情
            </el-button>
            <el-button type="success" link :icon="Edit" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="warning" link :icon="Switch" size="small" @click="handleTransfer(row)">
              调组
            </el-button>
            <el-button type="danger" link :icon="Delete" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <WaterDispenserForm
      ref="formRef"
      :model-list="modelList"
      @success="onFormSuccess"
    />

    <WaterDispenserDetail
      ref="detailRef"
    />

    <TransferForm
      ref="transferRef"
      @success="onTransferSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import {
  Plus, Refresh, Search, Edit, Delete, ZoomIn, Switch, Picture, Tools
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWaterDispenserPage, deleteWaterDispenser } from '@/api/waterDispenser'
import { getWaterModels } from '@/api/common'
import WaterDispenserForm from './WaterDispenserForm.vue'
import WaterDispenserDetail from './WaterDispenserDetail.vue'
import TransferForm from './TransferForm.vue'

const props = defineProps({
  currentGroupId: { type: Number, default: null },
  currentGroupName: { type: String, default: '全部区域' }
})

const emit = defineEmits(['refresh-tree', 'stats-change'])

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref(null)
const modelList = ref([])

const formRef = ref(null)
const detailRef = ref(null)
const transferRef = ref(null)

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value
    }
    if (props.currentGroupId) {
      params.groupId = props.currentGroupId
    }
    const res = await getWaterDispenserPage(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
      emit('stats-change', total.value)
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadModels = async () => {
  try {
    const res = await getWaterModels()
    modelList.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const handleAdd = () => {
  formRef.value.open('add', null, props.currentGroupId)
}

const handleEdit = (row) => {
  formRef.value.open('edit', row.id, row.groupId)
}

const handleView = (row) => {
  detailRef.value.open(row.id)
}

const handleTransfer = (row) => {
  transferRef.value.open(row)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除设备「${row.deviceNo}」吗？此操作不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    const res = await deleteWaterDispenser(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadList()
      emit('refresh-tree')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

const onFormSuccess = () => {
  loadList()
  emit('refresh-tree')
}

const onTransferSuccess = () => {
  loadList()
  emit('refresh-tree')
}

watch(() => props.currentGroupId, () => {
  pageNum.value = 1
  loadList()
})

onMounted(() => {
  loadList()
  loadModels()
})

defineExpose({ loadList })
</script>
