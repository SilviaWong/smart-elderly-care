<template>
  <div class="care-page">
    <!-- 顶部今日护理任务指标看板 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card total">
          <div class="kpi-icon"><el-icon><Calendar /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">今日护理任务</span>
            <span class="kpi-num">{{ summary.totalTasks }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card pending">
          <div class="kpi-icon"><el-icon><Clock /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待执行</span>
            <span class="kpi-num warning-num">{{ summary.pendingCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card in-progress">
          <div class="kpi-icon"><el-icon><Loading /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">执行中</span>
            <span class="kpi-num primary-num">{{ summary.inProgressCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card completed">
          <div class="kpi-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">已完成</span>
            <span class="kpi-num success-num">{{ summary.completedCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card missed">
          <div class="kpi-icon"><el-icon><WarningFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">漏执行</span>
            <span class="kpi-num danger-num">{{ summary.missedCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" :lg="4">
        <el-card shadow="never" class="kpi-card cancelled">
          <div class="kpi-icon"><el-icon><RemoveFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">已取消</span>
            <span class="kpi-num info-num">{{ summary.cancelledCount }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主体卡片 Tabs -->
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- Tab 1: 护理任务调度与执行看板 -->
        <el-tab-pane label="护理任务调度与执行" name="tasks">
          <div class="tab-toolbar">
            <el-form :inline="true" :model="taskQuery" class="filter-form">
              <el-form-item label="任务日期">
                <el-date-picker
                  v-model="taskQuery.taskDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                  style="width: 140px"
                  @change="fetchTasks"
                />
              </el-form-item>
              <el-form-item label="长者姓名">
                <el-input v-model="taskQuery.elderName" placeholder="姓名搜索" clearable style="width: 130px" />
              </el-form-item>
              <el-form-item label="任务状态">
                <el-select v-model="taskQuery.status" placeholder="全部状态" clearable style="width: 130px">
                  <el-option label="待执行 (PENDING)" value="PENDING" />
                  <el-option label="执行中 (IN_PROGRESS)" value="IN_PROGRESS" />
                  <el-option label="已完成 (COMPLETED)" value="COMPLETED" />
                  <el-option label="漏执行 (MISSED)" value="MISSED" />
                  <el-option label="已取消 (CANCELLED)" value="CANCELLED" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="fetchTasks">查询</el-button>
                <el-button :icon="Refresh" @click="resetTaskQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <div class="action-btn-group">
              <el-button type="success" :icon="Timer" @click="openGenerateDialog">按计划生成护理任务</el-button>
            </div>
          </div>

          <!-- 任务表格 -->
          <el-table :data="taskList" v-loading="taskLoading" stripe border style="width: 100%">
            <el-table-column prop="taskNo" label="任务编号" width="160" />
            <el-table-column prop="scheduledAt" label="计划执行时间" width="165" />
            <el-table-column label="服务长者 / 床位" min-width="160">
              <template #default="{ row }">
                <div class="elder-cell">
                  <span class="elder-name">{{ row.elderName }}</span>
                  <span class="bed-badge" v-if="row.bedInfo">{{ row.bedInfo }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="护理项目" min-width="160">
              <template #default="{ row }">
                <span class="care-item-title">{{ row.careItemName }}</span>
                <el-tag size="small" type="info" class="category-tag">{{ formatItemCategory(row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="责任护理员" width="120">
              <template #default="{ row }">
                <span>{{ row.assigneeName || '未指定' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getTaskStatusTagType(row.status)" effect="dark">
                  {{ formatTaskStatus(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作与闭环" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <!-- 指派 -->
                <el-button
                  v-if="row.status === 'PENDING'"
                  link
                  type="primary"
                  size="small"
                  @click="openAssignDialog(row)"
                >
                  指派
                </el-button>

                <!-- 开始执行 -->
                <el-button
                  v-if="row.status === 'PENDING'"
                  link
                  type="success"
                  size="small"
                  @click="handleTaskAction(row, 'START')"
                >
                  开始
                </el-button>

                <!-- 完成执行并留痕 -->
                <el-button
                  v-if="row.status === 'PENDING' || row.status === 'IN_PROGRESS'"
                  link
                  type="primary"
                  size="small"
                  @click="openCompleteDialog(row)"
                >
                  完成填报
                </el-button>

                <!-- 标记漏执行 -->
                <el-button
                  v-if="row.status === 'PENDING' || row.status === 'IN_PROGRESS'"
                  link
                  type="warning"
                  size="small"
                  @click="openMissPrompt(row)"
                >
                  漏执行
                </el-button>

                <!-- 查看执行记录 -->
                <el-button link type="info" size="small" @click="viewRecords(row)">记录</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="taskQuery.pageNum"
              v-model:page-size="taskQuery.pageSize"
              :total="taskTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchTasks"
              @current-change="fetchTasks"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 2: 长者护理计划管理 -->
        <el-tab-pane label="长者护理计划管理" name="plans">
          <div class="tab-toolbar">
            <el-form :inline="true" :model="planQuery" class="filter-form">
              <el-form-item label="计划单号">
                <el-input v-model="planQuery.planNo" placeholder="CP单号" clearable style="width: 150px" />
              </el-form-item>
              <el-form-item label="长者姓名">
                <el-input v-model="planQuery.elderName" placeholder="姓名搜索" clearable style="width: 130px" />
              </el-form-item>
              <el-form-item label="护理等级">
                <el-select v-model="planQuery.careLevelId" placeholder="全部等级" clearable style="width: 140px">
                  <el-option v-for="lvl in levelList" :key="lvl.id" :label="lvl.levelName" :value="lvl.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="planQuery.status" placeholder="全部" clearable style="width: 110px">
                  <el-option label="执行中 (ACTIVE)" value="ACTIVE" />
                  <el-option label="已暂停 (STOPPED)" value="STOPPED" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="fetchPlans">查询</el-button>
                <el-button :icon="Refresh" @click="resetPlanQuery">重置</el-button>
              </el-form-item>
            </el-form>
            <el-button type="primary" :icon="Plus" @click="openCreatePlanDialog">制定长者护理计划</el-button>
          </div>

          <!-- 计划表格 -->
          <el-table :data="planList" v-loading="planLoading" stripe border style="width: 100%">
            <el-table-column prop="planNo" label="计划编号" width="165" />
            <el-table-column label="长者姓名 / 床位" min-width="160">
              <template #default="{ row }">
                <div class="elder-cell">
                  <span class="elder-name">{{ row.elderName }}</span>
                  <span class="bed-badge" v-if="row.bedInfo">{{ row.bedInfo }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="careLevelName" label="护理等级" width="140">
              <template #default="{ row }">
                <el-tag size="small" type="warning" effect="plain">{{ row.careLevelName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="有效期起止" width="210">
              <template #default="{ row }">
                <span>{{ row.startDate }} ~ {{ row.endDate || '长期有效' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="itemCount" label="包含项目数" width="110" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.itemCount }} 项明细</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 'ACTIVE'"
                  @change="(val: boolean) => handleTogglePlan(row, val ? 'ACTIVE' : 'STOPPED')"
                />
              </template>
            </el-table-column>
            <el-table-column prop="creatorName" label="制定主管" width="110" />
            <el-table-column label="操作" width="160" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewPlanDetail(row)">明细详情</el-button>
                <el-button link type="success" size="small" @click="triggerPlanTask(row)">立即排班</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="planQuery.pageNum"
              v-model:page-size="planQuery.pageSize"
              :total="planTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchPlans"
              @current-change="fetchPlans"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 3: 护理等级与项目字典配置 -->
        <el-tab-pane label="护理分级与标准化项目配置" name="config">
          <el-row :gutter="24">
            <!-- 左侧：护理分级 -->
            <el-col :span="10">
              <div class="config-header">
                <span class="config-title">护理分级标准</span>
                <el-button type="primary" size="small" :icon="Plus" @click="openLevelDialog()">新增等级</el-button>
              </div>
              <el-table :data="levelList" border stripe size="small">
                <el-table-column prop="levelCode" label="编码" width="110" />
                <el-table-column prop="levelName" label="等级名称" width="130" />
                <el-table-column prop="description" label="适用标准" show-overflow-tooltip />
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-switch
                      size="small"
                      :model-value="row.status === 'ACTIVE'"
                      @change="(val: boolean) => handleToggleLevel(row, val ? 'ACTIVE' : 'INACTIVE')"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="70" align="center">
                  <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="openLevelDialog(row)">编辑</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-col>

            <!-- 右侧：护理项目 -->
            <el-col :span="14">
              <div class="config-header">
                <span class="config-title">标准化护理项目字典</span>
                <el-button type="primary" size="small" :icon="Plus" @click="openCareItemDialog()">新增项目</el-button>
              </div>
              <el-table :data="careItemList" border stripe size="small">
                <el-table-column prop="itemCode" label="项目代码" width="90" />
                <el-table-column prop="itemName" label="护理项目名称" width="150" />
                <el-table-column label="分类" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag size="small">{{ formatItemCategory(row.category) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="项目规范" show-overflow-tooltip />
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-switch
                      size="small"
                      :model-value="row.status === 'ACTIVE'"
                      @change="(val: boolean) => handleToggleCareItem(row, val ? 'ACTIVE' : 'INACTIVE')"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="70" align="center">
                  <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="openCareItemDialog(row)">编辑</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 弹窗 1: 一键批量生成护理任务 -->
    <el-dialog v-model="generateDialogVisible" title="按计划批量生成护理任务" width="460px" destroy-on-close>
      <el-form :model="generateForm" label-width="110px">
        <el-form-item label="排班任务日期" required>
          <el-date-picker
            v-model="generateForm.taskDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择目标任务日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="指定长者">
          <el-select v-model="generateForm.elderId" clearable filterable placeholder="全部有效长者计划" style="width: 100%">
            <el-option v-for="e in elderList" :key="e.id" :label="`${e.name} (${e.elderNo})`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon class="dialog-alert">
          系统具备严格<strong>任务幂等性校验</strong>，同一计划项在同一日期/执行窗口只生成一条任务，重复生成不会产生冗余任务。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="dialogSubmitting" @click="submitGenerate">开始生成任务</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2: 指派任务责任人 -->
    <el-dialog v-model="assignDialogVisible" title="指派护理任务责任人" width="440px" destroy-on-close>
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="任务编号">
          <span>{{ currentTask?.taskNo }}</span>
        </el-form-item>
        <el-form-item label="护理项目">
          <span>{{ currentTask?.elderName }} - {{ currentTask?.careItemName }}</span>
        </el-form-item>
        <el-form-item label="责任护理员" required>
          <el-select v-model="assignForm.assigneeId" placeholder="选择护理人员" style="width: 100%">
            <el-option v-for="u in staffList" :key="u.id" :label="`${u.realName} (${u.phone || '员工'})`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitAssign">确认指派</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 3: 任务完成填报（不可覆盖留痕） -->
    <el-dialog v-model="completeDialogVisible" title="完成护理任务与执行留痕" width="540px" destroy-on-close>
      <el-form :model="completeForm" label-width="100px">
        <el-form-item label="任务编号">
          <span>{{ currentTask?.taskNo }}</span>
        </el-form-item>
        <el-form-item label="执行结果" required>
          <el-radio-group v-model="completeForm.resultCode">
            <el-radio-button label="NORMAL">正常完成</el-radio-button>
            <el-radio-button label="ABNORMAL">异常反馈</el-radio-button>
            <el-radio-button label="REFUSED">长者拒绝</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="执行说明/反馈" required>
          <el-input
            v-model="completeForm.resultDesc"
            type="textarea"
            :rows="4"
            placeholder="请详述执行过程、长者体感状态、配合度、异常观察等..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="dialogSubmitting" @click="submitComplete">提交完成记录</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 4: 制定护理计划 -->
    <el-dialog v-model="createPlanDialogVisible" title="制定长者个性化护理计划" width="750px" destroy-on-close>
      <el-form ref="planFormRef" :model="planForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务长者" required>
              <el-select v-model="planForm.elderId" filterable placeholder="选择长者" style="width: 100%">
                <el-option v-for="e in elderList" :key="e.id" :label="`${e.name} (${e.elderNo})`" :value="e.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="护理等级" required>
              <el-select v-model="planForm.careLevelId" placeholder="选择护理等级" style="width: 100%">
                <el-option v-for="lvl in levelList" :key="lvl.id" :label="lvl.levelName" :value="lvl.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效起始日" required>
              <el-date-picker v-model="planForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期">
              <el-date-picker v-model="planForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="留空为长期有效" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="计划备注说明">
              <el-input v-model="planForm.remark" placeholder="如：家属交代防烫伤、晨起先服药等" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="items-header">
          <span class="items-title">计划明细项目配置 ({{ planForm.items.length }} 项)</span>
          <el-button type="primary" size="small" :icon="Plus" @click="addPlanItemRow">添加项目</el-button>
        </div>

        <el-table :data="planForm.items" border size="small" class="plan-items-table">
          <el-table-column label="护理项目" min-width="160">
            <template #default="{ row }">
              <el-select v-model="row.careItemId" placeholder="选择项目" size="small" style="width: 100%">
                <el-option v-for="ci in careItemList" :key="ci.id" :label="ci.itemName" :value="ci.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="频次类型" width="120">
            <template #default="{ row }">
              <el-select v-model="row.frequencyType" size="small">
                <el-option label="每日 (DAILY)" value="DAILY" />
                <el-option label="每周 (WEEKLY)" value="WEEKLY" />
                <el-option label="隔天 (INTERVAL)" value="INTERVAL_DAYS" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="执行时间窗口" width="180">
            <template #default="{ row }">
              <div class="time-range-cell">
                <el-time-picker v-model="row.timeWindowStart" placeholder="开始" value-format="HH:mm:ss" size="small" style="width: 85px" />
                <span>-</span>
                <el-time-picker v-model="row.timeWindowEnd" placeholder="结束" value-format="HH:mm:ss" size="small" style="width: 85px" />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="指定责任人" width="130">
            <template #default="{ row }">
              <el-select v-model="row.assigneeId" clearable placeholder="责任护理员" size="small" style="width: 100%">
                <el-option v-for="u in staffList" :key="u.id" :label="u.realName" :value="u.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removePlanItemRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="createPlanDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitCreatePlan">保存计划并生效</el-button>
      </template>
    </el-dialog>

    <!-- 抽屉: 计划全息明细 -->
    <el-drawer v-model="planDrawerVisible" title="长者护理计划全息明细" size="520px" destroy-on-close>
      <div v-if="currentPlanDetail" class="plan-detail-box">
        <div class="plan-header-card">
          <div class="plan-no-text">{{ currentPlanDetail.plan.planNo }}</div>
          <div class="plan-elder-info">
            {{ currentPlanDetail.plan.elderName }} ({{ currentPlanDetail.plan.bedInfo || '未排床' }})
          </div>
          <div class="plan-level-badge">
            <el-tag type="warning">{{ currentPlanDetail.plan.careLevelName }}</el-tag>
            <span class="plan-date-range">
              {{ currentPlanDetail.plan.startDate }} ~ {{ currentPlanDetail.plan.endDate || '长期' }}
            </span>
          </div>
        </div>

        <div class="detail-section-title">定时护理排班明细表</div>
        <el-table :data="currentPlanDetail.items" border size="small">
          <el-table-column prop="itemName" label="护理项目" />
          <el-table-column prop="frequencyType" label="频次" width="100">
            <template #default="{ row }">
              <span>{{ row.frequencyType === 'DAILY' ? '每日' : row.frequencyType }}</span>
            </template>
          </el-table-column>
          <el-table-column label="执行窗口" width="130">
            <template #default="{ row }">
              <span>{{ row.timeWindowStart || '08:00' }} - {{ row.timeWindowEnd || '09:00' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="assigneeName" label="责任人" width="100" />
        </el-table>
      </div>
    </el-drawer>

    <!-- 抽屉: 任务执行审计记录 -->
    <el-drawer v-model="recordsDrawerVisible" title="任务执行记录与结果留痕" size="460px" destroy-on-close>
      <div class="drawer-header-info" v-if="currentTask">
        <div class="task-no-text">{{ currentTask.taskNo }}</div>
        <div>{{ currentTask.elderName }} - {{ currentTask.careItemName }}</div>
      </div>
      <el-timeline v-loading="recordsLoading">
        <el-timeline-item
          v-for="rec in taskRecords"
          :key="rec.id"
          :timestamp="rec.executedAt"
          placement="top"
          :type="rec.resultCode === 'NORMAL' ? 'success' : 'danger'"
        >
          <el-card shadow="never" class="log-card">
            <div class="log-title">
              <el-tag size="small" :type="rec.resultCode === 'NORMAL' ? 'success' : 'danger'">
                {{ rec.resultCode === 'NORMAL' ? '正常完成' : rec.resultCode }}
              </el-tag>
              <span class="operator-name">执行人: {{ rec.executorName }}</span>
            </div>
            <div class="log-desc">{{ rec.resultDesc }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>

    <!-- 弹窗 5: 护理等级新增/编辑 -->
    <el-dialog v-model="levelDialogVisible" :title="levelForm.id ? '编辑护理等级' : '新增护理等级'" width="460px" destroy-on-close>
      <el-form :model="levelForm" label-width="100px">
        <el-form-item label="等级代码" required>
          <el-input v-model="levelForm.levelCode" placeholder="如：LEVEL_1" :disabled="!!levelForm.id" />
        </el-form-item>
        <el-form-item label="等级名称" required>
          <el-input v-model="levelForm.levelName" placeholder="如：一级全护理" />
        </el-form-item>
        <el-form-item label="适用标准说明">
          <el-input v-model="levelForm.description" type="textarea" :rows="3" placeholder="适用长者生理功能与照料要求..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitLevel">保存等级</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 6: 护理项目新增/编辑 -->
    <el-dialog v-model="careItemDialogVisible" :title="careItemForm.id ? '编辑护理项目' : '新增护理项目'" width="480px" destroy-on-close>
      <el-form :model="careItemForm" label-width="100px">
        <el-form-item label="项目代码" required>
          <el-input v-model="careItemForm.itemCode" placeholder="如：CI_001" :disabled="!!careItemForm.id" />
        </el-form-item>
        <el-form-item label="项目名称" required>
          <el-input v-model="careItemForm.itemName" placeholder="如：洗漱与口腔清洁" />
        </el-form-item>
        <el-form-item label="服务类别">
          <el-select v-model="careItemForm.category" style="width: 100%">
            <el-option label="生活照料 (DAILY_CARE)" value="DAILY_CARE" />
            <el-option label="躯体护理 (BODY_NURSING)" value="BODY_NURSING" />
            <el-option label="健康医疗 (HEALTH_CARE)" value="HEALTH_CARE" />
            <el-option label="康复辅助 (REHAB_CARE)" value="REHAB_CARE" />
            <el-option label="环境消杀 (ENV_CLEAN)" value="ENV_CLEAN" />
          </el-select>
        </el-form-item>
        <el-form-item label="护理规范说明">
          <el-input v-model="careItemForm.description" type="textarea" :rows="3" placeholder="操作标准、禁忌及防跌防烫要点..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="careItemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitCareItem">保存项目</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import {
  Calendar,
  Clock,
  Loading,
  CircleCheckFilled,
  WarningFilled,
  RemoveFilled,
  Search,
  Refresh,
  Plus,
  Timer
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCareTaskSummary,
  getCareTaskPage,
  generateCareTasks,
  assignCareTask,
  executeCareTaskAction,
  getCareTaskRecords,
  getCarePlanPage,
  getCarePlanDetail,
  createCarePlan,
  toggleCarePlanStatus,
  getCareLevels,
  createCareLevel,
  updateCareLevel,
  toggleCareLevelStatus,
  getCareItems,
  createCareItem,
  updateCareItem,
  toggleCareItemStatus,
  CareTaskVO,
  CareTaskSummaryVO,
  CarePlanVO,
  CarePlanDetailVO,
  CareLevelVO,
  CareItemVO,
  CareTaskRecordVO
} from '@/api/care'
import { getElderList } from '@/api/elder'
import { getUserList } from '@/api/system'

const activeTab = ref('tasks')

// 今日汇总看板
const summary = reactive<CareTaskSummaryVO>({
  totalTasks: 0,
  pendingCount: 0,
  inProgressCount: 0,
  completedCount: 0,
  missedCount: 0,
  cancelledCount: 0
})

// Tab 1: 任务
const taskLoading = ref(false)
const taskList = ref<CareTaskVO[]>([])
const taskTotal = ref(0)
const taskQuery = reactive({
  taskDate: getTodayDate(),
  elderName: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

// Tab 2: 计划
const planLoading = ref(false)
const planList = ref<CarePlanVO[]>([])
const planTotal = ref(0)
const planQuery = reactive({
  planNo: '',
  elderName: '',
  careLevelId: undefined as number | undefined,
  status: '',
  pageNum: 1,
  pageSize: 10
})

// Tab 3: 配置数据
const levelList = ref<CareLevelVO[]>([])
const careItemList = ref<CareItemVO[]>([])

// 基础下拉选项
const elderList = ref<any[]>([])
const staffList = ref<any[]>([])

// 弹窗状态与表单
const dialogSubmitting = ref(false)
const currentTask = ref<CareTaskVO | null>(null)

// 任务生成弹窗
const generateDialogVisible = ref(false)
const generateForm = reactive({
  taskDate: getTodayDate(),
  elderId: undefined as number | undefined
})

// 指派弹窗
const assignDialogVisible = ref(false)
const assignForm = reactive({ assigneeId: undefined as number | undefined })

// 完成弹窗
const completeDialogVisible = ref(false)
const completeForm = reactive({
  resultCode: 'NORMAL',
  resultDesc: ''
})

// 制定计划弹窗
const createPlanDialogVisible = ref(false)
const planForm = reactive({
  elderId: undefined as number | undefined,
  careLevelId: undefined as number | undefined,
  startDate: getTodayDate(),
  endDate: '',
  remark: '',
  items: [] as any[]
})

// 计划详情抽屉
const planDrawerVisible = ref(false)
const currentPlanDetail = ref<CarePlanDetailVO | null>(null)

// 任务执行记录抽屉
const recordsDrawerVisible = ref(false)
const recordsLoading = ref(false)
const taskRecords = ref<CareTaskRecordVO[]>([])

// 护理等级弹窗
const levelDialogVisible = ref(false)
const levelForm = reactive({
  id: undefined as number | undefined,
  levelCode: '',
  levelName: '',
  description: ''
})

// 护理项目弹窗
const careItemDialogVisible = ref(false)
const careItemForm = reactive({
  id: undefined as number | undefined,
  itemCode: '',
  itemName: '',
  category: 'DAILY_CARE',
  description: ''
})

// 数据加载方法
async function fetchSummary() {
  try {
    const res = await getCareTaskSummary(taskQuery.taskDate)
    Object.assign(summary, res.data)
  } catch (err) {
    console.error(err)
  }
}

async function fetchTasks() {
  taskLoading.value = true
  try {
    const res = await getCareTaskPage(taskQuery)
    taskList.value = res.data.list
    taskTotal.value = res.data.total
  } finally {
    taskLoading.value = false
  }
}

function resetTaskQuery() {
  taskQuery.taskDate = getTodayDate()
  taskQuery.elderName = ''
  taskQuery.status = ''
  taskQuery.pageNum = 1
  fetchTasks()
}

async function fetchPlans() {
  planLoading.value = true
  try {
    const res = await getCarePlanPage(planQuery)
    planList.value = res.data.list
    planTotal.value = res.data.total
  } finally {
    planLoading.value = false
  }
}

function resetPlanQuery() {
  planQuery.planNo = ''
  planQuery.elderName = ''
  planQuery.careLevelId = undefined
  planQuery.status = ''
  planQuery.pageNum = 1
  fetchPlans()
}

async function fetchConfig() {
  try {
    const lRes = await getCareLevels()
    levelList.value = lRes.data
    const iRes = await getCareItems()
    careItemList.value = iRes.data
  } catch (e) {
    console.error(e)
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
  if (tabName === 'tasks') {
    fetchTasks()
    fetchSummary()
  } else if (tabName === 'plans') {
    fetchPlans()
  } else if (tabName === 'config') {
    fetchConfig()
  }
}

// 任务操作
function openGenerateDialog() {
  generateForm.taskDate = getTodayDate()
  generateForm.elderId = undefined
  generateDialogVisible.value = true
}

async function submitGenerate() {
  dialogSubmitting.value = true
  try {
    const res = await generateCareTasks(generateForm)
    ElMessage.success(`任务生成完毕，新增生成 ${res.data} 条任务（幂等保护生效）`)
    generateDialogVisible.value = false
    fetchTasks()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

function openAssignDialog(row: CareTaskVO) {
  currentTask.value = row
  assignForm.assigneeId = row.assigneeId
  assignDialogVisible.value = true
}

async function submitAssign() {
  if (!assignForm.assigneeId || !currentTask.value) {
    ElMessage.warning('请选择护理人员')
    return
  }
  dialogSubmitting.value = true
  try {
    await assignCareTask(currentTask.value.id, { assigneeId: assignForm.assigneeId })
    ElMessage.success('责任人指派成功')
    assignDialogVisible.value = false
    fetchTasks()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleTaskAction(row: CareTaskVO, action: 'START') {
  try {
    await executeCareTaskAction(row.id, { action })
    ElMessage.success('已开始执行')
    fetchTasks()
    fetchSummary()
  } catch (e) {
    console.error(e)
  }
}

function openCompleteDialog(row: CareTaskVO) {
  currentTask.value = row
  completeForm.resultCode = 'NORMAL'
  completeForm.resultDesc = ''
  completeDialogVisible.value = true
}

async function submitComplete() {
  if (!completeForm.resultDesc.trim() || !currentTask.value) {
    ElMessage.warning('请填写执行反馈说明')
    return
  }
  dialogSubmitting.value = true
  try {
    await executeCareTaskAction(currentTask.value.id, {
      action: 'COMPLETE',
      resultCode: completeForm.resultCode,
      resultDesc: completeForm.resultDesc
    })
    ElMessage.success('任务完成填报已保存')
    completeDialogVisible.value = false
    fetchTasks()
    fetchSummary()
  } finally {
    dialogSubmitting.value = false
  }
}

async function openMissPrompt(row: CareTaskVO) {
  try {
    const { value } = await ElMessageBox.prompt('请输入漏执行原因（如长者外出就医）', '标记漏执行', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空'
    })
    await executeCareTaskAction(row.id, { action: 'MISS', resultDesc: value })
    ElMessage.success('已标记漏执行并留痕')
    fetchTasks()
    fetchSummary()
  } catch (e) {}
}

async function viewRecords(row: CareTaskVO) {
  currentTask.value = row
  recordsDrawerVisible.value = true
  recordsLoading.value = true
  try {
    const res = await getCareTaskRecords(row.id)
    taskRecords.value = res.data
  } finally {
    recordsLoading.value = false
  }
}

// 计划操作
function openCreatePlanDialog() {
  planForm.elderId = undefined
  planForm.careLevelId = levelList.value.length ? levelList.value[0].id : undefined
  planForm.startDate = getTodayDate()
  planForm.endDate = ''
  planForm.remark = ''
  planForm.items = [
    {
      careItemId: careItemList.value.length ? careItemList.value[0].id : undefined,
      frequencyType: 'DAILY',
      timeWindowStart: '08:00:00',
      timeWindowEnd: '09:00:00',
      assigneeId: undefined
    }
  ]
  createPlanDialogVisible.value = true
}

function addPlanItemRow() {
  planForm.items.push({
    careItemId: careItemList.value.length ? careItemList.value[0].id : undefined,
    frequencyType: 'DAILY',
    timeWindowStart: '12:00:00',
    timeWindowEnd: '13:00:00',
    assigneeId: undefined
  })
}

function removePlanItemRow(index: number) {
  if (planForm.items.length <= 1) {
    ElMessage.warning('计划至少需保留一项护理项目')
    return
  }
  planForm.items.splice(index, 1)
}

async function submitCreatePlan() {
  if (!planForm.elderId || !planForm.careLevelId || !planForm.startDate) {
    ElMessage.warning('请补全计划必要基础信息')
    return
  }
  dialogSubmitting.value = true
  try {
    await createCarePlan(planForm as any)
    ElMessage.success('护理计划制定成功并已生效')
    createPlanDialogVisible.value = false
    fetchPlans()
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleTogglePlan(row: CarePlanVO, status: string) {
  try {
    await toggleCarePlanStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 'ACTIVE' ? '计划已恢复执行' : '计划已暂停')
  } catch (e) {
    console.error(e)
  }
}

async function viewPlanDetail(row: CarePlanVO) {
  try {
    const res = await getCarePlanDetail(row.id)
    currentPlanDetail.value = res.data
    planDrawerVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

async function triggerPlanTask(row: CarePlanVO) {
  try {
    const res = await generateCareTasks({ taskDate: getTodayDate(), elderId: row.elderId })
    ElMessage.success(`已为长者生成今日任务 ${res.data} 条`)
    fetchSummary()
  } catch (e) {
    console.error(e)
  }
}

// 护理等级与项目操作
function openLevelDialog(row?: CareLevelVO) {
  if (row) {
    levelForm.id = row.id
    levelForm.levelCode = row.levelCode
    levelForm.levelName = row.levelName
    levelForm.description = row.description || ''
  } else {
    levelForm.id = undefined
    levelForm.levelCode = ''
    levelForm.levelName = ''
    levelForm.description = ''
  }
  levelDialogVisible.value = true
}

async function submitLevel() {
  if (!levelForm.levelCode.trim() || !levelForm.levelName.trim()) {
    ElMessage.warning('请填写等级编码与名称')
    return
  }
  dialogSubmitting.value = true
  try {
    if (levelForm.id) {
      await updateCareLevel(levelForm.id, levelForm)
      ElMessage.success('护理等级修改成功')
    } else {
      await createCareLevel(levelForm)
      ElMessage.success('护理等级新增成功')
    }
    levelDialogVisible.value = false
    fetchConfig()
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleToggleLevel(row: CareLevelVO, status: string) {
  try {
    await toggleCareLevelStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 'ACTIVE' ? '等级已启用' : '等级已停用')
  } catch (e) {
    console.error(e)
  }
}

function openCareItemDialog(row?: CareItemVO) {
  if (row) {
    careItemForm.id = row.id
    careItemForm.itemCode = row.itemCode
    careItemForm.itemName = row.itemName
    careItemForm.category = row.category || 'DAILY_CARE'
    careItemForm.description = row.description || ''
  } else {
    careItemForm.id = undefined
    careItemForm.itemCode = ''
    careItemForm.itemName = ''
    careItemForm.category = 'DAILY_CARE'
    careItemForm.description = ''
  }
  careItemDialogVisible.value = true
}

async function submitCareItem() {
  if (!careItemForm.itemCode.trim() || !careItemForm.itemName.trim()) {
    ElMessage.warning('请填写项目代码与名称')
    return
  }
  dialogSubmitting.value = true
  try {
    if (careItemForm.id) {
      await updateCareItem(careItemForm.id, careItemForm)
      ElMessage.success('护理项目更新成功')
    } else {
      await createCareItem(careItemForm)
      ElMessage.success('护理项目新增成功')
    }
    careItemDialogVisible.value = false
    fetchConfig()
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleToggleCareItem(row: CareItemVO, status: string) {
  try {
    await toggleCareItemStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 'ACTIVE' ? '项目已启用' : '项目已停用')
  } catch (e) {
    console.error(e)
  }
}

// 辅助方法
function getTodayDate(): string {
  const d = new Date()
  const pad = (n: number) => (n < 10 ? '0' + n : n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatTaskStatus(st?: string) {
  const map: Record<string, string> = {
    PENDING: '待执行',
    IN_PROGRESS: '执行中',
    COMPLETED: '已完成',
    MISSED: '漏执行',
    CANCELLED: '已取消'
  }
  return map[st || ''] || st || ''
}

function getTaskStatusTagType(st?: string): 'danger' | 'warning' | 'info' | 'success' | 'primary' {
  if (st === 'COMPLETED') return 'success'
  if (st === 'IN_PROGRESS') return 'primary'
  if (st === 'PENDING') return 'warning'
  if (st === 'MISSED') return 'danger'
  return 'info'
}

function formatItemCategory(cat?: string) {
  const map: Record<string, string> = {
    DAILY_CARE: '生活照料',
    BODY_NURSING: '躯体护理',
    HEALTH_CARE: '医疗护理',
    REHAB_CARE: '康复辅助',
    ENV_CLEAN: '环境消杀'
  }
  return map[cat || ''] || cat || '照护'
}

onMounted(() => {
  fetchSummary()
  fetchTasks()
  fetchConfig()
  loadEldersAndStaff()
})
</script>

<style scoped>
.care-page {
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
.kpi-card.pending .kpi-icon { background: #fffbe6; color: #faad14; }
.kpi-card.in-progress .kpi-icon { background: #e6fffb; color: #13c2c2; }
.kpi-card.completed .kpi-icon { background: #f6ffed; color: #52c41a; }
.kpi-card.missed .kpi-icon { background: #fff1f0; color: #ff4d4f; }
.kpi-card.cancelled .kpi-icon { background: #f5f5f5; color: #8c8c8c; }

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

.warning-num { color: #d48806; }
.primary-num { color: #08979c; }
.success-num { color: #389e0d; }
.danger-num { color: #cf1322; }
.info-num { color: #8c8c8c; }

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

.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.action-btn-group {
  display: flex;
  gap: 10px;
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

.care-item-title {
  font-weight: 500;
  color: #262626;
}

.category-tag {
  margin-left: 6px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.config-title {
  font-weight: 700;
  font-size: 15px;
  color: #1f1f1f;
}

.dialog-alert {
  margin-top: 10px;
}

.items-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  margin-bottom: 8px;
}

.items-title {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
}

.time-range-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.plan-items-table {
  margin-bottom: 12px;
}

.plan-detail-box {
  padding: 8px;
}

.plan-header-card {
  background: #f9fbfd;
  border: 1px solid #e6f4ff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.plan-no-text {
  font-size: 16px;
  font-weight: 700;
  color: #1677ff;
}

.plan-elder-info {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
  margin-top: 4px;
}

.plan-level-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.plan-date-range {
  font-size: 12px;
  color: #8c8c8c;
}

.detail-section-title {
  font-weight: 600;
  font-size: 14px;
  color: #1f1f1f;
  margin-bottom: 10px;
}

.drawer-header-info {
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.task-no-text {
  font-size: 16px;
  font-weight: 700;
  color: #1677ff;
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

.operator-name {
  font-size: 12px;
  color: #8c8c8c;
}

.log-desc {
  font-size: 13px;
  color: #262626;
  line-height: 1.5;
}
</style>
