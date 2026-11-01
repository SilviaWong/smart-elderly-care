<template>
  <div class="regulatory-page-container">
    <el-tabs v-model="activeTab" type="border-card" class="regulatory-tabs">
      <!-- ================= 选项卡 1：监管报送批次管理 ================= -->
      <el-tab-pane label="监管报送批次管理" name="batches">
        <!-- 顶部搜索与操作栏 -->
        <el-card shadow="never" class="filter-card">
          <el-form :inline="true" :model="batchQueryForm" class="filter-form">
            <el-form-item label="报送模板">
              <el-select
                v-model="batchQueryForm.templateId"
                clearable
                placeholder="全部报送模板"
                style="width: 240px"
              >
                <el-option
                  v-for="tpl in activeTemplateList"
                  :key="tpl.id"
                  :label="tpl.templateName + ' (' + tpl.versionNo + ')'"
                  :value="tpl.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="批次单号">
              <el-input
                v-model="batchQueryForm.batchNo"
                placeholder="搜索批次编号"
                clearable
                style="width: 200px"
              />
            </el-form-item>

            <el-form-item label="批次状态">
              <el-select
                v-model="batchQueryForm.status"
                clearable
                placeholder="全部状态"
                style="width: 150px"
              >
                <el-option label="已校验 (VALIDATED)" value="VALIDATED" />
                <el-option label="已提交 (SUBMITTED)" value="SUBMITTED" />
                <el-option label="监管退回 (REJECTED)" value="REJECTED" />
                <el-option label="备案完成 (CONFIRMED)" value="CONFIRMED" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :icon="Search" :loading="loadingBatches" @click="fetchBatchList">
                查询
              </el-button>
              <el-button :icon="Refresh" @click="resetBatchQuery">
                重置
              </el-button>
              <el-button type="success" :icon="Plus" @click="openGenerateModal">
                生成报送批次
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 批次列表表格 -->
        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="result-header">
              <span class="report-summary">
                监管报送批次总数：<strong>{{ batchListTotal }}</strong> 个
              </span>
              <div class="compliance-badge">
                <el-tag type="info" size="small" round>
                  🛡️ 遵循合规前置校验与批次全生命周期追溯机制
                </el-tag>
              </div>
            </div>
          </template>

          <el-table
            v-loading="loadingBatches"
            :data="batchList"
            stripe
            border
            style="width: 100%"
          >
            <el-table-column prop="batchNo" label="批次单号" width="180" align="center">
              <template #default="scope">
                <span class="font-mono text-primary">{{ scope.row.batchNo }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="templateName" label="报送模板 / 监管对象" min-width="220">
              <template #default="scope">
                <div class="template-cell">
                  <div class="t-name font-bold">{{ scope.row.templateName }}</div>
                  <div class="t-auth text-muted">
                    <el-tag size="small" effect="plain" type="info">{{ scope.row.businessDomain }}</el-tag>
                    <span>{{ scope.row.targetAuthority }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="业务统计周期" width="200" align="center">
              <template #default="scope">
                <span v-if="scope.row.periodStart || scope.row.periodEnd">
                  {{ scope.row.periodStart || '始' }} ~ {{ scope.row.periodEnd || '至今' }}
                </span>
                <span v-else class="text-muted">全量在册数据</span>
              </template>
            </el-table-column>

            <el-table-column prop="statusName" label="批次状态" width="120" align="center">
              <template #default="scope">
                <el-tag :type="getBatchStatusTag(scope.row.status)" size="small">
                  {{ scope.row.statusName }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="数据校验概况" width="180" align="center">
              <template #default="scope">
                <div v-if="scope.row.validationResult" class="val-summary">
                  <div class="val-counts">
                    <span>总 <strong>{{ scope.row.validationResult.totalRows }}</strong></span>
                    <span class="text-success">过 <strong>{{ scope.row.validationResult.passedRows }}</strong></span>
                    <span class="text-danger" v-if="scope.row.validationResult.failedRows > 0">
                      误 <strong>{{ scope.row.validationResult.failedRows }}</strong>
                    </span>
                  </div>
                  <el-tag
                    size="small"
                    :type="scope.row.validationResult.validationStatus === 'ALL_PASSED' ? 'success' : 'warning'"
                    effect="plain"
                  >
                    {{ scope.row.validationResult.validationStatus === 'ALL_PASSED' ? '全部合规' : '含异常项' }}
                  </el-tag>
                </div>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>

            <el-table-column prop="createdByName" label="创建人" width="110" align="center" />

            <el-table-column prop="createdAt" label="生成时间" width="160" align="center" />

            <el-table-column label="操作" width="260" align="center" fixed="right">
              <template #default="scope">
                <!-- 校验报告 -->
                <el-button
                  type="primary"
                  link
                  size="small"
                  :icon="View"
                  @click="openValidationDrawer(scope.row)"
                >
                  校验报告
                </el-button>

                <!-- 下载文件 -->
                <el-button
                  type="success"
                  link
                  size="small"
                  :icon="Download"
                  @click="handleDownload(scope.row)"
                >
                  下载 CSV
                </el-button>

                <!-- 登记提交 -->
                <el-button
                  v-if="scope.row.status === 'VALIDATED'"
                  type="warning"
                  link
                  size="small"
                  :icon="CircleCheckFilled"
                  @click="openSubmitModal(scope.row)"
                >
                  登记提交
                </el-button>
                <span v-else class="text-muted" style="margin-left: 8px; font-size: 12px;">已提交</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="batchQueryForm.pageNum"
              v-model:page-size="batchQueryForm.pageSize"
              :total="batchListTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchBatchList"
              @current-change="fetchBatchList"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ================= 选项卡 2：监管报送模板维护 ================= -->
      <el-tab-pane label="监管报送模板维护与字段映射" name="templates">
        <!-- 模板操作栏 -->
        <el-card shadow="never" class="filter-card">
          <div class="task-filter-bar">
            <el-form :inline="true" :model="templateQueryForm" class="filter-form">
              <el-form-item label="业务域">
                <el-select
                  v-model="templateQueryForm.businessDomain"
                  clearable
                  placeholder="全部业务域"
                  style="width: 160px"
                >
                  <el-option label="长者备案 (ELDER)" value="ELDER" />
                  <el-option label="床位空间 (BED)" value="BED" />
                  <el-option label="护理照料 (CARE)" value="CARE" />
                  <el-option label="安全风险 (RISK)" value="RISK" />
                </el-select>
              </el-form-item>

              <el-form-item label="关键字">
                <el-input
                  v-model="templateQueryForm.keyword"
                  placeholder="模板代码或名称"
                  clearable
                  style="width: 220px"
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :icon="Search" :loading="loadingTemplates" @click="fetchTemplateList">
                  查询
                </el-button>
                <el-button :icon="Refresh" @click="resetTemplateQuery">
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>

        <!-- 模板列表 -->
        <el-card shadow="never" class="result-card">
          <el-table
            v-loading="loadingTemplates"
            :data="templateList"
            stripe
            border
            style="width: 100%"
          >
            <el-table-column prop="templateCode" label="模板代码" width="180">
              <template #default="scope">
                <span class="font-mono">{{ scope.row.templateCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="templateName" label="模板名称" min-width="200" />
            <el-table-column label="目标监管部门" min-width="180">
              <template #default="scope">
                <span>{{ scope.row.fieldMapping?.targetAuthority || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="业务域" width="120" align="center">
              <template #default="scope">
                <el-tag size="small" type="primary" effect="plain">
                  {{ scope.row.fieldMapping?.businessDomain || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fieldCount" label="映射字段数" width="110" align="center">
              <template #default="scope">
                <el-tag type="info" size="small">{{ scope.row.fieldCount }} 个</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="versionNo" label="版本" width="90" align="center" />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 'ACTIVE' ? '启用中' : '已停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="scope">
                <el-button
                  type="primary"
                  link
                  size="small"
                  :icon="Document"
                  @click="openFieldMappingDrawer(scope.row)"
                >
                  查看字段映射
                </el-button>
                <el-button
                  type="success"
                  link
                  size="small"
                  :icon="Plus"
                  @click="quickGenerateWithTemplate(scope.row)"
                >
                  按此模板报送
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="templateQueryForm.pageNum"
              v-model:page-size="templateQueryForm.pageSize"
              :total="templateListTotal"
              :page-sizes="[10, 20]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchTemplateList"
              @current-change="fetchTemplateList"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 弹窗 1：生成新报送批次 -->
    <el-dialog
      v-model="generateModalVisible"
      title="抽取业务数据并生成合规报送批次"
      width="540px"
      destroy-on-close
    >
      <el-form label-width="110px">
        <el-form-item label="报送模板" required>
          <el-select
            v-model="generateForm.templateId"
            placeholder="请选择报送模板"
            style="width: 100%"
          >
            <el-option
              v-for="tpl in activeTemplateList"
              :key="tpl.id"
              :label="tpl.templateName + ' (' + tpl.templateCode + ' ' + tpl.versionNo + ')'"
              :value="tpl.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="业务周期">
          <el-date-picker
            v-model="periodRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="handlePeriodChange"
          />
          <div class="form-tip">选填，未选择日期时默认抽取机构当前全部在册/历史记录</div>
        </el-form-item>

        <el-form-item label="批次备注">
          <el-input
            v-model="generateForm.remark"
            placeholder="例如: 2026年10月度在住长者民政实名制例行备案"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-alert
          title="数据生成时将自动执行 18 位身份证、手机号规范及必填完整性校验，并流式输出 UTF-8 BOM CSV 文件。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateModalVisible = false">取消</el-button>
          <el-button type="primary" :loading="submittingGenerate" @click="confirmGenerateBatch">
            立即抽取与校验
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 抽屉 2：前置数据校验与定位错误报告 -->
    <el-drawer
      v-model="validationDrawerVisible"
      title="前置合规性数据校验报告"
      size="650px"
    >
      <div v-if="currentValidationResult" class="validation-drawer-content">
        <!-- 统计卡片 -->
        <div class="val-stat-bar">
          <div class="stat-card">
            <span class="label">提取总数</span>
            <span class="val">{{ currentValidationResult.totalRows }}</span>
          </div>
          <div class="stat-card text-success">
            <span class="label">校验通过</span>
            <span class="val">{{ currentValidationResult.passedRows }}</span>
          </div>
          <div class="stat-card text-danger">
            <span class="label">不合规项</span>
            <span class="val">{{ currentValidationResult.failedRows }}</span>
          </div>
          <div class="stat-card">
            <span class="label">校验状态</span>
            <span class="val">
              <el-tag :type="currentValidationResult.validationStatus === 'ALL_PASSED' ? 'success' : 'warning'">
                {{ currentValidationResult.validationStatus === 'ALL_PASSED' ? '全部合格' : '存在错误' }}
              </el-tag>
            </span>
          </div>
        </div>

        <el-divider content-position="left">不合规数据定位明细</el-divider>

        <div v-if="currentValidationResult.errors && currentValidationResult.errors.length > 0">
          <el-table :data="currentValidationResult.errors" stripe border size="small">
            <el-table-column prop="rowNum" label="行号" width="60" align="center" />
            <el-table-column prop="fieldLabel" label="监管字段" width="130" />
            <el-table-column prop="rejectedValue" label="原始值" width="120" show-overflow-tooltip>
              <template #default="scope">
                <span v-if="scope.row.rejectedValue">{{ scope.row.rejectedValue }}</span>
                <span v-else class="text-danger">[缺失为空]</span>
              </template>
            </el-table-column>
            <el-table-column prop="errorType" label="错误类型" width="120" align="center">
              <template #default="scope">
                <el-tag size="small" type="danger" effect="plain">
                  {{ scope.row.errorType === 'MISSING_REQUIRED' ? '必填项缺失' : '格式/规范不符' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="校验驳回原因与整改建议" min-width="180" />
          </el-table>
        </div>
        <div v-else class="empty-errors">
          <el-result
            icon="success"
            title="全部数据已通过合规校验"
            sub-title="所有抽取记录均已满足国家监管必填、18位公民身份证号及手机号格式规则要求，可放心提交。"
          />
        </div>
      </div>
    </el-drawer>

    <!-- 弹窗 3：报送批次提交登记 -->
    <el-dialog
      v-model="submitModalVisible"
      title="报送批次登记提交"
      width="520px"
    >
      <el-form label-width="120px">
        <el-form-item label="批次单号">
          <el-input :value="currentBatchForSubmit?.batchNo" disabled />
        </el-form-item>
        <el-form-item label="报送通道" required>
          <el-select v-model="submitForm.channel" style="width: 100%">
            <el-option label="默认标准化文件导出交付 (DEFAULT_FILE_EXPORT)" value="DEFAULT_FILE_EXPORT" />
            <el-option label="离线专网介质转存 (OFFLINE_DISK)" value="OFFLINE_DISK" />
            <el-option label="政务系统模拟接入网关 (MOCK_GOV_ADAPTER)" value="MOCK_GOV_ADAPTER" />
          </el-select>
        </el-form-item>
        <el-form-item label="外部回执单号">
          <el-input
            v-model="submitForm.externalReceiptNo"
            placeholder="若政务平台已有受理编号可在此填写 (选填)"
          />
        </el-form-item>
        <el-form-item label="提交说明">
          <el-input
            v-model="submitForm.submissionNotes"
            type="textarea"
            :rows="3"
            placeholder="填写本次报送提交备注说明..."
          />
        </el-form-item>
        <el-alert
          title="系统已通过预留 Adapter 执行标准化报送交付，严格遵循政务接口解耦规范，未收到官方真实接口与专网凭证时不声称已直连政务外网。"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="submitModalVisible = false">取消</el-button>
          <el-button type="primary" :loading="submittingSubmit" @click="confirmSubmitBatch">
            确认登记提交
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 抽屉 4：字段映射查看 -->
    <el-drawer
      v-model="fieldMappingDrawerVisible"
      :title="`字段映射配置 - ${currentTemplateForMapping?.templateName || ''}`"
      size="700px"
    >
      <div v-if="currentTemplateForMapping" class="field-mapping-content">
        <div class="template-meta-box">
          <p><strong>目标监管部门：</strong>{{ currentTemplateForMapping.fieldMapping?.targetAuthority || '-' }}</p>
          <p><strong>适用业务域：</strong>{{ currentTemplateForMapping.fieldMapping?.businessDomain || '-' }}</p>
          <p><strong>报送格式：</strong>{{ currentTemplateForMapping.fieldMapping?.fileFormat || 'CSV' }}</p>
          <p><strong>规范说明：</strong>{{ currentTemplateForMapping.fieldMapping?.description || '-' }}</p>
        </div>

        <el-divider content-position="left">映射字段明细 ({{ currentTemplateForMapping.fieldMapping?.fields?.length || 0 }} 项)</el-divider>

        <el-table
          :data="currentTemplateForMapping.fieldMapping?.fields || []"
          stripe
          border
          size="small"
        >
          <el-table-column prop="sortOrder" label="序号" width="60" align="center" />
          <el-table-column prop="sourceField" label="系统源属性" width="130" />
          <el-table-column prop="targetField" label="监管目标字段" width="140">
            <template #default="scope">
              <strong>{{ scope.row.targetField }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="required" label="必填" width="70" align="center">
            <template #default="scope">
              <el-tag size="small" :type="scope.row.required ? 'danger' : 'info'">
                {{ scope.row.required ? '必填' : '选填' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="validationRule" label="校验规则" width="120" align="center">
            <template #default="scope">
              <el-tag size="small" effect="plain" v-if="scope.row.validationRule">
                {{ scope.row.validationRule }}
              </el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="transformRule" label="枚举转换字典" min-width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ scope.row.transformRule || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Download,
  View,
  Document,
  CircleCheckFilled
} from '@element-plus/icons-vue'
import {
  pageBatches,
  generateBatch,
  getValidationResult,
  downloadBatchFile,
  submitBatch,
  pageTemplates,
  listActiveTemplates,
  RegulatoryBatchVO,
  RegulatoryBatchQueryDTO,
  RegulatoryBatchGenerateDTO,
  RegulatoryBatchSubmitDTO,
  RegulatoryTemplateVO,
  RegulatoryTemplateQueryDTO,
  BatchValidationResultVO
} from '@/api/regulatory'

const activeTab = ref<'batches' | 'templates'>('batches')

// 启用的模板列表
const activeTemplateList = ref<RegulatoryTemplateVO[]>([])

// 批次相关数据
const loadingBatches = ref(false)
const batchList = ref<RegulatoryBatchVO[]>([])
const batchListTotal = ref(0)
const batchQueryForm = reactive<RegulatoryBatchQueryDTO>({
  templateId: undefined,
  batchNo: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

// 模板相关数据
const loadingTemplates = ref(false)
const templateList = ref<RegulatoryTemplateVO[]>([])
const templateListTotal = ref(0)
const templateQueryForm = reactive<RegulatoryTemplateQueryDTO>({
  businessDomain: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

// 生成批次弹窗
const generateModalVisible = ref(false)
const submittingGenerate = ref(false)
const periodRange = ref<[string, string] | null>(null)
const generateForm = reactive<RegulatoryBatchGenerateDTO>({
  templateId: 1,
  periodStart: undefined,
  periodEnd: undefined,
  remark: ''
})

// 校验报告抽屉
const validationDrawerVisible = ref(false)
const currentValidationResult = ref<BatchValidationResultVO | null>(null)

// 登记提交弹窗
const submitModalVisible = ref(false)
const submittingSubmit = ref(false)
const currentBatchForSubmit = ref<RegulatoryBatchVO | null>(null)
const submitForm = reactive<RegulatoryBatchSubmitDTO>({
  channel: 'DEFAULT_FILE_EXPORT',
  externalReceiptNo: '',
  submissionNotes: ''
})

// 字段映射抽屉
const fieldMappingDrawerVisible = ref(false)
const currentTemplateForMapping = ref<RegulatoryTemplateVO | null>(null)

function getBatchStatusTag(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'CONFIRMED': return 'success'
    case 'SUBMITTED': return 'success'
    case 'VALIDATED': return 'warning'
    case 'REJECTED': return 'danger'
    default: return 'info'
  }
}

async function fetchActiveTemplates() {
  try {
    const res = await listActiveTemplates()
    if (res.data) {
      activeTemplateList.value = res.data
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取有效报送模板失败')
  }
}

async function fetchBatchList() {
  loadingBatches.value = true
  try {
    const res = await pageBatches({
      templateId: batchQueryForm.templateId || undefined,
      batchNo: batchQueryForm.batchNo?.trim() || undefined,
      status: batchQueryForm.status || undefined,
      pageNum: batchQueryForm.pageNum,
      pageSize: batchQueryForm.pageSize
    })
    if (res.data) {
      batchList.value = res.data.list || []
      batchListTotal.value = res.data.total || 0
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取监管报送批次列表失败')
  } finally {
    loadingBatches.value = false
  }
}

function resetBatchQuery() {
  batchQueryForm.templateId = undefined
  batchQueryForm.batchNo = ''
  batchQueryForm.status = ''
  batchQueryForm.pageNum = 1
  fetchBatchList()
}

async function fetchTemplateList() {
  loadingTemplates.value = true
  try {
    const res = await pageTemplates({
      businessDomain: templateQueryForm.businessDomain || undefined,
      keyword: templateQueryForm.keyword?.trim() || undefined,
      pageNum: templateQueryForm.pageNum,
      pageSize: templateQueryForm.pageSize
    })
    if (res.data) {
      templateList.value = res.data.list || []
      templateListTotal.value = res.data.total || 0
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取报送模板列表失败')
  } finally {
    loadingTemplates.value = false
  }
}

function resetTemplateQuery() {
  templateQueryForm.businessDomain = ''
  templateQueryForm.keyword = ''
  templateQueryForm.pageNum = 1
  fetchTemplateList()
}

function handlePeriodChange(val: [string, string] | null) {
  if (val && val.length === 2) {
    generateForm.periodStart = val[0]
    generateForm.periodEnd = val[1]
  } else {
    generateForm.periodStart = undefined
    generateForm.periodEnd = undefined
  }
}

function openGenerateModal() {
  periodRange.value = null
  generateForm.periodStart = undefined
  generateForm.periodEnd = undefined
  generateForm.remark = ''
  if (activeTemplateList.value.length > 0 && !generateForm.templateId) {
    generateForm.templateId = activeTemplateList.value[0].id
  }
  generateModalVisible.value = true
}

function quickGenerateWithTemplate(tpl: RegulatoryTemplateVO) {
  generateForm.templateId = tpl.id
  periodRange.value = null
  generateForm.periodStart = undefined
  generateForm.periodEnd = undefined
  generateForm.remark = `按模板 ${tpl.templateName} 发起的数据备案`
  generateModalVisible.value = true
}

async function confirmGenerateBatch() {
  if (!generateForm.templateId) {
    ElMessage.warning('请选择报送模板')
    return
  }
  submittingGenerate.value = true
  try {
    await generateBatch({
      templateId: generateForm.templateId,
      periodStart: generateForm.periodStart,
      periodEnd: generateForm.periodEnd,
      remark: generateForm.remark?.trim() || undefined
    })
    ElMessage.success('报送批次抽取与合规校验完成！')
    generateModalVisible.value = false
    activeTab.value = 'batches'
    fetchBatchList()
  } catch (err: any) {
    ElMessage.error(err.message || '生成报送批次失败')
  } finally {
    submittingGenerate.value = false
  }
}

async function openValidationDrawer(batch: RegulatoryBatchVO) {
  try {
    const res = await getValidationResult(batch.id)
    currentValidationResult.value = res.data
    validationDrawerVisible.value = true
  } catch (err: any) {
    ElMessage.error(err.message || '获取校验结果失败')
  }
}

async function handleDownload(batch: RegulatoryBatchVO) {
  try {
    ElMessage.info('正在请求下载合规报送 CSV 文件...')
    const res = await downloadBatchFile(batch.id)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = batch.fileName || `${batch.batchNo}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('报送文件下载完成')
  } catch (err: any) {
    ElMessage.error(err.message || '文件下载失败')
  }
}

function openSubmitModal(batch: RegulatoryBatchVO) {
  currentBatchForSubmit.value = batch
  submitForm.channel = 'DEFAULT_FILE_EXPORT'
  submitForm.externalReceiptNo = ''
  submitForm.submissionNotes = ''
  submitModalVisible.value = true
}

async function confirmSubmitBatch() {
  if (!currentBatchForSubmit.value) return
  submittingSubmit.value = true
  try {
    await submitBatch(currentBatchForSubmit.value.id, {
      channel: submitForm.channel,
      externalReceiptNo: submitForm.externalReceiptNo?.trim() || undefined,
      submissionNotes: submitForm.submissionNotes?.trim() || undefined
    })
    ElMessage.success('报送批次已成功登记提交！')
    submitModalVisible.value = false
    fetchBatchList()
  } catch (err: any) {
    ElMessage.error(err.message || '登记提交失败')
  } finally {
    submittingSubmit.value = false
  }
}

function openFieldMappingDrawer(tpl: RegulatoryTemplateVO) {
  currentTemplateForMapping.value = tpl
  fieldMappingDrawerVisible.value = true
}

onMounted(() => {
  fetchActiveTemplates()
  fetchBatchList()
  fetchTemplateList()
})
</script>

<style scoped lang="scss">
.regulatory-page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .regulatory-tabs {
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

      .report-summary {
        font-size: 14px;
        color: #475569;

        strong {
          color: #2563eb;
          font-weight: 700;
        }
      }
    }
  }

  .font-mono {
    font-family: monospace;
    font-weight: 600;
  }

  .text-muted {
    color: #94a3b8;
    font-size: 12px;
  }

  .text-success {
    color: #10b981;
  }

  .text-danger {
    color: #ef4444;
  }

  .template-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .t-name {
      font-size: 14px;
      color: #1e293b;
    }

    .t-auth {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
    }
  }

  .val-summary {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .val-counts {
      display: flex;
      gap: 8px;
      font-size: 12px;
    }
  }

  .pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 18px;
  }

  .form-tip {
    font-size: 12px;
    color: #94a3b8;
    margin-top: 4px;
  }

  .validation-drawer-content {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .val-stat-bar {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;

      .stat-card {
        padding: 12px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        display: flex;
        flex-direction: column;
        align-items: center;

        .label {
          font-size: 12px;
          color: #64748b;
        }

        .val {
          font-size: 20px;
          font-weight: 700;
          margin-top: 4px;
        }
      }
    }
  }

  .template-meta-box {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 14px;
    font-size: 13px;
    color: #334155;
    line-height: 1.8;
  }
}
</style>
