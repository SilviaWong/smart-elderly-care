<template>
  <div class="admission-page">
    <!-- 顶部床位概览与指标看板 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <el-card shadow="never" class="kpi-card total">
          <div class="kpi-icon">
            <el-icon><House /></el-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-title">总床位数</span>
            <span class="kpi-num">{{ summary.totalBeds }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <el-card shadow="never" class="kpi-card occupied">
          <div class="kpi-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-title">在住床位 / 入住率</span>
            <div class="kpi-num-row">
              <span class="kpi-num">{{ summary.occupiedBeds }}</span>
              <el-tag type="primary" size="small" effect="plain" class="rate-tag">
                {{ summary.occupancyRate }}%
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <el-card shadow="never" class="kpi-card available">
          <div class="kpi-icon">
            <el-icon><Check /></el-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-title">空闲可用床位</span>
            <span class="kpi-num success-num">{{ summary.availableBeds }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <el-card shadow="never" class="kpi-card maintenance">
          <div class="kpi-icon">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-title">维修/停用中</span>
            <span class="kpi-num warning-num">{{ summary.maintenanceBeds + summary.disabledBeds }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主体区域切换 Tabs -->
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- Tab 1: 床位全景看板 -->
        <el-tab-pane label="床位全景看板" name="board">
          <div class="board-toolbar">
            <div class="filter-group">
              <el-select v-model="boardFilter.buildingId" placeholder="全部楼栋" clearable style="width: 160px" @change="loadSpaceTree">
                <el-option v-for="bld in buildingList" :key="bld.id" :label="bld.buildingName" :value="bld.id" />
              </el-select>
              <el-radio-group v-model="boardFilter.status" size="default" class="status-radio">
                <el-radio-button label="ALL">全部 ({{ summary.totalBeds }})</el-radio-button>
                <el-radio-button label="AVAILABLE">空闲 ({{ summary.availableBeds }})</el-radio-button>
                <el-radio-button label="OCCUPIED">在住 ({{ summary.occupiedBeds }})</el-radio-button>
                <el-radio-button label="MAINTENANCE">维修 ({{ summary.maintenanceBeds }})</el-radio-button>
              </el-radio-group>
            </div>
            <div class="action-group">
              <el-button type="primary" icon="Plus" @click="openCheckInDialog()">办理长者入住</el-button>
              <el-button icon="Refresh" @click="loadSpaceTree">刷新看板</el-button>
            </div>
          </div>

          <!-- 楼栋及房间床位卡片网格 -->
          <div v-loading="boardLoading" class="buildings-container">
            <el-empty v-if="filteredSpaceTree.length === 0" description="暂无符合条件的楼栋与床位" />
            <div v-for="bld in filteredSpaceTree" :key="bld.id" class="building-section">
              <div class="building-header">
                <div class="bld-title">
                  <el-icon><House /></el-icon>
                  <span class="name">{{ bld.name }}</span>
                  <el-tag size="small" type="info">{{ bld.code }}</el-tag>
                </div>
                <div class="bld-stats">
                  <span>总床位: {{ bld.totalBeds }}</span>
                  <span class="split">|</span>
                  <span class="success-text">空闲: {{ bld.availableBeds }}</span>
                  <span class="split">|</span>
                  <span class="primary-text">在住: {{ bld.occupiedBeds }}</span>
                </div>
              </div>

              <div class="rooms-grid">
                <div v-for="room in bld.rooms" :key="room.id" class="room-card">
                  <div class="room-header">
                    <div class="room-no">
                      <span class="floor">{{ room.floorNo }}</span>
                      <span class="no">{{ room.roomNo }}室</span>
                    </div>
                    <el-tag size="small" :type="getRoomTypeTag(room.roomType)">
                      {{ formatRoomType(room.roomType) }}
                    </el-tag>
                  </div>

                  <div class="beds-list">
                    <div
                      v-for="bed in room.beds"
                      :key="bed.id"
                      class="bed-item"
                      :class="bed.status.toLowerCase()"
                    >
                      <div class="bed-top">
                        <span class="bed-no">{{ bed.bedNo }}</span>
                        <el-tag size="small" :type="getBedStatusTag(bed.status)" effect="light">
                          {{ formatBedStatus(bed.status) }}
                        </el-tag>
                      </div>

                      <!-- 在住信息 -->
                      <div v-if="bed.status === 'OCCUPIED'" class="elder-occupant">
                        <div class="avatar-circle">
                          {{ bed.currentElderName ? bed.currentElderName.slice(0, 1) : '长' }}
                        </div>
                        <div class="occupant-info">
                          <span class="name">{{ bed.currentElderName || '--' }}</span>
                          <span class="no">{{ bed.currentElderNo || '--' }}</span>
                        </div>
                      </div>

                      <!-- 空闲提示 -->
                      <div v-else-if="bed.status === 'AVAILABLE'" class="empty-bed">
                        <span class="desc">床位整洁 空置待入</span>
                      </div>

                      <!-- 维修/停用提示 -->
                      <div v-else class="maint-bed">
                        <span class="desc">暂停接待 维护中</span>
                      </div>

                      <!-- 卡片底部快捷操作 -->
                      <div class="bed-actions">
                        <template v-if="bed.status === 'AVAILABLE'">
                          <el-button
                            type="primary"
                            size="small"
                            link
                            @click="openCheckInDialog(bed)"
                          >
                            办理入住
                          </el-button>
                          <el-button
                            size="small"
                            link
                            @click="handleSetBedStatus(bed.id, 'MAINTENANCE')"
                          >
                            设为维修
                          </el-button>
                        </template>

                        <template v-else-if="bed.status === 'OCCUPIED'">
                          <el-button
                            type="primary"
                            size="small"
                            link
                            @click="openTransferDialog(bed)"
                          >
                            调床
                          </el-button>
                          <el-button
                            type="danger"
                            size="small"
                            link
                            @click="openDischargeDialog(bed)"
                          >
                            退住
                          </el-button>
                          <el-button
                            size="small"
                            link
                            @click="openChangeLogsDialog(bed.currentAdmissionId!)"
                          >
                            日志
                          </el-button>
                        </template>

                        <template v-else>
                          <el-button
                            type="success"
                            size="small"
                            link
                            @click="handleSetBedStatus(bed.id, 'AVAILABLE')"
                          >
                            恢复可用
                          </el-button>
                        </template>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 入住登记台账 -->
        <el-tab-pane label="入住登记台账" name="records">
          <div class="records-filter">
            <el-form :inline="true" :model="admissionsQuery" class="filter-form">
              <el-form-item label="入住单号">
                <el-input v-model="admissionsQuery.admissionNo" placeholder="单号" clearable style="width: 160px" />
              </el-form-item>
              <el-form-item label="长者姓名">
                <el-input v-model="admissionsQuery.elderName" placeholder="姓名" clearable style="width: 130px" />
              </el-form-item>
              <el-form-item label="床位编号">
                <el-input v-model="admissionsQuery.bedNo" placeholder="床位号" clearable style="width: 120px" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="admissionsQuery.status" placeholder="全部状态" clearable style="width: 130px">
                  <el-option label="在住 (ACTIVE)" value="ACTIVE" />
                  <el-option label="已退住 (DISCHARGED)" value="DISCHARGED" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="loadAdmissions">查询</el-button>
                <el-button icon="Refresh" @click="resetAdmissionsQuery">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="admissionList" v-loading="admissionsLoading" border stripe style="width: 100%">
            <el-table-column prop="admissionNo" label="入住单号" min-width="170">
              <template #default="{ row }">
                <span class="font-mono text-primary">{{ row.admissionNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="长者姓名" min-width="120">
              <template #default="{ row }">
                <div class="elder-cell">
                  <span class="name">{{ row.elderName }}</span>
                  <span class="no">{{ row.elderNo }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="位置 / 床位" min-width="160">
              <template #default="{ row }">
                <span>{{ row.buildingName }} - {{ row.roomNo }}室 - {{ row.bedNo }}床</span>
              </template>
            </el-table-column>
            <el-table-column prop="careLevelCode" label="护理等级" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ formatCareLevel(row.careLevelCode) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="depositAmount" label="押金金额" width="110" align="right">
              <template #default="{ row }">
                <span class="deposit-amount">¥{{ Number(row.depositAmount).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="admissionDate" label="入住日期" width="120" align="center" />
            <el-table-column label="退住日期" width="120" align="center">
              <template #default="{ row }">
                <span>{{ row.actualDischargeDate || row.plannedDischargeDate || '--' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '在住中' : '已退住' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operatorName" label="经办人" width="110" align="center" />
            <el-table-column label="操作" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <template v-if="row.status === 'ACTIVE'">
                  <el-button type="primary" size="small" link @click="openTransferDialogByRow(row)">
                    调床
                  </el-button>
                  <el-button type="danger" size="small" link @click="openDischargeDialogByRow(row)">
                    退住
                  </el-button>
                </template>
                <el-button size="small" link @click="openChangeLogsDialog(row.id)">
                  流转日志
                </el-button>
                <el-button size="small" link @click="viewAdmissionDetail(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="admissionsQuery.pageNum"
              v-model:page-size="admissionsQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="admissionsTotal"
              @size-change="loadAdmissions"
              @current-change="loadAdmissions"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 3: 空间资源配置 -->
        <el-tab-pane label="空间资源管理" name="spaces">
          <div class="space-toolbar">
            <div class="tip">
              维护机构楼栋、房间与床位基础档案；支持按房间增添与状态标记
            </div>
            <div class="btns">
              <el-button type="primary" icon="Plus" @click="openBuildingDialog">新增楼栋</el-button>
              <el-button type="success" icon="Plus" @click="openRoomDialog">新增房间</el-button>
              <el-button type="warning" icon="Plus" @click="openBedDialog">新增床位</el-button>
            </div>
          </div>

          <el-table :data="buildingList" border stripe style="width: 100%">
            <el-table-column prop="buildingCode" label="楼栋编码" width="140" />
            <el-table-column prop="buildingName" label="楼栋名称" min-width="160" />
            <el-table-column prop="address" label="所在位置" min-width="200" />
            <el-table-column prop="roomCount" label="房间数量" width="100" align="center" />
            <el-table-column prop="bedCount" label="床位总数" width="100" align="center" />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '启用中' : '已停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 弹窗 1: 办理长者入住登记 -->
    <el-dialog
      v-model="checkInVisible"
      title="办理长者入住登记"
      width="680px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="并发事务保障说明"
        description="提交入住时，系统将使用数据库独占行级排他锁 (SELECT ... FOR UPDATE) 锁定目标床位，杜绝多人并发同时占床；并在同一事务中原子更新长者档案状态并记入床位变更历史。"
        style="margin-bottom: 20px"
      />

      <el-form ref="checkInFormRef" :model="checkInForm" :rules="checkInRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="入住长者" prop="elderId">
              <el-select
                v-model="checkInForm.elderId"
                placeholder="请选择待入住长者"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="elder in pendingElderList"
                  :key="elder.id"
                  :label="`${elder.name} (${elder.elderNo})`"
                  :value="elder.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标床位" prop="bedId">
              <el-select
                v-model="checkInForm.bedId"
                placeholder="请选择空闲床位"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="bed in availableBedsList"
                  :key="bed.id"
                  :label="`${bed.buildingName} - ${bed.roomNo}室 - ${bed.bedNo}`"
                  :value="bed.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="入住日期" prop="admissionDate">
              <el-date-picker
                v-model="checkInForm.admissionDate"
                type="date"
                placeholder="选择入住日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划退住" prop="plannedDischargeDate">
              <el-date-picker
                v-model="checkInForm.plannedDischargeDate"
                type="date"
                placeholder="计划离院退住日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="护理等级" prop="careLevelCode">
              <el-select v-model="checkInForm.careLevelCode" placeholder="请选择护理等级" style="width: 100%">
                <el-option label="一级护理" value="CARE_LEVEL_1" />
                <el-option label="二级护理" value="CARE_LEVEL_2" />
                <el-option label="三级护理" value="CARE_LEVEL_3" />
                <el-option label="特级护理" value="SPECIAL_CARE" />
                <el-option label="认知症专护" value="DEMENTIA_CARE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="押金金额" prop="depositAmount">
              <el-input-number
                v-model="checkInForm.depositAmount"
                :min="0"
                :step="1000"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="checkInForm.contractNo" placeholder="如 CT-2026-001" />
        </el-form-item>

        <el-form-item label="办理备注" prop="remark">
          <el-input
            v-model="checkInForm.remark"
            type="textarea"
            :rows="3"
            placeholder="登记入住健康状况及特殊照护注意事项"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="checkInVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitCheckIn">确认办理入住</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2: 办理长者调床 -->
    <el-dialog
      v-model="transferVisible"
      title="办理长者调床"
      width="560px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="防死锁调床事务机制"
        description="调床操作涉及两个床位资源的同步变更。系统按主键全局升序加锁以杜绝多长者交叉互调时发生并发死锁，并在同一事务中原子释放原床位并占用新床位。"
        style="margin-bottom: 20px"
      />

      <div class="transfer-info-box">
        <div class="row">
          <span class="label">调床长者:</span>
          <span class="val font-bold">{{ currentTargetBed?.currentElderName || currentTargetAdmission?.elderName }}</span>
        </div>
        <div class="row">
          <span class="label">当前床位:</span>
          <span class="val text-danger">{{ currentTargetBed ? `${currentTargetBed.roomNo}室 - ${currentTargetBed.bedNo}床` : `${currentTargetAdmission?.roomNo}室 - ${currentTargetAdmission?.bedNo}床` }}</span>
        </div>
      </div>

      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="90px">
        <el-form-item label="目标新床" prop="newBedId">
          <el-select
            v-model="transferForm.newBedId"
            placeholder="请选择空闲目标床位"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="bed in availableBedsList"
              :key="bed.id"
              :label="`${bed.buildingName} - ${bed.roomNo}室 - ${bed.bedNo}`"
              :value="bed.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调床原因" prop="reason">
          <el-input
            v-model="transferForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请填写长者调床原因（如身体状况变化、采光通风需求等）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitTransfer">确认调床</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 3: 办理长者退住 -->
    <el-dialog
      v-model="dischargeVisible"
      title="办理长者退住"
      width="560px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="dischargePrecheck" class="precheck-container">
        <el-alert
          v-if="dischargePrecheck.blockingReasons && dischargePrecheck.blockingReasons.length > 0"
          type="error"
          show-icon
          :closable="false"
          title="退住阻断提示"
          style="margin-bottom: 12px"
        >
          <template #default>
            <ul>
              <li v-for="(reason, idx) in dischargePrecheck.blockingReasons" :key="idx">{{ reason }}</li>
            </ul>
          </template>
        </el-alert>

        <el-alert
          v-if="dischargePrecheck.warnings && dischargePrecheck.warnings.length > 0"
          type="warning"
          show-icon
          :closable="false"
          title="费用与前置提醒"
          style="margin-bottom: 12px"
        >
          <template #default>
            <ul>
              <li v-for="(warn, idx) in dischargePrecheck.warnings" :key="idx">{{ warn }}</li>
            </ul>
          </template>
        </el-alert>
      </div>

      <el-form ref="dischargeFormRef" :model="dischargeForm" :rules="dischargeRules" label-width="110px">
        <el-form-item label="实际退住日" prop="actualDischargeDate">
          <el-date-picker
            v-model="dischargeForm.actualDischargeDate"
            type="date"
            placeholder="实际离院退住日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="退住原因" prop="dischargeReason">
          <el-input
            v-model="dischargeForm.dischargeReason"
            type="textarea"
            :rows="3"
            placeholder="如康复出院、回家照护、转往医疗机构等"
          />
        </el-form-item>

        <el-form-item v-if="dischargePrecheck && dischargePrecheck.warnings.length > 0" label="确认授权">
          <el-checkbox v-model="dischargeForm.forceDischarge">
            已确认上述提醒，确认办理离院并释放床位
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dischargeVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitLoading" @click="submitDischarge">确认退住释放床位</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 4: 床位流转历史时间线 -->
    <el-dialog
      v-model="changeLogsVisible"
      title="床位变动与流转审计历史"
      width="600px"
      destroy-on-close
    >
      <div v-loading="logsLoading" class="timeline-container">
        <el-empty v-if="changeLogs.length === 0" description="暂无变动日志" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="log in changeLogs"
            :key="log.id"
            :timestamp="log.changedAt"
            :type="getChangeLogType(log.changeType)"
            placement="top"
          >
            <el-card shadow="never" class="log-card">
              <div class="log-title">
                <el-tag size="small" :type="getChangeLogType(log.changeType)">
                  {{ formatChangeType(log.changeType) }}
                </el-tag>
                <span class="operator">经办人: {{ log.changedByName }}</span>
              </div>
              <div class="log-detail">
                <span v-if="log.changeType === 'CHECK_IN'">
                  初次入住并分配床位: <strong>{{ log.toBedNo }}</strong>
                </span>
                <span v-else-if="log.changeType === 'TRANSFER'">
                  从原床位 <strong>{{ log.fromBedNo }}</strong> 调至新床位 <strong>{{ log.toBedNo }}</strong>
                </span>
                <span v-else-if="log.changeType === 'DISCHARGE'">
                  办理退住，释放原床位 <strong>{{ log.fromBedNo }}</strong>
                </span>
              </div>
              <div v-if="log.changeReason" class="log-reason">
                变动原因/备注: {{ log.changeReason }}
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
      <template #footer>
        <el-button @click="changeLogsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 5: 新增楼栋 -->
    <el-dialog v-model="buildingDialogVisible" title="新增楼栋" width="460px" destroy-on-close>
      <el-form ref="buildingFormRef" :model="buildingForm" :rules="buildingRules" label-width="90px">
        <el-form-item label="楼栋编码" prop="buildingCode">
          <el-input v-model="buildingForm.buildingCode" placeholder="如 BLD-03" />
        </el-form-item>
        <el-form-item label="楼栋名称" prop="buildingName">
          <el-input v-model="buildingForm.buildingName" placeholder="如 3号护理中心" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="buildingForm.address" placeholder="园区内具体位置" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buildingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitCreateBuilding">确定</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 6: 新增房间 -->
    <el-dialog v-model="roomDialogVisible" title="新增房间" width="460px" destroy-on-close>
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="90px">
        <el-form-item label="所属楼栋" prop="buildingId">
          <el-select v-model="roomForm.buildingId" placeholder="选择楼栋" style="width: 100%">
            <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在楼层" prop="floorNo">
          <el-input v-model="roomForm.floorNo" placeholder="如 1F, 2F" />
        </el-form-item>
        <el-form-item label="房间编号" prop="roomNo">
          <el-input v-model="roomForm.roomNo" placeholder="如 101, 202" />
        </el-form-item>
        <el-form-item label="房间房型" prop="roomType">
          <el-select v-model="roomForm.roomType" placeholder="选择房型" style="width: 100%">
            <el-option label="单人间" value="SINGLE" />
            <el-option label="双人间" value="DOUBLE" />
            <el-option label="套间" value="SUITE" />
            <el-option label="护理专房" value="CARE_ROOM" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitCreateRoom">确定</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 7: 新增床位 -->
    <el-dialog v-model="bedDialogVisible" title="新增床位" width="460px" destroy-on-close>
      <el-form ref="bedFormRef" :model="bedForm" :rules="bedRules" label-width="90px">
        <el-form-item label="所属房间" prop="roomId">
          <el-select v-model="bedForm.roomId" placeholder="选择所属房间" filterable style="width: 100%">
            <el-option
              v-for="r in allRoomsList"
              :key="r.id"
              :label="`${r.buildingName || ''} - ${r.roomNo}室`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="床位编号" prop="bedNo">
          <el-input v-model="bedForm.bedNo" placeholder="如 101-01, CARE-01" />
        </el-form-item>
        <el-form-item label="床位类型" prop="bedType">
          <el-select v-model="bedForm.bedType" placeholder="选择床位类型" style="width: 100%">
            <el-option label="普通标准床" value="STANDARD" />
            <el-option label="单人护理床" value="SINGLE" />
            <el-option label="电动翻身护理床" value="ELECTRIC_NURSING" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bedDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitCreateBed">确定</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 8: 入住单详情 -->
    <el-dialog v-model="detailVisible" title="入住详情信息" width="560px" destroy-on-close>
      <div v-if="detailRecord" class="detail-box">
        <div class="detail-row"><span class="k">入住单号:</span><span class="v font-mono">{{ detailRecord.admissionNo }}</span></div>
        <div class="detail-row"><span class="k">长者姓名:</span><span class="v">{{ detailRecord.elderName }} ({{ detailRecord.elderNo }})</span></div>
        <div class="detail-row"><span class="k">分配床位:</span><span class="v">{{ detailRecord.buildingName }} - {{ detailRecord.roomNo }}室 - {{ detailRecord.bedNo }}床</span></div>
        <div class="detail-row"><span class="k">合同编号:</span><span class="v">{{ detailRecord.contractNo || '--' }}</span></div>
        <div class="detail-row"><span class="k">护理等级:</span><span class="v">{{ formatCareLevel(detailRecord.careLevelCode) }}</span></div>
        <div class="detail-row"><span class="k">押金金额:</span><span class="v text-primary">¥{{ Number(detailRecord.depositAmount).toFixed(2) }}</span></div>
        <div class="detail-row"><span class="k">入住日期:</span><span class="v">{{ detailRecord.admissionDate }}</span></div>
        <div class="detail-row"><span class="k">计划退住:</span><span class="v">{{ detailRecord.plannedDischargeDate || '--' }}</span></div>
        <div class="detail-row"><span class="k">实际退住:</span><span class="v">{{ detailRecord.actualDischargeDate || '--' }}</span></div>
        <div class="detail-row"><span class="k">退住原因:</span><span class="v">{{ detailRecord.dischargeReason || '--' }}</span></div>
        <div class="detail-row"><span class="k">经办人:</span><span class="v">{{ detailRecord.operatorName || '--' }}</span></div>
        <div class="detail-row"><span class="k">办理时间:</span><span class="v">{{ detailRecord.createdAt }}</span></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import {
  House,
  User,
  Check,
  Warning,
  Plus,
  Refresh,
  Search
} from '@element-plus/icons-vue'
import {
  getBedBoardSummary,
  getSpaceTree,
  getBuildings,
  getRooms,
  getAvailableBeds,
  getAdmissions,
  checkIn,
  transferBed,
  precheckDischarge,
  discharge,
  getChangeLogs,
  updateBedStatus,
  createBuilding,
  createRoom,
  createBed,
  BedBoardSummaryVO,
  SpaceTreeVO,
  BuildingVO,
  RoomVO,
  BedVO,
  AdmissionVO,
  AdmissionQueryDTO,
  AdmissionPrecheckVO,
  BedChangeLogVO
} from '@/api/admission'
import { getElders, ElderListVO } from '@/api/elder'

const activeTab = ref('board')
const boardLoading = ref(false)
const admissionsLoading = ref(false)
const submitLoading = ref(false)
const logsLoading = ref(false)

// 看板汇总指标
const summary = reactive<BedBoardSummaryVO>({
  totalBeds: 0,
  occupiedBeds: 0,
  availableBeds: 0,
  maintenanceBeds: 0,
  disabledBeds: 0,
  occupancyRate: 0
})

// 空间树与楼栋列表
const spaceTree = ref<SpaceTreeVO[]>([])
const buildingList = ref<BuildingVO[]>([])
const allRoomsList = ref<RoomVO[]>([])
const availableBedsList = ref<BedVO[]>([])
const pendingElderList = ref<ElderListVO[]>([])

// 看板筛选条件
const boardFilter = reactive({
  buildingId: undefined as number | undefined,
  status: 'ALL'
})

// 根据过滤条件计算后的空间树
const filteredSpaceTree = computed(() => {
  return spaceTree.value
    .filter(bld => !boardFilter.buildingId || bld.id === boardFilter.buildingId)
    .map(bld => {
      const filteredRooms = bld.rooms.map(room => {
        const filteredBeds = room.beds.filter(bed => {
          if (boardFilter.status === 'ALL') return true
          return bed.status === boardFilter.status
        })
        return {
          ...room,
          beds: filteredBeds
        }
      }).filter(room => room.beds.length > 0 || boardFilter.status === 'ALL')

      return {
        ...bld,
        rooms: filteredRooms
      }
    }).filter(bld => bld.rooms.length > 0 || boardFilter.status === 'ALL')
})

// 入住台账查询
const admissionsQuery = reactive<AdmissionQueryDTO>({
  admissionNo: '',
  elderName: '',
  bedNo: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})
const admissionList = ref<AdmissionVO[]>([])
const admissionsTotal = ref(0)

// 弹窗状态
const checkInVisible = ref(false)
const transferVisible = ref(false)
const dischargeVisible = ref(false)
const changeLogsVisible = ref(false)
const buildingDialogVisible = ref(false)
const roomDialogVisible = ref(false)
const bedDialogVisible = ref(false)
const detailVisible = ref(false)

// 当前操作上下文对象
const currentTargetBed = ref<BedVO | null>(null)
const currentTargetAdmission = ref<AdmissionVO | null>(null)
const dischargePrecheck = ref<AdmissionPrecheckVO | null>(null)
const changeLogs = ref<BedChangeLogVO[]>([])
const detailRecord = ref<AdmissionVO | null>(null)

// 表单引用
const checkInFormRef = ref<FormInstance>()
const transferFormRef = ref<FormInstance>()
const dischargeFormRef = ref<FormInstance>()
const buildingFormRef = ref<FormInstance>()
const roomFormRef = ref<FormInstance>()
const bedFormRef = ref<FormInstance>()

// 表单数据
const checkInForm = reactive({
  elderId: undefined as number | undefined,
  bedId: undefined as number | undefined,
  admissionDate: new Date().toISOString().split('T')[0],
  plannedDischargeDate: '',
  careLevelCode: 'CARE_LEVEL_2',
  depositAmount: 5000,
  contractNo: '',
  remark: ''
})

const transferForm = reactive({
  newBedId: undefined as number | undefined,
  reason: ''
})

const dischargeForm = reactive({
  actualDischargeDate: new Date().toISOString().split('T')[0],
  dischargeReason: '',
  forceDischarge: false
})

const buildingForm = reactive({
  buildingCode: '',
  buildingName: '',
  address: ''
})

const roomForm = reactive({
  buildingId: undefined as number | undefined,
  floorNo: '1F',
  roomNo: '',
  roomType: 'DOUBLE'
})

const bedForm = reactive({
  roomId: undefined as number | undefined,
  bedNo: '',
  bedType: 'STANDARD'
})

// 校验规则
const checkInRules = {
  elderId: [{ required: true, message: '请选择长者', trigger: 'change' }],
  bedId: [{ required: true, message: '请选择目标床位', trigger: 'change' }],
  admissionDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }]
}

const transferRules = {
  newBedId: [{ required: true, message: '请选择目标新床位', trigger: 'change' }],
  reason: [{ required: true, message: '请填写调床原因', trigger: 'blur' }]
}

const dischargeRules = {
  actualDischargeDate: [{ required: true, message: '请选择实际退住日期', trigger: 'change' }]
}

const buildingRules = {
  buildingCode: [{ required: true, message: '请输入楼栋编码', trigger: 'blur' }],
  buildingName: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }]
}

const roomRules = {
  buildingId: [{ required: true, message: '请选择所属楼栋', trigger: 'change' }],
  roomNo: [{ required: true, message: '请输入房间编号', trigger: 'blur' }]
}

const bedRules = {
  roomId: [{ required: true, message: '请选择所属房间', trigger: 'change' }],
  bedNo: [{ required: true, message: '请输入床位编号', trigger: 'blur' }]
}

// 格式化与标签工具函数
function formatRoomType(type?: string) {
  switch (type) {
    case 'SINGLE': return '单人间'
    case 'DOUBLE': return '双人间'
    case 'SUITE': return '套间'
    case 'CARE_ROOM': return '护理房'
    default: return '普通间'
  }
}

function getRoomTypeTag(type?: string) {
  switch (type) {
    case 'SINGLE': return 'success'
    case 'DOUBLE': return 'primary'
    case 'SUITE': return 'warning'
    case 'CARE_ROOM': return 'danger'
    default: return 'info'
  }
}

function formatBedStatus(status: string) {
  switch (status) {
    case 'AVAILABLE': return '空闲'
    case 'OCCUPIED': return '在住'
    case 'MAINTENANCE': return '维修'
    case 'DISABLED': return '停用'
    default: return status
  }
}

function getBedStatusTag(status: string) {
  switch (status) {
    case 'AVAILABLE': return 'success'
    case 'OCCUPIED': return 'primary'
    case 'MAINTENANCE': return 'warning'
    case 'DISABLED': return 'info'
    default: return 'info'
  }
}

function formatCareLevel(code?: string) {
  switch (code) {
    case 'CARE_LEVEL_1': return '一级护理'
    case 'CARE_LEVEL_2': return '二级护理'
    case 'CARE_LEVEL_3': return '三级护理'
    case 'SPECIAL_CARE': return '特级护理'
    case 'DEMENTIA_CARE': return '认知症专护'
    default: return code || '--'
  }
}

function formatChangeType(type: string) {
  switch (type) {
    case 'CHECK_IN': return '办理入住'
    case 'TRANSFER': return '调整床位'
    case 'DISCHARGE': return '办理退住'
    default: return type
  }
}

function getChangeLogType(type: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  switch (type) {
    case 'CHECK_IN': return 'primary'
    case 'TRANSFER': return 'warning'
    case 'DISCHARGE': return 'success'
    default: return 'info'
  }
}

// 加载看板汇总数据
async function loadSummary() {
  try {
    const res = await getBedBoardSummary()
    if (res.data) {
      Object.assign(summary, res.data)
    }
  } catch (err: any) {
    console.error('加载床位看板统计失败', err)
  }
}

// 加载空间树
async function loadSpaceTree() {
  boardLoading.value = true
  try {
    const res = await getSpaceTree()
    spaceTree.value = res.data || []
  } catch (err: any) {
    ElMessage.error(err.message || '加载空间数据失败')
  } finally {
    boardLoading.value = false
  }
}

// 加载楼栋及房间
async function loadBuildingsAndRooms() {
  try {
    const [bldRes, roomRes] = await Promise.all([getBuildings(), getRooms()])
    buildingList.value = bldRes.data || []
    allRoomsList.value = roomRes.data || []
  } catch (err: any) {
    console.error('加载楼栋房间字典失败', err)
  }
}

// 加载可用空闲床位列表
async function loadAvailableBeds() {
  try {
    const res = await getAvailableBeds()
    availableBedsList.value = res.data || []
  } catch (err: any) {
    console.error('加载空闲床位失败', err)
  }
}

// 加载待入住长者候选人
async function loadPendingElders() {
  try {
    const res = await getElders({ status: 'PENDING', pageSize: 100 })
    pendingElderList.value = res.data.list || []
  } catch (err: any) {
    console.error('加载待入住长者候选失败', err)
  }
}

// 加载入住记录台账
async function loadAdmissions() {
  admissionsLoading.value = true
  try {
    const res = await getAdmissions(admissionsQuery)
    admissionList.value = res.data.list || []
    admissionsTotal.value = res.data.total || 0
  } catch (err: any) {
    ElMessage.error(err.message || '加载入住记录失败')
  } finally {
    admissionsLoading.value = false
  }
}

function resetAdmissionsQuery() {
  admissionsQuery.admissionNo = ''
  admissionsQuery.elderName = ''
  admissionsQuery.bedNo = ''
  admissionsQuery.status = ''
  admissionsQuery.pageNum = 1
  loadAdmissions()
}

function handleTabChange(tab: any) {
  if (tab === 'records') {
    loadAdmissions()
  } else if (tab === 'spaces') {
    loadBuildingsAndRooms()
  } else {
    loadSpaceTree()
    loadSummary()
  }
}

// 办理入住操作
async function openCheckInDialog(preselectedBed?: BedVO) {
  await Promise.all([loadAvailableBeds(), loadPendingElders()])
  checkInForm.elderId = undefined
  checkInForm.bedId = preselectedBed ? preselectedBed.id : undefined
  checkInForm.admissionDate = new Date().toISOString().split('T')[0]
  checkInForm.plannedDischargeDate = ''
  checkInForm.depositAmount = 5000
  checkInForm.contractNo = ''
  checkInForm.remark = ''
  checkInVisible.value = true
}

async function submitCheckIn() {
  if (!checkInFormRef.value) return
  await checkInFormRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      await checkIn({
        elderId: checkInForm.elderId!,
        bedId: checkInForm.bedId!,
        admissionDate: checkInForm.admissionDate,
        plannedDischargeDate: checkInForm.plannedDischargeDate || undefined,
        careLevelCode: checkInForm.careLevelCode,
        depositAmount: checkInForm.depositAmount,
        contractNo: checkInForm.contractNo || undefined,
        remark: checkInForm.remark || undefined
      })
      ElMessage.success('长者入住办理成功，床位已原子锁定占用')
      checkInVisible.value = false
      loadSummary()
      loadSpaceTree()
      if (activeTab.value === 'records') loadAdmissions()
    } catch (err: any) {
      ElMessage.error(err.message || '办理入住失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 调床操作
async function openTransferDialog(bed: BedVO) {
  currentTargetBed.value = bed
  currentTargetAdmission.value = null
  await loadAvailableBeds()
  transferForm.newBedId = undefined
  transferForm.reason = ''
  transferVisible.value = true
}

async function openTransferDialogByRow(row: AdmissionVO) {
  currentTargetAdmission.value = row
  currentTargetBed.value = null
  await loadAvailableBeds()
  transferForm.newBedId = undefined
  transferForm.reason = ''
  transferVisible.value = true
}

async function submitTransfer() {
  if (!transferFormRef.value) return
  await transferFormRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    const admissionId = currentTargetBed.value?.currentAdmissionId || currentTargetAdmission.value?.id
    if (!admissionId) {
      ElMessage.error('无法获取有效入住单ID')
      submitLoading.value = false
      return
    }

    try {
      await transferBed(admissionId, {
        newBedId: transferForm.newBedId!,
        reason: transferForm.reason
      })
      ElMessage.success('长者调床办理成功，旧床位已释放，新床位已占用')
      transferVisible.value = false
      loadSummary()
      loadSpaceTree()
      if (activeTab.value === 'records') loadAdmissions()
    } catch (err: any) {
      ElMessage.error(err.message || '办理调床失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 退住操作
async function openDischargeDialog(bed: BedVO) {
  currentTargetBed.value = bed
  currentTargetAdmission.value = null
  const admissionId = bed.currentAdmissionId!
  try {
    const precheckRes = await precheckDischarge(admissionId)
    dischargePrecheck.value = precheckRes.data
  } catch (err: any) {
    dischargePrecheck.value = null
  }
  dischargeForm.actualDischargeDate = new Date().toISOString().split('T')[0]
  dischargeForm.dischargeReason = ''
  dischargeForm.forceDischarge = false
  dischargeVisible.value = true
}

async function openDischargeDialogByRow(row: AdmissionVO) {
  currentTargetAdmission.value = row
  currentTargetBed.value = null
  try {
    const precheckRes = await precheckDischarge(row.id)
    dischargePrecheck.value = precheckRes.data
  } catch (err: any) {
    dischargePrecheck.value = null
  }
  dischargeForm.actualDischargeDate = new Date().toISOString().split('T')[0]
  dischargeForm.dischargeReason = ''
  dischargeForm.forceDischarge = false
  dischargeVisible.value = true
}

async function submitDischarge() {
  if (!dischargeFormRef.value) return
  await dischargeFormRef.value.validate(async valid => {
    if (!valid) return
    const admissionId = currentTargetBed.value?.currentAdmissionId || currentTargetAdmission.value?.id
    if (!admissionId) return

    submitLoading.value = true
    try {
      await discharge(admissionId, {
        actualDischargeDate: dischargeForm.actualDischargeDate,
        dischargeReason: dischargeForm.dischargeReason,
        forceDischarge: dischargeForm.forceDischarge
      })
      ElMessage.success('长者退住办理完成，床位已恢复空闲，长者档案已更新')
      dischargeVisible.value = false
      loadSummary()
      loadSpaceTree()
      if (activeTab.value === 'records') loadAdmissions()
    } catch (err: any) {
      ElMessage.error(err.message || '办理退住失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 变更床位状态（维修/可用）
async function handleSetBedStatus(bedId: number, status: string) {
  const actionText = status === 'MAINTENANCE' ? '设为维修中' : '恢复为可用'
  try {
    await ElMessageBox.confirm(`确定要将该床位${actionText}吗？`, '状态变更确认', {
      type: 'warning'
    })
    await updateBedStatus(bedId, status)
    ElMessage.success(`床位已${actionText}`)
    loadSummary()
    loadSpaceTree()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '修改床位状态失败')
    }
  }
}

// 查看床位流转日志
async function openChangeLogsDialog(admissionId: number) {
  changeLogsVisible.value = true
  logsLoading.value = true
  try {
    const res = await getChangeLogs(admissionId)
    changeLogs.value = res.data || []
  } catch (err: any) {
    ElMessage.error(err.message || '加载变动日志失败')
  } finally {
    logsLoading.value = false
  }
}

// 查看入住详情
function viewAdmissionDetail(row: AdmissionVO) {
  detailRecord.value = row
  detailVisible.value = true
}

// 空间资源维护新增弹窗
function openBuildingDialog() {
  buildingForm.buildingCode = ''
  buildingForm.buildingName = ''
  buildingForm.address = ''
  buildingDialogVisible.value = true
}

async function submitCreateBuilding() {
  if (!buildingFormRef.value) return
  await buildingFormRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      await createBuilding(buildingForm)
      ElMessage.success('新增楼栋成功')
      buildingDialogVisible.value = false
      loadBuildingsAndRooms()
    } catch (err: any) {
      ElMessage.error(err.message || '新增楼栋失败')
    } finally {
      submitLoading.value = false
    }
  })
}

function openRoomDialog() {
  roomForm.buildingId = buildingList.value[0]?.id
  roomForm.floorNo = '1F'
  roomForm.roomNo = ''
  roomForm.roomType = 'DOUBLE'
  roomDialogVisible.value = true
}

async function submitCreateRoom() {
  if (!roomFormRef.value) return
  await roomFormRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      await createRoom({
        buildingId: roomForm.buildingId!,
        floorNo: roomForm.floorNo,
        roomNo: roomForm.roomNo,
        roomType: roomForm.roomType
      })
      ElMessage.success('新增房间成功')
      roomDialogVisible.value = false
      loadBuildingsAndRooms()
    } catch (err: any) {
      ElMessage.error(err.message || '新增房间失败')
    } finally {
      submitLoading.value = false
    }
  })
}

function openBedDialog() {
  bedForm.roomId = allRoomsList.value[0]?.id
  bedForm.bedNo = ''
  bedForm.bedType = 'STANDARD'
  bedDialogVisible.value = true
}

async function submitCreateBed() {
  if (!bedFormRef.value) return
  await bedFormRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      await createBed({
        roomId: bedForm.roomId!,
        bedNo: bedForm.bedNo,
        bedType: bedForm.bedType
      })
      ElMessage.success('新增床位成功')
      bedDialogVisible.value = false
      loadSummary()
      loadBuildingsAndRooms()
    } catch (err: any) {
      ElMessage.error(err.message || '新增床位失败')
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => {
  loadSummary()
  loadSpaceTree()
  loadBuildingsAndRooms()
})
</script>

<style scoped>
.admission-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* KPI 指标卡片 */
.kpi-row {
  margin-bottom: 4px;
}

.kpi-card {
  display: flex;
  align-items: center;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.kpi-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  width: 100%;
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.kpi-card.total .kpi-icon {
  background: #f1f5f9;
  color: #475569;
}

.kpi-card.occupied .kpi-icon {
  background: #eff6ff;
  color: #2563eb;
}

.kpi-card.available .kpi-icon {
  background: #ecfdf5;
  color: #059669;
}

.kpi-card.maintenance .kpi-icon {
  background: #fffbeb;
  color: #d97706;
}

.kpi-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-title {
  font-size: 13px;
  color: #64748b;
}

.kpi-num {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.kpi-num-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.success-num {
  color: #059669;
}

.warning-num {
  color: #d97706;
}

.rate-tag {
  font-weight: 600;
}

/* 主卡片与 Tabs */
.main-card {
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.board-toolbar,
.space-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 楼栋看板容器 */
.buildings-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.building-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

.building-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf2f7;
}

.bld-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bld-title .name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.bld-stats {
  font-size: 13px;
  color: #64748b;
}

.bld-stats .split {
  margin: 0 8px;
  color: #cbd5e1;
}

.success-text {
  color: #059669;
  font-weight: 600;
}

.primary-text {
  color: #2563eb;
  font-weight: 600;
}

/* 房间卡片与床位列表 */
.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.room-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e2e8f0;
}

.room-no {
  display: flex;
  align-items: center;
  gap: 6px;
}

.room-no .floor {
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
}

.room-no .no {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.beds-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bed-item {
  border-radius: 6px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
}

.bed-item.available {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.bed-item.occupied {
  background: #f0f7ff;
  border-color: #bfdbfe;
}

.bed-item.maintenance,
.bed-item.disabled {
  background: #fffbeb;
  border-color: #fde68a;
}

.bed-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.bed-no {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.elder-occupant {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #3b82f6;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
}

.occupant-info {
  display: flex;
  flex-direction: column;
}

.occupant-info .name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.occupant-info .no {
  font-size: 11px;
  color: #64748b;
  font-family: monospace;
}

.empty-bed,
.maint-bed {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.bed-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 6px;
  border-top: 1px dashed rgba(0, 0, 0, 0.06);
}

/* 入住登记台账 */
.records-filter {
  margin-bottom: 12px;
}

.elder-cell {
  display: flex;
  flex-direction: column;
}

.elder-cell .name {
  font-weight: 600;
  color: #0f172a;
}

.elder-cell .no {
  font-size: 11px;
  color: #64748b;
  font-family: monospace;
}

.deposit-amount {
  font-family: monospace;
  font-weight: 600;
  color: #059669;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 调床信息卡 */
.transfer-info-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.transfer-info-box .row {
  display: flex;
  gap: 12px;
  font-size: 13px;
}

.transfer-info-box .label {
  color: #64748b;
  width: 70px;
}

/* 时间线 */
.timeline-container {
  max-height: 480px;
  overflow-y: auto;
  padding: 10px 12px;
}

.log-card {
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.log-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.log-title .operator {
  font-size: 12px;
  color: #64748b;
}

.log-detail {
  font-size: 13px;
  color: #1e293b;
  margin-bottom: 4px;
}

.log-reason {
  font-size: 12px;
  color: #64748b;
  font-style: italic;
}

/* 详情卡片 */
.detail-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: flex;
  font-size: 13px;
  line-height: 1.6;
}

.detail-row .k {
  width: 100px;
  color: #64748b;
}

.detail-row .v {
  flex: 1;
  color: #1e293b;
}
</style>
