<template>
  <div class="report-page-container">
    <el-tabs v-model="activeTab" type="border-card" class="report-tabs">
      <!-- ================= 选项卡 1：业务多维报表中心 ================= -->
      <el-tab-pane label="业务综合报表中心" name="data">
        <!-- 顶部搜索筛选条件栏 -->
        <el-card shadow="never" class="filter-card">
          <el-form :inline="true" :model="queryForm" class="filter-form">
            <!-- 报表类型选择 -->
            <el-form-item label="报表类型">
              <el-select
                v-model="queryForm.reportType"
                placeholder="请选择报表类型"
                style="width: 240px"
                @change="handleReportTypeChange"
              >
                <el-option
                  v-for="item in reportTypes"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <!-- 统计日期区间 -->
            <el-form-item label="业务日期">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 260px"
                @change="handleDateChange"
              />
            </el-form-item>

            <!-- 业务状态 -->
            <el-form-item label="状态筛选">
              <el-select
                v-model="queryForm.status"
                clearable
                placeholder="全部状态"
                style="width: 150px"
              >
                <el-option
                  v-for="st in currentStatusOptions"
                  :key="st.value"
                  :label="st.label"
                  :value="st.value"
                />
              </el-select>
            </el-form-item>

            <!-- 关键字查询 -->
            <el-form-item label="综合搜索">
              <el-input
                v-model="queryForm.keyword"
                placeholder="长者姓名/编号/单号/房号"
                clearable
                style="width: 220px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>

            <!-- 操作按钮 -->
            <el-form-item>
              <el-button type="primary" :icon="Search" :loading="loadingData" @click="handleSearch">
                查询报表
              </el-button>
              <el-button :icon="Refresh" @click="handleReset">
                重置
              </el-button>
              <el-button type="success" :icon="Download" @click="openExportModal">
                提交异步导出任务
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 报表内容结果区 -->
        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="result-header">
              <div class="result-title">
                <el-tag type="primary" effect="dark" size="small">{{ reportResult.reportTypeName || '业务报表' }}</el-tag>
                <span class="report-summary">共检索到 <strong>{{ reportResult.data?.total ?? 0 }}</strong> 条业务数据记录</span>
              </div>
              <div class="export-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>大数据量报表推荐点击「提交异步导出任务」，后台流式生成 CSV，不阻塞页面且防内存溢出</span>
              </div>
            </div>
          </template>

          <!-- 动态列业务数据表格 -->
          <el-table
            v-loading="loadingData"
            :data="reportResult.data?.list || []"
            stripe
            border
            style="width: 100%"
            class="report-table"
          >
            <el-table-column type="index" label="序号" width="65" align="center" />
            <el-table-column
              v-for="col in reportResult.columns || []"
              :key="col.prop"
              :prop="col.prop"
              :label="col.label"
              :width="col.width || undefined"
              :min-width="col.width ? undefined : 130"
              :align="col.align || 'left'"
            >
              <template #default="scope">
                <!-- 针对状态类的友好显示 -->
                <span v-if="col.prop.toLowerCase().includes('statusname') || col.prop.toLowerCase().endsWith('status')">
                  <el-tag size="small" :type="getStatusTagType(scope.row[col.prop])">
                    {{ scope.row[col.prop] || '-' }}
                  </el-tag>
                </span>
                <!-- 针对金额类的友好显示 -->
                <span v-else-if="col.prop.toLowerCase().includes('amount') || col.prop.toLowerCase().includes('price')">
                  <span class="amount-cell">¥ {{ formatMoney(scope.row[col.prop]) }}</span>
                </span>
                <!-- 普通文本内容 -->
                <span v-else>{{ scope.row[col.prop] ?? '-' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页器 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="queryForm.pageNum"
              v-model:page-size="queryForm.pageSize"
              :total="reportResult.data?.total || 0"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSearch"
              @current-change="handleSearch"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ================= 选项卡 2：异步导出任务中心 ================= -->
      <el-tab-pane label="异步导出任务中心" name="tasks">
        <!-- 任务筛选与运维栏 -->
        <el-card shadow="never" class="filter-card">
          <div class="task-filter-bar">
            <el-form :inline="true" :model="taskQueryForm" class="filter-form">
              <el-form-item label="报表类型">
                <el-select
                  v-model="taskQueryForm.reportType"
                  clearable
                  placeholder="全部报表类型"
                  style="width: 200px"
                >
                  <el-option
                    v-for="item in reportTypes"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="任务状态">
                <el-select
                  v-model="taskQueryForm.status"
                  clearable
                  placeholder="全部状态"
                  style="width: 150px"
                >
                  <el-option label="排队中 (PENDING)" value="PENDING" />
                  <el-option label="正在生成 (PROCESSING)" value="PROCESSING" />
                  <el-option label="已完成 (SUCCESS)" value="SUCCESS" />
                  <el-option label="生成失败 (FAILED)" value="FAILED" />
                </el-select>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :icon="Search" :loading="loadingTasks" @click="fetchTaskList">
                  筛选任务
                </el-button>
                <el-button :icon="Refresh" @click="resetTaskQuery">
                  刷新列表
                </el-button>
              </el-form-item>
            </el-form>

            <div class="maintenance-actions">
              <el-popconfirm
                title="确定清理 7 天前已过期的物理导出文件吗？"
                confirm-button-text="确认清理"
                cancel-button-text="取消"
                @confirm="handleCleanExpired"
              >
                <template #reference>
                  <el-button type="danger" plain :icon="Delete">
                    清理已过期导出文件
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </el-card>

        <!-- 任务中心数据表格 -->
        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="result-header">
              <span class="report-summary">
                已生成与处理中的导出任务共 <strong>{{ taskListTotal }}</strong> 个
              </span>
              <div class="auto-refresh-tip">
                <el-tag type="info" size="small" round>
                  安全机制：导出文件保留 7 天，后台多租户严格隔离，UTF-8 BOM 兼容 Excel
                </el-tag>
              </div>
            </div>
          </template>

          <el-table
            v-loading="loadingTasks"
            :data="taskList"
            stripe
            border
            style="width: 100%"
          >
            <el-table-column prop="taskNo" label="任务单号" width="180" align="center">
              <template #default="scope">
                <span class="font-mono">{{ scope.row.taskNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="reportTypeName" label="报表类别" min-width="160" />
            <el-table-column prop="fileName" label="生成文件名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="statusName" label="任务状态" width="120" align="center">
              <template #default="scope">
                <el-tag
                  size="small"
                  :type="getTaskStatusTagType(scope.row.status)"
                  effect="light"
                >
                  <el-icon v-if="scope.row.status === 'PROCESSING'" class="is-loading"><Loading /></el-icon>
                  {{ scope.row.statusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalRows" label="导出记录行数" width="120" align="right">
              <template #default="scope">
                <span v-if="scope.row.totalRows != null" class="font-mono">
                  {{ scope.row.totalRows }} 行
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdByName" label="发起人" width="110" align="center" />
            <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
            <el-table-column prop="finishedAt" label="完成时间" width="160" align="center">
              <template #default="scope">
                <span>{{ scope.row.finishedAt || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="有效期至" width="180" align="center">
              <template #default="scope">
                <div v-if="scope.row.expireAt">
                  <span>{{ scope.row.expireAt }}</span>
                  <div v-if="scope.row.isExpired">
                    <el-tag type="danger" size="small" effect="plain">已失效过期</el-tag>
                  </div>
                </div>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center" fixed="right">
              <template #default="scope">
                <!-- 导出成功且未过期：下载按钮 -->
                <el-button
                  v-if="scope.row.status === 'SUCCESS' && !scope.row.isExpired"
                  type="primary"
                  size="small"
                  :icon="Download"
                  @click="handleDownload(scope.row)"
                >
                  下载 CSV
                </el-button>

                <!-- 导出成功但已过期：已失效按钮 -->
                <el-tooltip
                  v-else-if="scope.row.status === 'SUCCESS' && scope.row.isExpired"
                  content="该文件已超过7天存储有效期并被自动安全清理，请重新提交导出"
                  placement="top"
                >
                  <el-button type="info" size="small" disabled>
                    已过期
                  </el-button>
                </el-tooltip>

                <!-- 导出失败：查看错误原因 -->
                <el-button
                  v-else-if="scope.row.status === 'FAILED'"
                  type="danger"
                  size="small"
                  plain
                  @click="showErrorDetail(scope.row)"
                >
                  失败详情
                </el-button>

                <!-- 处理中/排队中 -->
                <el-button
                  v-else
                  type="info"
                  size="small"
                  plain
                  disabled
                >
                  后台生成中...
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 任务分页器 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="taskQueryForm.pageNum"
              v-model:page-size="taskQueryForm.pageSize"
              :total="taskListTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchTaskList"
              @current-change="fetchTaskList"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 提交异步导出任务弹窗 -->
    <el-dialog
      v-model="exportModalVisible"
      title="提交后台异步流式报表导出任务"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="110px">
        <el-form-item label="报表类别">
          <el-input :value="getCurrentReportTypeName()" disabled />
        </el-form-item>
        <el-form-item label="过滤条件概览">
          <div class="filter-summary-box">
            <div><strong>统计时间：</strong>{{ dateRange?.[0] || '不限' }} ~ {{ dateRange?.[1] || '不限' }}</div>
            <div><strong>状态筛选：</strong>{{ queryForm.status || '全部状态' }}</div>
            <div><strong>搜索关键词：</strong>{{ queryForm.keyword || '无' }}</div>
          </div>
        </el-form-item>
        <el-form-item label="自定义文件名">
          <el-input
            v-model="exportCustomFileName"
            placeholder="例如: 2026年10月度在住长者流转明细 (选填)"
            maxlength="60"
            show-word-limit
          />
        </el-form-item>
        <el-alert
          title="系统将采用分批游标流式写入 CSV（带 UTF-8 BOM），不会占用前台网络，任务生成后可在「异步导出任务中心」随时下载。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="exportModalVisible = false">取消</el-button>
          <el-button type="primary" :loading="submittingExport" @click="confirmCreateExportTask">
            确认提交导出
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导出失败原因弹窗 -->
    <el-dialog
      v-model="errorModalVisible"
      title="导出任务异常详情"
      width="480px"
    >
      <el-alert
        :title="currentErrorTask?.errorMessage || '未知系统异常'"
        type="error"
        :closable="false"
        show-icon
      />
      <div class="error-meta" style="margin-top: 16px; font-size: 13px; color: #64748b;">
        <p>任务编号：<strong>{{ currentErrorTask?.taskNo }}</strong></p>
        <p>发生时间：{{ currentErrorTask?.finishedAt || currentErrorTask?.createdAt }}</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="errorModalVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  Download,
  Delete,
  InfoFilled,
  Loading
} from '@element-plus/icons-vue'
import {
  queryBusinessReport,
  createExportTask,
  pageExportTasks,
  downloadExportFile,
  cleanExpiredExportFiles,
  BusinessReportResultVO,
  ReportQueryDTO,
  ReportExportTaskVO,
  ExportTaskQueryDTO
} from '@/api/report'

// 激活的选项卡
const activeTab = ref<'data' | 'tasks'>('data')

// 8 类报表定义
const reportTypes = [
  { value: 'ADMISSION', label: '长者入住/退住流转报表' },
  { value: 'BED', label: '机构床位使用与状态报表' },
  { value: 'SERVICE_ORDER', label: '服务工单派单与执行报表' },
  { value: 'CARE_TASK', label: '护理任务排班与履约报表' },
  { value: 'HEALTH_ALERT', label: '健康体征越限预警报表' },
  { value: 'FINANCE_BILLING', label: '财务账单与实收明细报表' },
  { value: 'IOT_DEVICE', label: '物联设备档案与绑定报表' },
  { value: 'RISK_EVENT', label: '安全风险事件闭环管理报表' }
]

// 动态状态选项
const statusOptionsMap: Record<string, Array<{ label: string; value: string }>> = {
  ADMISSION: [
    { label: '在住 (CHECKED_IN)', value: 'CHECKED_IN' },
    { label: '已退住 (DISCHARGED)', value: 'DISCHARGED' },
    { label: '已转床 (TRANSFERRED)', value: 'TRANSFERRED' }
  ],
  BED: [
    { label: '空闲 (VACANT)', value: 'VACANT' },
    { label: '已占用 (OCCUPIED)', value: 'OCCUPIED' },
    { label: '维护中 (MAINTENANCE)', value: 'MAINTENANCE' }
  ],
  SERVICE_ORDER: [
    { label: '待派单 (PENDING)', value: 'PENDING' },
    { label: '已派单 (ASSIGNED)', value: 'ASSIGNED' },
    { label: '进行中 (IN_PROGRESS)', value: 'IN_PROGRESS' },
    { label: '待确认 (COMPLETED)', value: 'COMPLETED' },
    { label: '已归档 (CONFIRMED)', value: 'CONFIRMED' },
    { label: '已取消 (CANCELLED)', value: 'CANCELLED' }
  ],
  CARE_TASK: [
    { label: '待执行 (PENDING)', value: 'PENDING' },
    { label: '已完成 (COMPLETED)', value: 'COMPLETED' },
    { label: '已跳过 (SKIPPED)', value: 'SKIPPED' }
  ],
  HEALTH_ALERT: [
    { label: '待处理 (UNTREATED)', value: 'UNTREATED' },
    { label: '已处理 (TREATED)', value: 'TREATED' },
    { label: '已忽略 (IGNORED)', value: 'IGNORED' }
  ],
  FINANCE_BILLING: [
    { label: '待缴费 (UNPAID)', value: 'UNPAID' },
    { label: '部分缴费 (PARTIAL)', value: 'PARTIAL' },
    { label: '已结清 (PAID)', value: 'PAID' },
    { label: '已作废 (CANCELLED)', value: 'CANCELLED' }
  ],
  IOT_DEVICE: [
    { label: '正常在线 (ONLINE)', value: 'ONLINE' },
    { label: '离线状态 (OFFLINE)', value: 'OFFLINE' },
    { label: '告警中 (ALARM)', value: 'ALARM' },
    { label: '报废 (RETIRED)', value: 'RETIRED' }
  ],
  RISK_EVENT: [
    { label: '待分派 (PENDING)', value: 'PENDING' },
    { label: '处置中 (PROCESSING)', value: 'PROCESSING' },
    { label: '已关闭 (CLOSED)', value: 'CLOSED' }
  ]
}

const currentStatusOptions = computed(() => {
  return statusOptionsMap[queryForm.reportType] || []
})

// 查询表单
const dateRange = ref<[string, string] | null>(null)
const queryForm = reactive<ReportQueryDTO>({
  reportType: 'ADMISSION',
  startDate: '',
  endDate: '',
  status: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

// 报表结果数据
const loadingData = ref(false)
const reportResult = reactive<BusinessReportResultVO>({
  reportType: 'ADMISSION',
  reportTypeName: '长者入住与退住流转报表',
  columns: [],
  data: {
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: 10,
    totalPages: 0
  }
})

// 导出任务弹窗状态
const exportModalVisible = ref(false)
const exportCustomFileName = ref('')
const submittingExport = ref(false)

// 任务列表相关状态
const loadingTasks = ref(false)
const taskList = ref<ReportExportTaskVO[]>([])
const taskListTotal = ref(0)
const taskQueryForm = reactive<ExportTaskQueryDTO>({
  reportType: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

// 失败详情弹窗
const errorModalVisible = ref(false)
const currentErrorTask = ref<ReportExportTaskVO | null>(null)

function handleDateChange(val: [string, string] | null) {
  if (val && val.length === 2) {
    queryForm.startDate = val[0]
    queryForm.endDate = val[1]
  } else {
    queryForm.startDate = ''
    queryForm.endDate = ''
  }
}

function handleReportTypeChange() {
  queryForm.status = ''
  queryForm.pageNum = 1
  handleSearch()
}

function getCurrentReportTypeName(): string {
  const item = reportTypes.find(t => t.value === queryForm.reportType)
  return item ? item.label : queryForm.reportType
}

function formatMoney(val: any): string {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getStatusTagType(text?: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  if (!text) return 'info'
  if (text.includes('完成') || text.includes('在住') || text.includes('已结清') || text.includes('在线') || text.includes('已关闭')) {
    return 'success'
  }
  if (text.includes('待') || text.includes('中') || text.includes('进行') || text.includes('部分')) {
    return 'warning'
  }
  if (text.includes('失败') || text.includes('退住') || text.includes('告警') || text.includes('高危')) {
    return 'danger'
  }
  return 'info'
}

function getTaskStatusTagType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'PROCESSING': return 'primary' as any
    case 'PENDING': return 'info'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

async function handleSearch() {
  loadingData.value = true
  try {
    const res = await queryBusinessReport({
      reportType: queryForm.reportType,
      startDate: queryForm.startDate || undefined,
      endDate: queryForm.endDate || undefined,
      status: queryForm.status || undefined,
      keyword: queryForm.keyword?.trim() || undefined,
      pageNum: queryForm.pageNum,
      pageSize: queryForm.pageSize
    })
    if (res.data) {
      reportResult.reportType = res.data.reportType
      reportResult.reportTypeName = res.data.reportTypeName
      reportResult.columns = res.data.columns || []
      reportResult.data = res.data.data
    }
  } catch (err: any) {
    ElMessage.error(err.message || '查询业务报表失败')
  } finally {
    loadingData.value = false
  }
}

function handleReset() {
  dateRange.value = null
  queryForm.startDate = ''
  queryForm.endDate = ''
  queryForm.status = ''
  queryForm.keyword = ''
  queryForm.pageNum = 1
  handleSearch()
}

function openExportModal() {
  exportCustomFileName.value = ''
  exportModalVisible.value = true
}

async function confirmCreateExportTask() {
  submittingExport.value = true
  try {
    const filterData = {
      startDate: queryForm.startDate || undefined,
      endDate: queryForm.endDate || undefined,
      status: queryForm.status || undefined,
      keyword: queryForm.keyword?.trim() || undefined
    }

    await createExportTask({
      reportType: queryForm.reportType,
      filterJson: JSON.stringify(filterData),
      customFileName: exportCustomFileName.value.trim() || undefined
    })

    ElMessage.success('异步导出任务已成功提交！后台正在分批生成 CSV 文件')
    exportModalVisible.value = false
    activeTab.value = 'tasks'
    fetchTaskList()
  } catch (err: any) {
    ElMessage.error(err.message || '提交导出任务失败')
  } finally {
    submittingExport.value = false
  }
}

async function fetchTaskList() {
  loadingTasks.value = true
  try {
    const res = await pageExportTasks({
      reportType: taskQueryForm.reportType || undefined,
      status: taskQueryForm.status || undefined,
      pageNum: taskQueryForm.pageNum,
      pageSize: taskQueryForm.pageSize
    })
    if (res.data) {
      taskList.value = res.data.list || []
      taskListTotal.value = res.data.total || 0
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取导出任务列表失败')
  } finally {
    loadingTasks.value = false
  }
}

function resetTaskQuery() {
  taskQueryForm.reportType = undefined
  taskQueryForm.status = undefined
  taskQueryForm.pageNum = 1
  fetchTaskList()
}

async function handleDownload(task: ReportExportTaskVO) {
  if (task.isExpired) {
    ElMessage.warning('该导出文件已超过 7 天安全保留期，已被清理，请重新提交导出')
    return
  }
  try {
    ElMessage.info('正在请求下载报表文件...')
    const res = await downloadExportFile(task.id)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = task.fileName || `${task.taskNo}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('报表文件下载成功')
  } catch (err: any) {
    ElMessage.error(err.message || '文件下载失败')
  }
}

function showErrorDetail(task: ReportExportTaskVO) {
  currentErrorTask.value = task
  errorModalVisible.value = true
}

async function handleCleanExpired() {
  try {
    const res = await cleanExpiredExportFiles()
    ElMessage.success(`清理完成，已安全删除 ${res.data ?? 0} 个过期临时文件`)
    fetchTaskList()
  } catch (err: any) {
    ElMessage.error(err.message || '清理过期文件失败')
  }
}

onMounted(() => {
  handleSearch()
  fetchTaskList()
})
</script>

<style scoped lang="scss">
.report-page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .report-tabs {
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

    :deep(.el-tabs__header) {
      background: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
      border-radius: 12px 12px 0 0;
    }

    :deep(.el-tabs__item) {
      font-weight: 600;
      height: 48px;
      line-height: 48px;

      &.is-active {
        background-color: #ffffff;
        color: #2563eb;
      }
    }
  }

  .filter-card {
    border-radius: 10px;
    border: 1px solid #e2e8f0;
    margin-bottom: 16px;
    background: #fafbfc;

    .filter-form {
      margin-bottom: -18px;
    }

    .task-filter-bar {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 12px;
    }
  }

  .result-card {
    border-radius: 10px;
    border: 1px solid #e2e8f0;

    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;

      .result-title {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .report-summary {
        font-size: 14px;
        color: #475569;

        strong {
          color: #2563eb;
          font-weight: 700;
        }
      }

      .export-tip {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #64748b;
      }
    }

    .amount-cell {
      font-family: monospace;
      font-weight: 600;
      color: #059669;
    }

    .font-mono {
      font-family: monospace;
      font-weight: 600;
    }

    .pagination-container {
      display: flex;
      justify-content: flex-end;
      margin-top: 18px;
    }
  }

  .filter-summary-box {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    padding: 10px 14px;
    font-size: 13px;
    color: #475569;
    line-height: 1.8;
  }
}
</style>
