<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="72%"
    destroy-on-close
    @closed="reset"
  >
    <el-form :model="form" label-width="110px" v-loading="loading">
      <el-row :gutter="16">
        <el-col :span="10">
          <el-form-item label="抽检区域" required>
            <el-tree-select
              v-model="form.scopeGroupId"
              :data="groupTree"
              :props="{ label: 'label', value: 'id', children: 'children' }"
              check-strictly
              :render-after-expand="false"
              placeholder="选择园区 / 楼栋 / 楼层"
              style="width: 100%;"
              @change="onScopeChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="采样人">
            <el-input v-model="defaultSampler" placeholder="默认当前操作人" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="采样时间">
            <el-date-picker
              v-model="defaultSampleTime"
              type="datetime"
              placeholder="批量填充"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%;"
              @change="fillSampleTime"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="批次备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
      </el-form-item>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px;">
        <template #title>
          从设备档案选择饮水机；草稿可随时增删设备与修改结果。提交时按后台生效阈值自动判定，并固化设备编号/型号/区域路径与阈值快照。
        </template>
      </el-alert>

      <div style="margin-bottom: 8px;">
        <el-button type="primary" plain size="small" :icon="Plus" @click="openDevicePicker">
          添加设备
        </el-button>
        <el-button size="small" :icon="MagicStick" @click="loadScopeDevices" :disabled="!form.scopeGroupId">
          一键带入区域内设备
        </el-button>
        <span v-if="thresholdHint" style="margin-left: 10px; color: #94a3b8; font-size: 12px;">
          {{ thresholdHint }}
        </span>
      </div>

      <el-table :data="form.items" border size="small" max-height="430">
        <el-table-column label="设备编号" width="140">
          <template #default="{ row }">{{ row.deviceNo }}</template>
        </el-table-column>
        <el-table-column label="型号" width="120">
          <template #default="{ row }">{{ row.model }}</template>
        </el-table-column>
        <el-table-column label="当前区域" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.groupPath }}</template>
        </el-table-column>
        <el-table-column label="余氯 mg/L" width="130">
          <template #default="{ row }">
            <el-input v-model="row.residualChlorine" placeholder="0.03~0.80" />
          </template>
        </el-table-column>
        <el-table-column label="浊度 NTU" width="120">
          <template #default="{ row }">
            <el-input v-model="row.turbidity" placeholder="≤1" />
          </template>
        </el-table-column>
        <el-table-column label="温度 ℃" width="110">
          <template #default="{ row }">
            <el-input v-model="row.temperature" placeholder="0~40" />
          </template>
        </el-table-column>
        <el-table-column label="气味" width="120">
          <template #default="{ row }">
            <el-select v-model="row.odor" placeholder="气味结论" style="width: 100%;">
              <el-option label="无异味" value="无异味" />
              <el-option label="正常" value="正常" />
              <el-option label="有异味" value="有异味" />
              <el-option label="刺鼻" value="刺鼻" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="现场照片" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openPhotoUpload(row)">
              <el-icon><Picture /></el-icon>
              <span style="margin-left: 2px;">{{ (row.photos || []).length || '上传' }}</span>
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" :icon="Delete" @click="removeItem($index)" />
          </template>
        </el-table-column>
      </el-table>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="info" :loading="saving" @click="onSave(false)">保存草稿</el-button>
      <el-button type="primary" :loading="submitting" @click="onSave(true)">提交进入待复核</el-button>
    </template>

    <!-- 设备选择 -->
    <el-dialog v-model="pickerVisible" title="从设备档案选择饮水机" width="760px" append-to-body>
      <div style="display: flex; gap: 8px; margin-bottom: 10px;">
        <el-tree-select
          v-model="pickerGroupId"
          :data="groupTree"
          :props="{ label: 'label', value: 'id', children: 'children' }"
          check-strictly
          :render-after-expand="false"
          placeholder="按区域筛选"
          clearable
          style="width: 260px;"
          @change="loadPickerDevices"
        />
        <el-input v-model="pickerKeyword" placeholder="设备编号/型号" clearable style="width: 220px;" @keyup.enter="loadPickerDevices" />
        <el-button type="primary" plain @click="loadPickerDevices">查询</el-button>
      </div>
      <el-table
        ref="pickerTableRef"
        :data="pickerData"
        border
        size="small"
        height="380"
        @selection-change="onPickerSelectionChange"
        row-key="id"
      >
        <el-table-column type="selection" width="45" reserve-selection />
        <el-table-column prop="deviceNo" label="设备编号" width="140" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="groupPath" label="所属区域" min-width="200" show-overflow-tooltip />
        <el-table-column label="设备状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
            <el-tag v-if="row.pendingRetest === 1" type="warning" size="small" style="margin-top: 2px;">待复检</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker">添加已选（{{ pickerSelection.length }}）</el-button>
      </template>
    </el-dialog>

    <!-- 照片上传 -->
    <el-dialog v-model="photoVisible" title="现场照片" width="520px" append-to-body>
      <el-upload
        list-type="picture-card"
        :file-list="photoFileList"
        :before-upload="beforePhotoUpload"
        :http-request="handlePhotoUpload"
        :on-remove="onPhotoRemove"
        accept="image/*"
      >
        <el-icon><Plus /></el-icon>
      </el-upload>
      <template #footer>
        <el-button type="primary" @click="photoVisible = false">完成</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Plus, Delete, Picture, MagicStick } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getBuildingGroupTree } from '@/api/buildingGroup'
