<template>
  <div class="health-page">
    <!-- 医疗免责声明警示条 -->
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      class="medical-disclaimer-banner"
    >
      <template #title>
        <span class="disclaimer-title">系统说明与健康监测免责声明</span>
      </template>
      <div class="disclaimer-content">
        本系统健康体征监测与异常预警仅供养老机构日常照护参考，不构成任何医疗诊断或临床治疗建议。如长者突发危急体征或急性不适，请立即由专业医护人员处置或呼叫120急救送医。
      </div>
    </el-alert>

    <!-- 顶部预警概览与统计看板 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="6" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card total">
          <div class="kpi-icon"><el-icon><BellFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">累计预警总数</span>
            <span class="kpi-num">{{ summary.totalAlerts }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="5" :lg="5">
        <el-card shadow="never" class="kpi-card critical">
          <div class="kpi-icon"><el-icon><WarningFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">危急重度预警</span>
            <div class="kpi-num-row">
              <span class="kpi-num critical-num">{{ summary.criticalCount }}</span>
              <el-tag v-if="summary.criticalCount > 0" type="danger" size="small" effect="dark" class="blink-tag">需急处</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="5" :lg="5">
        <el-card shadow="never" class="kpi-card open">
          <div class="kpi-icon"><el-icon><Clock /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待处理预警 (OPEN)</span>
            <span class="kpi-num warning-num">{{ summary.openCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="5" :lg="5">
        <el-card shadow="never" class="kpi-card processing">
          <div class="kpi-icon"><el-icon><Loading /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">正在处理中</span>
            <span class="kpi-num info-num">{{ summary.processingCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="5" :lg="5">
        <el-card shadow="never" class="kpi-card resolved">
          <div class="kpi-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">已处置闭环</span>
            <span class="kpi-num success-num">{{ summary.resolvedCount }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主体多标签功能卡片 -->
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- Tab 1: 异常预警处置中心 -->
        <el-tab-pane label="健康预警处置中心" name="alerts">
          <!-- 筛选区域 -->
          <div class="filter-header">
            <el-form :inline="true" :model="alertQuery" class="filter-form">
              <el-form-item label="预警单号">
                <el-input v-model="alertQuery.alertNo" placeholder="ALT号搜索" clearable style="width: 150px" />
              </el-form-item>
              <el-form-item label="长者姓名">
                <el-input v-model="alertQuery.elderName" placeholder="输入姓名" clearable style="width: 130px" />
              </el-form-item>
              <el-form-item label="预警级别">
                <el-select v-model="alertQuery.severity" placeholder="全部级别" clearable style="width: 120px">
                  <el-option label="危急 (CRITICAL)" value="CRITICAL" />
                  <el-option label="高风险 (HIGH)" value="HIGH" />
                  <el-option label="中度 (MEDIUM)" value="MEDIUM" />
                  <el-option label="提示 (LOW)" value="LOW" />
                </el-select>
              </el-form-item>
              <el-form-item label="处置状态">
                <el-select v-model="alertQuery.status" placeholder="全部状态" clearable style="width: 120px">
                  <el-option label="待处理 (OPEN)" value="OPEN" />
                  <el-option label="处理中 (PROCESSING)" value="PROCESSING" />
                  <el-option label="已处理 (RESOLVED)" value="RESOLVED" />
                  <el-option label="已忽略 (IGNORED)" value="IGNORED" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="fetchAlerts">查询</el-button>
                <el-button :icon="Refresh" @click="resetAlertQuery">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 预警表格 -->
          <el-table :data="alertList" v-loading="alertLoading" stripe border style="width: 100%">
            <el-table-column prop="alertNo" label="预警单号" width="165" />
            <el-table-column label="长者姓名 / 床位" min-width="160">
              <template #default="{ row }">
                <div class="elder-cell">
                  <span class="elder-name">{{ row.elderName || '未知长者' }}</span>
                  <span class="bed-badge" v-if="row.bedInfo">{{ row.bedInfo }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="监测指标" width="140">
              <template #default="{ row }">
                <span>{{ row.metricName }}</span>
                <span class="unit-text" v-if="row.unit"> ({{ row.unit }})</span>
              </template>
            </el-table-column>
            <el-table-column label="触发异常数值" width="130" align="center">
              <template #default="{ row }">
                <span class="abnormal-val" :class="getSeverityClass(row.severity)">
                  {{ row.alertValue }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="预警级别" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getSeverityTagType(row.severity)" effect="dark">
                  {{ formatSeverity(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ruleName" label="命中规则" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getAlertStatusTagType(row.status)">
                  {{ formatAlertStatus(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="责任人" width="110">
              <template #default="{ row }">
                <span>{{ row.assigneeName || '未指派' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="预警时间" width="165" />
            <el-table-column label="操作" width="190" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'OPEN'"
                  link
                  type="primary"
                  size="small"
                  @click="openAssignDialog(row)"
                >
                  指派
                </el-button>
                <el-button
                  v-if="row.status === 'OPEN' || row.status === 'PROCESSING'"
                  link
                  type="success"
                  size="small"
                  @click="openHandleDialog(row)"
                >
                  处置复核
                </el-button>
                <el-button link type="info" size="small" @click="viewAlertDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="alertQuery.pageNum"
              v-model:page-size="alertQuery.pageSize"
              :total="alertTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchAlerts"
              @current-change="fetchAlerts"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 2: 长者健康体征记录 -->
        <el-tab-pane label="长者健康体征记录" name="records">
          <div class="tab-toolbar">
            <el-form :inline="true" :model="recordQuery" class="filter-form">
              <el-form-item label="长者姓名">
                <el-input v-model="recordQuery.elderName" placeholder="姓名搜索" clearable style="width: 140px" />
              </el-form-item>
              <el-form-item label="体征指标">
                <el-select v-model="recordQuery.metricId" placeholder="全部指标" clearable style="width: 160px">
                  <el-option
                    v-for="m in metricList"
                    :key="m.id"
                    :label="`${m.metricName} (${m.unit || '-'})`"
                    :value="m.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="fetchRecords">查询</el-button>
                <el-button :icon="Refresh" @click="resetRecordQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <div class="action-btn-group">
              <el-button type="primary" :icon="Plus" @click="openSingleRecordDialog">录入单项体征</el-button>
              <el-button type="success" :icon="DocumentAdd" @click="openBatchRecordDialog">批量体征套检录入</el-button>
            </div>
          </div>

          <!-- 体征记录表格 -->
          <el-table :data="recordList" v-loading="recordLoading" stripe border style="width: 100%">
            <el-table-column prop="measuredAt" label="测量时间" width="165" />
            <el-table-column label="长者姓名 / 床位" min-width="160">
              <template #default="{ row }">
                <div class="elder-cell">
                  <span class="elder-name">{{ row.elderName }}</span>
                  <span class="bed-badge" v-if="row.bedInfo">{{ row.bedInfo }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="metricName" label="指标名称" width="140" />
            <el-table-column label="测量数值" width="130" align="center">
              <template #default="{ row }">
                <span class="vital-value">{{ row.metricValue }}</span>
                <span class="unit-text">{{ row.unitSnapshot || '' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sourceType" label="采集来源" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.sourceType === 'IOT_DEVICE' ? 'warning' : 'info'">
                  {{ row.sourceType === 'IOT_DEVICE' ? '物联设备' : '人工录入' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recorderName" label="记录人" width="110" />
            <el-table-column prop="remark" label="说明/长者主诉" min-width="180" show-overflow-tooltip />
            <el-table-column label="趋势分析" width="120" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" :icon="TrendCharts" @click="openTrendDialog(row)">
                  历史波动
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="recordQuery.pageNum"
              v-model:page-size="recordQuery.pageSize"
              :total="recordTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchRecords"
              @current-change="fetchRecords"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 3: 预警规则维护 -->
        <el-tab-pane label="健康预警规则维护" name="rules">
          <div class="tab-toolbar">
            <div class="rules-desc">
              系统将根据启用的阈值规则自动触发健康告警，并执行<strong>时间窗口去重</strong>以防止同类预警轰炸。
            </div>
            <el-button type="primary" :icon="Plus" @click="openRuleDialog()">新增预警规则</el-button>
          </div>

          <el-table :data="ruleList" v-loading="ruleLoading" stripe border style="width: 100%">
            <el-table-column prop="ruleName" label="规则名称" min-width="160" />
            <el-table-column label="关联指标" width="140">
              <template #default="{ row }">
                <span>{{ row.metricName }}</span>
                <span class="unit-text" v-if="row.unit"> ({{ row.unit }})</span>
              </template>
            </el-table-column>
            <el-table-column label="条件判定" width="140" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ formatConditionType(row.conditionType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="异常阈值范围" width="160" align="center">
              <template #default="{ row }">
                <span v-if="row.conditionType === 'GT'">高于 {{ row.maxValue }}</span>
                <span v-else-if="row.conditionType === 'LT'">低于 {{ row.minValue }}</span>
                <span v-else-if="row.conditionType === 'OUT_OF_RANGE'">
                  &lt; {{ row.minValue }} 或 &gt; {{ row.maxValue }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="预警级别" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getSeverityTagType(row.severity)" effect="plain">
                  {{ formatSeverity(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="去重窗口 (分钟)" width="130" align="center">
              <template #default="{ row }">
                <span>{{ row.dedupMinutes }} 分钟</span>
              </template>
            </el-table-column>
            <el-table-column label="启用状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled === 1"
                  @change="(val: boolean) => handleToggleRule(row, val ? 1 : 0)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openRuleDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 4: 健康指标字典 -->
        <el-tab-pane label="健康指标字典" name="metrics">
          <div class="tab-toolbar">
            <div class="rules-desc">定义机构统一监测的健康生理体征代码、单位及测量数据类型。</div>
            <el-button type="primary" :icon="Plus" @click="openMetricDialog()">新增指标</el-button>
          </div>

          <el-table :data="metricList" v-loading="metricLoading" stripe border style="width: 100%">
            <el-table-column prop="metricCode" label="指标编码" width="140" />
            <el-table-column prop="metricName" label="指标名称" width="160" />
            <el-table-column prop="unit" label="度量单位" width="110" align="center" />
            <el-table-column prop="valueType" label="数据类型" width="110" align="center" />
            <el-table-column prop="description" label="指标说明 / 临床参考范围" min-width="260" show-overflow-tooltip />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 'ACTIVE'"
                  @change="(val: boolean) => handleToggleMetric(row, val ? 'ACTIVE' : 'INACTIVE')"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openMetricDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 弹窗 1: 指派预警责任人 -->
    <el-dialog v-model="assignDialogVisible" title="指派预警处理责任人" width="460px" destroy-on-close>
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="预警单号">
          <el-input :model-value="currentAlert?.alertNo" disabled />
        </el-form-item>
        <el-form-item label="长者姓名">
          <el-input :model-value="currentAlert?.elderName" disabled />
        </el-form-item>
        <el-form-item label="处理责任人" required>
          <el-select v-model="assignForm.assigneeId" placeholder="选择护士/医生/主管" style="width: 100%">
            <el-option v-for="u in staffList" :key="u.id" :label="`${u.realName} (${u.phone || '员工'})`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitAssign">确认指派</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2: 预警处置复核 -->
    <el-dialog v-model="handleDialogVisible" title="预警处置复核与闭环记录" width="560px" destroy-on-close>
      <el-form :model="handleForm" label-width="110px">
        <el-form-item label="预警单号">
          <span>{{ currentAlert?.alertNo }}</span>
        </el-form-item>
        <el-form-item label="长者及指标">
          <span>{{ currentAlert?.elderName }} - {{ currentAlert?.metricName }} ({{ currentAlert?.alertValue }} {{ currentAlert?.unit }})</span>
        </el-form-item>
        <el-form-item label="处置结果" required>
          <el-radio-group v-model="handleForm.status">
            <el-radio-button label="RESOLVED">已处理解决</el-radio-button>
            <el-radio-button label="IGNORED">忽略 (长者活动后生理波动/误触)</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处置复核说明" required>
          <el-input
            v-model="handleForm.handleResult"
            type="textarea"
            :rows="4"
            placeholder="请详述护士处置措施、遵医嘱用药、物理降温、吸氧、复测体征数值或家属告知记录..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="dialogSubmitting" @click="submitHandle">提交处置记录</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 3: 单项体征录入 -->
    <el-dialog v-model="singleRecordDialogVisible" title="录入长者健康体征" width="520px" destroy-on-close>
      <el-form ref="singleFormRef" :model="singleRecordForm" :rules="singleRecordRules" label-width="100px">
        <el-form-item label="选择长者" prop="elderId">
          <el-select
            v-model="singleRecordForm.elderId"
            filterable
            placeholder="搜索长者姓名"
            style="width: 100%"
          >
            <el-option v-for="e in elderOptions" :key="e.id" :label="`${e.name} (${e.elderNo})`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="体征指标" prop="metricId">
          <el-select v-model="singleRecordForm.metricId" placeholder="选择指标" style="width: 100%">
            <el-option
              v-for="m in metricList"
              :key="m.id"
              :label="`${m.metricName} [${m.metricCode}] (${m.unit || '-'})`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="测量数值" prop="metricValue">
          <el-input-number
            v-model="singleRecordForm.metricValue"
            :precision="2"
            :step="0.5"
            style="width: 100%"
            placeholder="输入测量值"
          />
        </el-form-item>
        <el-form-item label="测量时间" prop="measuredAt">
          <el-date-picker
            v-model="singleRecordForm.measuredAt"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="主诉/说明">
          <el-input v-model="singleRecordForm.remark" type="textarea" :rows="2" placeholder="如长者静息后测量、体感微温等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="singleRecordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitSingleRecord">保存并评估预警</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 4: 批量体征套检录入 -->
    <el-dialog v-model="batchRecordDialogVisible" title="批量录入长者体征套检" width="650px" destroy-on-close>
      <el-form :model="batchRecordForm" label-width="100px">
        <el-form-item label="选择长者" required>
          <el-select v-model="batchRecordForm.elderId" filterable placeholder="选择长者" style="width: 100%">
            <el-option v-for="e in elderOptions" :key="e.id" :label="`${e.name} (${e.elderNo})`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="测量时间" required>
          <el-date-picker
            v-model="batchRecordForm.measuredAt"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="套检项目">
          <el-table :data="batchItems" border size="small">
            <el-table-column prop="metricName" label="指标名称" width="160">
              <template #default="{ row }">
                <span>{{ row.metricName }} ({{ row.unit }})</span>
              </template>
            </el-table-column>
            <el-table-column label="测量数值">
              <template #default="{ row }">
                <el-input-number v-model="row.metricValue" :precision="1" :step="0.5" style="width: 100%" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item label="巡检备注">
          <el-input v-model="batchRecordForm.remark" placeholder="例行晨检/夜巡体征抽检" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRecordDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="dialogSubmitting" @click="submitBatchRecord">批量保存并评估</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 5: 历史体征趋势折线图/数据 -->
    <el-dialog v-model="trendDialogVisible" title="健康体征历史波动趋势" width="700px" destroy-on-close>
      <div class="trend-header">
        <span class="trend-elder">{{ currentTrendElderName }} - {{ currentTrendMetricName }}</span>
        <el-radio-group v-model="trendDays" size="small" @change="fetchTrendData">
          <el-radio-button :label="7">近7天</el-radio-button>
          <el-radio-button :label="14">近14天</el-radio-button>
          <el-radio-button :label="30">近30天</el-radio-button>
        </el-radio-group>
      </div>

      <div class="trend-list" v-loading="trendLoading">
        <el-table :data="trendPoints" border size="small" max-height="350">
          <el-table-column prop="measuredAt" label="测量时间" width="180" />
          <el-table-column prop="metricValue" label="测量数值" align="center">
            <template #default="{ row }">
              <span class="vital-value">{{ row.metricValue }}</span> {{ row.unitSnapshot }}
            </template>
          </el-table-column>
          <el-table-column prop="sourceType" label="来源" align="center" width="120" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="trendDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 6: 预警规则新增/编辑 -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.id ? '编辑预警规则' : '新增预警规则'" width="560px" destroy-on-close>
      <el-form :model="ruleForm" label-width="110px">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.ruleName" placeholder="如：高血压危急预警" />
        </el-form-item>
        <el-form-item label="关联指标" required>
          <el-select v-model="ruleForm.metricId" placeholder="选择监测指标" style="width: 100%">
            <el-option v-for="m in metricList" :key="m.id" :label="`${m.metricName} [${m.metricCode}]`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件判定" required>
          <el-select v-model="ruleForm.conditionType" style="width: 100%">
            <el-option label="高于上限 (GT)" value="GT" />
            <el-option label="低于下限 (LT)" value="LT" />
            <el-option label="超出区间 (OUT_OF_RANGE)" value="OUT_OF_RANGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="下限阈值 (Min)" v-if="ruleForm.conditionType === 'LT' || ruleForm.conditionType === 'OUT_OF_RANGE'">
          <el-input-number v-model="ruleForm.minValue" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="上限阈值 (Max)" v-if="ruleForm.conditionType === 'GT' || ruleForm.conditionType === 'OUT_OF_RANGE'">
          <el-input-number v-model="ruleForm.maxValue" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预警级别" required>
          <el-select v-model="ruleForm.severity" style="width: 100%">
            <el-option label="危急 (CRITICAL)" value="CRITICAL" />
            <el-option label="高风险 (HIGH)" value="HIGH" />
            <el-option label="中度 (MEDIUM)" value="MEDIUM" />
            <el-option label="提示 (LOW)" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="去重时间窗口" required>
          <el-input-number v-model="ruleForm.dedupMinutes" :min="1" :max="1440" style="width: 100%" />
          <span class="form-tip">单位为分钟。同长者同规则在设定期限内不重复弹出告警，防止告警轰炸。</span>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch :model-value="ruleForm.enabled === 1" @change="(val: boolean) => ruleForm.enabled = val ? 1 : 0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitRule">保存规则</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 7: 指标新增/编辑 -->
    <el-dialog v-model="metricDialogVisible" :title="metricForm.id ? '编辑健康指标' : '新增健康指标'" width="520px" destroy-on-close>
      <el-form :model="metricForm" label-width="100px">
        <el-form-item label="指标编码" required>
          <el-input v-model="metricForm.metricCode" placeholder="如：SBP, DBP, TEMP, SPO2" :disabled="!!metricForm.id" />
        </el-form-item>
        <el-form-item label="指标名称" required>
          <el-input v-model="metricForm.metricName" placeholder="如：收缩压" />
        </el-form-item>
        <el-form-item label="度量单位">
          <el-input v-model="metricForm.unit" placeholder="如：mmHg, ℃, %, bpm" />
        </el-form-item>
        <el-form-item label="参考范围说明">
          <el-input v-model="metricForm.description" type="textarea" :rows="3" placeholder="如：正常参考范围 90-139 mmHg" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metricDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitMetric">保存指标</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import {
  BellFilled,
  WarningFilled,
  Clock,
  Loading,
  CircleCheckFilled,
  Search,
  Refresh,
  Plus,
  DocumentAdd,
  TrendCharts
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getHealthAlertSummary,
  getHealthAlertPage,
  getHealthRecordPage,
  getHealthMetrics,
  getHealthAlertRules,
  getHealthTrend,
  assignHealthAlert,
  handleHealthAlert,
  createHealthRecord,
  batchCreateHealthRecords,
  createHealthAlertRule,
  updateHealthAlertRule,
  toggleHealthAlertRule,
  createHealthMetric,
  updateHealthMetric,
  toggleHealthMetricStatus,
  HealthAlertVO,
  HealthAlertSummaryVO,
  HealthRecordVO,
  HealthMetricVO,
  HealthAlertRuleVO,
  HealthTrendPointVO
} from '@/api/health'
import { getElderList } from '@/api/elder'
import { getUserList } from '@/api/system'

const activeTab = ref('alerts')

// KPI 看板
const summary = reactive<HealthAlertSummaryVO>({
  totalAlerts: 0,
  openCount: 0,
  processingCount: 0,
  resolvedCount: 0,
  ignoredCount: 0,
  criticalCount: 0
})

// Tab 1: 预警列表
const alertLoading = ref(false)
const alertList = ref<HealthAlertVO[]>([])
const alertTotal = ref(0)
const alertQuery = reactive({
  alertNo: '',
  elderName: '',
  severity: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

// Tab 2: 体征记录
const recordLoading = ref(false)
const recordList = ref<HealthRecordVO[]>([])
const recordTotal = ref(0)
const recordQuery = reactive({
  elderName: '',
  metricId: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

// Tab 3: 预警规则
const ruleLoading = ref(false)
const ruleList = ref<HealthAlertRuleVO[]>([])

// Tab 4: 指标列表
const metricLoading = ref(false)
const metricList = ref<HealthMetricVO[]>([])

// 下拉候选项
const elderOptions = ref<any[]>([])
const staffList = ref<any[]>([])

// 弹窗状态与表单
const dialogSubmitting = ref(false)
const currentAlert = ref<HealthAlertVO | null>(null)

// 指派弹窗
const assignDialogVisible = ref(false)
const assignForm = reactive({ assigneeId: undefined as number | undefined })

// 处置弹窗
const handleDialogVisible = ref(false)
const handleForm = reactive({
  status: 'RESOLVED' as 'RESOLVED' | 'IGNORED',
  handleResult: ''
})

// 单项录入弹窗
const singleRecordDialogVisible = ref(false)
const singleRecordForm = reactive({
  elderId: undefined as number | undefined,
  metricId: undefined as number | undefined,
  metricValue: 0,
  measuredAt: '',
  remark: ''
})
const singleRecordRules = {
  elderId: [{ required: true, message: '请选择长者', trigger: 'change' }],
  metricId: [{ required: true, message: '请选择指标', trigger: 'change' }],
  metricValue: [{ required: true, message: '请输入测量数值', trigger: 'blur' }],
  measuredAt: [{ required: true, message: '请选择测量时间', trigger: 'change' }]
}

// 批量套检录入弹窗
const batchRecordDialogVisible = ref(false)
const batchRecordForm = reactive({
  elderId: undefined as number | undefined,
  measuredAt: '',
  remark: ''
})
const batchItems = ref<Array<{ metricId: number; metricName: string; unit: string; metricValue: number }>>([])

// 趋势弹窗
const trendDialogVisible = ref(false)
const trendLoading = ref(false)
const trendPoints = ref<HealthTrendPointVO[]>([])
const trendDays = ref(7)
const currentTrendElderId = ref<number | null>(null)
const currentTrendMetricId = ref<number | null>(null)
const currentTrendElderName = ref('')
const currentTrendMetricName = ref('')

// 规则新增/编辑弹窗
const ruleDialogVisible = ref(false)
const ruleForm = reactive({
  id: undefined as number | undefined,
  ruleName: '',
  metricId: undefined as number | undefined,
  conditionType: 'GT',
  minValue: undefined as number | undefined,
  maxValue: undefined as number | undefined,
  severity: 'HIGH',
  dedupMinutes: 30,
  enabled: 1
})

// 指标新增/编辑弹窗
const metricDialogVisible = ref(false)
const metricForm = reactive({
  id: undefined as number | undefined,
  metricCode: '',
  metricName: '',
  unit: '',
  description: ''
})

// 加载数据
async function fetchSummary() {
  try {
    const res = await getHealthAlertSummary()
    Object.assign(summary, res.data)
  } catch (err) {
    console.error(err)
  }
}

async function fetchAlerts() {
  alertLoading.value = true
  try {
    const res = await getHealthAlertPage(alertQuery)
    alertList.value = res.data.list
    alertTotal.value = res.data.total
  } finally {
    alertLoading.value = false
  }
}

function resetAlertQuery() {
  alertQuery.alertNo = ''
  alertQuery.elderName = ''
  alertQuery.severity = ''
  alertQuery.status = ''
  alertQuery.pageNum = 1
  fetchAlerts()
}

async function fetchRecords() {
  recordLoading.value = true
  try {
    const res = await getHealthRecordPage(recordQuery)
    recordList.value = res.data.list
    recordTotal.value = res.data.total
  } finally {
    recordLoading.value = false
  }
}

function resetRecordQuery() {
  recordQuery.elderName = ''
  recordQuery.metricId = undefined
  recordQuery.pageNum = 1
  fetchRecords()
}

async function fetchRules() {
  ruleLoading.value = true
  try {
    const res = await getHealthAlertRules()
    ruleList.value = res.data
  } finally {
    ruleLoading.value = false
  }
}

async function fetchMetrics() {
  metricLoading.value = true
  try {
    const res = await getHealthMetrics()
    metricList.value = res.data
  } finally {
    metricLoading.value = false
  }
}

async function loadEldersAndStaff() {
  try {
    const elderRes = await getElderList({ pageNum: 1, pageSize: 200 })
    elderOptions.value = elderRes.data.list
  } catch (e) {
    console.error(e)
  }
  try {
    const userRes = await getUserList({ pageNum: 1, pageSize: 100 })
    staffList.value = userRes.data.list
  } catch (e) {
    console.error(e)
  }
}

function handleTabChange(tabName: any) {
  if (tabName === 'alerts') {
    fetchAlerts()
    fetchSummary()
  } else if (tabName === 'records') {
    fetchRecords()
  } else if (tabName === 'rules') {
    fetchRules()
  } else if (tabName === 'metrics') {
    fetchMetrics()
  }
}

// 弹窗操作方法
function openAssignDialog(row: HealthAlertVO) {
  currentAlert.value = row
  assignForm.assigneeId = row.assigneeId
  assignDialogVisible.value = true
}

async function submitAssign() {
  if (!assignForm.assigneeId || !currentAlert.value) {
    ElMessage.warning('请选择责任人')
    return
  }
  dialogSubmitting.value = true
  try {
    await assignHealthAlert(currentAlert.value.id, { assigneeId: assignForm.assigneeId })
    ElMessage.success('责任人指派成功')
    assignDialogVisible.value = false
    fetchAlerts()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

function openHandleDialog(row: HealthAlertVO) {
  currentAlert.value = row
  handleForm.status = 'RESOLVED'
  handleForm.handleResult = ''
  handleDialogVisible.value = true
}

async function submitHandle() {
  if (!handleForm.handleResult.trim() || !currentAlert.value) {
    ElMessage.warning('请填写处置复核说明')
    return
  }
  dialogSubmitting.value = true
  try {
    await handleHealthAlert(currentAlert.value.id, {
      status: handleForm.status,
      handleResult: handleForm.handleResult
    })
    ElMessage.success('预警处置闭环完成')
    handleDialogVisible.value = false
    fetchAlerts()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

function viewAlertDetail(row: HealthAlertVO) {
  currentAlert.value = row
  // 可以直接弹出处置弹窗或查看
  openHandleDialog(row)
}

function openSingleRecordDialog() {
  singleRecordForm.elderId = undefined
  singleRecordForm.metricId = metricList.value.length ? metricList.value[0].id : undefined
  singleRecordForm.metricValue = 0
  singleRecordForm.measuredAt = formatNow()
  singleRecordForm.remark = ''
  singleRecordDialogVisible.value = true
}

async function submitSingleRecord() {
  if (!singleRecordForm.elderId || !singleRecordForm.metricId) {
    ElMessage.warning('请补全必要长者和指标信息')
    return
  }
  dialogSubmitting.value = true
  try {
    await createHealthRecord({
      elderId: singleRecordForm.elderId,
      metricId: singleRecordForm.metricId,
      metricValue: singleRecordForm.metricValue,
      measuredAt: singleRecordForm.measuredAt,
      remark: singleRecordForm.remark
    })
    ElMessage.success('体征录入成功，已完成预警规则评估')
    singleRecordDialogVisible.value = false
    fetchRecords()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

function openBatchRecordDialog() {
  batchRecordForm.elderId = undefined
  batchRecordForm.measuredAt = formatNow()
  batchRecordForm.remark = '常规晨间健康体征巡检'
  // 选取常见的 SBP, DBP, HR, TEMP, SPO2
  batchItems.value = metricList.value.map(m => {
    let defaultVal = 0
    if (m.metricCode === 'SBP') defaultVal = 120
    else if (m.metricCode === 'DBP') defaultVal = 80
    else if (m.metricCode === 'HR') defaultVal = 75
    else if (m.metricCode === 'TEMP') defaultVal = 36.5
    else if (m.metricCode === 'SPO2') defaultVal = 98
    return {
      metricId: m.id,
      metricName: m.metricName,
      unit: m.unit || '',
      metricValue: defaultVal
    }
  })
  batchRecordDialogVisible.value = true
}

async function submitBatchRecord() {
  if (!batchRecordForm.elderId) {
    ElMessage.warning('请选择长者')
    return
  }
  dialogSubmitting.value = true
  try {
    await batchCreateHealthRecords({
      elderId: batchRecordForm.elderId,
      measuredAt: batchRecordForm.measuredAt,
      remark: batchRecordForm.remark,
      items: batchItems.value.map(i => ({ metricId: i.metricId, metricValue: i.metricValue }))
    })
    ElMessage.success('批量体征录入成功，已自动完成异常预警筛查')
    batchRecordDialogVisible.value = false
    fetchRecords()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

function openTrendDialog(row: HealthRecordVO) {
  currentTrendElderId.value = row.elderId
  currentTrendMetricId.value = row.metricId
  currentTrendElderName.value = row.elderName || '长者'
  currentTrendMetricName.value = row.metricName || '体征'
  trendDays.value = 7
  trendDialogVisible.value = true
  fetchTrendData()
}

async function fetchTrendData() {
  if (!currentTrendElderId.value || !currentTrendMetricId.value) return
  trendLoading.value = true
  try {
    const res = await getHealthTrend(currentTrendElderId.value, currentTrendMetricId.value, trendDays.value)
    trendPoints.value = res.data
  } finally {
    trendLoading.value = false
  }
}

function openRuleDialog(row?: HealthAlertRuleVO) {
  if (row) {
    ruleForm.id = row.id
    ruleForm.ruleName = row.ruleName
    ruleForm.metricId = row.metricId
    ruleForm.conditionType = row.conditionType
    ruleForm.minValue = row.minValue
    ruleForm.maxValue = row.maxValue
    ruleForm.severity = row.severity
    ruleForm.dedupMinutes = row.dedupMinutes
    ruleForm.enabled = row.enabled
  } else {
    ruleForm.id = undefined
    ruleForm.ruleName = ''
    ruleForm.metricId = metricList.value.length ? metricList.value[0].id : undefined
    ruleForm.conditionType = 'GT'
    ruleForm.minValue = undefined
    ruleForm.maxValue = 160
    ruleForm.severity = 'HIGH'
    ruleForm.dedupMinutes = 30
    ruleForm.enabled = 1
  }
  ruleDialogVisible.value = true
}

async function submitRule() {
  if (!ruleForm.ruleName.trim() || !ruleForm.metricId) {
    ElMessage.warning('请填写规则名称并选择指标')
    return
  }
  dialogSubmitting.value = true
  try {
    if (ruleForm.id) {
      await updateHealthAlertRule(ruleForm.id, ruleForm)
      ElMessage.success('预警规则修改成功')
    } else {
      await createHealthAlertRule(ruleForm as any)
      ElMessage.success('预警规则新增成功')
    }
    ruleDialogVisible.value = false
    fetchRules()
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleToggleRule(row: HealthAlertRuleVO, enabled: number) {
  try {
    await toggleHealthAlertRule(row.id, enabled)
    row.enabled = enabled
    ElMessage.success(enabled === 1 ? '规则已启用' : '规则已停用')
  } catch (e) {
    console.error(e)
  }
}

function openMetricDialog(row?: HealthMetricVO) {
  if (row) {
    metricForm.id = row.id
    metricForm.metricCode = row.metricCode
    metricForm.metricName = row.metricName
    metricForm.unit = row.unit || ''
    metricForm.description = row.description || ''
  } else {
    metricForm.id = undefined
    metricForm.metricCode = ''
    metricForm.metricName = ''
    metricForm.unit = ''
    metricForm.description = ''
  }
  metricDialogVisible.value = true
}

async function submitMetric() {
  if (!metricForm.metricCode.trim() || !metricForm.metricName.trim()) {
    ElMessage.warning('请填写指标编码与名称')
    return
  }
  dialogSubmitting.value = true
  try {
    if (metricForm.id) {
      await updateHealthMetric(metricForm.id, metricForm)
      ElMessage.success('指标更新成功')
    } else {
      await createHealthMetric(metricForm)
      ElMessage.success('指标新增成功')
    }
    metricDialogVisible.value = false
    fetchMetrics()
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleToggleMetric(row: HealthMetricVO, status: string) {
  try {
    await toggleHealthMetricStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 'ACTIVE' ? '指标已启用' : '指标已停用')
  } catch (e) {
    console.error(e)
  }
}

// 辅助格式化方法
function formatSeverity(sev?: string) {
  if (sev === 'CRITICAL') return '危急'
  if (sev === 'HIGH') return '高风险'
  if (sev === 'MEDIUM') return '中度'
  if (sev === 'LOW') return '轻度提示'
  return sev || '普通'
}

function getSeverityTagType(sev?: string): 'danger' | 'warning' | 'info' | 'primary' {
  if (sev === 'CRITICAL' || sev === 'HIGH') return 'danger'
  if (sev === 'MEDIUM') return 'warning'
  return 'info'
}

function getSeverityClass(sev?: string) {
  if (sev === 'CRITICAL') return 'critical-text'
  if (sev === 'HIGH') return 'high-text'
  if (sev === 'MEDIUM') return 'medium-text'
  return 'low-text'
}

function formatAlertStatus(st?: string) {
  if (st === 'OPEN') return '待处理'
  if (st === 'PROCESSING') return '处理中'
  if (st === 'RESOLVED') return '已处置'
  if (st === 'IGNORED') return '已忽略'
  return st || ''
}

function getAlertStatusTagType(st?: string): 'danger' | 'warning' | 'success' | 'info' {
  if (st === 'OPEN') return 'danger'
  if (st === 'PROCESSING') return 'warning'
  if (st === 'RESOLVED') return 'success'
  return 'info'
}

function formatConditionType(ct?: string) {
  if (ct === 'GT') return '高于上限 (GT)'
  if (ct === 'LT') return '低于下限 (LT)'
  if (ct === 'OUT_OF_RANGE') return '超出区间 (OUT_OF_RANGE)'
  return ct || ''
}

function formatNow(): string {
  const d = new Date()
  const pad = (n: number) => (n < 10 ? '0' + n : n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(() => {
  fetchSummary()
  fetchAlerts()
  fetchMetrics()
  loadEldersAndStaff()
})
</script>

<style scoped>
.health-page {
  padding: 16px;
  background-color: #f7f9fa;
  min-height: calc(100vh - 84px);
}

.medical-disclaimer-banner {
  margin-bottom: 16px;
  border-radius: 8px;
  border: 1px solid #ffd591;
  background: linear-gradient(135deg, #fffbe6 0%, #fff7e6 100%);
}

.disclaimer-title {
  font-weight: 700;
  font-size: 14px;
  color: #d46b08;
}

.disclaimer-content {
  font-size: 13px;
  color: #873800;
  line-height: 1.5;
  margin-top: 4px;
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
.kpi-card.critical .kpi-icon { background: #fff1f0; color: #ff4d4f; }
.kpi-card.open .kpi-icon { background: #fff7e6; color: #fa8c16; }
.kpi-card.processing .kpi-icon { background: #e6fffb; color: #13c2c2; }
.kpi-card.resolved .kpi-icon { background: #f6ffed; color: #52c41a; }

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

.critical-num { color: #cf1322; }
.warning-num { color: #d46b08; }
.info-num { color: #08979c; }
.success-num { color: #389e0d; }

.kpi-num-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.blink-tag {
  animation: pulse-red 1.8s infinite;
}

@keyframes pulse-red {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

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

.filter-header {
  margin-bottom: 12px;
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

.abnormal-val {
  font-weight: 700;
  font-size: 15px;
}

.vital-value {
  font-weight: 700;
  color: #262626;
  font-size: 14px;
}

.critical-text { color: #cf1322; }
.high-text { color: #f5222d; }
.medium-text { color: #fa8c16; }
.low-text { color: #1677ff; }

.unit-text {
  font-size: 12px;
  color: #8c8c8c;
  margin-left: 3px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
  display: block;
  line-height: 1.4;
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.trend-elder {
  font-weight: 600;
  font-size: 15px;
  color: #1f1f1f;
}
</style>
