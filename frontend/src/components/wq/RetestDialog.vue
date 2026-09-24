<template>
  <el-dialog v-model="visible" title="提交复检结果" width="560px" destroy-on-close>
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px;">
      <template #title>
        复检关联原不合格项，判定沿用批次提交时固化的阈值；历次复检结果全部保留。
        同一设备全部未关闭不合格项复检合格后才解除“待复检”，停用设备不会被自动启用。
      </template>
    </el-alert>

    <el-descriptions :column="2" border size="small" style="margin-bottom: 12px;">
      <el-descriptions-item label="批次">{{ batchNo }}</el-descriptions-item>
      <el-descriptions-item label="设备编号">{{ failItem?.deviceNoSnapshot }}</el-descriptions-item>
      <el-descriptions-item label="复检指标">{{ failItem?.metricName }}</el-descriptions-item>
      <el-descriptions-item label="原始采样值">{{ failItem?.metricValue }}</el-descriptions-item>
      <el-descriptions-item label="固化阈值" :span="2">{{ thresholdText }}</el-descriptions-item>
    </el-descriptions>

    <el-form label-width="100px">
      <el-form-item v-if="failItem?.metricCode !== 'odor'" label="复检数值" required>
        <el-input v-model="form.metricValue" placeholder="请输入数值">
          <template #append>{{ unitText }}</template>
        </el-input>
      </el-form-item>
      <el-form-item v-else label="气味结论" required>
        <el-select v-model="form.odor" placeholder="选择气味结论" style="width: 100%;">
          <el-option label="无异味" value="无异味" />
          <el-option label="正常" value="正常" />
          <el-option label="有异味" value="有异味" />
          <el-option label="刺鼻" value="刺鼻" />
        </el-select>
      </el-form-item>
      <el-form-item label="复检时间" required>
        <el-date-picker
          v-model="form.retestTime"
          type="datetime"
          placeholder="选择复检采样时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%;"
        />
      </el-form-item>
      <el-form-item label="复检人">
        <el-input :model-value="currentUser" disabled />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.comment" type="textarea" :rows="2" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="现场照片">
        <el-upload
          list-type="picture-card"
          :file-list="fileList"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          :on-remove="onRemove"
          accept="image/*"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="confirm">提交复检结果</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { submitWqRetest } from '@/api/wqInspection'
import { uploadImage } from '@/api/common'
import { compressAndUpload } from '@/utils/imageCompress'
import { currentUser, newRequestId } from '@/utils/user'

const emit = defineEmits(['done'])

const visible = ref(false)
const loading = ref(false)
const failItem = ref(null)
const batchNo = ref('')
const fileList = ref([])

const form = reactive({
  metricValue: '',
  odor: '',
  retestTime: '',
  comment: '',
  photos: []
})

const threshold = computed(() => {
  try {
    return JSON.parse(failItem.value?.thresholdSnapshot || '{}')
  } catch (e) {
    return {}
  }
})

const thresholdText = computed(() => {
  const t = threshold.value
  if (t.judgeType === 'range') return `${t.minValue} ~ ${t.maxValue} ${t.unit || ''}`
  if (t.judgeType === 'enum') return `合格结论：${(t.passValues || []).join(' / ')}`
  return '—'
})

const unitText = computed(() => threshold.value.unit || '')

const open = (item, bNo) => {
  failItem.value = item
  batchNo.value = bNo
  form.metricValue = ''
  form.odor = ''
  form.comment = ''
  form.photos = []
  form.retestTime = nowLocal()
  fileList.value = []
  visible.value = true
}

const nowLocal = () => {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const beforeUpload = (file) => file.type.startsWith('image/') || (ElMessage.error('只能上传图片'), false)

const handleUpload = async (options) => {
  try {
    const res = await compressAndUpload(options.file, uploadImage)
    if (res.code === 200) {
      form.photos.push(res.data)
      fileList.value.push({ name: res.data, url: res.data })
    }
  } catch (e) {
    ElMessage.error('照片上传失败')
  }
}

const onRemove = (file) => {
  const url = file.url || file.name
  form.photos = form.photos.filter(u => u !== url)
}

const confirm = async () => {
  if (failItem.value.metricCode === 'odor') {
    if (!form.odor) return ElMessage.warning('请选择气味结论')
  } else if (form.metricValue === '' || Number.isNaN(Number(form.metricValue))) {
    return ElMessage.warning('请填写有效的复检数值')
  }
  if (!form.retestTime) return ElMessage.warning('请选择复检时间')

  loading.value = true
  try {
    const payload = {
      failItemId: failItem.value.id,
      retestOperator: currentUser.value,
      requestId: newRequestId(),
      retestTime: form.retestTime,
      comment: form.comment,
      photos: form.photos
    }
    if (failItem.value.metricCode === 'odor') {
      payload.odor = form.odor
    } else {
      payload.metricValue = Number(form.metricValue)
    }
    const res = await submitWqRetest(payload)
    if (res.code === 200) {
      ElMessage.success('复检结果已保存')
      visible.value = false
      emit('done')
    }
  } catch (e) {
    // 409 已被他人处理：关闭弹窗并由父组件刷新
    visible.value = false
    emit('done')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
