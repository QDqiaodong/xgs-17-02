<template>
  <el-dialog v-model="visible" title="复核确认：合格项 / 开启复检" width="860px" destroy-on-close>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px;">
      <template #title>
        合格设备将确认合格；不合格设备的每个不合格指标都会开启复检，批次进入「待复检」。全部合格则批次直接关闭。
      </template>
    </el-alert>

    <el-table :data="rows" border size="small" max-height="420">
      <el-table-column prop="deviceNoSnapshot" label="设备编号" width="150" />
      <el-table-column prop="groupPathSnapshot" label="采样位置" min-width="180" show-overflow-tooltip />
      <el-table-column label="自动判定" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.itemResult === 'PASS' ? 'success' : 'danger'" size="small">
            {{ row.itemResultName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="不合格指标" width="180">
        <template #default="{ row }">
          <el-tag v-for="m in row.failMetrics" :key="m" type="danger" size="small" style="margin: 2px;">
            {{ metricLabel(m) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="复核意见" min-width="200">
        <template #default="{ row }">
          <el-input v-model="row.comment" placeholder="可填写复核意见" size="small" />
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="confirm">确认复核结果</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { reviewWqBatch } from '@/api/wqInspection'
import { currentUser, newRequestId } from '@/utils/user'

const props = defineProps({
  batch: { type: Object, required: true }
})
const emit = defineEmits(['done'])

const visible = ref(false)
const loading = ref(false)
const rows = ref([])

const metricNameMap = {
  residual_chlorine: '余氯',
  turbidity: '浊度',
  temperature: '温度',
  odor: '气味'
}
const metricLabel = (code) => metricNameMap[code] || code

const open = () => {
  rows.value = (props.batch.items || []).map(i => ({
    sampleItemId: i.id,
    itemResult: i.itemResult,
    itemResultName: i.itemResultName,
    deviceNoSnapshot: i.deviceNoSnapshot,
    groupPathSnapshot: i.groupPathSnapshot,
    failMetrics: i.failMetrics || [],
    comment: i.reviewComment || ''
  }))
  visible.value = true
}

const confirm = async () => {
  loading.value = true
  try {
    const res = await reviewWqBatch({
      batchId: props.batch.id,
      reviewer: currentUser.value,
      requestId: newRequestId(),
      action: 'RETEST',
      itemReviews: rows.value.map(r => ({
        sampleItemId: r.sampleItemId,
        // 严格以后端固化阈值判定为准：判定合格 -> 确认合格；不合格 -> 开启复检
        reviewResult: r.itemResult === 'PASS' ? 'PASS' : 'RETEST',
        comment: r.comment
      }))
    })
    if (res.code === 200) {
      ElMessage.success('复核完成')
      visible.value = false
      emit('done')
    }
  } catch (e) {
    visible.value = false
    emit('done')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
