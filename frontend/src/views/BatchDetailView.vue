<template>
  <div class="page-container" style="display: block;">
    <div class="panel-card" v-loading="loading">
      <div class="panel-header">
        <div class="panel-title">
          <el-button :icon="Back" link @click="goBack">返回</el-button>
          <span style="margin-left: 8px;">批次详情</span>
          <el-tag :type="statusTagType(detail.status)" effect="dark" style="margin-left: 10px;">
            {{ detail.statusName }}
          </el-tag>
          <el-tag v-if="detail.submitter === currentUser" type="info" size="small" style="margin-left: 6px;">
            我提交的批次
          </el-tag>
        </div>
        <div>
          <el-button
            v-if="canEdit"
            type="primary"
            :icon="Edit"
            size="small"
            @click="openEdit"
          >
            {{ detail.status === 'RETURNED' ? '按退回意见修改' : '编辑草稿' }}
          </el-button>
          <el-button type="success" :icon="Refresh" size="small" @click="loadDetail">刷新最新状态</el-button>
        </div>
      </div>

      <div class="panel-body" v-if="detail.id">
        <!-- 基本信息 -->
        <el-descriptions :column="3" border size="small" class="mb-16">
          <el-descriptions-item label="批次编号">{{ detail.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="抽检区域">{{ detail.scopeGroupPath }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ detail.submitter }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(detail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="复核人">{{ detail.reviewer || '—' }}</el-descriptions-item>
          <el-descriptions-item label="复核时间">{{ formatTime(detail.reviewTime) }}</el-descriptions-item>
          <el-descriptions-item label="关闭人">{{ detail.closeOperator || '—' }}</el-descriptions-item>
          <el-descriptions-item label="关闭时间">{{ formatTime(detail.closeTime) }}</el-descriptions-item>
          <el-descriptions-item label="批次备注">{{ detail.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="detail.status === 'RETURNED'"
          type="error"
          :closable="false"
          show-icon
          class="mb-16"
          title="批次已被整批退回"
          :description="`退回原因：${detail.returnReason || '—'}（复核人：${detail.reviewer}）。提交人可修改采样数据后重新提交，阈值将在重新提交时再次固化。`"
        />

        <!-- 固化阈值 -->
        <el-collapse v-model="activeCollapse" class="mb-16">
          <el-collapse-item name="threshold" title="提交时固化的判定阈值（后续阈值调整不影响本批次）">
            <el-table :data="frozenThresholds" border size="small">
              <el-table-column prop="metricName" label="指标" width="120" />
              <el-table-column prop="unit" label="单位" width="90" />
              <el-table-column label="判定规则">
                <template #default="{ row }">
                  <span v-if="row.judgeType === 'range'">
                    {{ row.minValue }} ~ {{ row.maxValue }} {{ row.unit }}（含边界）
                  </span>
                  <span v-else>合格结论：{{ (row.passValues || []).join(' / ') }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>

        <!-- 复核操作区 -->
        <div v-if="detail.status === 'PENDING_REVIEW'" class="review-bar mb-16">
          <el-alert type="warning" :closable="false" style="margin-bottom: 10px;">
            <template #title>
              当前批次待复核。复核人不能与提交人为同一人；可整批退回（需填原因），或确认合格项并为不合格设备开启复检。
            </template>
          </el-alert>
          <el-button type="danger" :icon="Back" :loading="reviewing" @click="openReturnDialog">整批退回</el-button>
          <el-button type="primary" :icon="CircleCheck" :loading="reviewing" @click="openConfirmDialog">
            确认合格项并开启复检
          </el-button>
          <span v-if="detail.submitter === currentUser" style="margin-left: 10px; color: #f56c6c; font-size: 13px;">
            你是提交人（{{ detail.submitter }}），不能复核本批次，请切换复核人账号
          </span>
        </div>

        <!-- 原始采样 -->
        <h3 class="section-title">原始采样记录（提交当时快照）</h3>
        <el-table :data="detail.items" border size="small" class="mb-16">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-box">
                <div class="snapshot-line">
                  <strong>历史采样位置（提交时快照，不可变）：</strong>{{ row.groupPathSnapshot }}
                  <el-tag size="small" type="info" style="margin-left: 8px;">编号 {{ row.deviceNoSnapshot }}</el-tag>
                  <el-tag size="small" type="info" style="margin-left: 4px;">型号 {{ row.modelSnapshot }}</el-tag>
                </div>
                <div class="snapshot-line">
                  <strong>设备当前所属区域（实时档案对照）：</strong>{{ row.currentGroupPath }}
                  <el-tag size="small" style="margin-left: 8px;">当前编号 {{ row.currentDeviceNo || '—' }}</el-tag>
                  <el-tag
                    size="small"
                    :type="row.deviceStatus === 1 ? 'success' : 'danger'"
                    style="margin-left: 4px;"
                  >
                    {{ row.deviceStatus === 1 ? '设备启用中' : '设备已停用' }}
                  </el-tag>
                  <el-tag v-if="row.devicePendingRetest === 1" size="small" type="warning" style="margin-left: 4px;">
                    设备待复检
                  </el-tag>
                </div>
                <div v-if="row.groupPathSnapshot !== row.currentGroupPath" class="snapshot-line moved-hint">
                  提示：该设备之后发生过调组/改名，历史记录仍以采样时位置为准，未被悄悄改写。
                </div>
                <div class="photo-box">
                  <strong>现场照片：</strong>
                  <el-image
                    v-for="(url, idx) in row.photos"
                    :key="idx"
                    :src="url"
                    :preview-src-list="row.photos"
                    :initial-index="idx"
                    fit="cover"
                    class="photo-thumb"
                  />
                  <span v-if="!row.photos || !row.photos.length" style="color: #94a3b8;">无</span>
                </div>
                <div v-if="row.reviewComment" class="snapshot-line">
                  <strong>复核意见：</strong>{{ row.reviewComment }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="deviceNoSnapshot" label="设备编号(快照)" width="150" />
          <el-table-column prop="modelSnapshot" label="型号(快照)" width="120" />
          <el-table-column prop="groupPathSnapshot" label="采样位置(快照)" min-width="200" show-overflow-tooltip />
          <el-table-column prop="sampler" label="采样人" width="90" />
          <el-table-column label="采样时间" width="150">
            <template #default="{ row }">{{ formatTime(row.sampleTime) }}</template>
          </el-table-column>
          <el-table-column label="余氯" width="90">
            <template #default="{ row }">
              <span :class="{ 'metric-fail': row.failMetrics.includes('residual_chlorine') }">
                {{ row.residualChlorine ?? '—' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="浊度" width="80">
            <template #default="{ row }">
              <span :class="{ 'metric-fail': row.failMetrics.includes('turbidity') }">
                {{ row.turbidity ?? '—' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="温度" width="80">
            <template #default="{ row }">
              <span :class="{ 'metric-fail': row.failMetrics.includes('temperature') }">
                {{ row.temperature ?? '—' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="气味" width="90">
            <template #default="{ row }">
              <span :class="{ 'metric-fail': row.failMetrics.includes('odor') }">
                {{ row.odor || '—' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="判定" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.itemResult === 'PASS' ? 'success' : 'danger'" size="small" effect="dark">
                {{ row.itemResultName }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="复核结论" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.reviewResult === 'PASS'" type="success" size="small">确认合格</el-tag>
              <el-tag v-else-if="row.reviewResult === 'FAIL'" type="warning" size="small">开启复检</el-tag>
              <span v-else style="color: #94a3b8;">—</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 不合格项复检链路 -->
        <template v-if="detail.failItems && detail.failItems.length">
          <h3 class="section-title">不合格项与复检链路</h3>
          <el-table :data="detail.failItems" border size="small" class="mb-16">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-box">
                  <el-timeline style="padding: 8px 0 0 8px;">
                    <el-timeline-item type="danger" :timestamp="`原始采样 · 开启人 ${row.openReviewer} · ${formatTime(row.openTime)}`">
                      指标「{{ row.metricName }}」原始值 <b>{{ row.metricValue }}</b>，
                      阈值：{{ formatThreshold(row.thresholdSnapshot) }}
                    </el-timeline-item>
                    <el-timeline-item
                      v-for="r in row.retests"
                      :key="r.id"
                      :type="r.retestResult === 'PASS' ? 'success' : 'warning'"
                      :timestamp="`${r.retestResultName} · ${r.retestOperator} · ${formatTime(r.retestTime) || r.createTime}`"
                    >
                      复检值：
                      <b>{{ row.metricCode === 'odor' ? r.odor : r.metricValue }}</b>
                      <span v-if="r.comment" style="margin-left: 8px;">备注：{{ r.comment }}</span>
                      <div class="photo-box" v-if="r.photos && r.photos.length">
                        <el-image
                          v-for="(url, idx) in r.photos"
                          :key="idx"
                          :src="url"
                          :preview-src-list="r.photos"
                          :initial-index="idx"
                          fit="cover"
                          class="photo-thumb"
                        />
                      </div>
                    </el-timeline-item>
                    <el-timeline-item v-if="row.status !== 'OPEN'" type="success"
                      :timestamp="`关闭 · ${row.closeOperator || '—'} · ${formatTime(row.closeTime)}`">
                      {{ row.statusName }}
                    </el-timeline-item>
                  </el-timeline>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="deviceNoSnapshot" label="设备编号" width="140" />
            <el-table-column prop="metricName" label="不合格指标" width="110" />
            <el-table-column prop="metricValue" label="原始值" width="110" />
            <el-table-column label="复检次数" width="90" align="center">
              <template #default="{ row }">{{ (row.retests || []).length }}</template>
            </el-table-column>
            <el-table-column label="最近复检" width="110" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.retests && row.retests.length"
                  :type="row.retests[row.retests.length - 1].retestResult === 'PASS' ? 'success' : 'danger'"
                  size="small">
                  {{ row.retests[row.retests.length - 1].retestResultName }}
                </el-tag>
                <span v-else style="color: #94a3b8;">未复检</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'OPEN' ? 'warning' : (row.status === 'CLOSED_PASS' ? 'success' : 'info')" size="small">
                  {{ row.statusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="canRetest && row.status === 'OPEN'"
                  type="primary"
                  link
                  size="small"
                  @click="openRetest(row)"
                >
                  提交复检
                </el-button>
                <span v-else style="color: #94a3b8;">—</span>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 状态流水 -->
        <h3 class="section-title">状态变化流水</h3>
        <el-timeline>
          <el-timeline-item
            v-for="log in detail.statusLogs"
            :key="log.id"
            :timestamp="`${log.operator || '—'} · ${formatTime(log.createTime)}`"
            :type="log.toStatus === 'CLOSED' ? 'success' : (log.toStatus === 'RETURNED' ? 'danger' : 'primary')"
          >
            <el-tag size="small" type="info">{{ statusLabel(log.fromStatus) }}</el-tag>
            →
            <el-tag size="small" :type="statusTagType(log.toStatus)">{{ statusLabel(log.toStatus) }}</el-tag>
            <span style="margin-left: 8px;">{{ log.detail }}</span>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>

    <BatchForm ref="batchFormRef" @success="onFormChanged" />
    <ReturnDialog ref="returnRef" :batch="detail" @done="onActionDone" />
    <ReviewConfirmDialog ref="confirmRef" :batch="detail" @done="onActionDone" />
    <RetestDialog ref="retestRef" @done="onActionDone" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, Edit, Refresh, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getWqBatchDetail } from '@/api/wqInspection'
import { currentUser } from '@/utils/user'
import { formatTime } from '@/utils/wqFormat'
import BatchForm from '@/components/wq/BatchForm.vue'
import ReturnDialog from '@/components/wq/ReturnDialog.vue'
import ReviewConfirmDialog from '@/components/wq/ReviewConfirmDialog.vue'
import RetestDialog from '@/components/wq/RetestDialog.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const reviewing = ref(false)
const detail = ref({})
const activeCollapse = ref(['threshold'])

const batchFormRef = ref(null)
const returnRef = ref(null)
const confirmRef = ref(null)
const retestRef = ref(null)

const statusMap = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待复核',
  RETURNED: '退回',
  PENDING_RETEST: '待复检',
  CLOSED: '已关闭'
}
const statusLabel = (s) => statusMap[s] || (s || '新建')

const statusTagType = (s) => ({
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  RETURNED: 'danger',
  PENDING_RETEST: 'warning',
  CLOSED: 'success'
}[s] || 'info')

const canEdit = computed(() =>
  detail.value.status === 'DRAFT' || detail.value.status === 'RETURNED')
const canRetest = computed(() => detail.value.status === 'PENDING_RETEST')

const frozenThresholds = computed(() => {
  try {
    return Object.values(JSON.parse(detail.value.thresholdSnapshot || '{}'))
  } catch (e) {
    return []
  }
})

const formatThreshold = (json) => {
  try {
    const t = JSON.parse(json)
    if (t.judgeType === 'range') return `${t.minValue}~${t.maxValue} ${t.unit || ''}`
    return `合格结论:${(t.passValues || []).join('/')}`
  } catch (e) {
    return '—'
  }
}

const loadDetail = async (silent) => {
  if (!silent) loading.value = true
  try {
    const res = await getWqBatchDetail(route.params.id)
    if (res.code === 200) {
      detail.value = res.data
    }
  } catch (e) {
    // 409 等由拦截器提示
  } finally {
    loading.value = false
  }
}

const goBack = () => router.push('/inspection')

const openEdit = () => {
  batchFormRef.value.openEdit(route.params.id)
}

const onFormChanged = () => {
  loadDetail()
}

const openReturnDialog = () => {
  if (detail.value.submitter === currentUser.value) {
    ElMessage.warning('复核人不能复核自己提交的批次')
    return
  }
  returnRef.value.open()
}

const openConfirmDialog = () => {
  if (detail.value.submitter === currentUser.value) {
    ElMessage.warning('复核人不能复核自己提交的批次')
    return
  }
  confirmRef.value.open()
}

const openRetest = (failItem) => {
  retestRef.value.open(failItem, detail.value.batchNo)
}

/**
 * 写操作完成（或被他人抢先 409）后统一刷新，
 * 让第二名复核人/复检人看到“数据已被他人处理”后的最新状态。
 */
const onActionDone = () => {
  reviewing.value = false
  loadDetail()
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.section-title {
  font-size: 15px;
  font-weight: 600;
  margin: 18px 0 10px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.review-bar {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 12px;
}
.expand-box {
  padding: 8px 16px;
  background: #f8fafc;
}
.snapshot-line {
  line-height: 28px;
  font-size: 13px;
}
.moved-hint {
  color: #d97706;
  background: #fef3c7;
  border-radius: 4px;
  padding: 4px 8px;
  display: inline-block;
  margin-top: 4px;
}
.photo-box {
  margin-top: 8px;
  font-size: 13px;
}
.photo-thumb {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  margin: 4px 6px 0 0;
  border: 1px solid #e2e8f0;
}
.metric-fail {
  color: #dc2626;
  font-weight: 700;
}
</style>