import { getWaterDispenserPage } from '@/api/waterDispenser'
import { saveWqDraft, submitWqBatch, getWqThresholdList, getWqBatchDetail } from '@/api/wqInspection'
import { uploadImage } from '@/api/common'
import { compressAndUpload } from '@/utils/imageCompress'
import { currentUser, newRequestId } from '@/utils/user'

const emit = defineEmits(['success'])

const visible = ref(false)
const drawerTitle = ref('发起水质抽检批次')
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const editId = ref(null)
const groupTree = ref([])
const defaultSampler = ref(currentUser.value)
const defaultSampleTime = ref('')
const thresholds = ref([])

const form = reactive({
  scopeGroupId: null,
  remark: '',
  items: []
})

const thresholdHint = ref('')

const reset = () => {
  editId.value = null
  form.scopeGroupId = null
  form.remark = ''
  form.items = []
  defaultSampler.value = currentUser.value
  defaultSampleTime.value = ''
  thresholdHint.value = ''
}

const openCreate = async (groupId) => {
  visible.value = true
  drawerTitle.value = '发起水质抽检批次'
  reset()
  if (groupId) form.scopeGroupId = groupId
  await loadBase()
}

const openEdit = async (batchId) => {
  visible.value = true
  drawerTitle.value = '编辑抽检批次（草稿/退回）'
  reset()
  await loadBase()
  loading.value = true
  try {
    const res = await getWqBatchDetail(batchId)
    const d = res.data
    editId.value = d.id
    form.scopeGroupId = d.scopeGroupId
    form.remark = d.remark || ''
    form.items = (d.items || []).map(i => ({
      id: i.id,
      deviceId: i.deviceId,
      deviceNo: i.deviceNoSnapshot,
      model: i.modelSnapshot,
      groupPath: i.groupPathSnapshot,
      sampler: i.sampler || '',
      sampleTime: toLocalInput(i.sampleTime),
      residualChlorine: i.residualChlorine ?? '',
      turbidity: i.turbidity ?? '',
      temperature: i.temperature ?? '',
      odor: i.odor || '',
      photos: [...(i.photos || [])]
    }))
  } finally {
    loading.value = false
  }
}

