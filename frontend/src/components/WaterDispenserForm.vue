<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="640px"
    @closed="resetForm"
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="设备编号" prop="deviceNo">
            <el-input v-model="formData.deviceNo" placeholder="如：WD20240001" maxlength="50" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="设备型号" prop="model">
            <el-select
              v-model="formData.model"
              filterable
              placeholder="选择或输入型号"
              style="width: 100%;"
              allow-create
              default-first-option
              @change="onModelChange"
            >
              <el-option
                v-for="m in modelList"
                :key="m.name"
                :label="m.name"
                :value="m.name"
              >
                <span style="float: left;">{{ m.name }}</span>
                <span style="float: right; color: #8492a6; font-size: 12px;">
                  {{ m.brand }} · {{ m.defaultSpec }}
                </span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="安装规格" prop="spec">
            <el-input v-model="formData.spec" placeholder="如：立式冷热型" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="安装日期">
            <el-date-picker
              v-model="formData.installDate"
              type="date"
              placeholder="选择安装日期"
              value-format="YYYY-MM-DD"
              style="width: 100%;"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="出水类型" prop="waterType">
            <el-checkbox-group v-model="waterTypeList">
              <el-checkbox label="冷水" />
              <el-checkbox label="热水" />
              <el-checkbox label="温水" />
              <el-checkbox label="冰水" />
            </el-checkbox-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="所属区域" prop="groupId">
            <el-tree-select
              v-model="formData.groupId"
              :data="groupTree"
              :props="{ label: 'label', value: 'id', children: 'children' }"
              check-strictly
              :render-after-expand="false"
              placeholder="选择楼层节点"
              style="width: 100%;"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="设备状态">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">正常</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="设备图片">
        <el-upload
          class="image-uploader"
          :show-file-list="false"
          :before-upload="beforeUpload"
          accept="image/*"
        >
          <div v-if="formData.imageUrl" class="image-preview-wrapper">
            <img :src="formData.imageUrl" class="image-preview" />
            <div class="image-mask">
              <el-icon :size="24"><Refresh /></el-icon>
            </div>
          </div>
          <div v-else class="uploader-box">
            <el-icon :size="28" color="#94a3b8"><Plus /></el-icon>
            <div style="margin-top: 6px; color: #94a3b8; font-size: 12px;">上传图片（自动压缩）</div>
          </div>
        </el-upload>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item v-if="mode === 'edit'" label="调整原因">
        <el-input v-model="formData.transferReason" type="textarea" :rows="2" placeholder="如：分组调整变更（仅分组修改时记录）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">确认保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { saveWaterDispenser, updateWaterDispenser, getWaterDispenser } from '@/api/waterDispenser'
import { getBuildingGroupTree, getBuildingGroupLeafList } from '@/api/buildingGroup'
import { uploadImage } from '@/api/common'
import { compressAndUpload } from '@/utils/imageCompress'

const props = defineProps({
  modelList: { type: Array, default: () => [] }
})

const emit = defineEmits(['success'])

const visible = ref(false)
const mode = ref('add')
const submitting = ref(false)
const formRef = ref(null)
const groupTree = ref([])
const waterTypeList = ref([])

const formData = reactive({
  id: null,
  deviceNo: '',
  model: '',
  spec: '',
  waterType: '',
  imageUrl: '',
  groupId: null,
  status: 1,
  installDate: '',
  remark: '',
  transferReason: ''
})

const formRules = {
  deviceNo: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  model: [{ required: true, message: '请选择设备型号', trigger: 'change' }],
  groupId: [{ required: true, message: '请选择所属区域', trigger: 'change' }]
}

const dialogTitle = computed(() => mode.value === 'edit' ? '编辑饮水机设备' : '新增饮水机设备')

const loadGroupTree = async () => {
  try {
    const res = await getBuildingGroupTree()
    groupTree.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const onModelChange = (val) => {
  const modelInfo = props.modelList.find(m => m.name === val)
  if (modelInfo) {
    if (!formData.spec) formData.spec = modelInfo.defaultSpec || ''
    if (!waterTypeList.value.length && modelInfo.defaultWaterType) {
      waterTypeList.value = modelInfo.defaultWaterType.split(',').filter(Boolean)
    }
  }
}

const beforeUpload = async (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  try {
    submitting.value = true
    const res = await compressAndUpload(file, uploadImage)
    if (res.code === 200) {
      formData.imageUrl = res.data
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('图片上传失败')
  } finally {
    submitting.value = false
  }
  return false
}

const open = async (m, id, defaultGroupId) => {
  mode.value = m
  visible.value = true
  await loadGroupTree()
  if (defaultGroupId) {
    formData.groupId = defaultGroupId
  }
  if (m === 'edit' && id) {
    try {
      const res = await getWaterDispenser(id)
      if (res.code === 200 && res.data) {
        Object.assign(formData, res.data)
        waterTypeList.value = (res.data.waterType || '').split(',').filter(Boolean)
      }
    } catch (e) {
      console.error(e)
    }
  }
}

const resetForm = () => {
  formRef.value && formRef.value.resetFields()
  Object.assign(formData, {
    id: null, deviceNo: '', model: '', spec: '', waterType: '',
    imageUrl: '', groupId: null, status: 1, installDate: '', remark: '', transferReason: ''
  })
  waterTypeList.value = []
  submitting.value = false
}

const submitForm = async () => {
  if (!formRef.value) return
  formData.waterType = waterTypeList.value.join(',')
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      let res
      if (mode.value === 'edit') {
        res = await updateWaterDispenser(formData)
      } else {
        res = await saveWaterDispenser(formData)
      }
      if (res.code === 200) {
        ElMessage.success(mode.value === 'edit' ? '更新成功' : '新增成功')
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

<style lang="scss" scoped>
.image-uploader {
  :deep(.el-upload) {
    display: block;
  }
}
.uploader-box, .image-preview-wrapper {
  width: 120px;
  height: 120px;
  border: 2px dashed #e2e8f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    border-color: #409eff;
  }
}
.image-preview-wrapper {
  position: relative;
  border-style: solid;
  border-color: #e2e8f0;
  overflow: hidden;
  .image-preview {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .image-mask {
    position: absolute;
    inset: 0;
    background: rgba(0,0,0,0.5);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.2s;
  }
  &:hover .image-mask {
    opacity: 1;
  }
}
</style>
