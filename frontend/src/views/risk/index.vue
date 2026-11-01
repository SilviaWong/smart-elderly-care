<template>
  <div class="risk-event-page">
    <!-- 顶部风险事件 KPI 看板 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card total">
          <div class="kpi-icon"><el-icon><WarningFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">风险事件总数</span>
            <span class="kpi-num">{{ summary.totalCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card reported">
          <div class="kpi-icon"><el-icon><Bell /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待分派处置</span>
            <span class="kpi-num warning-num">{{ summary.reportedCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card processing">
          <div class="kpi-icon"><el-icon><Loading /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">处置中</span>
            <span class="kpi-num primary-num">{{ (summary.assignedCount || 0) + (summary.processingCount || 0) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card pending-review">
          <div class="kpi-icon"><el-icon><Stamp /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待主管复核</span>
            <span class="kpi-num purple-num">{{ summary.pendingReviewCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card high-critical">
          <div class="kpi-icon"><el-icon><Notification /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">高危事件预警</span>
            <span class="kpi-num danger-num pulse-badge">{{ summary.highCriticalCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card closed">
          <div class="kpi-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">已结案关闭</span>
            <span class="kpi-num success-num">{{ summary.closedCount }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主工作区卡片 -->
    <el-card shadow="never" class="main-card">
      <div class="header-actions">
        <div class="title-area">
          <h2 class="page-title">安全与风险事件工作台</h2>
          <span class="page-subtitle">涵盖院内跌倒、走失、突发急症与设备SOS等事件全生命周期分派、现场处置、主管复核与轨迹审计</span>
        </div>
        <div class="action-buttons">
          <el-button type="primary" :icon="Plus" @click="handleOpenReport">
            登记风险事件
          </el-button>
          <el-button :icon="Refresh" @click="loadAllData">
            刷新
          </el-button>
        </div>
      </div>

      <!-- 搜索与筛选工具栏 -->
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="综合搜索">
          <el-input
            v-model="queryForm.keyword"
            placeholder="单号 / 地点 / 现场描述 / 结论"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="queryForm.eventType" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="跌倒事件 (FALL)" value="FALL" />
            <el-option label="长者走失 (LOST)" value="LOST" />
            <el-option label="突发急症 (SUDDEN_ILLNESS)" value="SUDDEN_ILLNESS" />
            <el-option label="设备SOS报警 (DEVICE_SOS)" value="DEVICE_SOS" />
            <el-option label="噎食误吸 (CHOKING)" value="CHOKING" />
            <el-option label="争执纠纷 (CONFLICT)" value="CONFLICT" />
            <el-option label="其他事件 (OTHER)" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="queryForm.severity" placeholder="全部等级" clearable style="width: 130px">
            <el-option label="低风险 (LOW)" value="LOW" />
            <el-option label="中风险 (MEDIUM)" value="MEDIUM" />
            <el-option label="高风险 (HIGH)" value="HIGH" />
            <el-option label="危急 (CRITICAL)" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="流转状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="待分派 (REPORTED)" value="REPORTED" />
            <el-option label="已分派 (ASSIGNED)" value="ASSIGNED" />
            <el-option label="处置中 (PROCESSING)" value="PROCESSING" />
            <el-option label="待复核 (PENDING_REVIEW)" value="PENDING_REVIEW" />
            <el-option label="已结案 (CLOSED)" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="涉及长者">
          <el-select
            v-model="queryForm.elderId"
            placeholder="请选择长者"
            filterable
            clearable
            style="width: 160px"
          >
            <el-option
              v-for="elder in elderOptions"
              :key="elder.id"
              :label="elder.name + ' (' + elder.elderNo + ')'"
              :value="elder.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="发生时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 320px"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 风险事件数据表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        class="custom-table"
      >
        <el-table-column prop="eventNo" label="事件编号" width="160" fixed="left">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" class="mono-link" @click="handleViewDetail(row.id)">
              {{ row.eventNo }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="涉及长者与床位" width="180">
          <template #default="{ row }">
            <div v-if="row.elderName" class="elder-cell">
              <span class="elder-name">{{ row.elderName }}</span>
              <span class="elder-bed" :title="row.bedInfo">{{ row.bedInfo || '-' }}</span>
            </div>
            <span v-else class="text-muted">公共区域/设施</span>
          </template>
        </el-table-column>

        <el-table-column prop="eventTypeName" label="事件类型" width="130">
          <template #default="{ row }">
            <el-tag :type="getEventTypeTagType(row.eventType)" effect="light" class="type-tag">
              {{ row.eventTypeName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="severityName" label="严重程度" width="130">
          <template #default="{ row }">
            <div class="severity-badge-wrapper">
              <span
                class="severity-dot"
                :class="'dot-' + row.severity.toLowerCase()"
              ></span>
              <el-tag
                :type="getSeverityTagType(row.severity)"
                effect="dark"
                class="severity-tag"
                :class="{ 'pulse-tag': row.severity === 'CRITICAL' }"
              >
                {{ row.severityName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="statusName" label="当前状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" effect="plain" class="status-tag">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="locationDesc" label="发生地点" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-icon class="mr-1 text-muted"><Location /></el-icon>
            <span>{{ row.locationDesc || '院内未指明地点' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="occurredAt" label="发生时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ row.occurredAt }}</span>
          </template>
        </el-table-column>

        <el-table-column label="责任人 / 上报人" width="160">
          <template #default="{ row }">
            <div class="staff-cell">
              <div><span class="text-muted">负责人: </span><strong>{{ row.assigneeName }}</strong></div>
              <div class="text-muted small-text">上报: {{ row.reporterName }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" size="small" @click="handleViewDetail(row.id)">
                详情轨迹
              </el-button>

              <!-- 未结案时支持分派/转派 -->
              <el-button
                v-if="row.status !== 'CLOSED'"
                link
                type="warning"
                size="small"
                @click="handleOpenAssign(row)"
              >
                {{ row.assigneeId ? '转派' : '分派' }}
              </el-button>

              <!-- 未结案时支持处置 -->
              <el-button
                v-if="row.status === 'REPORTED' || row.status === 'ASSIGNED' || row.status === 'PROCESSING'"
                link
                type="primary"
                size="small"
                @click="handleOpenProcess(row)"
              >
                {{ row.status === 'PROCESSING' ? '进展' : '接单处置' }}
              </el-button>

              <!-- 处置中可提审 -->
              <el-button
                v-if="row.status === 'PROCESSING'"
                link
                type="info"
                size="small"
                @click="handleOpenSubmitReview(row)"
              >
                提审
              </el-button>

              <!-- 待复核状态的主管审核 -->
              <el-button
                v-if="row.status === 'PENDING_REVIEW'"
                link
                type="danger"
                size="small"
                @click="handleOpenReview(row)"
              >
                主管复核
              </el-button>

              <!-- 普通结案关闭按钮 -->
              <el-button
                v-if="row.status !== 'CLOSED' && row.status !== 'PENDING_REVIEW'"
                link
                type="success"
                size="small"
                @click="handleOpenClose(row)"
              >
                结案
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>

    <!-- 1. 登记上报风险事件弹窗 -->
    <el-dialog
      v-model="reportDialogVisible"
      title="登记上报安全风险事件"
      width="640px"
      destroy-on-close
      class="custom-dialog"
    >
      <el-form
        ref="reportFormRef"
        :model="reportForm"
        :rules="reportRules"
        label-width="110px"
        class="form-padding"
      >
        <el-form-item label="涉及长者" prop="elderId">
          <el-select
            v-model="reportForm.elderId"
            placeholder="请选择涉事长者（公共区域事件可不选）"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="elder in elderOptions"
              :key="elder.id"
              :label="elder.name + ' (' + elder.elderNo + ')'"
              :value="elder.id"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="事件类型" prop="eventType">
              <el-select v-model="reportForm.eventType" placeholder="选择事件类型" style="width: 100%">
                <el-option label="跌倒事件 (FALL)" value="FALL" />
                <el-option label="长者走失 (LOST)" value="LOST" />
                <el-option label="突发急症 (SUDDEN_ILLNESS)" value="SUDDEN_ILLNESS" />
                <el-option label="设备SOS求助 (DEVICE_SOS)" value="DEVICE_SOS" />
                <el-option label="噎食误吸 (CHOKING)" value="CHOKING" />
                <el-option label="争执纠纷 (CONFLICT)" value="CONFLICT" />
                <el-option label="其他安全事件 (OTHER)" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="严重程度" prop="severity">
              <el-select v-model="reportForm.severity" placeholder="选择严重等级" style="width: 100%">
                <el-option label="低风险 / 轻微 (LOW)" value="LOW" />
                <el-option label="中风险 / 一般 (MEDIUM)" value="MEDIUM" />
                <el-option label="高风险 / 严重 (HIGH)" value="HIGH" />
                <el-option label="危急 / 特大 (CRITICAL)" value="CRITICAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="发生时间" prop="occurredAt">
              <el-date-picker
                v-model="reportForm.occurredAt"
                type="datetime"
                placeholder="发生精确时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="直接指派负责人">
              <el-select
                v-model="reportForm.assigneeId"
                placeholder="可选直接指定责任人"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="user.realName + ' (' + user.username + ')'"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="发生具体地点" prop="locationDesc">
          <el-input
            v-model="reportForm.locationDesc"
            placeholder="如：1号楼2层连廊休息区、中心花园长椅旁"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="现场情况描述" prop="description">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详述现场长者状态、伤情或异常表现、目击人员、已采取的初步紧急措施等"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitReport">确认上报</el-button>
      </template>
    </el-dialog>

    <!-- 2. 指派负责人弹窗 -->
    <el-dialog
      v-model="assignDialogVisible"
      :title="'指派处理负责人 - ' + (activeRow?.eventNo || '')"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="当前事件">
          <span class="strong-text">{{ activeRow?.eventTypeName }} ({{ activeRow?.severityName }})</span>
        </el-form-item>
        <el-form-item label="指派负责人" required>
          <el-select
            v-model="assignForm.assigneeId"
            placeholder="请选择处理负责人"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.realName + ' (' + user.username + ')'"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务要求与备注">
          <el-input
            v-model="assignForm.remark"
            type="textarea"
            :rows="3"
            placeholder="填写现场要求、注意事项或协同要求"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitAssign">确认指派</el-button>
      </template>
    </el-dialog>

    <!-- 3. 现场处置登记 / 进展记录弹窗 -->
    <el-dialog
      v-model="processDialogVisible"
      :title="(activeRow?.status === 'PROCESSING' ? '记录处置进展 - ' : '接单现场处置 - ') + (activeRow?.eventNo || '')"
      width="540px"
      destroy-on-close
    >
      <el-alert
        v-if="activeRow?.requiresReview"
        title="注意：该事件为高风险/危急等级，处置完毕后须提交主管复核审核方可结案"
        type="warning"
        show-icon
        :closable="false"
        class="mb-3"
      />
      <el-form label-width="100px">
        <el-form-item label="处置记录内容" required>
          <el-input
            v-model="processForm.content"
            type="textarea"
            :rows="5"
            placeholder="请详述现场介入措施、生命体征监测结果、医疗救治或送医陪诊、长者意识及情绪状况等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitProcess">保存处置记录</el-button>
      </template>
    </el-dialog>

    <!-- 4. 提交主管复核弹窗 -->
    <el-dialog
      v-model="submitReviewDialogVisible"
      :title="'提交主管复核 - ' + (activeRow?.eventNo || '')"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="110px">
        <el-form-item label="复核申请说明" required>
          <el-input
            v-model="submitReviewForm.content"
            type="textarea"
            :rows="4"
            placeholder="请简述已落实的处置措施、家属知情同意情况、长者当前稳定状态，并说明提请主管复核审查理由"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitReviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitSubmitReview">提交复核</el-button>
      </template>
    </el-dialog>

    <!-- 5. 主管复核审批弹窗 -->
    <el-dialog
      v-model="reviewDialogVisible"
      :title="'主管复核审批 - ' + (activeRow?.eventNo || '')"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="110px">
        <el-form-item label="复核决定" required>
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :label="true">审核通过，予以结案归档</el-radio>
            <el-radio :label="false">审核驳回，退回处置阶段整改</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="reviewForm.approved" label="结案处理结论" required>
          <el-input
            v-model="reviewForm.conclusion"
            type="textarea"
            :rows="4"
            placeholder="填写主管最终调查结论、善后防范措施及管理整改要求（必填）"
          />
        </el-form-item>

        <el-form-item v-else label="驳回整改原因" required>
          <el-input
            v-model="reviewForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="填写驳回原因、需要补充的检查报告或待落实的防范措施（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button
          :type="reviewForm.approved ? 'success' : 'danger'"
          :loading="submitLoading"
          @click="submitReview"
        >
          {{ reviewForm.approved ? '通过并结案' : '驳回整改' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 6. 结案关闭弹窗 -->
    <el-dialog
      v-model="closeDialogVisible"
      :title="'结案关闭 - ' + (activeRow?.eventNo || '')"
      width="520px"
      destroy-on-close
    >
      <el-alert
        v-if="activeRow?.requiresReview"
        title="高风险/危急事件按规则应由机构主管复核，若您为主管可在此直接录入结论结案"
        type="info"
        show-icon
        :closable="false"
        class="mb-3"
      />
      <el-form label-width="110px">
        <el-form-item label="结案处理结论" required>
          <el-input
            v-model="closeForm.conclusion"
            type="textarea"
            :rows="4"
            placeholder="请详细录入事件最终处置结果、长者康复情况、家属沟通反馈及防范后续发生的改进措施（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitLoading" @click="submitClose">确认结案关闭</el-button>
      </template>
    </el-dialog>

    <!-- 7. 详情与轨迹时间线抽屉 -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="风险事件全景与流转轨迹时间线"
      size="620px"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="drawer-content">
        <template v-if="detailData">
          <!-- 基本信息卡片 -->
          <el-card shadow="never" class="detail-summary-card mb-4">
            <div class="summary-header">
              <span class="event-no-badge">{{ detailData.event.eventNo }}</span>
              <div class="header-tags">
                <el-tag :type="getSeverityTagType(detailData.event.severity)" effect="dark">
                  {{ detailData.event.severityName }}
                </el-tag>
                <el-tag :type="getStatusTagType(detailData.event.status)" effect="plain">
                  {{ detailData.event.statusName }}
                </el-tag>
              </div>
            </div>

            <el-descriptions :column="2" border class="mt-3" size="small">
              <el-descriptions-item label="涉事长者">
                {{ detailData.event.elderName || '公共区域/未关联' }}
                <span v-if="detailData.event.elderNo" class="text-muted">({{ detailData.event.elderNo }})</span>
              </el-descriptions-item>
              <el-descriptions-item label="床位位置">
                {{ detailData.event.bedInfo || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="事件类型">
                {{ detailData.event.eventTypeName }}
              </el-descriptions-item>
              <el-descriptions-item label="发生时间">
                {{ detailData.event.occurredAt }}
              </el-descriptions-item>
              <el-descriptions-item label="发生地点" :span="2">
                {{ detailData.event.locationDesc || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="上报人员">
                {{ detailData.event.reporterName }}
              </el-descriptions-item>
              <el-descriptions-item label="处理负责人">
                <strong>{{ detailData.event.assigneeName }}</strong>
              </el-descriptions-item>
              <el-descriptions-item label="现场描述" :span="2">
                <div class="desc-content">{{ detailData.event.description }}</div>
              </el-descriptions-item>
              <el-descriptions-item v-if="detailData.event.conclusion" label="结案结论" :span="2">
                <div class="conclusion-content">
                  <el-icon class="mr-1 text-success"><CircleCheck /></el-icon>
                  {{ detailData.event.conclusion }}
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 处置轨迹时间线 -->
          <div class="timeline-container">
            <h3 class="timeline-title">
              <el-icon class="mr-1"><Clock /></el-icon>
              处理流转轨迹审计 ({{ detailData.logs.length }} 步)
            </h3>

            <el-timeline class="custom-timeline">
              <el-timeline-item
                v-for="log in detailData.logs"
                :key="log.id"
                :timestamp="log.createdAt"
                placement="top"
                :type="getTimelineItemType(log.action)"
                :hollow="log.action === 'REPORT'"
              >
                <div class="timeline-card">
                  <div class="timeline-header">
                    <span class="action-name">{{ log.actionName }}</span>
                    <span class="operator-badge">{{ log.operatorName }}</span>
                  </div>
                  <div v-if="log.fromStatusName && log.toStatusName" class="transition-badge">
                    <span class="from-st">{{ log.fromStatusName }}</span>
                    <el-icon class="mx-1"><Right /></el-icon>
                    <span class="to-st">{{ log.toStatusName }}</span>
                  </div>
                  <div class="timeline-body">
                    {{ log.content }}
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus,
  Refresh,
  Search,
  RefreshLeft,
  WarningFilled,
  Bell,
  Loading,
  Stamp,
  Notification,
  CircleCheckFilled,
  Location,
  Clock,
  Right,
  CircleCheck
} from '@element-plus/icons-vue'
import {
  getRiskEventPage,
  getRiskEventSummary,
  getRiskEventDetail,
  reportRiskEvent,
  assignRiskEvent,
  startProcessRiskEvent,
  updateRiskEventProgress,
  submitRiskEventReview,
  reviewRiskEvent,
  closeRiskEvent,
  RiskEventVO,
  RiskEventSummaryVO,
  RiskEventDetailVO
} from '@/api/risk'
import { fetchElders } from '@/api/elder'
import { fetchUsers } from '@/api/system'

// 数据加载状态
const loading = ref(false)
const submitLoading = ref(false)
const detailLoading = ref(false)

// 统计看板
const summary = reactive<RiskEventSummaryVO>({
  totalCount: 0,
  reportedCount: 0,
  assignedCount: 0,
  processingCount: 0,
  pendingReviewCount: 0,
  closedCount: 0,
  highCriticalCount: 0
})

// 筛选表单与表格数据
const queryForm = reactive({
  keyword: '',
  eventType: '',
  severity: '',
  status: '',
  elderId: undefined as number | undefined,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10
})
const dateRange = ref<[string, string] | null>(null)
const tableData = ref<RiskEventVO[]>([])
const total = ref(0)

// 下拉候选项
const elderOptions = ref<{ id: number; name: string; elderNo: string }[]>([])
const userOptions = ref<{ id: number; realName: string; username: string }[]>([])

// 弹窗状态与对象
const activeRow = ref<RiskEventVO | null>(null)

// 1. 上报弹窗
const reportDialogVisible = ref(false)
const reportFormRef = ref()
const reportForm = reactive({
  elderId: undefined as number | undefined,
  eventType: 'FALL',
  severity: 'MEDIUM',
  occurredAt: '',
  locationDesc: '',
  description: '',
  assigneeId: undefined as number | undefined
})
const reportRules = {
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
  occurredAt: [{ required: true, message: '请选择发生时间', trigger: 'change' }],
  description: [{ required: true, message: '请输入现场情况描述', trigger: 'blur' }]
}

// 2. 指派弹窗
const assignDialogVisible = ref(false)
const assignForm = reactive({
  assigneeId: undefined as number | undefined,
  remark: ''
})

// 3. 处置弹窗
const processDialogVisible = ref(false)
const processForm = reactive({
  content: ''
})

// 4. 提审弹窗
const submitReviewDialogVisible = ref(false)
const submitReviewForm = reactive({
  content: ''
})

// 5. 复核弹窗
const reviewDialogVisible = ref(false)
const reviewForm = reactive({
  approved: true,
  conclusion: '',
  rejectReason: ''
})

// 6. 结案弹窗
const closeDialogVisible = ref(false)
const closeForm = reactive({
  conclusion: ''
})

// 7. 详情时间线抽屉
const detailDrawerVisible = ref(false)
const detailData = ref<RiskEventDetailVO | null>(null)

// 生命周期
onMounted(() => {
  loadAllData()
  loadOptions()
})

const loadAllData = () => {
  loadSummary()
  loadTable()
}

// 加载看板汇总
const loadSummary = async () => {
  try {
    const res = await getRiskEventSummary()
    if (res.code === 200 && res.data) {
      Object.assign(summary, res.data)
    }
  } catch (err) {
    console.error('加载看板数据失败', err)
  }
}

// 加载分页列表
const loadTable = async () => {
  loading.value = true
  try {
    const res = await getRiskEventPage({
      ...queryForm
    })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.list
      total.value = res.data.total
    }
  } catch (err) {
    console.error('加载风险事件列表失败', err)
  } finally {
    loading.value = false
  }
}

// 加载长者和人员下拉候选项
const loadOptions = async () => {
  try {
    const [elderRes, userRes] = await Promise.all([
      fetchElders({ pageNum: 1, pageSize: 200 }),
      fetchUsers({ pageNum: 1, pageSize: 100 })
    ])
    if (elderRes.code === 200 && elderRes.data?.list) {
      elderOptions.value = elderRes.data.list.map(e => ({
        id: e.id,
        name: e.name,
        elderNo: e.elderNo
      }))
    }
    if (userRes.code === 200 && userRes.data?.list) {
      userOptions.value = userRes.data.list.map(u => ({
        id: u.id,
        realName: u.realName,
        username: u.username
      }))
    }
  } catch (err) {
    console.warn('获取关联下拉项异常', err)
  }
}

const handleDateChange = (val: [string, string] | null) => {
  if (val && val.length === 2) {
    queryForm.startTime = val[0]
    queryForm.endTime = val[1]
  } else {
    queryForm.startTime = undefined
    queryForm.endTime = undefined
  }
}

const handleQuery = () => {
  queryForm.pageNum = 1
  loadTable()
}

const handleResetQuery = () => {
  queryForm.keyword = ''
  queryForm.eventType = ''
  queryForm.severity = ''
  queryForm.status = ''
  queryForm.elderId = undefined
  queryForm.startTime = undefined
  queryForm.endTime = undefined
  dateRange.value = null
  handleQuery()
}

// 1. 打开上报弹窗
const handleOpenReport = () => {
  reportForm.elderId = undefined
  reportForm.eventType = 'FALL'
  reportForm.severity = 'MEDIUM'
  reportForm.occurredAt = new Date().toISOString().replace('T', ' ').substring(0, 19)
  reportForm.locationDesc = ''
  reportForm.description = ''
  reportForm.assigneeId = undefined
  reportDialogVisible.value = true
}

const submitReport = async () => {
  if (!reportFormRef.value) return
  await reportFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const res = await reportRiskEvent({
        ...reportForm
      })
      if (res.code === 200) {
        ElMessage.success('安全风险事件登记上报成功')
        reportDialogVisible.value = false
        loadAllData()
      }
    } catch (err: any) {
      ElMessage.error(err.message || '上报失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 2. 指派负责人
const handleOpenAssign = (row: RiskEventVO) => {
  activeRow.value = row
  assignForm.assigneeId = row.assigneeId
  assignForm.remark = ''
  assignDialogVisible.value = true
}

const submitAssign = async () => {
  if (!activeRow.value || !assignForm.assigneeId) {
    ElMessage.warning('请选择处理负责人')
    return
  }
  submitLoading.value = true
  try {
    const res = await assignRiskEvent(activeRow.value.id, {
      assigneeId: assignForm.assigneeId,
      remark: assignForm.remark
    })
    if (res.code === 200) {
      ElMessage.success('责任人指派成功')
      assignDialogVisible.value = false
      loadAllData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '指派失败')
  } finally {
    submitLoading.value = false
  }
}

// 3. 现场处置
const handleOpenProcess = (row: RiskEventVO) => {
  activeRow.value = row
  processForm.content = ''
  processDialogVisible.value = true
}

const submitProcess = async () => {
  if (!activeRow.value || !processForm.content.trim()) {
    ElMessage.warning('请填写现场处置措施或进展记录')
    return
  }
  submitLoading.value = true
  try {
    const res = activeRow.value.status === 'PROCESSING'
      ? await updateRiskEventProgress(activeRow.value.id, { content: processForm.content.trim() })
      : await startProcessRiskEvent(activeRow.value.id, { content: processForm.content.trim() })
    if (res.code === 200) {
      ElMessage.success('处置记录保存成功')
      processDialogVisible.value = false
      loadAllData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 4. 提交复核
const handleOpenSubmitReview = (row: RiskEventVO) => {
  activeRow.value = row
  submitReviewForm.content = ''
  submitReviewDialogVisible.value = true
}

const submitSubmitReview = async () => {
  if (!activeRow.value || !submitReviewForm.content.trim()) {
    ElMessage.warning('请填写复核申请说明')
    return
  }
  submitLoading.value = true
  try {
    const res = await submitRiskEventReview(activeRow.value.id, {
      content: submitReviewForm.content.trim()
    })
    if (res.code === 200) {
      ElMessage.success('已提交主管复核')
      submitReviewDialogVisible.value = false
      loadAllData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '提交复核失败')
  } finally {
    submitLoading.value = false
  }
}

// 5. 主管复核
const handleOpenReview = (row: RiskEventVO) => {
  activeRow.value = row
  reviewForm.approved = true
  reviewForm.conclusion = ''
  reviewForm.rejectReason = ''
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (!activeRow.value) return
  if (reviewForm.approved && !reviewForm.conclusion.trim()) {
    ElMessage.warning('审核通过必须填写结案处理结论')
    return
  }
  if (!reviewForm.approved && !reviewForm.rejectReason.trim()) {
    ElMessage.warning('审核驳回必须填写驳回原因与整改要求')
    return
  }
  submitLoading.value = true
  try {
    const res = await reviewRiskEvent(activeRow.value.id, {
      approved: reviewForm.approved,
      conclusion: reviewForm.conclusion.trim(),
      rejectReason: reviewForm.rejectReason.trim()
    })
    if (res.code === 200) {
      ElMessage.success(reviewForm.approved ? '复核通过，事件已结案关闭' : '已驳回整改')
      reviewDialogVisible.value = false
      loadAllData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '复核审批失败')
  } finally {
    submitLoading.value = false
  }
}

// 6. 结案关闭
const handleOpenClose = (row: RiskEventVO) => {
  activeRow.value = row
  closeForm.conclusion = ''
  closeDialogVisible.value = true
}

const submitClose = async () => {
  if (!activeRow.value || !closeForm.conclusion.trim()) {
    ElMessage.warning('结案必须填写处理结论')
    return
  }
  submitLoading.value = true
  try {
    const res = await closeRiskEvent(activeRow.value.id, {
      conclusion: closeForm.conclusion.trim()
    })
    if (res.code === 200) {
      ElMessage.success('事件已结案关闭')
      closeDialogVisible.value = false
      loadAllData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '结案失败')
  } finally {
    submitLoading.value = false
  }
}

// 7. 查看详情与时间线抽屉
const handleViewDetail = async (id: number) => {
  detailDrawerVisible.value = true
  detailLoading.value = true
  try {
    const res = await getRiskEventDetail(id)
    if (res.code === 200 && res.data) {
      detailData.value = res.data
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取事件详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 样式与标签辅助映射
const getEventTypeTagType = (type: string) => {
  switch (type?.toUpperCase()) {
    case 'FALL': return 'warning'
    case 'LOST': return 'danger'
    case 'SUDDEN_ILLNESS': return 'danger'
    case 'DEVICE_SOS': return 'info'
    case 'CHOKING': return 'danger'
    case 'CONFLICT': return 'warning'
    default: return 'info'
  }
}

const getSeverityTagType = (sev: string) => {
  switch (sev?.toUpperCase()) {
    case 'CRITICAL': return 'danger'
    case 'HIGH': return 'warning'
    case 'MEDIUM': return ''
    case 'LOW': return 'info'
    default: return 'info'
  }
}

const getStatusTagType = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'REPORTED': return 'info'
    case 'ASSIGNED': return 'warning'
    case 'PROCESSING': return 'primary'
    case 'PENDING_REVIEW': return 'danger'
    case 'CLOSED': return 'success'
    default: return 'info'
  }
}

const getTimelineItemType = (action: string) => {
  switch (action?.toUpperCase()) {
    case 'REPORT': return 'info'
    case 'ASSIGN': return 'warning'
    case 'START_PROCESS': return 'primary'
    case 'PROGRESS_UPDATE': return 'primary'
    case 'SUBMIT_REVIEW': return 'warning'
    case 'REVIEW_REJECT': return 'danger'
    case 'CLOSE': return 'success'
    default: return 'info'
  }
}
</script>

<style scoped>
.risk-event-page {
  padding: 16px;
  background-color: #f6f8fb;
  min-height: calc(100vh - 84px);
}

/* KPI 统计卡片 */
.kpi-row {
  margin-bottom: 16px;
}
.kpi-card {
  border-radius: 8px;
  display: flex;
  align-items: center;
  transition: all 0.25s ease;
  border: 1px solid #e2e8f0;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
:deep(.kpi-card .el-card__body) {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  width: 100%;
}
.kpi-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-right: 12px;
}
.kpi-content {
  display: flex;
  flex-direction: column;
}
.kpi-title {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 2px;
}
.kpi-num {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

.total .kpi-icon { background: #e0f2fe; color: #0284c7; }
.reported .kpi-icon { background: #fef3c7; color: #d97706; }
.processing .kpi-icon { background: #e0e7ff; color: #4f46e5; }
.pending-review .kpi-icon { background: #f3e8ff; color: #9333ea; }
.high-critical .kpi-icon { background: #fee2e2; color: #dc2626; }
.closed .kpi-icon { background: #dcfce7; color: #16a34a; }

.warning-num { color: #d97706; }
.primary-num { color: #4f46e5; }
.purple-num { color: #9333ea; }
.danger-num { color: #dc2626; }
.success-num { color: #16a34a; }

.pulse-badge {
  display: inline-block;
  animation: pulse-danger 2s infinite;
}

@keyframes pulse-danger {
  0% { transform: scale(1); }
  50% { transform: scale(1.08); color: #b91c1c; }
  100% { transform: scale(1); }
}

/* 主内容卡片 */
.main-card {
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 4px 0;
}
.page-subtitle {
  font-size: 13px;
  color: #64748b;
}
.action-buttons {
  display: flex;
  gap: 10px;
}

/* 筛选表单 */
.filter-form {
  margin-bottom: 12px;
}

/* 表格及单元格 */
.custom-table {
  border-radius: 6px;
  overflow: hidden;
}
.mono-link {
  font-family: Menlo, Monaco, Consolas, monospace;
  font-weight: 600;
}
.elder-cell {
  display: flex;
  flex-direction: column;
}
.elder-name {
  font-weight: 600;
  color: #1e293b;
}
.elder-bed {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.text-muted {
  color: #94a3b8;
}
.small-text {
  font-size: 12px;
}
.time-text {
  font-size: 13px;
  color: #475569;
}
.staff-cell {
  font-size: 13px;
  line-height: 1.4;
}

/* 严重程度标签带呼吸点 */
.severity-badge-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}
.severity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.dot-critical { background: #ef4444; box-shadow: 0 0 6px rgba(239, 68, 68, 0.6); }
.dot-high { background: #f97316; }
.dot-medium { background: #eab308; }
.dot-low { background: #3b82f6; }

.pulse-tag {
  animation: pulse-border 1.5s infinite;
}
@keyframes pulse-border {
  0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.5); }
  70% { box-shadow: 0 0 0 6px rgba(239, 68, 68, 0); }
  100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

/* 详情抽屉 */
.drawer-content {
  padding: 0 4px;
}
.detail-summary-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.event-no-badge {
  font-family: Menlo, Monaco, Consolas, monospace;
  font-weight: 700;
  font-size: 16px;
  color: #0f172a;
}
.header-tags {
  display: flex;
  gap: 8px;
}
.desc-content {
  line-height: 1.6;
  color: #334155;
  white-space: pre-line;
}
.conclusion-content {
  line-height: 1.6;
  color: #166534;
  background: #f0fdf4;
  padding: 8px 12px;
  border-radius: 4px;
  border: 1px solid #bbf7d0;
  display: flex;
  align-items: flex-start;
}
.text-success {
  color: #16a34a;
}

/* 时间线 */
.timeline-container {
  padding: 8px 0;
}
.timeline-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.timeline-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}
.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.action-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}
.operator-badge {
  font-size: 12px;
  color: #475569;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 12px;
}
.transition-badge {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}
.from-st, .to-st {
  font-weight: 500;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 1px 6px;
  border-radius: 3px;
}
.to-st {
  color: #0f172a;
  font-weight: 600;
}
.timeline-body {
  font-size: 13px;
  line-height: 1.5;
  color: #334155;
  white-space: pre-line;
}
</style>
