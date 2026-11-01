<template>
  <div class="iot-page">
    <!-- 顶部 KPI 统计看板 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card total-dev">
          <div class="kpi-icon"><el-icon><Cpu /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">物联网设备总数</span>
            <span class="kpi-num">{{ summary.totalDevices }} 台</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card active-dev">
          <div class="kpi-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">在线运行设备</span>
            <span class="kpi-num success-num">{{ summary.activeDevices }} 台</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card bound-dev">
          <div class="kpi-icon"><el-icon><Connection /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">已绑定就养设备</span>
            <span class="kpi-num purple-num">{{ summary.boundDevices }} 台</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card unbound-dev">
          <div class="kpi-icon"><el-icon><Box /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">空闲就绪设备</span>
            <span class="kpi-num primary-num">{{ summary.unboundDevices }} 台</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card open-event">
          <div class="kpi-icon"><el-icon><BellFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待处置告警事件</span>
            <span class="kpi-num danger-num">{{ summary.openEventCount }} 起</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card today-data">
          <div class="kpi-icon"><el-icon><DataLine /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">今日接收遥测点</span>
            <span class="kpi-num warning-num">{{ summary.todayTelemetryCount }} 次</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 选项卡主内容区 -->
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 选项卡 1：设备台账与绑定管理 -->
        <el-tab-pane label="设备台账与绑定管理" name="devices">
          <div class="tab-header">
            <div class="header-text">
              <h3 class="tab-title">设备台账与长者/空间绑定</h3>
              <span class="tab-subtitle">集中维护智能手环、体征床垫、血压计、跌倒雷达、SOS呼叫器等物联网终端；排他锁保障长者与床位绑定安全</span>
            </div>
            <div class="header-btns">
              <el-button type="primary" :icon="Plus" @click="handleOpenCreateDevice">
                登记新设备
              </el-button>
              <el-button type="success" :icon="Position" @click="handleSwitchToSimulator">
                模拟上报发射台
              </el-button>
              <el-button :icon="Refresh" @click="loadDevices">刷新</el-button>
            </div>
          </div>

          <!-- 搜索筛选表单 -->
          <el-form :inline="true" :model="deviceQuery" class="filter-form">
            <el-form-item label="设备类型">
              <el-select v-model="deviceQuery.deviceType" placeholder="全部类型" clearable style="width: 150px">
                <el-option label="智能手环/手表" value="SMART_WATCH" />
                <el-option label="睡眠体征床垫" value="SMART_MATTRESS" />
                <el-option label="智能血压计" value="BLOOD_PRESSURE" />
                <el-option label="智能血糖仪" value="BLOOD_GLUCOSE" />
                <el-option label="一键呼叫器 (SOS)" value="SOS_BUTTON" />
                <el-option label="跌倒检测雷达" value="RADAR_FALL" />
                <el-option label="红外体温计" value="THERMOMETER" />
              </el-select>
            </el-form-item>
            <el-form-item label="运行状态">
              <el-select v-model="deviceQuery.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option label="在线运行 (ACTIVE)" value="ACTIVE" />
                <el-option label="设备离线 (OFFLINE)" value="OFFLINE" />
                <el-option label="故障告警 (FAULT)" value="FAULT" />
                <el-option label="已停用 (INACTIVE)" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item label="绑定状态">
              <el-select v-model="deviceQuery.bindStatus" placeholder="全部" clearable style="width: 120px">
                <el-option label="已绑定就养" value="BOUND" />
                <el-option label="空闲就绪" value="UNBOUND" />
              </el-select>
            </el-form-item>
            <el-form-item label="综合搜索">
              <el-input
                v-model="deviceQuery.keyword"
                placeholder="编码 / 名称 / SN / 位置..."
                clearable
                :prefix-icon="Search"
                style="width: 210px"
                @keyup.enter="handleDeviceSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleDeviceSearch">查询</el-button>
              <el-button :icon="RefreshRight" @click="handleDeviceReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 设备列表表格 -->
          <el-table :data="deviceList" v-loading="deviceLoading" stripe border style="width: 100%">
            <el-table-column prop="deviceCode" label="设备编码" min-width="150">
              <template #default="{ row }">
                <span class="mono-code"><b>{{ row.deviceCode }}</b></span>
              </template>
            </el-table-column>
            <el-table-column prop="deviceName" label="设备名称" min-width="170">
              <template #default="{ row }">
                <span class="device-cell-name">{{ row.deviceName }}</span>
                <span class="sub-model-tag">{{ row.vendor || '通用' }} {{ row.model || '' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="deviceTypeName" label="类型" width="130" align="center">
              <template #default="{ row }">
                <el-tag effect="light">{{ row.deviceTypeName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="installLocation" label="安装/摆放位置" min-width="150" show-overflow-tooltip />
            <el-table-column prop="status" label="运行状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getDeviceStatusTag(row.status)">{{ row.statusName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前绑定对象" min-width="180">
              <template #default="{ row }">
                <div v-if="row.bindStatus === 'BOUND'">
                  <el-tag type="success" size="small" style="margin-right: 6px">已绑定</el-tag>
                  <b v-if="row.boundElderName" class="bound-elder-text">{{ row.boundElderName }}</b>
                  <span v-if="row.boundBedInfo && row.boundBedInfo !== '-'" class="bound-bed-text"> ({{ row.boundBedInfo }})</span>
                </div>
                <div v-else>
                  <el-tag type="info" size="small">空闲未绑定</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="lastOnlineAt" label="最后在线时间" width="160" align="center" />
            <el-table-column label="操作" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.bindStatus !== 'BOUND'"
                  type="primary"
                  size="small"
                  @click="handleOpenBind(row)"
                >
                  绑定
                </el-button>
                <el-button
                  v-else
                  type="warning"
                  size="small"
                  @click="handleOpenUnbind(row)"
                >
                  解绑
                </el-button>
                <el-button link type="primary" size="small" @click="handleOpenEditDevice(row)">
                  编辑
                </el-button>
                <el-button link type="info" size="small" @click="handleOpenHistory(row)">
                  历史
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 设备分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="deviceQuery.pageNum"
              v-model:page-size="deviceQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="deviceTotal"
              @size-change="loadDevices"
              @current-change="loadDevices"
            />
          </div>
        </el-tab-pane>

        <!-- 选项卡 2：模拟数据接入工作台与遥测流水 -->
        <el-tab-pane label="模拟数据接入与遥测流水" name="telemetry">
          <!-- 模拟器快速发射台 -->
          <div class="simulator-card">
            <div class="sim-header">
              <div class="sim-title">
                <el-icon><Position /></el-icon>
                <span><b>物联网硬件数据模拟上报发射台</b> (内置厂商协议 Adapter 自动解析，异常度量自动识别告警)</span>
              </div>
              <el-tag type="success" effect="dark">协议隔离 Adapter 已装载</el-tag>
            </div>

            <el-form :model="simForm" label-width="110px" class="sim-form">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="目标设备" required>
                    <el-select
                      v-model="simForm.deviceCode"
                      placeholder="选择目标接入设备"
                      filterable
                      style="width: 100%"
                      @change="handleSimDeviceChange"
                    >
                      <el-option
                        v-for="d in deviceOptions"
                        :key="d.id"
                        :label="`[${d.deviceCode}] ${d.deviceName}`"
                        :value="d.deviceCode"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="厂商通信协议">
                    <el-select v-model="simForm.protocol" style="width: 100%">
                      <el-option label="标准通用协议 (STANDARD)" value="STANDARD" />
                      <el-option label="华为健康云协议 (HUAWEI_HEALTH)" value="HUAWEI_HEALTH" />
                      <el-option label="涂鸦智能私有协议 (TUYA_DPS)" value="TUYA_DPS" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="电池电量(%)">
                    <el-input-number v-model="simForm.battery" :min="1" :max="100" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="6">
                  <el-form-item label="心率 (bpm)">
                    <el-input-number v-model="simForm.heartRate" :min="30" :max="220" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="血氧 (SpO2 %)">
                    <el-input-number v-model="simForm.spo2" :min="70" :max="100" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="收缩压 (mmHg)">
                    <el-input-number v-model="simForm.systolic" :min="60" :max="220" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="舒张压 (mmHg)">
                    <el-input-number v-model="simForm.diastolic" :min="40" :max="140" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="6">
                  <el-form-item label="呼吸频率">
                    <el-input-number v-model="simForm.respiration" :min="5" :max="50" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="体温 (℃)">
                    <el-input-number v-model="simForm.temperature" :min="34.0" :max="42.0" :precision="1" :step="0.1" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="睡眠在床状态">
                    <el-switch v-model="simForm.inBed" active-text="在床" inactive-text="离床" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="异常事件模拟">
                    <el-checkbox v-model="simForm.sos">一键SOS</el-checkbox>
                    <el-checkbox v-model="simForm.fallDetected">雷达跌倒</el-checkbox>
                  </el-form-item>
                </el-col>
              </el-row>

              <div class="sim-actions">
                <span class="sim-tip">💡 提示：心率高于 120 或低于 45、血氧低于 90、电量低于 15% 将自动触发设备告警事件与健康档案桥接！</span>
                <el-button type="success" :icon="Position" :loading="simSubmitting" @click="handleSendMockTelemetry">
                  发射模拟遥测上报
                </el-button>
              </div>
            </el-form>
          </div>

          <!-- 遥测数据明细历史 -->
          <div class="tab-header" style="margin-top: 16px">
            <div class="header-text">
              <h3 class="tab-title">设备遥测采集数据明细</h3>
              <span class="tab-subtitle">支持按长者追溯历史穿戴及床位传感器指标，全量保留原始上报报文 JSON</span>
            </div>
            <div class="header-btns">
              <el-button :icon="Refresh" @click="loadTelemetryData">刷新遥测</el-button>
            </div>
          </div>

          <el-form :inline="true" :model="dataQuery" class="filter-form">
            <el-form-item label="长者档案">
              <el-select v-model="dataQuery.elderId" placeholder="按就养长者筛选" clearable filterable style="width: 160px">
                <el-option
                  v-for="e in elderOptions"
                  :key="e.id"
                  :label="`${e.name} (${e.elderNo})`"
                  :value="e.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="指标类型">
              <el-select v-model="dataQuery.metricCode" placeholder="全部指标" clearable style="width: 150px">
                <el-option label="心率 (HEART_RATE)" value="HEART_RATE" />
                <el-option label="血氧 (SPO2)" value="SPO2" />
                <el-option label="呼吸率 (RESPIRATORY_RATE)" value="RESPIRATORY_RATE" />
                <el-option label="收缩压 (SBP)" value="SBP" />
                <el-option label="舒张压 (DBP)" value="DBP" />
                <el-option label="体温 (BODY_TEMP)" value="BODY_TEMP" />
                <el-option label="在床状态 (IN_BED_STATUS)" value="IN_BED_STATUS" />
                <el-option label="设备电量 (BATTERY)" value="BATTERY" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleDataSearch">查询</el-button>
              <el-button :icon="RefreshRight" @click="handleDataReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="telemetryList" v-loading="dataLoading" stripe border style="width: 100%">
            <el-table-column prop="collectedAt" label="采集时间" width="160" align="center" />
            <el-table-column prop="deviceCode" label="设备编码" width="150">
              <template #default="{ row }">
                <span class="mono-code">{{ row.deviceCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="deviceName" label="设备名称" min-width="160" />
            <el-table-column prop="elderName" label="关联长者" width="120">
              <template #default="{ row }">
                <b class="elder-cell-name">{{ row.elderName }}</b>
              </template>
            </el-table-column>
            <el-table-column prop="metricName" label="指标项" width="160" align="center">
              <template #default="{ row }">
                <el-tag effect="plain">{{ row.metricName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="metricValue" label="标准化读数" width="140" align="right">
              <template #default="{ row }">
                <b class="metric-num">{{ Number(row.metricValue).toFixed(2) }}</b>
                <span class="metric-unit"> {{ row.unit }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="receivedAt" label="接收时间" width="160" align="center" />
            <el-table-column label="原始报文" width="100" align="center">
              <template #default="{ row }">
                <el-popover placement="left" :width="320" trigger="click">
                  <template #reference>
                    <el-button link type="primary" size="small">查看JSON</el-button>
                  </template>
                  <pre class="raw-json-box">{{ formatJson(row.rawPayload) }}</pre>
                </el-popover>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="dataQuery.pageNum"
              v-model:page-size="dataQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="dataTotal"
              @size-change="loadTelemetryData"
              @current-change="loadTelemetryData"
            />
          </div>
        </el-tab-pane>

        <!-- 选项卡 3：设备告警事件处置 -->
        <el-tab-pane label="设备告警与事件处置" name="events">
          <div class="tab-header">
            <div class="header-text">
              <h3 class="tab-title">设备异常事件处置中心</h3>
              <span class="tab-subtitle">实时监控紧急 SOS 求救、卫生间跌倒雷达预警、夜间离床超时、传感器低电量及体征读数越限事件</span>
            </div>
            <div class="header-btns">
              <el-button :icon="Refresh" @click="loadEvents">刷新事件</el-button>
            </div>
          </div>

          <el-form :inline="true" :model="eventQuery" class="filter-form">
            <el-form-item label="事件类型">
              <el-select v-model="eventQuery.eventType" placeholder="全部类型" clearable style="width: 160px">
                <el-option label="SOS 紧急求救" value="SOS" />
                <el-option label="跌倒检测告警" value="FALL_DETECTED" />
                <el-option label="夜间离床超时" value="LEAVE_BED_OVERTIME" />
                <el-option label="指标读数异常" value="ABNORMAL_METRIC" />
                <el-option label="设备低电量" value="LOW_BATTERY" />
                <el-option label="设备通信离线" value="OFFLINE" />
              </el-select>
            </el-form-item>
            <el-form-item label="严重级别">
              <el-select v-model="eventQuery.severity" placeholder="全部级别" clearable style="width: 120px">
                <el-option label="特急重大 (CRITICAL)" value="CRITICAL" />
                <el-option label="高风险 (HIGH)" value="HIGH" />
                <el-option label="中度注意 (MEDIUM)" value="MEDIUM" />
                <el-option label="低度提醒 (LOW)" value="LOW" />
              </el-select>
            </el-form-item>
            <el-form-item label="处置状态">
              <el-select v-model="eventQuery.eventStatus" placeholder="全部状态" clearable style="width: 120px">
                <el-option label="待处置 (OPEN)" value="OPEN" />
                <el-option label="处置中 (PROCESSING)" value="PROCESSING" />
                <el-option label="已结案 (CLOSED)" value="CLOSED" />
                <el-option label="已忽略 (IGNORED)" value="IGNORED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleEventSearch">查询</el-button>
              <el-button :icon="RefreshRight" @click="handleEventReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="eventList" v-loading="eventLoading" stripe border style="width: 100%">
            <el-table-column prop="occurredAt" label="发生时间" width="160" align="center" />
            <el-table-column prop="eventTypeName" label="事件类型" min-width="160">
              <template #default="{ row }">
                <el-tag :type="getEventTypeTag(row.eventType)">
                  {{ row.eventTypeName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="severityName" label="级别" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getSeverityTag(row.severity)" effect="dark">{{ row.severityName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deviceName" label="涉事设备 / 位置" min-width="180">
              <template #default="{ row }">
                <b>{{ row.deviceName }}</b>
                <div class="sub-bed-tag">{{ row.installLocation || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="elderName" label="长者" width="110" align="center" />
            <el-table-column prop="eventStatusName" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getEventStatusTag(row.eventStatus)">{{ row.eventStatusName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="handleResult" label="处置结论与措施" min-width="180" show-overflow-tooltip />
            <el-table-column prop="handledByName" label="处置人" width="110" align="center" />
            <el-table-column label="操作" width="110" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.eventStatus !== 'CLOSED'"
                  type="primary"
                  size="small"
                  @click="handleOpenHandleEvent(row)"
                >
                  处置
                </el-button>
                <span v-else class="text-muted">已结案</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="eventQuery.pageNum"
              v-model:page-size="eventQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="eventTotal"
              @size-change="loadEvents"
              @current-change="loadEvents"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 对话框 1：新增/编辑设备 -->
    <el-dialog
      v-model="deviceDialogVisible"
      :title="deviceForm.id ? '编辑设备资料' : '登记新增物联网设备'"
      width="560px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="deviceFormRef" :model="deviceForm" :rules="deviceRules" label-width="110px">
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input v-model="deviceForm.deviceCode" placeholder="如 DEV_WATCH_001" :disabled="!!deviceForm.id" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="deviceForm.deviceName" placeholder="如 华为智能体征手环" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="deviceForm.deviceType" style="width: 100%">
                <el-option label="智能手环/手表" value="SMART_WATCH" />
                <el-option label="睡眠体征床垫" value="SMART_MATTRESS" />
                <el-option label="智能血压计" value="BLOOD_PRESSURE" />
                <el-option label="智能血糖仪" value="BLOOD_GLUCOSE" />
                <el-option label="一键呼叫器 (SOS)" value="SOS_BUTTON" />
                <el-option label="跌倒检测雷达" value="RADAR_FALL" />
                <el-option label="红外体温计" value="THERMOMETER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备厂商">
              <el-input v-model="deviceForm.vendor" placeholder="如 HUAWEI, OMRON" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格型号">
              <el-input v-model="deviceForm.model" placeholder="如 Band-B6-Pro" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序列号(SN)">
              <el-input v-model="deviceForm.serialNo" placeholder="出厂硬件序列号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="安装位置">
          <el-input v-model="deviceForm.installLocation" placeholder="如 1号楼 101室 随身/床头/卫生间" />
        </el-form-item>
        <el-form-item label="运行状态">
          <el-select v-model="deviceForm.status" style="width: 100%">
            <el-option label="在线运行 (ACTIVE)" value="ACTIVE" />
            <el-option label="设备离线 (OFFLINE)" value="OFFLINE" />
            <el-option label="故障检修 (FAULT)" value="FAULT" />
            <el-option label="停用注销 (INACTIVE)" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deviceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="deviceSubmitting" @click="handleSubmitDevice">
          保存设备
        </el-button>
      </template>
    </el-dialog>

    <!-- 对话框 2：设备绑定 -->
    <el-dialog
      v-model="bindDialogVisible"
      title="绑定物联网设备"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="currentDevice" class="bind-summary-box">
        <div class="summary-line"><span>设备编码：</span><b>{{ currentDevice.deviceCode }}</b></div>
        <div class="summary-line"><span>设备名称：</span><span>{{ currentDevice.deviceName }}</span></div>
        <div class="summary-line"><span>设备类型：</span><el-tag size="small">{{ currentDevice.deviceTypeName }}</el-tag></div>
      </div>

      <el-form ref="bindFormRef" :model="bindForm" label-width="110px" style="margin-top: 16px">
        <el-form-item label="绑定类型" required>
          <el-radio-group v-model="bindForm.bindType">
            <el-radio label="ELDER">长者随身佩戴</el-radio>
            <el-radio label="BED">床位固定监测</el-radio>
            <el-radio label="SPACE">房间公共空间</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="bindForm.bindType === 'ELDER'" label="就养长者" required>
          <el-select v-model="bindForm.elderId" placeholder="搜索并选择长者" filterable style="width: 100%">
            <el-option
              v-for="e in elderOptions"
              :key="e.id"
              :label="`${e.name} (${e.elderNo})`"
              :value="e.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="bindForm.bindType === 'BED'" label="目标床位" required>
          <el-select v-model="bindForm.bedId" placeholder="选择安装床位" filterable style="width: 100%">
            <el-option
              v-for="b in bedOptions"
              :key="b.id"
              :label="`${b.buildingName || ''} ${b.roomNo ? b.roomNo + '室' : ''} ${b.bedNo}床`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="handleSubmitBind">
          确认绑定
        </el-button>
      </template>
    </el-dialog>

    <!-- 对话框 3：设备解绑 -->
    <el-dialog
      v-model="unbindDialogVisible"
      title="解除设备绑定"
      width="460px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="currentDevice" class="bind-summary-box">
        <div class="summary-line"><span>设备编码：</span><b>{{ currentDevice.deviceCode }}</b></div>
        <div class="summary-line"><span>当前绑定：</span><b class="danger-text">{{ currentDevice.boundElderName || currentDevice.boundBedInfo }}</b></div>
      </div>
      <el-form :model="unbindForm" label-width="100px" style="margin-top: 16px">
        <el-form-item label="解绑原因">
          <el-input v-model="unbindForm.remark" type="textarea" :rows="3" placeholder="如长者退住、设备调换等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unbindDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="unbindSubmitting" @click="handleSubmitUnbind">
          确认解除绑定
        </el-button>
      </template>
    </el-dialog>

    <!-- 对话框 4：处置告警事件 -->
    <el-dialog
      v-model="handleDialogVisible"
      title="处置设备异常告警"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="currentEvent" class="bind-summary-box">
        <div class="summary-line"><span>事件类型：</span><el-tag :type="getEventTypeTag(currentEvent.eventType)">{{ currentEvent.eventTypeName }}</el-tag></div>
        <div class="summary-line"><span>发生时间：</span><span>{{ currentEvent.occurredAt }}</span></div>
        <div class="summary-line"><span>涉及设备：</span><span>{{ currentEvent.deviceName }} ({{ currentEvent.installLocation }})</span></div>
      </div>
      <el-form :model="handleForm" label-width="100px" style="margin-top: 16px">
        <el-form-item label="处理状态" required>
          <el-radio-group v-model="handleForm.eventStatus">
            <el-radio label="PROCESSING">处置中</el-radio>
            <el-radio label="CLOSED">已结案</el-radio>
            <el-radio label="IGNORED">误报忽略</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处置结论" required>
          <el-input
            v-model="handleForm.handleResult"
            type="textarea"
            :rows="3"
            placeholder="请详细记录现场排查处置情况、长者安抚情况或设备检修措施"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleSubmitting" @click="handleSubmitHandleEvent">
          保存处置结论
        </el-button>
      </template>
    </el-dialog>

    <!-- 抽屉：设备历史绑定记录 -->
    <el-drawer
      v-model="historyDrawerVisible"
      title="设备绑定与使用流转历史"
      size="560px"
      destroy-on-close
    >
      <div v-if="selectedHistoryDevice" class="history-drawer-content">
        <div class="dev-info-bar">
          <b>[{{ selectedHistoryDevice.deviceCode }}] {{ selectedHistoryDevice.deviceName }}</b>
          <el-tag size="small">{{ selectedHistoryDevice.deviceTypeName }}</el-tag>
        </div>

        <el-timeline style="margin-top: 16px">
          <el-timeline-item
            v-for="b in bindingHistoryList"
            :key="b.id"
            :timestamp="b.boundAt"
            placement="top"
            :type="b.status === 'BOUND' ? 'primary' : 'info'"
          >
            <el-card shadow="never" class="history-item-card">
              <div class="hist-line">
                <span>绑定对象：</span>
                <b v-if="b.elderName && b.elderName !== '-'">{{ b.elderName }} (长者编号: {{ b.elderNo }})</b>
                <b v-else-if="b.bedInfo && b.bedInfo !== '-'">{{ b.bedInfo }}</b>
                <span v-else>公共空间</span>
              </div>
              <div class="hist-line">
                <span>绑定模式：</span><span>{{ b.bindTypeName }}</span>
                <el-tag :type="b.status === 'BOUND' ? 'success' : 'info'" size="small" style="margin-left: 8px">
                  {{ b.statusName }}
                </el-tag>
              </div>
              <div v-if="b.unboundAt" class="hist-line text-muted">
                <span>解绑时间：</span><span>{{ b.unboundAt }}</span>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
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
  RefreshRight,
  Cpu,
  CircleCheckFilled,
  Connection,
  Box,
  BellFilled,
  DataLine,
  Position
} from '@element-plus/icons-vue'
import {
  DeviceVO,
  DeviceQueryDTO,
  DeviceDTO,
  DeviceBindDTO,
  DeviceUnbindDTO,
  DeviceBindingVO,
  DeviceDataVO,
  DeviceDataQueryDTO,
  DeviceEventVO,
  DeviceEventQueryDTO,
  DeviceEventHandleDTO,
  DeviceReportDTO,
  IotSummaryVO,
  pageDevices,
  createDevice,
  updateDevice,
  bindDevice,
  unbindDevice,
  pageBindings,
  getIotSummary,
  reportTelemetry,
  pageTelemetryData,
  pageEvents,
  handleEvent
} from '@/api/iot'
import { fetchElders } from '@/api/elder'
import { getBeds } from '@/api/admission'

const activeTab = ref('devices')

// 综合看板指标
const summary = ref<IotSummaryVO>({
  totalDevices: 0,
  activeDevices: 0,
  offlineDevices: 0,
  faultDevices: 0,
  boundDevices: 0,
  unboundDevices: 0,
  openEventCount: 0,
  todayTelemetryCount: 0
})

const loadSummary = async () => {
  try {
    const res = await getIotSummary()
    if (res.data) summary.value = res.data
  } catch (err) {
    console.error('加载IoT统计指标失败', err)
  }
}

/* ================= 1. 设备台账与绑定 ================= */
const deviceLoading = ref(false)
const deviceList = ref<DeviceVO[]>([])
const deviceTotal = ref(0)
const deviceQuery = reactive<DeviceQueryDTO>({
  deviceType: '',
  status: '',
  bindStatus: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const elderOptions = ref<any[]>([])
const bedOptions = ref<any[]>([])
const deviceOptions = ref<DeviceVO[]>([])

const loadDevices = async () => {
  deviceLoading.value = true
  try {
    const res = await pageDevices(deviceQuery)
    if (res.data) {
      deviceList.value = res.data.list
      deviceTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载设备列表失败', err)
  } finally {
    deviceLoading.value = false
  }
}

const loadAuxOptions = async () => {
  try {
    const [eRes, bRes, dRes] = await Promise.all([
      fetchElders({ pageSize: 100 }),
      getBeds(),
      pageDevices({ pageSize: 100 })
    ])
    if (eRes.data) elderOptions.value = eRes.data.list
    if (bRes.data) bedOptions.value = bRes.data
    if (dRes.data) deviceOptions.value = dRes.data.list
  } catch (err) {
    console.error(err)
  }
}

const handleDeviceSearch = () => {
  deviceQuery.pageNum = 1
  loadDevices()
}

const handleDeviceReset = () => {
  deviceQuery.deviceType = ''
  deviceQuery.status = ''
  deviceQuery.bindStatus = ''
  deviceQuery.keyword = ''
  deviceQuery.pageNum = 1
  loadDevices()
}

// 设备新增与编辑
const deviceDialogVisible = ref(false)
const deviceSubmitting = ref(false)
const deviceFormRef = ref()
const deviceForm = reactive<DeviceDTO & { id?: number }>({
  id: undefined,
  deviceCode: '',
  deviceName: '',
  deviceType: 'SMART_WATCH',
  vendor: '',
  model: '',
  serialNo: '',
  installLocation: '',
  status: 'ACTIVE'
})

const deviceRules = {
  deviceCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }]
}

const handleOpenCreateDevice = () => {
  deviceForm.id = undefined
  deviceForm.deviceCode = 'DEV_' + Math.random().toString(36).substring(2, 8).toUpperCase()
  deviceForm.deviceName = ''
  deviceForm.deviceType = 'SMART_WATCH'
  deviceForm.vendor = 'HUAWEI'
  deviceForm.model = ''
  deviceForm.serialNo = ''
  deviceForm.installLocation = '随身佩戴'
  deviceForm.status = 'ACTIVE'
  deviceDialogVisible.value = true
}

const handleOpenEditDevice = (row: DeviceVO) => {
  deviceForm.id = row.id
  deviceForm.deviceCode = row.deviceCode
  deviceForm.deviceName = row.deviceName
  deviceForm.deviceType = row.deviceType
  deviceForm.vendor = row.vendor || ''
  deviceForm.model = row.model || ''
  deviceForm.serialNo = row.serialNo || ''
  deviceForm.installLocation = row.installLocation || ''
  deviceForm.status = row.status
  deviceDialogVisible.value = true
}

const handleSubmitDevice = async () => {
  if (!deviceFormRef.value) return
  await deviceFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    deviceSubmitting.value = true
    try {
      if (deviceForm.id) {
        await updateDevice(deviceForm.id, deviceForm)
        ElMessage.success('设备信息更新成功')
      } else {
        await createDevice(deviceForm)
        ElMessage.success('设备台账登记成功')
      }
      deviceDialogVisible.value = false
      loadDevices()
      loadSummary()
    } catch (err: any) {
      ElMessage.error(err.message || '保存设备失败')
    } finally {
      deviceSubmitting.value = false
    }
  })
}

// 绑定设备
const bindDialogVisible = ref(false)
const bindSubmitting = ref(false)
const currentDevice = ref<DeviceVO | null>(null)
const bindFormRef = ref()
const bindForm = reactive<DeviceBindDTO>({
  deviceId: 0,
  elderId: undefined,
  bedId: undefined,
  bindType: 'ELDER'
})

const handleOpenBind = (row: DeviceVO) => {
  currentDevice.value = row
  bindForm.deviceId = row.id
  bindForm.elderId = undefined
  bindForm.bedId = undefined
  bindForm.bindType = row.deviceType === 'SMART_MATTRESS' ? 'BED' : 'ELDER'
  bindDialogVisible.value = true
}

const handleSubmitBind = async () => {
  if (bindForm.bindType === 'ELDER' && !bindForm.elderId) {
    ElMessage.warning('请选择绑定的长者')
    return
  }
  if (bindForm.bindType === 'BED' && !bindForm.bedId) {
    ElMessage.warning('请选择绑定的床位')
    return
  }
  bindSubmitting.value = true
  try {
    await bindDevice(bindForm)
    ElMessage.success('设备绑定成功！已建立排他占用关联')
    bindDialogVisible.value = false
    loadDevices()
    loadSummary()
  } catch (err: any) {
    ElMessage.error(err.message || '绑定设备失败')
  } finally {
    bindSubmitting.value = false
  }
}

// 解绑设备
const unbindDialogVisible = ref(false)
const unbindSubmitting = ref(false)
const unbindForm = reactive<DeviceUnbindDTO>({
  deviceId: 0,
  remark: ''
})

const handleOpenUnbind = (row: DeviceVO) => {
  currentDevice.value = row
  unbindForm.deviceId = row.id
  unbindForm.remark = ''
  unbindDialogVisible.value = true
}

const handleSubmitUnbind = async () => {
  unbindSubmitting.value = true
  try {
    await unbindDevice(unbindForm)
    ElMessage.success('设备已成功解绑')
    unbindDialogVisible.value = false
    loadDevices()
    loadSummary()
  } catch (err: any) {
    ElMessage.error(err.message || '解绑失败')
  } finally {
    unbindSubmitting.value = false
  }
}

// 设备历史绑定记录抽屉
const historyDrawerVisible = ref(false)
const selectedHistoryDevice = ref<DeviceVO | null>(null)
const bindingHistoryList = ref<DeviceBindingVO[]>([])

const handleOpenHistory = async (row: DeviceVO) => {
  selectedHistoryDevice.value = row
  historyDrawerVisible.value = true
  try {
    const res = await pageBindings({ deviceId: row.id, pageSize: 50 })
    if (res.data) bindingHistoryList.value = res.data.list
  } catch (err) {
    console.error(err)
  }
}

/* ================= 2. 模拟接入工作台与遥测数据 ================= */
const simSubmitting = ref(false)
const simForm = reactive({
  deviceCode: 'DEV_WATCH_001',
  protocol: 'STANDARD',
  battery: 88,
  heartRate: 75,
  spo2: 98,
  systolic: 125,
  diastolic: 80,
  respiration: 18,
  temperature: 36.6,
  inBed: true,
  sos: false,
  fallDetected: false
})

const handleSwitchToSimulator = () => {
  activeTab.value = 'telemetry'
}

const handleSimDeviceChange = (code: string) => {
  const dev = deviceOptions.value.find(d => d.deviceCode === code)
  if (dev && dev.vendor && dev.vendor.toUpperCase().includes('HUAWEI')) {
    simForm.protocol = 'HUAWEI_HEALTH'
  } else {
    simForm.protocol = 'STANDARD'
  }
}

const handleSendMockTelemetry = async () => {
  if (!simForm.deviceCode) {
    ElMessage.warning('请选择目标设备')
    return
  }
  simSubmitting.value = true
  try {
    const payload: DeviceReportDTO = {
      deviceCode: simForm.deviceCode,
      protocol: simForm.protocol,
      battery: simForm.battery,
      sos: simForm.sos,
      fallDetected: simForm.fallDetected,
      metrics: [
        { metricCode: 'HEART_RATE', metricValue: simForm.heartRate, unit: 'bpm' },
        { metricCode: 'SPO2', metricValue: simForm.spo2, unit: '%' },
        { metricCode: 'SBP', metricValue: simForm.systolic, unit: 'mmHg' },
        { metricCode: 'DBP', metricValue: simForm.diastolic, unit: 'mmHg' },
        { metricCode: 'RESPIRATORY_RATE', metricValue: simForm.respiration, unit: '次/分' },
        { metricCode: 'BODY_TEMP', metricValue: simForm.temperature, unit: '℃' },
        { metricCode: 'IN_BED_STATUS', metricValue: simForm.inBed ? 1 : 0, unit: '状态' },
        { metricCode: 'BATTERY', metricValue: simForm.battery, unit: '%' }
      ]
    }
    await reportTelemetry(payload)
    ElMessage.success('模拟遥测数据发射成功！适配器已完成指标规整与事件识别')
    loadTelemetryData()
    loadSummary()
    loadDevices()
    if (simForm.sos || simForm.fallDetected || simForm.battery < 15 || simForm.heartRate > 120) {
      loadEvents()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '模拟数据上报失败')
  } finally {
    simSubmitting.value = false
  }
}

// 遥测数据明细列表
const dataLoading = ref(false)
const telemetryList = ref<DeviceDataVO[]>([])
const dataTotal = ref(0)
const dataQuery = reactive<DeviceDataQueryDTO>({
  elderId: undefined,
  metricCode: '',
  pageNum: 1,
  pageSize: 10
})

const loadTelemetryData = async () => {
  dataLoading.value = true
  try {
    const res = await pageTelemetryData(dataQuery)
    if (res.data) {
      telemetryList.value = res.data.list
      dataTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载遥测数据失败', err)
  } finally {
    dataLoading.value = false
  }
}

const handleDataSearch = () => {
  dataQuery.pageNum = 1
  loadTelemetryData()
}

const handleDataReset = () => {
  dataQuery.elderId = undefined
  dataQuery.metricCode = ''
  dataQuery.pageNum = 1
  loadTelemetryData()
}

const formatJson = (jsonStr?: string) => {
  if (!jsonStr) return '{}'
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

/* ================= 3. 设备告警事件处置 ================= */
const eventLoading = ref(false)
const eventList = ref<DeviceEventVO[]>([])
const eventTotal = ref(0)
const eventQuery = reactive<DeviceEventQueryDTO>({
  eventType: '',
  severity: '',
  eventStatus: '',
  pageNum: 1,
  pageSize: 10
})

const loadEvents = async () => {
  eventLoading.value = true
  try {
    const res = await pageEvents(eventQuery)
    if (res.data) {
      eventList.value = res.data.list
      eventTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载事件失败', err)
  } finally {
    eventLoading.value = false
  }
}

const handleEventSearch = () => {
  eventQuery.pageNum = 1
  loadEvents()
}

const handleEventReset = () => {
  eventQuery.eventType = ''
  eventQuery.severity = ''
  eventQuery.eventStatus = ''
  eventQuery.pageNum = 1
  loadEvents()
}

// 事件处置弹窗
const handleDialogVisible = ref(false)
const handleSubmitting = ref(false)
const currentEvent = ref<DeviceEventVO | null>(null)
const handleForm = reactive<DeviceEventHandleDTO>({
  eventStatus: 'CLOSED',
  handleResult: ''
})

const handleOpenHandleEvent = (row: DeviceEventVO) => {
  currentEvent.value = row
  handleForm.eventStatus = 'CLOSED'
  handleForm.handleResult = row.handleResult || ''
  handleDialogVisible.value = true
}

const handleSubmitHandleEvent = async () => {
  if (!currentEvent.value) return
  if (!handleForm.handleResult) {
    ElMessage.warning('请填写现场处置结论与排查结果')
    return
  }
  handleSubmitting.value = true
  try {
    await handleEvent(currentEvent.value.id, handleForm)
    ElMessage.success('事件处置结论已保存记录')
    handleDialogVisible.value = false
    loadEvents()
    loadSummary()
  } catch (err: any) {
    ElMessage.error(err.message || '处置失败')
  } finally {
    handleSubmitting.value = false
  }
}

// 标签页切换响应
const handleTabChange = (tabName: string) => {
  if (tabName === 'devices') {
    loadDevices()
  } else if (tabName === 'telemetry') {
    loadTelemetryData()
  } else if (tabName === 'events') {
    loadEvents()
  }
}

// 辅助样式工具
const getDeviceStatusTag = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'ACTIVE': return 'success'
    case 'OFFLINE': return 'info'
    case 'FAULT': return 'danger'
    case 'INACTIVE': return 'warning'
    default: return 'info'
  }
}

const getEventTypeTag = (type: string) => {
  switch (type?.toUpperCase()) {
    case 'SOS': return 'danger'
    case 'FALL_DETECTED': return 'danger'
    case 'LOW_BATTERY': return 'warning'
    case 'ABNORMAL_METRIC': return 'danger'
    case 'LEAVE_BED_OVERTIME': return 'warning'
    default: return 'info'
  }
}

const getSeverityTag = (sev: string) => {
  switch (sev?.toUpperCase()) {
    case 'CRITICAL': return 'danger'
    case 'HIGH': return 'warning'
    case 'MEDIUM': return 'primary'
    default: return 'info'
  }
}

const getEventStatusTag = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'OPEN': return 'danger'
    case 'PROCESSING': return 'warning'
    case 'CLOSED': return 'success'
    default: return 'info'
  }
}

onMounted(() => {
  loadSummary()
  loadDevices()
  loadAuxOptions()
})
</script>

<style scoped>
.iot-page {
  padding: 16px;
  background-color: #f6f8fb;
  min-height: calc(100vh - 64px);
}

/* 顶部 KPI */
.kpi-row {
  margin-bottom: 16px;
}
.kpi-card {
  border-radius: 8px;
  border: 1px solid #eef2f8;
  transition: all 0.25s ease;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
}
.kpi-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  padding: 16px;
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
  flex-shrink: 0;
}
.total-dev .kpi-icon { background: #e8f3ff; color: #1677ff; }
.active-dev .kpi-icon { background: #e6f8ea; color: #52c41a; }
.bound-dev .kpi-icon { background: #f9f0ff; color: #722ed1; }
.unbound-dev .kpi-icon { background: #f0f5ff; color: #2f54eb; }
.open-event .kpi-icon { background: #fff1f0; color: #ff4d4f; }
.today-data .kpi-icon { background: #fffbe6; color: #faad14; }

.kpi-content {
  display: flex;
  flex-direction: column;
}
.kpi-title {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
}
.kpi-num {
  font-size: 20px;
  font-weight: 700;
  color: #262626;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
.primary-num { color: #1677ff; }
.success-num { color: #52c41a; }
.danger-num { color: #ff4d4f; }
.warning-num { color: #faad14; }
.purple-num { color: #722ed1; }

/* 主工作台卡片 */
.main-card {
  border-radius: 8px;
  border: 1px solid #eef2f8;
}
.tab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.tab-title {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
}
.tab-subtitle {
  font-size: 12px;
  color: #8c8c8c;
}

.filter-form {
  margin-bottom: 12px;
}

.device-cell-name {
  font-weight: 600;
  color: #1f1f1f;
  display: block;
}
.sub-model-tag {
  font-size: 12px;
  color: #8c8c8c;
}
.bound-elder-text {
  color: #1677ff;
}
.bound-bed-text {
  color: #595959;
  font-size: 13px;
}

.mono-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #595959;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 模拟发射台 */
.simulator-card {
  background: #fbfdff;
  border: 1px solid #d4e8ff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}
.sim-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.sim-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  color: #1677ff;
  gap: 8px;
}
.sim-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
}
.sim-tip {
  font-size: 12px;
  color: #8c8c8c;
}

/* 遥测展示 */
.metric-num {
  font-size: 15px;
  color: #1677ff;
}
.metric-unit {
  font-size: 12px;
  color: #8c8c8c;
}
.raw-json-box {
  background: #272822;
  color: #f8f8f2;
  padding: 10px;
  border-radius: 4px;
  font-size: 11px;
  max-height: 240px;
  overflow: auto;
}

/* 绑定对话框 */
.bind-summary-box {
  background: #f8fafc;
  border-radius: 6px;
  padding: 12px 16px;
  border-left: 4px solid #1677ff;
}
.summary-line {
  font-size: 13px;
  line-height: 24px;
  color: #595959;
}
.summary-line b {
  color: #1f1f1f;
}
.danger-text { color: #ff4d4f; }

/* 抽屉样式 */
.history-drawer-content {
  padding: 4px;
}
.dev-info-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f0f7ff;
  border-radius: 6px;
  margin-bottom: 12px;
}
.history-item-card {
  border: 1px solid #f0f0f0;
}
.hist-line {
  font-size: 13px;
  line-height: 22px;
  color: #595959;
}
.text-muted {
  color: #bfbfbf;
}
</style>