const toLocalInput = (t) => {
  if (!t) return ''
  if (Array.isArray(t)) {
    const pad = (n) => String(n).padStart(2, '0')
    return `${t[0]}-${pad(t[1])}-${pad(t[2])}T${pad(t[3] || 0)}:${pad(t[4] || 0)}`
  }
  return String(t).replace(' ', 'T').slice(0, 16)
}

const loadBase = async () => {
  const [treeRes, thRes] = await Promise.all([getBuildingGroupTree(), getWqThresholdList()])
  groupTree.value = treeRes.data || []
  thresholds.value = (thRes.data || []).filter(t => t.enabled === 1)
  thresholdHint.value = thresholds.value.map(t => {
    if (t.judgeType === 'range') {
      return `${t.metricName} ${t.minValue}~${t.maxValue}${t.unit}`
    }
    return `${t.metricName}合格:${t.passValues}`
  }).join('；')
}

const onScopeChange = () => {}

const fillSampleTime = (val) => {
  if (!val) return
  form.items.forEach(i => { if (!i.sampleTime) i.sampleTime = val })
}

// ---------------- 设备选择 ----------------
const pickerVisible = ref(false)
const pickerData = ref([])
const pickerGroupId = ref(null)
const pickerKeyword = ref('')
const pickerSelection = ref([])
const pickerTableRef = ref(null)

const openDevicePicker = () => {
  pickerVisible.value = true
  pickerGroupId.value = form.scopeGroupId
  pickerSelection.value = []
  loadPickerDevices()
}

const loadPickerDevices = async () => {
  const res = await getWaterDispenserPage({
    pageNum: 1,
    pageSize: 200,
    groupId: pickerGroupId.value || undefined,
    keyword: pickerKeyword.value || undefined
  })
  pickerData.value = (res.data.records || []).filter(
    d => !form.items.some(i => i.deviceId === d.id)
  )
}

const onPickerSelectionChange = (rows) => {
  pickerSelection.value = rows
}

const confirmPicker = () => {
  pickerSelection.value.forEach(d => {
    form.items.push({
      deviceId: d.id,
      deviceNo: d.deviceNo,
      model: d.model,
      groupPath: d.groupPath,
      sampler: defaultSampler.value,
      sampleTime: defaultSampleTime.value || '',
      residualChlorine: '',
      turbidity: '',
      temperature: '',
      odor: '',
      photos: []
    })
  })
  pickerVisible.value = false
}

const loadScopeDevices = async () => {
  const res = await getWaterDispenserPage({
    pageNum: 1,
    pageSize: 500,
    groupId: form.scopeGroupId
  })
  let added = 0
  ;(res.data.records || []).forEach(d => {
    if (!form.items.some(i => i.deviceId === d.id)) {
      form.items.push({
        deviceId: d.id,
        deviceNo: d.deviceNo,
        model: d.model,
        groupPath: d.groupPath,
        sampler: defaultSampler.value,
        sampleTime: defaultSampleTime.value || '',
        residualChlorine: '', turbidity: '', temperature: '', odor: '', photos: []
      })
      added++
    }
  })
  ElMessage.success(added ? `已带入 ${added} 台设备` : '区域内设备均已添加')
}

const removeItem = (index) => {
  form.items.splice(index, 1)
}

// ---------------- 照片 ----------------
const photoVisible = ref(false)
const photoRow = ref(null)
const photoFileList = ref([])

const openPhotoUpload = (row) => {
  photoRow.value = row
  if (!row.photos) row.photos = []
  photoFileList.value = row.photos.map(url => ({ name: url, url }))
  photoVisible.value = true
}

const beforePhotoUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  return true
}

const handlePhotoUpload = async (options) => {
  try {
    const res = await compressAndUpload(options.file, uploadImage)
    if (res.code === 200) {
      const url = res.data
      photoRow.value.photos.push(url)
      photoFileList.value.push({ name: url, url })
      ElMessage.success('照片已上传')
    }
  } catch (e) {
    ElMessage.error('照片上传失败')
  }
}

