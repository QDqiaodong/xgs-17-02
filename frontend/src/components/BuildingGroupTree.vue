<template>
  <div>
    <div class="panel-header">
      <div class="panel-title">
        <el-icon><OfficeBuilding /></el-icon>
        <span>楼栋分组树</span>
      </div>
      <el-button type="primary" size="small" :icon="Plus" @click="handleAddRoot">
        新增园区
      </el-button>
    </div>
    <div class="panel-body" style="padding: 12px;">
      <div class="tree-toolbar">
        <el-input
          v-model="filterText"
          placeholder="搜索分组名称"
          size="small"
          clearable
          :prefix-icon="Search"
        />
      </div>
      <el-tree
        ref="elTreeRef"
        :data="treeData"
        node-key="id"
        :props="{ label: 'label', children: 'children' }"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        :current-node-key="currentNodeKey"
        highlight-current
        default-expand-all
        @node-click="handleNodeClick"
        @node-contextmenu="handleNodeContextMenu"
      >
        <template #default="{ node, data }">
          <span class="tree-node" @contextmenu.prevent="showContextMenu($event, data)">
            <el-icon class="node-icon" :class="`icon-type-${data.type}`">
              <component :is="getIcon(data.type)" />
            </el-icon>
            <span class="node-label">{{ data.label }}</span>
            <el-tag v-if="data.deviceCount > 0" size="small" type="info" round style="margin-left: 6px;">
              {{ data.deviceCount }}台
            </el-tag>
          </span>
        </template>
      </el-tree>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="480px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="上级分组" v-if="formData.parentId">
          <span style="color: #475569;">{{ parentName || '无' }}</span>
        </el-form-item>
        <el-form-item label="分组类型" prop="type">
          <el-select v-model="formData.type" style="width: 100%;" :disabled="isEdit">
            <el-option label="园区" :value="1" />
            <el-option label="楼栋" :value="2" />
            <el-option label="楼层" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>

    <div
      v-show="contextMenuVisible"
      class="context-menu"
      :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
      @click="contextMenuVisible = false"
    >
      <div class="context-menu-item" @click="handleAddChild">
        <el-icon><Plus /></el-icon> 新增子节点
      </div>
      <div class="context-menu-item" @click="handleEdit">
        <el-icon><Edit /></el-icon> 编辑
      </div>
      <div class="context-menu-item danger" @click="handleDelete">
        <el-icon><Delete /></el-icon> 删除
      </div>
    </div>
    <div v-if="contextMenuVisible" class="context-menu-mask" @click="contextMenuVisible = false"></div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import {
  House, HomeFilled, OfficeBuilding, Plus, Edit, Delete, Search
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBuildingGroupTree, saveBuildingGroup, updateBuildingGroup, deleteBuildingGroup, getBuildingGroup
} from '@/api/buildingGroup'

const emit = defineEmits(['refresh-dispenser', 'current-change'])

const elTreeRef = ref(null)
const treeData = ref([])
const filterText = ref('')
const currentNodeKey = ref(null)
const dialogVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const parentName = ref('')
const contextMenuVisible = ref(false)
const contextMenuPosition = reactive({ x: 0, y: 0 })
const currentContextData = ref(null)

const formData = reactive({
  id: null,
  parentId: null,
  type: 1,
  name: '',
  sortOrder: 0,
  description: ''
})

const formRules = {
  type: [{ required: true, message: '请选择分组类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑分组' : '新增分组')

const getIcon = (type) => {
  switch (type) {
    case 1: return OfficeBuilding
    case 2: return House
    case 3: return HomeFilled
    default: return OfficeBuilding
  }
}

const filterNode = (value, data) => {
  if (!value) return true
  return data.label.includes(value)
}

watch(filterText, (val) => {
  elTreeRef.value && elTreeRef.value.filter(val)
})

const loadTree = async () => {
  try {
    const res = await getBuildingGroupTree()
    treeData.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const handleNodeClick = (data) => {
  emit('current-change', data)
}

const handleAddRoot = () => {
  isEdit.value = false
  formData.parentId = 0
  formData.type = 1
  parentName.value = '（根节点）'
  dialogVisible.value = true
}

const showContextMenu = (event, data) => {
  currentContextData.value = data
  contextMenuPosition.x = event.clientX
  contextMenuPosition.y = event.clientY
  contextMenuVisible.value = true
}

const handleNodeContextMenu = (event, data) => {
  showContextMenu(event, data)
}

const handleAddChild = async () => {
  if (!currentContextData.value) return
  isEdit.value = false
  formData.parentId = currentContextData.value.id
  formData.type = currentContextData.value.type < 3 ? currentContextData.value.type + 1 : 3
  formData.name = ''
  formData.sortOrder = 0
  formData.description = ''
  parentName.value = currentContextData.value.label
  dialogVisible.value = true
}

const handleEdit = async () => {
  if (!currentContextData.value) return
  try {
    const res = await getBuildingGroup(currentContextData.value.id)
    if (res.code === 200 && res.data) {
      isEdit.value = true
      Object.assign(formData, res.data)
      if (res.data.parentId && res.data.parentId > 0) {
        const parent = findNode(treeData.value, res.data.parentId)
        parentName.value = parent ? parent.label : ''
      } else {
        parentName.value = '（根节点）'
      }
      dialogVisible.value = true
    }
  } catch (e) {
    console.error(e)
  }
}

const findNode = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children && node.children.length) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

const handleDelete = async () => {
  if (!currentContextData.value) return
  try {
    await ElMessageBox.confirm(
      `确定要删除分组「${currentContextData.value.label}」吗？存在子节点或设备时无法删除。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    const res = await deleteBuildingGroup(currentContextData.value.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadTree()
      emit('refresh-dispenser')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

const resetForm = () => {
  formRef.value && formRef.value.resetFields()
  Object.assign(formData, {
    id: null, parentId: null, type: 1, name: '', sortOrder: 0, description: ''
  })
  currentContextData.value = null
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      let res
      if (isEdit.value) {
        res = await updateBuildingGroup(formData)
      } else {
        res = await saveBuildingGroup(formData)
      }
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
        dialogVisible.value = false
        await loadTree()
        emit('refresh-dispenser')
      }
    } catch (e) {
      console.error(e)
    }
  })
}

const handleGlobalClick = () => {
  contextMenuVisible.value = false
}

onMounted(() => {
  loadTree()
  document.addEventListener('click', handleGlobalClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleGlobalClick)
})

defineExpose({ loadTree })
</script>

<style lang="scss" scoped>
.tree-toolbar {
  margin-bottom: 12px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  flex: 1;
  font-size: 14px;
  padding: 4px 0;

  .node-icon {
    margin-right: 6px;
    font-size: 16px;
  }
  .icon-type-1 { color: #1d4ed8; }
  .icon-type-2 { color: #15803d; }
  .icon-type-3 { color: #b45309; }
}

.context-menu {
  position: fixed;
  z-index: 3000;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 6px 0;
  min-width: 140px;

  &-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    font-size: 14px;
    color: #334155;
    cursor: pointer;
    transition: all 0.15s;

    &:hover {
      background: #f0f9ff;
      color: #1d4ed8;
    }
    &.danger {
      color: #dc2626;
      &:hover {
        background: #fef2f2;
        color: #b91c1c;
      }
    }
  }
}

.context-menu-mask {
  position: fixed;
  inset: 0;
  z-index: 2999;
}
</style>
