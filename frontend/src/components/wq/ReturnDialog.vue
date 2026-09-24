<template>
  <el-dialog v-model="visible" title="整批退回" width="520px" destroy-on-close>
    <el-alert type="error" :closable="false" show-icon style="margin-bottom: 12px;">
      <template #title>退回后批次回到提交人处，可修改采样数据并重新提交（阈值重新固化）。</template>
    </el-alert>
    <el-form label-width="80px">
      <el-form-item label="复核人">
        <el-input :model-value="currentUser" disabled />
      </el-form-item>
      <el-form-item label="退回原因" required>
        <el-input v-model="reason" type="textarea" :rows="4" maxlength="500" show-word-limit
          placeholder="请说明退回原因，提交人将据此整改" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="loading" @click="confirm">确认整批退回</el-button>
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
const reason = ref('')

const open = () => {
  reason.value = ''
  visible.value = true
}

const confirm = async () => {
  if (!reason.value.trim()) {
    ElMessage.warning('请填写退回原因')
    return
  }
  loading.value = true
  try {
    const res = await reviewWqBatch({
      batchId: props.batch.id,
      reviewer: currentUser.value,
      requestId: newRequestId(),
      action: 'RETURN',
      reason: reason.value.trim()
    })
    if (res.code === 200) {
      ElMessage.success('已整批退回')
      visible.value = false
      emit('done')
    }
  } catch (e) {
    // 409：已被他人处理，父页面会刷新到最新状态
    visible.value = false
    emit('done')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