const onPhotoRemove = (file) => {
  const url = file.url || file.name
  photoRow.value.photos = photoRow.value.photos.filter(u => u !== url)
}

// ---------------- 保存/提交 ----------------
const buildPayload = () => {
  if (!form.scopeGroupId) {
    ElMessage.warning('请选择抽检区域')
    return null
  }
  if (!form.items.length) {
    ElMessage.warning('请至少添加一台饮水机')
    return null
  }
  const deviceIds = form.items.map(i => i.deviceId)
  if (new Set(deviceIds).size !== deviceIds.length) {
    ElMessage.warning('同一设备不能重复添加')
    return null
  }
  return {
    id: editId.value,
    scopeGroupId: form.scopeGroupId,
    remark: form.remark,
    operator: currentUser.value,
    items: form.items.map(i => ({
      id: i.id || null,
      deviceId: i.deviceId,
      sampler: i.sampler || defaultSampler.value || currentUser.value,
      sampleTime: i.sampleTime || null,
      residualChlorine: i.residualChlorine === '' ? null : Number(i.residualChlorine),
      turbidity: i.turbidity === '' ? null : Number(i.turbidity),
      temperature: i.temperature === '' ? null : Number(i.temperature),
      odor: i.odor || null,
      photos: i.photos || []
    }))
  }
}

const onSave = async (submit) => {
  const payload = buildPayload()
  if (!payload) return
  if (submit) {
    // 提交前前端按生效阈值先提示一次（最终以后端提交时刻固化阈值判定为准）
    const invalid = preValidate(payload)
    if (invalid.length) {
      ElMessage.warning(invalid[0])
      return
    }
  }
  if (submit) {
    submitting.value = true
  } else {
    saving.value = true
  }
  try {
    if (submit) {
      const res = await submitWqBatch({
        id: payload.id,
        submitter: currentUser.value,
        requestId: newRequestId(),
        remark: payload.remark,
        items: payload.items
      })
      if (res.code === 200) {
        ElMessage.success('提交成功，已进入待复核')
        visible.value = false
        emit('success', editId.value)
      }
    } else {
      const res = await saveWqDraft(payload)
      if (res.code === 200) {
        editId.value = res.data
        ElMessage.success('草稿已保存')
        visible.value = false
        emit('success', editId.value)
      }
    }
  } catch (e) {
    // 409 等冲突提示已由拦截器统一给出；草稿/批次可能被他人处理
  } finally {
    submitting.value = false
    saving.value = false
  }
}

/** 仅用于前端即时提示；判定以后端固化阈值为唯一依据 */
const preValidate = (payload) => {
  const errors = []
  const thMap = Object.fromEntries(thresholds.value.map(t => [t.metricCode, t]))
  payload.items.forEach(i => {
    const no = form.items.find(x => x.deviceId === i.deviceId)?.deviceNo
    if (!i.sampleTime) errors.push(`设备 ${no} 缺少采样时间`)
    ;['residualChlorine', 'turbidity', 'temperature'].forEach(code => {
      const t = thMap[code]
      const v = i[code]
      if (v === null || v === undefined || Number.isNaN(v)) {
        errors.push(`设备 ${no} 的${t?.metricName || code}未填写`)
      } else if (t && t.judgeType === 'range' && (v < Number(t.minValue) || v > Number(t.maxValue))) {
        // 允许提交不合格值（本就需要走复核复检），这里不拦截
      }
    })
    const odorTh = thMap.odor
    if (!i.odor) errors.push(`设备 ${no} 的气味结论未选择`)
    else if (odorTh && !odorTh.passValues.split(',').map(s => s.trim()).includes(i.odor)) {
      // 不合格气味值同样允许提交
    }
  })
  return errors
}

defineExpose({ openCreate, openEdit })
</script>
