<template>
  <div class="page-container" style="display: block;">
    <div class="panel-card">
      <div class="panel-header">
        <div class="panel-title">
          <el-icon><SetUp /></el-icon>
          <span>水质指标生效阈值</span>
        </div>
        <el-button type="success" :icon="Refresh" size="small" @click="load">刷新</el-button>
      </div>
      <div class="panel-body">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px;">
          <template #title>
            阈值调整只对之后新提交的批次生效；已提交批次（含复检）始终按提交时固化的阈值快照判定，历史结论不会被改写。
          </template>
        </el-alert>

        <el-table :data="list" v-loading="loading" border>
          <el-table-column prop="metricName" label="指标" width="120" />
          <el-table-column prop="unit" label="单位" width="90" align="center" />
          <el-table-column label="判定方式" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.judgeType === 'range' ? '数值区间' : '结论枚举' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="合格范围 / 合格结论" min-width="320">
            <template #default="{ row }">
              <template v-if="row.judgeType === 'range'">
                <el-input-number v-model="row.minValue" :precision="3" :step="0.01" :controls="false" size="small" style="width: 130px;" />
                <span style="margin: 0 8px;">~</span>
                <el-input-number v-model="row.maxValue" :precision="3" :step="0.01" :controls="false" size="small" style="width: 130px;" />
                <span style="margin-left: 8px; color: #94a3b8;">{{ row.unit }}（含边界）</span>
              </template>
              <el-input v-else v-model="row.passValues" placeholder="多个合格结论用英文逗号分隔，如：无异味,正常" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0"
                active-text="启用" inactive-text="停用" inline-prompt />
            </template>
          </el-table-column>
          <el-table-column label="最近维护" width="170">
            <template #default="{ row }">
              <div style="font-size: 12px; color: #64748b;">
                {{ row.updateOperator || '—' }} · {{ formatTime(row.updateTime) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" :loading="savingId === row.id" @click="save(row)">
                保存
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { SetUp, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getWqThresholdList, updateWqThreshold } from '@/api/wqInspection'
import { currentUser } from '@/utils/user'
import { formatTime } from '@/utils/wqFormat'

const list = ref([])
const loading = ref(false)
const savingId = ref(null)

const load = async () => {
  loading.value = true
  try {
    const res = await getWqThresholdList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const save = async (row) => {
  if (row.judgeType === 'range') {
    if (row.minValue === null || row.maxValue === null) {
      return ElMessage.warning(`${row.metricName}上下限不能为空`)
    }
    if (Number(row.minValue) > Number(row.maxValue)) {
      return ElMessage.warning(`${row.metricName}下限不能大于上限`)
    }
  } else if (!row.passValues || !row.passValues.trim()) {
    return ElMessage.warning(`${row.metricName}合格结论不能为空`)
  }
  savingId.value = row.id
  try {
    const res = await updateWqThreshold({
      id: row.id,
      minValue: row.minValue,
      maxValue: row.maxValue,
      passValues: row.passValues,
      enabled: row.enabled,
      operator: currentUser.value
    })
    if (res.code === 200) {
      ElMessage.success(res.message || '阈值已更新')
      load()
    }
  } finally {
    savingId.value = null
  }
}

onMounted(load)
</script>
