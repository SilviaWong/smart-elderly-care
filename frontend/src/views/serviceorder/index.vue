<template>
  <div class="service-order-page">
    <!-- 顶部工单 KPI 统计看板 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card total">
          <div class="kpi-icon"><el-icon><Tickets /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">工单总数</span>
            <span class="kpi-num">{{ summary.totalOrders }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card pending-assign">
          <div class="kpi-icon"><el-icon><User /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待派单</span>
            <span class="kpi-num warning-num">{{ summary.pendingAssignCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card assigned">
          <div class="kpi-icon"><el-icon><Clock /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">已派待接单</span>
            <span class="kpi-num info-num">{{ summary.assignedCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card in-progress">
          <div class="kpi-icon"><el-icon><Loading /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">正在执行中</span>
            <span class="kpi-num primary-num">{{ summary.inProgressCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card pending-confirm">
          <div class="kpi-icon"><el-icon><Checked /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待主管验收</span>
            <span class="kpi-num orange-num">{{ summary.pendingConfirmCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card completed">
          <div class="kpi-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">已完成闭环</span>
            <span class="kpi-num success-num">{{ summary.completedCount }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主体区域切换 Tabs -->
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- Tab 1: 服务工单流转列表 -->
        <el-tab-pane label="服务工单流转调度" name="orders">
          <div class="tab-toolbar">
            <el-form :inline="true" :model="queryForm" class="filter-form">
              <el-form-item label="工单号">
                <el-input v-model="queryForm.orderNo" placeholder="输入SO单号" clearable style="width: 150px" />
              </el-form-item>
              <el-form-item label="长者姓名">
                <el-input v-model="queryForm.elderName" placeholder="姓名搜索" clearable style="width: 130px" />
              </el-form-item>
              <el-form-item label="工单状态">
                <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 140px">
                  <el-option label="待派单 (PENDING_ASSIGN)" value="PENDING_ASSIGN" />
                  <el-option label="已派单 (ASSIGNED)" value="ASSIGNED" />
                  <el-option label="已接单 (ACCEPTED)" value="ACCEPTED" />
                  <el-option label="执行中 (IN_PROGRESS)" value="IN_PROGRESS" />
                  <el-option label="待验收 (PENDING_CONFIRM)" value="PENDING_CONFIRM" />
                  <el-option label="已完成 (COMPLETED)" value="COMPLETED" />
                  <el-option label="已退回 (RETURNED)" value="RETURNED" />
                  <el-option label="已拒绝 (REJECTED)" value="REJECTED" />
                  <el-option label="已取消 (CANCELLED)" value="CANCELLED" />
                </el-select>
              </el-form-item>
              <el-form-item label="优先级">
                <el-select v-model="queryForm.priority" placeholder="全部" clearable style="width: 100px">
                  <el-option label="普通" value="NORMAL" />
                  <el-option label="紧急" value="HIGH" />
                  <el-option label="特急" value="URGENT" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="fetchOrders">查询</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-button type="primary" :icon="Plus" @click="openCreateOrderDialog">创建服务工单</el-button>
          </div>

          <!-- 工单表格 -->
          <el-table :data="orderList" v-loading="loading" stripe border style="width: 100%">
            <el-table-column prop="orderNo" label="工单编号" width="165" />
            <el-table-column label="长者姓名 / 床位" min-width="160">
              <template #default="{ row }">
                <div class="elder-cell">
                  <span class="elder-name">{{ row.elderName || '未知长者' }}</span>
                  <span class="bed-badge" v-if="row.bedInfo">{{ row.bedInfo }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="服务项目" min-width="170">
              <template #default="{ row }">
                <div class="item-cell">
                  <span class="item-name">{{ row.itemName }}</span>
                  <div class="item-tags">
                    <el-tag size="small" type="info">¥{{ row.standardPrice }}</el-tag>
                    <el-tag v-if="row.needConfirm === 1" size="small" type="warning">需主管验收</el-tag>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="优先级" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="getPriorityTagType(row.priority)" size="small">
                  {{ formatPriority(row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="工单状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)" effect="dark">
                  {{ formatStatus(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="指派护理员" width="110">
              <template #default="{ row }">
                <span>{{ row.assigneeName || '未指派' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="expectedStartAt" label="期望执行时间" width="165" />
            <el-table-column label="操作与流转" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <!-- 派单/转派 -->
                <el-button
                  v-if="row.status === 'PENDING_ASSIGN' || row.status === 'ASSIGNED' || row.status === 'REJECTED' || row.status === 'RETURNED'"
                  link
                  type="primary"
                  size="small"
                  @click="openAssignDialog(row)"
                >
                  派单
                </el-button>

                <!-- 接单 -->
                <el-button
                  v-if="row.status === 'ASSIGNED'"
                  link
                  type="success"
                  size="small"
                  @click="handleAction(row, 'ACCEPT')"
                >
                  接单
                </el-button>

                <!-- 拒单 -->
                <el-button
                  v-if="row.status === 'ASSIGNED'"
                  link
                  type="danger"
                  size="small"
                  @click="openActionPrompt(row, 'REJECT', '请输入拒单原因')"
                >
                  拒单
                </el-button>

                <!-- 开始执行 -->
                <el-button
                  v-if="row.status === 'ACCEPTED'"
                  link
                  type="primary"
                  size="small"
                  @click="handleAction(row, 'START')"
                >
                  开始
                </el-button>

                <!-- 完成执行 -->
                <el-button
                  v-if="row.status === 'IN_PROGRESS'"
                  link
                  type="success"
                  size="small"
                  @click="openCompleteDialog(row)"
                >
                  完成
                </el-button>

                <!-- 验收确认 -->
                <el-button
                  v-if="row.status === 'PENDING_CONFIRM'"
                  link
                  type="warning"
                  size="small"
                  @click="openConfirmDialog(row)"
                >
                  验收
                </el-button>

                <!-- 取消 -->
                <el-button
                  v-if="row.status !== 'COMPLETED' && row.status !== 'CANCELLED'"
                  link
                  type="danger"
                  size="small"
                  @click="openActionPrompt(row, 'CANCEL', '请输入取消原因')"
                >
                  取消
                </el-button>

                <!-- 查看日志时间线 -->
                <el-button link type="info" size="small" @click="openLogsDrawer(row)">日志</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="queryForm.pageNum"
              v-model:page-size="queryForm.pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchOrders"
              @current-change="fetchOrders"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 2: 服务项目目录 -->
        <el-tab-pane label="服务项目目录配置" name="items">
          <div class="tab-toolbar">
            <div class="rules-desc">配置机构日常照护、康复理疗、助浴清洁等标准化服务项目与价格体系。</div>
            <el-button type="primary" :icon="Plus" @click="openItemDialog()">新增服务项目</el-button>
          </div>

          <el-table :data="itemList" v-loading="itemLoading" stripe border style="width: 100%">
            <el-table-column prop="itemCode" label="项目代码" width="120" />
            <el-table-column prop="itemName" label="服务名称" min-width="160" />
            <el-table-column prop="category" label="分类" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ formatCategory(row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="standardPrice" label="标准单价" width="110" align="right">
              <template #default="{ row }">
                <span class="price-val">¥{{ row.standardPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="计价单位" width="90" align="center" />
            <el-table-column label="需要主管验收" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.needConfirm === 1 ? 'warning' : 'info'" size="small">
                  {{ row.needConfirm === 1 ? '是 (需复核)' : '否 (免验)' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="项目要求与服务规范" min-width="240" show-overflow-tooltip />
            <el-table-column label="启用状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 'ACTIVE'"
                  @change="(val: boolean) => handleToggleItemStatus(row, val ? 'ACTIVE' : 'INACTIVE')"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openItemDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 弹窗 1: 创建服务工单 -->
    <el-dialog v-model="createDialogVisible" title="创建服务工单" width="560px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="服务长者" prop="elderId">
          <el-select v-model="createForm.elderId" filterable placeholder="选择长者" style="width: 100%">
            <el-option v-for="e in elderList" :key="e.id" :label="`${e.name} (${e.elderNo})`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务项目" prop="serviceItemId">
          <el-select v-model="createForm.serviceItemId" placeholder="选择服务项目" style="width: 100%">
            <el-option
              v-for="item in activeItemList"
              :key="item.id"
              :label="`${item.itemName} (¥${item.standardPrice}/${item.unit})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="createForm.priority">
            <el-radio-button label="NORMAL">普通</el-radio-button>
            <el-radio-button label="HIGH">紧急</el-radio-button>
            <el-radio-button label="URGENT">特急</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="指派护理员">
          <el-select v-model="createForm.assigneeId" clearable placeholder="留空则先建单后派单" style="width: 100%">
            <el-option v-for="u in staffList" :key="u.id" :label="`${u.realName} (${u.phone || '护理人员'})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望执行时间">
          <el-date-picker
            v-model="createForm.expectedStartAt"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择期望上门服务时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="服务诉求/说明">
          <el-input v-model="createForm.requestDesc" type="textarea" :rows="3" placeholder="请输入具体服务要求、长者当前状态等..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitCreateOrder">创建工单</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2: 指派/转派工单 -->
    <el-dialog v-model="assignDialogVisible" title="工单派单与指派" width="460px" destroy-on-close>
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="工单编号">
          <span>{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="长者与项目">
          <span>{{ currentOrder?.elderName }} - {{ currentOrder?.itemName }}</span>
        </el-form-item>
        <el-form-item label="指派责任人" required>
          <el-select v-model="assignForm.assigneeId" placeholder="选择护理人员" style="width: 100%">
            <el-option v-for="u in staffList" :key="u.id" :label="`${u.realName} (${u.phone || '护理员'})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="派单备注">
          <el-input v-model="assignForm.remark" placeholder="如安排在午餐前完成" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 3: 完成执行填报 -->
    <el-dialog v-model="completeDialogVisible" title="完成服务并提交执行结果" width="520px" destroy-on-close>
      <el-form :model="completeForm" label-width="110px">
        <el-form-item label="工单编号">
          <span>{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="执行结果描述" required>
          <el-input
            v-model="completeForm.executionResult"
            type="textarea"
            :rows="4"
            placeholder="请详述本次服务执行情况、长者配合度、体征表现、完成结果..."
          />
        </el-form-item>
        <el-form-item label="温馨提示" v-if="currentOrder?.needConfirm === 1">
          <el-alert type="warning" :closable="false" show-icon>
            该项目配置为“需主管验收”，提交后工单将流转至【待验收】状态，由主管或创建人复核后终结。
          </el-alert>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="dialogSubmitting" @click="submitComplete">确认提交完成</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 4: 主管验收确认 -->
    <el-dialog v-model="confirmDialogVisible" title="主管服务验收与评价" width="520px" destroy-on-close>
      <el-form :model="confirmForm" label-width="110px">
        <el-form-item label="工单编号">
          <span>{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="执行人及结果">
          <div class="result-box">{{ currentOrder?.executionResult || '暂无描述' }}</div>
        </el-form-item>
        <el-form-item label="验收动作">
          <el-radio-group v-model="confirmForm.action">
            <el-radio-button label="CONFIRM">验收通过并完成</el-radio-button>
            <el-radio-button label="RETURN">验收不通过退回重改</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验收审核批注">
          <el-input v-model="confirmForm.remark" type="textarea" :rows="3" placeholder="填写验收核验意见..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitConfirm">确认验收</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 5: 新增/编辑服务项目 -->
    <el-dialog v-model="itemDialogVisible" :title="itemForm.id ? '编辑服务项目' : '新增服务项目'" width="540px" destroy-on-close>
      <el-form :model="itemForm" label-width="110px">
        <el-form-item label="项目代码" required>
          <el-input v-model="itemForm.itemCode" placeholder="如：SRV-001" :disabled="!!itemForm.id" />
        </el-form-item>
        <el-form-item label="服务名称" required>
          <el-input v-model="itemForm.itemName" placeholder="如：晨间整理与翻身" />
        </el-form-item>
        <el-form-item label="服务分类" required>
          <el-select v-model="itemForm.category" style="width: 100%">
            <el-option label="生活照料 (DAILY_CARE)" value="DAILY_CARE" />
            <el-option label="医疗护理 (HEALTH_NURSING)" value="HEALTH_NURSING" />
            <el-option label="康复理疗 (REHAB)" value="REHAB" />
            <el-option label="心理慰藉 (PSYCHOLOGY)" value="PSYCHOLOGY" />
            <el-option label="居室保洁 (CLEANING)" value="CLEANING" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准单价 (元)" required>
          <el-input-number v-model="itemForm.standardPrice" :precision="2" :step="10" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计价单位" required>
          <el-input v-model="itemForm.unit" placeholder="如：次, 小时, 天" />
        </el-form-item>
        <el-form-item label="是否需主管验收">
          <el-switch :model-value="itemForm.needConfirm === 1" @change="(val: boolean) => itemForm.needConfirm = val ? 1 : 0" />
        </el-form-item>
        <el-form-item label="服务规范说明">
          <el-input v-model="itemForm.description" type="textarea" :rows="3" placeholder="服务流程、标准规范与注意事项..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitItem">保存服务项目</el-button>
      </template>
    </el-dialog>

    <!-- 抽屉: 工单全流程操作日志与时间线 -->
    <el-drawer v-model="logsDrawerVisible" title="工单全流程生命周期审计时间线" size="460px" destroy-on-close>
      <div v-if="currentOrder" class="drawer-header-info">
        <div class="order-no">{{ currentOrder.orderNo }}</div>
        <div class="order-elder">{{ currentOrder.elderName }} | {{ currentOrder.itemName }}</div>
      </div>
      <el-timeline class="order-timeline" v-loading="logsLoading">
        <el-timeline-item
          v-for="log in orderLogs"
          :key="log.id"
          :timestamp="log.createdAt"
          placement="top"
          :type="getTimelineType(log.action)"
        >
          <el-card shadow="never" class="log-card">
            <div class="log-title">
              <span class="action-tag">{{ formatAction(log.action) }}</span>
              <span class="operator-name">{{ log.operatorName }}</span>
            </div>
            <div class="log-status-change" v-if="log.toStatus">
              状态: <strong>{{ formatStatus(log.toStatus) }}</strong>
            </div>
            <div class="log-remark" v-if="log.remark">说明: {{ log.remark }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import {
  Tickets,
  User,
  Clock,
  Loading,
  Checked,
  CircleCheckFilled,
  Search,
  Refresh,
  Plus
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getServiceOrderSummary,
  getServiceOrderPage,
  getServiceItemList,
  createServiceOrder,
  assignServiceOrder,
  executeServiceOrderAction,
  getServiceOrderLogs,
  createServiceItem,
  updateServiceItem,
  toggleServiceItemStatus,
  ServiceOrderVO,
  ServiceOrderSummaryVO,
  ServiceItemVO,
  ServiceOrderLogVO
} from '@/api/serviceorder'
import { getElderList } from '@/api/elder'
import { getUserList } from '@/api/system'

const activeTab = ref('orders')

// KPI 看板
const summary = reactive<ServiceOrderSummaryVO>({
  totalOrders: 0,
  pendingAssignCount: 0,
  assignedCount: 0,
  inProgressCount: 0,
  pendingConfirmCount: 0,
  completedCount: 0
})

// Tab 1: 工单列表
const loading = ref(false)
const orderList = ref<ServiceOrderVO[]>([])
const total = ref(0)
const queryForm = reactive({
  orderNo: '',
  elderName: '',
  status: '',
  priority: '',
  pageNum: 1,
  pageSize: 10
})

// Tab 2: 服务项目
const itemLoading = ref(false)
const itemList = ref<ServiceItemVO[]>([])
const activeItemList = computed(() => itemList.value.filter(i => i.status === 'ACTIVE'))

// 下拉候选项
const elderList = ref<any[]>([])
const staffList = ref<any[]>([])

// 弹窗状态
const dialogSubmitting = ref(false)
const currentOrder = ref<ServiceOrderVO | null>(null)

// 创建工单
const createDialogVisible = ref(false)
const createForm = reactive({
  elderId: undefined as number | undefined,
  serviceItemId: undefined as number | undefined,
  priority: 'NORMAL',
  assigneeId: undefined as number | undefined,
  expectedStartAt: '',
  requestDesc: ''
})
const createRules = {
  elderId: [{ required: true, message: '请选择长者', trigger: 'change' }],
  serviceItemId: [{ required: true, message: '请选择服务项目', trigger: 'change' }]
}

// 派单
const assignDialogVisible = ref(false)
const assignForm = reactive({
  assigneeId: undefined as number | undefined,
  remark: ''
})

// 完成
const completeDialogVisible = ref(false)
const completeForm = reactive({ executionResult: '' })

// 验收
const confirmDialogVisible = ref(false)
const confirmForm = reactive({
  action: 'CONFIRM' as 'CONFIRM' | 'RETURN',
  remark: ''
})

// 服务项目弹窗
const itemDialogVisible = ref(false)
const itemForm = reactive({
  id: undefined as number | undefined,
  itemCode: '',
  itemName: '',
  category: 'DAILY_CARE',
  standardPrice: 30,
  unit: '次',
  needConfirm: 0,
  description: ''
})

// 操作日志抽屉
const logsDrawerVisible = ref(false)
const logsLoading = ref(false)
const orderLogs = ref<ServiceOrderLogVO[]>([])

// 加载数据
async function fetchSummary() {
  try {
    const res = await getServiceOrderSummary()
    Object.assign(summary, res.data)
  } catch (err) {
    console.error(err)
  }
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getServiceOrderPage(queryForm)
    orderList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.orderNo = ''
  queryForm.elderName = ''
  queryForm.status = ''
  queryForm.priority = ''
  queryForm.pageNum = 1
  fetchOrders()
}

async function fetchItems() {
  itemLoading.value = true
  try {
    const res = await getServiceItemList()
    itemList.value = res.data
  } finally {
    itemLoading.value = false
  }
}

async function loadEldersAndStaff() {
  try {
    const eRes = await getElderList({ pageNum: 1, pageSize: 200 })
    elderList.value = eRes.data.list
  } catch (e) {
    console.error(e)
  }
  try {
    const uRes = await getUserList({ pageNum: 1, pageSize: 100 })
    staffList.value = uRes.data.list
  } catch (e) {
    console.error(e)
  }
}

function handleTabChange(tabName: any) {
  if (tabName === 'orders') {
    fetchOrders()
    fetchSummary()
  } else if (tabName === 'items') {
    fetchItems()
  }
}

// 工单操作
function openCreateOrderDialog() {
  createForm.elderId = undefined
  createForm.serviceItemId = activeItemList.value.length ? activeItemList.value[0].id : undefined
  createForm.priority = 'NORMAL'
  createForm.assigneeId = undefined
  createForm.expectedStartAt = ''
  createForm.requestDesc = ''
  createDialogVisible.value = true
}

async function submitCreateOrder() {
  if (!createForm.elderId || !createForm.serviceItemId) {
    ElMessage.warning('请选择长者和服务项目')
    return
  }
  dialogSubmitting.value = true
  try {
    await createServiceOrder(createForm as any)
    ElMessage.success('服务工单创建成功')
    createDialogVisible.value = false
    fetchOrders()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

function openAssignDialog(row: ServiceOrderVO) {
  currentOrder.value = row
  assignForm.assigneeId = row.assigneeId
  assignForm.remark = ''
  assignDialogVisible.value = true
}

async function submitAssign() {
  if (!assignForm.assigneeId || !currentOrder.value) {
    ElMessage.warning('请选择被指派的护理人员')
    return
  }
  dialogSubmitting.value = true
  try {
    await assignServiceOrder(currentOrder.value.id, {
      assigneeId: assignForm.assigneeId,
      remark: assignForm.remark
    })
    ElMessage.success('工单派发成功')
    assignDialogVisible.value = false
    fetchOrders()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleAction(row: ServiceOrderVO, action: 'ACCEPT' | 'START') {
  try {
    await executeServiceOrderAction(row.id, { action })
    ElMessage.success(action === 'ACCEPT' ? '接单成功' : '已开始执行服务')
    fetchOrders()
    fetchSummary()
  } catch (e) {
    console.error(e)
  }
}

async function openActionPrompt(row: ServiceOrderVO, action: 'REJECT' | 'CANCEL', promptTitle: string) {
  try {
    const { value } = await ElMessageBox.prompt(promptTitle, '操作提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空'
    })
    await executeServiceOrderAction(row.id, { action, remark: value })
    ElMessage.success('操作成功')
    fetchOrders()
    fetchSummary()
  } catch (e) {
    // cancelled
  }
}

function openCompleteDialog(row: ServiceOrderVO) {
  currentOrder.value = row
  completeForm.executionResult = ''
  completeDialogVisible.value = true
}

async function submitComplete() {
  if (!completeForm.executionResult.trim() || !currentOrder.value) {
    ElMessage.warning('请填写服务执行结果描述')
    return
  }
  dialogSubmitting.value = true
  try {
    await executeServiceOrderAction(currentOrder.value.id, {
      action: 'COMPLETE',
      executionResult: completeForm.executionResult
    })
    ElMessage.success('已完成服务执行提交')
    completeDialogVisible.value = false
    fetchOrders()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

function openConfirmDialog(row: ServiceOrderVO) {
  currentOrder.value = row
  confirmForm.action = 'CONFIRM'
  confirmForm.remark = '验收合格'
  confirmDialogVisible.value = true
}

async function submitConfirm() {
  if (!currentOrder.value) return
  dialogSubmitting.value = true
  try {
    await executeServiceOrderAction(currentOrder.value.id, {
      action: confirmForm.action,
      remark: confirmForm.remark
    })
    ElMessage.success(confirmForm.action === 'CONFIRM' ? '验收通过，工单全流程闭环' : '已退回工单')
    confirmDialogVisible.value = false
    fetchOrders()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

async function openLogsDrawer(row: ServiceOrderVO) {
  currentOrder.value = row
  logsDrawerVisible.value = true
  logsLoading.value = true
  try {
    const res = await getServiceOrderLogs(row.id)
    orderLogs.value = res.data
  } finally {
    logsLoading.value = false
  }
}

// 服务项目配置操作
function openItemDialog(row?: ServiceItemVO) {
  if (row) {
    itemForm.id = row.id
    itemForm.itemCode = row.itemCode
    itemForm.itemName = row.itemName
    itemForm.category = row.category
    itemForm.standardPrice = row.standardPrice
    itemForm.unit = row.unit
    itemForm.needConfirm = row.needConfirm
    itemForm.description = row.description
  } else {
    itemForm.id = undefined
    itemForm.itemCode = ''
    itemForm.itemName = ''
    itemForm.category = 'DAILY_CARE'
    itemForm.standardPrice = 30
    itemForm.unit = '次'
    itemForm.needConfirm = 0
    itemForm.description = ''
  }
  itemDialogVisible.value = true
}

async function submitItem() {
  if (!itemForm.itemCode.trim() || !itemForm.itemName.trim()) {
    ElMessage.warning('请填写项目代码与名称')
    return
  }
  dialogSubmitting.value = true
  try {
    if (itemForm.id) {
      await updateServiceItem(itemForm.id, itemForm as any)
      ElMessage.success('服务项目更新成功')
    } else {
      await createServiceItem(itemForm as any)
      ElMessage.success('服务项目新增成功')
    }
    itemDialogVisible.value = false
    fetchItems()
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleToggleItemStatus(row: ServiceItemVO, status: string) {
  try {
    await toggleServiceItemStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 'ACTIVE' ? '项目已启用' : '项目已停用')
  } catch (e) {
    console.error(e)
  }
}

// 辅助展示
function formatPriority(pri?: string) {
  if (pri === 'URGENT') return '特急'
  if (pri === 'HIGH') return '紧急'
  return '普通'
}

function getPriorityTagType(pri?: string): 'danger' | 'warning' | 'info' {
  if (pri === 'URGENT') return 'danger'
  if (pri === 'HIGH') return 'warning'
  return 'info'
}

function formatStatus(st?: string) {
  const map: Record<string, string> = {
    PENDING_ASSIGN: '待派单',
    ASSIGNED: '已派单',
    ACCEPTED: '已接单',
    IN_PROGRESS: '执行中',
    PENDING_CONFIRM: '待验收',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REJECTED: '已拒绝',
    RETURNED: '已退回'
  }
  return map[st || ''] || st || ''
}

function getStatusTagType(st?: string): 'danger' | 'warning' | 'info' | 'success' | 'primary' {
  if (st === 'COMPLETED') return 'success'
  if (st === 'IN_PROGRESS') return 'primary'
  if (st === 'PENDING_CONFIRM') return 'warning'
  if (st === 'PENDING_ASSIGN' || st === 'ASSIGNED') return 'info'
  if (st === 'CANCELLED' || st === 'REJECTED' || st === 'RETURNED') return 'danger'
  return 'info'
}

function formatCategory(cat?: string) {
  const map: Record<string, string> = {
    DAILY_CARE: '生活照料',
    HEALTH_NURSING: '医疗护理',
    REHAB: '康复理疗',
    PSYCHOLOGY: '心理慰藉',
    CLEANING: '居室保洁'
  }
  return map[cat || ''] || cat || ''
}

function formatAction(act?: string) {
  const map: Record<string, string> = {
    CREATE: '创建工单',
    ASSIGN: '指派人员',
    ACCEPT: '接单确认',
    START: '开始执行',
    COMPLETE: '完成执行',
    CONFIRM: '主管验收通过',
    REJECT: '拒单',
    RETURN: '退回',
    CANCEL: '取消工单'
  }
  return map[act || ''] || act || ''
}

function getTimelineType(act?: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (act === 'CREATE') return 'info'
  if (act === 'COMPLETE' || act === 'CONFIRM') return 'success'
  if (act === 'CANCEL' || act === 'REJECT') return 'danger'
  if (act === 'RETURN') return 'warning'
  return 'primary'
}

onMounted(() => {
  fetchSummary()
  fetchOrders()
  fetchItems()
  loadEldersAndStaff()
})
</script>

<style scoped>
.service-order-page {
  padding: 16px;
  background-color: #f7f9fa;
  min-height: calc(100vh - 84px);
}

.kpi-row {
  margin-bottom: 16px;
}

.kpi-card {
  border-radius: 10px;
  border: 1px solid #eef0f4;
  display: flex;
  align-items: center;
  transition: all 0.25s ease;
  background: #ffffff;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 14px;
}

.kpi-card.total .kpi-icon { background: #e6f4ff; color: #1677ff; }
.kpi-card.pending-assign .kpi-icon { background: #fff7e6; color: #fa8c16; }
.kpi-card.assigned .kpi-icon { background: #f0f5ff; color: #2f54eb; }
.kpi-card.in-progress .kpi-icon { background: #e6fffb; color: #13c2c2; }
.kpi-card.pending-confirm .kpi-icon { background: #fffbe6; color: #faad14; }
.kpi-card.completed .kpi-icon { background: #f6ffed; color: #52c41a; }

.kpi-content {
  flex: 1;
}

.kpi-title {
  font-size: 13px;
  color: #8c8c8c;
  display: block;
}

.kpi-num {
  font-size: 22px;
  font-weight: 700;
  color: #1f1f1f;
  margin-top: 4px;
  display: inline-block;
}

.warning-num { color: #d46b08; }
.info-num { color: #2f54eb; }
.primary-num { color: #0958d9; }
.orange-num { color: #d48806; }
.success-num { color: #389e0d; }

.main-card {
  border-radius: 10px;
  border: 1px solid #eef0f4;
  background: #ffffff;
}

.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.rules-desc {
  font-size: 13px;
  color: #595959;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.elder-cell {
  display: flex;
  flex-direction: column;
}

.elder-name {
  font-weight: 600;
  color: #262626;
}

.bed-badge {
  font-size: 11px;
  color: #1677ff;
  background: #e6f4ff;
  padding: 1px 6px;
  border-radius: 4px;
  width: fit-content;
  margin-top: 3px;
}

.item-cell {
  display: flex;
  flex-direction: column;
}

.item-name {
  font-weight: 500;
  color: #262626;
}

.item-tags {
  display: flex;
  gap: 6px;
  margin-top: 4px;
}

.price-val {
  font-weight: 600;
  color: #fa541c;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.result-box {
  background: #f5f5f5;
  padding: 10px 14px;
  border-radius: 6px;
  color: #262626;
  font-size: 13px;
  line-height: 1.6;
}

.drawer-header-info {
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.order-no {
  font-size: 16px;
  font-weight: 700;
  color: #1677ff;
}

.order-elder {
  font-size: 13px;
  color: #595959;
  margin-top: 4px;
}

.order-timeline {
  padding-left: 8px;
}

.log-card {
  border-radius: 8px;
}

.log-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.action-tag {
  font-weight: 600;
  color: #1f1f1f;
}

.operator-name {
  font-size: 12px;
  color: #8c8c8c;
}

.log-status-change {
  font-size: 12px;
  color: #595959;
  margin-bottom: 4px;
}

.log-remark {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}
</style>
