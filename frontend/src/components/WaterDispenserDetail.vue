<template>
  <el-dialog v-model="visible" title="设备详情" width="600px" destroy-on-close>
    <div v-if="data" class="detail-container">
      <div class="detail-header">
        <div class="header-left">
          <el-image
            v-if="data.imageUrl"
            :src="data.imageUrl"
            :preview-src-list="[data.imageUrl]"
            fit="cover"
            style="width: 80px; height: 80px; border-radius: 8px;"
          />
          <div v-else class="no-image">
            <el-icon :size="36" color="#cbd5e1"><Picture /></el-icon>
          </div>
          <div class="device-info">
            <div class="device-no">{{ data.deviceNo }}</div>
            <el-tag :type="data.status === 1 ? 'success' : 'danger'" effect="dark" size="small">
              {{ data.statusName }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-descriptions :column="2" border size="default" style="margin-top: 16px;">
        <el-descriptions-item label="设备型号">
          <el-tag type="warning" effect="light">{{ data.model }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="安装规格">{{ data.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出水类型" :span="2">
          <el-tag
            v-for="t in (data.waterType || '').split(',').filter(Boolean)"
            :key="t"
            size="small"
            type="info"
            effect="plain"
            style="margin: 2px;"
          >
            {{ t }}
          </el-tag>
          <span v-if="!data.waterType">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="所属区域" :span="2">
          <el-icon color="#1d4ed8"><Location /></el-icon>
          <span style="margin-left: 4px;">{{ data.groupPath }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="安装日期">{{ data.installDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="建档时间">{{ formatTime(data.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(data.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ data.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 20px;">
        <div class="sub-title">
          <el-icon><Clock /></el-icon>
          <span>分组调整历史记录</span>
        </div>
        <el-table :data="logList" v-loading="logLoading" size="small" border stripe style="margin-top: 8px;">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="oldGroupPath" label="原区域">
            <template #default="{ row }">{{ row.oldGroupPath || '（初始建档）' }}</template>
          </el-table-column>
          <el-table-column prop="newGroupPath" label="新区域" />
          <el-table-column prop="transferReason" label="原因" width="120" show-overflow-tooltip />
          <el-table-column prop="operator" label="操作人" width="80" />
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { Picture, Location, Clock } from '@element-plus/icons-vue'
import { getWaterDispenser } from '@/api/waterDispenser'
import { getTransferLogPage } from '@/api/waterDispenser'

const visible = ref(false)
const data = ref(null)
const logList = ref([])
const logLoading = ref(false)

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

const open = async (id) => {
  visible.value = true
  data.value = null
  logList.value = []
  try {
    const res = await getWaterDispenser(id)
    if (res.code === 200) {
      data.value = res.data
    }
    logLoading.value = true
    const logRes = await getTransferLogPage({ pageNum: 1, pageSize: 50, deviceId: id })
    if (logRes.code === 200 && logRes.data) {
      logList.value = logRes.data.records || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    logLoading.value = false
  }
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .header-left {
    display: flex;
    gap: 16px;
    align-items: center;
    .no-image {
      width: 80px;
      height: 80px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .device-info {
      .device-no {
        font-size: 20px;
        font-weight: 700;
        color: #1e293b;
        margin-bottom: 8px;
      }
    }
  }
}
.sub-title {
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
