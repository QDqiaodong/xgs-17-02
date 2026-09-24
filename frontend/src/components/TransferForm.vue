<template>
  <el-dialog v-model="visible" title="分组归属调整登记" width="520px" destroy-on-close @open="loadGroups">
    <div v-if="device" style="margin-bottom: 16px;">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          <div>
            当前设备：<strong>{{ device.deviceNo }}</strong>（{{ device.model }}）
          </div>
          <div style="margin-top: 4px; color: #64748b;">
            原所属区域：{{ device.groupPath }}
          </div>
        </template>
      </el-alert>
    </div>
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="新归属区域" prop="newGroupId">
        <el-tree-select
          v-model="formData.newGroupId"
          :data="groupTree"
          :props="{ label: 'label', value: 'id', children: 'children' }"
          check-strictly
          :render-after-expand="false"
          placeholder="选择目标楼层节点"
          style="width: 100%;"
        />
      </el-form-item>
      <el-form-item label="调整原因" prop="transferReason">
        <el-input v-model="formData.transferReason" type="textarea" :rows="3" placeholder="请输入调整原因" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="操作人">
        <el-input v-model="formData.operator" placeholder="默认 admin" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">确认调整</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getBuildingGroupTree, getBuildingGroupLeafList } from '@/api/buildingGroup'
import { getWaterDispenser, updateWaterDispenser } from '@/api/waterDispenser'

const emit = defineEmits(['success'])

const visible = ref(false)
const device = ref(null)
const submitting = ref(false)
const groupTree = ref([])
const formRef = ref(null)

const formData = reactive({
  newGroupId: null,
  transferReason: '',
  operator: 'admin'
})

const formRules = {
  newGroupId: [{ required: true, message: '请选择新的归属区域', trigger: 'change' }],
  transferReason: [{ required: true, message: '请输入调整原因', trigger: 'blur' }]
}

const loadGroups = async () => {
  try {
    const res = await getBuildingGroupTree()
    groupTree.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const open = async (row) => {
  visible.value = true
  device.value = row
  formData.newGroupId = null
  formData.transferReason = ''
  formData.operator = 'admin'
}

const submitForm = async () => {
  if (!formRef.value || !device.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const detailRes = await getWaterDispenser(device.value.id)
      if (detailRes.code !== 200 || !detailRes.data) {
        ElMessage.error('获取设备信息失败')
        return
      }
      const payload = {
        ...detailRes.data,
        groupId: formData.newGroupId,
        transferReason: formData.transferReason
      }
      const res = await updateWaterDispenser(payload)
      if (res.code === 200) {
        ElMessage.success('分组调整成功')
        visible.value = false
        emit('success')
      }
    } catch (e) {
      console.error(e)
    } finally {
      submitting.value = false
    }
  })
}

defineExpose({ open })
</script>
