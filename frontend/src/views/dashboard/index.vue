<template>
  <div class="dashboard-container">
    <!-- 顶部欢迎与运营概览卡片 -->
    <el-card class="welcome-header-card" shadow="never">
      <div class="welcome-wrapper">
        <div class="user-greeting">
          <div class="avatar-box">
            <el-icon class="avatar-icon"><User /></el-icon>
          </div>
          <div class="greeting-content">
            <div class="greeting-title">
              <h2>欢迎回来，{{ userStore.userInfo?.realName || '运营管理员' }}</h2>
              <el-tag type="success" effect="light" round size="small" class="role-tag">
                {{ userStore.userInfo?.role || '机构主管' }}
              </el-tag>
            </div>
            <p class="greeting-subtitle">
              智慧养老综合运营管理平台 · 机构实时全景数据监控与运营调度
            </p>
          </div>
        </div>
        <div class="header-actions">
          <div class="sync-time" v-if="lastUpdated">
            <el-icon><Clock /></el-icon>
            <span>数据更新于：{{ lastUpdated }}</span>
          </div>
          <el-button type="primary" plain :icon="Refresh" :loading="loading" @click="fetchStats">
            刷新大屏
          </el-button>
          <el-button type="primary" :icon="TrendCharts" @click="router.push('/report')">
            报表导出中心
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 核心运营 KPI 指标网格 (8项核心指标) -->
    <div class="kpi-grid">
      <!-- 1. 在住长者 -->
      <el-card shadow="hover" class="kpi-card border-blue" :body-style="{ padding: '18px 20px' }">
        <div class="kpi-inner">
          <div class="kpi-icon-wrap bg-blue">
            <el-icon><User /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">在住在册长者</span>
            <div class="kpi-value">
              <span class="number text-blue">{{ stats.activeElderCount ?? 0 }}</span>
              <span class="unit">位</span>
            </div>
            <div class="kpi-footer">
              <span>在册总数</span>
              <el-link type="primary" :underline="false" @click="router.push('/elder')">查看长者档案 &gt;</el-link>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 2. 床位占用率 -->
      <el-card shadow="hover" class="kpi-card border-green" :body-style="{ padding: '18px 20px' }">
        <div class="kpi-inner">
          <div class="kpi-icon-wrap bg-green">
            <el-icon><HomeFilled /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">床位占用率</span>
            <div class="kpi-value">
              <span class="number text-green">{{ stats.bedOccupancyRate ?? 0 }}</span>
              <span class="unit">%</span>
            </div>
            <div class="kpi-progress">
              <el-progress
                :percentage="Number(stats.bedOccupancyRate) || 0"
                :stroke-width="6"
                :show-text="false"
                status="success"
              />
            </div>
            <div class="kpi-footer">
              <span>占用 {{ stats.occupiedBedCount ?? 0 }} / 总 {{ stats.totalBedCount ?? 0 }} 张</span>
              <el-link type="success" :underline="false" @click="router.push('/admission')">床位调度 &gt;</el-link>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 3. 待派工单 -->
      <el-card shadow="hover" class="kpi-card border-orange" :body-style="{ padding: '18px 20px' }">
        <div class="kpi-inner">
          <div class="kpi-icon-wrap bg-orange">
            <el-icon><Service /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">待处理服务工单</span>
            <div class="kpi-value">
              <span class="number text-orange">{{ stats.pendingOrderCount ?? 0 }}</span>
              <span class="unit">单</span>
            </div>
            <div class="kpi-footer">
              <span>待派工或待执行</span>
              <el-link type="warning" :underline="false" @click="router.push('/serviceorder')">工单派发 &gt;</el-link>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 4. 今日护理完成率 -->
      <el-card shadow="hover" class="kpi-card border-purple" :body-style="{ padding: '18px 20px' }">
        <div class="kpi-inner">
          <div class="kpi-icon-wrap bg-purple">
            <el-icon><Finished /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">今日护理任务完成率</span>
            <div class="kpi-value">
              <span class="number text-purple">{{ stats.careCompletionRate ?? 0 }}</span>
              <span class="unit">%</span>
            </div>
            <div class="kpi-progress">
              <el-progress
                :percentage="Number(stats.careCompletionRate) || 0"
                :stroke-width="6"
                :show-text="false"
                color="#8b5cf6"
              />
            </div>
            <div class="kpi-footer">
              <span>按护理计划执行</span>
              <el-link type="primary" :underline="false" @click="router.push('/care')">护理排班 &gt;</el-link>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 5. 健康体征预警 -->
      <el-card shadow="hover" class="kpi-card border-red" :body-style="{ padding: '18px 20px' }">
        <div class="kpi-inner">
          <div class="kpi-icon-wrap bg-red">
            <el-icon><FirstAidKit /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">待处置健康预警</span>
            <div class="kpi-value">
              <span class="number text-red">{{ stats.healthAlertCount ?? 0 }}</span>
              <span class="unit">条</span>
            </div>
            <div class="kpi-footer">
              <span>生命体征越限报警</span>
              <el-link type="danger" :underline="false" @click="router.push('/health')">健康看板 &gt;</el-link>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 6. 当月财务收款 -->
      <el-card shadow="hover" class="kpi-card border-emerald" :body-style="{ padding: '18px 20px' }">
        <div class="kpi-inner">
          <div class="kpi-icon-wrap bg-emerald">
            <el-icon><Money /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">当月财务收款</span>
            <div class="kpi-value">
              <span class="unit-prefix">¥</span>
              <span class="number text-emerald">{{ formatCurrency(stats.currentMonthRevenue) }}</span>
            </div>
            <div class="kpi-footer">
              <span>本自然月实收账款</span>
              <el-link type="success" :underline="false" @click="router.push('/finance')">账单流水 &gt;</el-link>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 7. 安全风险事件 -->
      <el-card shadow="hover" class="kpi-card border-amber" :body-style="{ padding: '18px 20px' }">
        <div class="kpi-inner">
          <div class="kpi-icon-wrap bg-amber">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">待处置安全风险</span>
            <div class="kpi-value">
              <span class="number text-amber">{{ stats.openRiskEventCount ?? 0 }}</span>
              <span class="unit">起</span>
            </div>
            <div class="kpi-footer">
              <span>防跌倒/走失/火险</span>
              <el-link type="warning" :underline="false" @click="router.push('/risk')">风险处置 &gt;</el-link>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 8. IoT 物联设备遥测点 -->
      <el-card shadow="hover" class="kpi-card border-cyan" :body-style="{ padding: '18px 20px' }">
        <div class="kpi-inner">
          <div class="kpi-icon-wrap bg-cyan">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">今日IoT遥测点</span>
            <div class="kpi-value">
              <span class="number text-cyan">{{ stats.todayTelemetryCount ?? 0 }}</span>
              <span class="unit">次</span>
            </div>
            <div class="kpi-footer">
              <span>智能床垫/手环/SOS</span>
              <el-link type="primary" :underline="false" @click="router.push('/iot')">物联网中心 &gt;</el-link>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 运营态势图表与风险监控分析 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 近7日长者流转与护理趋势 (双图表选项卡) -->
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="chart-header">
              <div class="chart-title-group">
                <el-icon class="title-icon"><TrendCharts /></el-icon>
                <span class="title-text">近 7 日业务动态与履约趋势</span>
              </div>
              <el-radio-group v-model="activeTrendTab" size="small">
                <el-radio-button value="admission">长者入住/退住流转</el-radio-button>
                <el-radio-button value="care">护理计划执行履约</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <!-- 1. 入住/退住流转柱状分析 -->
          <div v-if="activeTrendTab === 'admission'" class="trend-chart-container">
            <div class="chart-legend">
              <span class="legend-item"><span class="legend-dot in"></span>新办理入住</span>
              <span class="legend-item"><span class="legend-dot out"></span>已办理退住</span>
            </div>
            <div class="bar-chart-wrapper">
              <div
                v-for="item in stats.admissionTrend"
                :key="item.date"
                class="bar-column-group"
              >
                <div class="bar-pair">
                  <!-- 入住柱 -->
                  <el-tooltip :content="`${item.date} 入住: ${item.checkInCount} 人`" placement="top">
                    <div
                      class="bar-item bar-in"
                      :style="{ height: getBarHeight(item.checkInCount, maxAdmissionCount) }"
                    >
                      <span class="bar-val" v-if="item.checkInCount > 0">{{ item.checkInCount }}</span>
                    </div>
                  </el-tooltip>
                  <!-- 退住柱 -->
                  <el-tooltip :content="`${item.date} 退住: ${item.dischargeCount} 人`" placement="top">
                    <div
                      class="bar-item bar-out"
                      :style="{ height: getBarHeight(item.dischargeCount, maxAdmissionCount) }"
                    >
                      <span class="bar-val" v-if="item.dischargeCount > 0">{{ item.dischargeCount }}</span>
                    </div>
                  </el-tooltip>
                </div>
                <div class="column-date">{{ formatDateLabel(item.date) }}</div>
              </div>
            </div>
          </div>

          <!-- 2. 护理任务执行对比分析 -->
          <div v-else class="trend-chart-container">
            <div class="chart-legend">
              <span class="legend-item"><span class="legend-dot care-plan"></span>计划任务数</span>
              <span class="legend-item"><span class="legend-dot care-done"></span>已完成任务数</span>
            </div>
            <div class="bar-chart-wrapper">
              <div
                v-for="item in stats.careTaskTrend"
                :key="item.date"
                class="bar-column-group"
              >
                <div class="bar-pair">
                  <!-- 计划总数 -->
                  <el-tooltip :content="`${item.date} 计划: ${item.totalCount} 项`" placement="top">
                    <div
                      class="bar-item bar-plan"
                      :style="{ height: getBarHeight(item.totalCount, maxCareCount) }"
                    >
                      <span class="bar-val" v-if="item.totalCount > 0">{{ item.totalCount }}</span>
                    </div>
                  </el-tooltip>
                  <!-- 完成数 -->
                  <el-tooltip :content="`${item.date} 已完成: ${item.completedCount} 项`" placement="top">
                    <div
                      class="bar-item bar-done"
                      :style="{ height: getBarHeight(item.completedCount, maxCareCount) }"
                    >
                      <span class="bar-val" v-if="item.completedCount > 0">{{ item.completedCount }}</span>
                    </div>
                  </el-tooltip>
                </div>
                <div class="column-date">{{ formatDateLabel(item.date) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 安全风险事件等级分布与预警快报 -->
      <el-col :xs="24" :lg="8">
        <el-card class="chart-card risk-distribution-card" shadow="never">
          <template #header>
            <div class="chart-header">
              <div class="chart-title-group">
                <el-icon class="title-icon text-amber"><WarningFilled /></el-icon>
                <span class="title-text">安全风险事件待处置分布</span>
              </div>
              <el-tag size="small" type="danger" effect="plain" v-if="stats.openRiskEventCount > 0">
                {{ stats.openRiskEventCount }} 起待处置
              </el-tag>
            </div>
          </template>

          <div class="risk-distribution-body">
            <div class="risk-level-list">
              <div class="risk-level-item">
                <div class="level-meta">
                  <span class="level-name text-red">极高风险 (CRITICAL)</span>
                  <span class="level-count">{{ stats.riskDistribution?.CRITICAL ?? 0 }} 起</span>
                </div>
                <el-progress
                  :percentage="getRiskPercentage('CRITICAL')"
                  :color="'#ef4444'"
                  :stroke-width="8"
                />
              </div>

              <div class="risk-level-item">
                <div class="level-meta">
                  <span class="level-name text-orange">高风险 (HIGH)</span>
                  <span class="level-count">{{ stats.riskDistribution?.HIGH ?? 0 }} 起</span>
                </div>
                <el-progress
                  :percentage="getRiskPercentage('HIGH')"
                  :color="'#f97316'"
                  :stroke-width="8"
                />
              </div>

              <div class="risk-level-item">
                <div class="level-meta">
                  <span class="level-name text-amber">中风险 (MEDIUM)</span>
                  <span class="level-count">{{ stats.riskDistribution?.MEDIUM ?? 0 }} 起</span>
                </div>
                <el-progress
                  :percentage="getRiskPercentage('MEDIUM')"
                  :color="'#f59e0b'"
                  :stroke-width="8"
                />
              </div>

              <div class="risk-level-item">
                <div class="level-meta">
                  <span class="level-name text-blue">低风险 (LOW)</span>
                  <span class="level-count">{{ stats.riskDistribution?.LOW ?? 0 }} 起</span>
                </div>
                <el-progress
                  :percentage="getRiskPercentage('LOW')"
                  :color="'#3b82f6'"
                  :stroke-width="8"
                />
              </div>
            </div>

            <!-- 快捷处理通道 -->
            <div class="quick-risk-actions">
              <el-alert
                v-if="stats.openRiskEventCount > 0"
                title="存在未闭环处置的安全风险，请责任护工及时干预"
                type="warning"
                :closable="false"
                show-icon
              />
              <el-alert
                v-else
                title="当前暂无未关闭安全隐患，运行环境良好"
                type="success"
                :closable="false"
                show-icon
              />
              <div class="action-btn-row">
                <el-button type="warning" plain class="w-100" @click="router.push('/risk')">
                  进入安全风险应急管理
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 业务快捷导航直通车 -->
    <el-card class="quick-nav-card" shadow="never">
      <template #header>
        <div class="quick-nav-header">
          <span class="section-title">核心业务快捷直通工作台</span>
          <span class="section-subtitle">点击直达对应业务管理页面</span>
        </div>
      </template>
      <div class="quick-nav-grid">
        <div class="nav-item" @click="router.push('/elder')">
          <div class="nav-icon bg-blue"><el-icon><User /></el-icon></div>
          <span class="nav-title">长者全景档案</span>
          <span class="nav-desc">入档/健康评估</span>
        </div>
        <div class="nav-item" @click="router.push('/admission')">
          <div class="nav-icon bg-green"><el-icon><HomeFilled /></el-icon></div>
          <span class="nav-title">入住与床位</span>
          <span class="nav-desc">办理入住/转床</span>
        </div>
        <div class="nav-item" @click="router.push('/serviceorder')">
          <div class="nav-icon bg-orange"><el-icon><Service /></el-icon></div>
          <span class="nav-title">服务工单调度</span>
          <span class="nav-desc">派单/确认/完成</span>
        </div>
        <div class="nav-item" @click="router.push('/health')">
          <div class="nav-icon bg-red"><el-icon><FirstAidKit /></el-icon></div>
          <span class="nav-title">健康监测预警</span>
          <span class="nav-desc">体征越限/指标</span>
        </div>
        <div class="nav-item" @click="router.push('/care')">
          <div class="nav-icon bg-purple"><el-icon><Finished /></el-icon></div>
          <span class="nav-title">护理计划任务</span>
          <span class="nav-desc">等级护理/日任务</span>
        </div>
        <div class="nav-item" @click="router.push('/dining')">
          <div class="nav-icon bg-amber"><el-icon><Bowl /></el-icon></div>
          <span class="nav-title">餐饮与订餐</span>
          <span class="nav-desc">营养餐单/配餐</span>
        </div>
        <div class="nav-item" @click="router.push('/finance')">
          <div class="nav-icon bg-emerald"><el-icon><Money /></el-icon></div>
          <span class="nav-title">财务与收费</span>
          <span class="nav-desc">账单/缴费/退费</span>
        </div>
        <div class="nav-item" @click="router.push('/iot')">
          <div class="nav-icon bg-cyan"><el-icon><Cpu /></el-icon></div>
          <span class="nav-title">物联设备接入</span>
          <span class="nav-desc">床垫/手环/遥测</span>
        </div>
        <div class="nav-item highlight-nav" @click="router.push('/report')">
          <div class="nav-icon bg-indigo"><el-icon><TrendCharts /></el-icon></div>
          <span class="nav-title">综合报表中心</span>
          <span class="nav-desc">多维统计/异步导出</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import {
  User,
  HomeFilled,
  Service,
  Finished,
  FirstAidKit,
  Money,
  WarningFilled,
  Cpu,
  TrendCharts,
  Refresh,
  Clock,
  Bowl
} from '@element-plus/icons-vue'
import { getDashboardStats, DashboardStatsVO } from '@/api/dashboard'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const lastUpdated = ref('')
const activeTrendTab = ref<'admission' | 'care'>('admission')

const stats = reactive<DashboardStatsVO>({
  activeElderCount: 0,
  totalBedCount: 0,
  occupiedBedCount: 0,
  bedOccupancyRate: 0,
  pendingOrderCount: 0,
  careCompletionRate: 0,
  healthAlertCount: 0,
  currentMonthRevenue: 0,
  openRiskEventCount: 0,
  todayTelemetryCount: 0,
  admissionTrend: [],
  careTaskTrend: [],
  riskDistribution: {}
})

// 计算最大入住数用于柱状高度归一化
const maxAdmissionCount = computed(() => {
  let max = 5
  if (!stats.admissionTrend || stats.admissionTrend.length === 0) return max
  for (const item of stats.admissionTrend) {
    if (item.checkInCount > max) max = item.checkInCount
    if (item.dischargeCount > max) max = item.dischargeCount
  }
  return max
})

// 计算最大护理任务数用于柱状高度归一化
const maxCareCount = computed(() => {
  let max = 10
  if (!stats.careTaskTrend || stats.careTaskTrend.length === 0) return max
  for (const item of stats.careTaskTrend) {
    if (item.totalCount > max) max = item.totalCount
  }
  return max
})

function getBarHeight(val: number, max: number): string {
  if (!val || val <= 0) return '4px'
  const percentage = Math.min(100, Math.max(8, Math.round((val / max) * 100)))
  return `${percentage}%`
}

function formatDateLabel(dateStr: string): string {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  if (parts.length === 3) {
    return `${parts[1]}/${parts[2]}`
  }
  return dateStr
}

function formatCurrency(val?: number): string {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getRiskPercentage(level: string): number {
  const total = Number(stats.openRiskEventCount) || 0
  if (total <= 0) return 0
  const count = stats.riskDistribution?.[level] || 0
  return Math.min(100, Math.round((count / total) * 100))
}

async function fetchStats() {
  loading.value = true
  try {
    const res = await getDashboardStats()
    if (res.data) {
      Object.assign(stats, res.data)
      const now = new Date()
      lastUpdated.value = now.toLocaleTimeString('zh-CN', { hour12: false })
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取运营看板统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// 顶部卡片
.welcome-header-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);

  .welcome-wrapper {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    .user-greeting {
      display: flex;
      align-items: center;
      gap: 16px;

      .avatar-box {
        width: 52px;
        height: 52px;
        border-radius: 14px;
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-size: 24px;
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
      }

      .greeting-content {
        .greeting-title {
          display: flex;
          align-items: center;
          gap: 10px;

          h2 {
            font-size: 20px;
            font-weight: 700;
            color: #0f172a;
            margin: 0;
          }

          .role-tag {
            font-weight: 600;
          }
        }

        .greeting-subtitle {
          font-size: 13px;
          color: #64748b;
          margin: 6px 0 0 0;
        }
      }
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 14px;

      .sync-time {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: #94a3b8;
      }
    }
  }
}

// KPI 网格
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;

  .kpi-card {
    border-radius: 12px;
    border: 1px solid #e2e8f0;
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
    }

    &.border-blue { border-top: 3px solid #3b82f6; }
    &.border-green { border-top: 3px solid #10b981; }
    &.border-orange { border-top: 3px solid #f97316; }
    &.border-purple { border-top: 3px solid #8b5cf6; }
    &.border-red { border-top: 3px solid #ef4444; }
    &.border-emerald { border-top: 3px solid #059669; }
    &.border-amber { border-top: 3px solid #f59e0b; }
    &.border-cyan { border-top: 3px solid #06b6d4; }

    .kpi-inner {
      display: flex;
      gap: 14px;

      .kpi-icon-wrap {
        width: 46px;
        height: 46px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-size: 22px;
        flex-shrink: 0;

        &.bg-blue { background: linear-gradient(135deg, #3b82f6, #2563eb); }
        &.bg-green { background: linear-gradient(135deg, #10b981, #059669); }
        &.bg-orange { background: linear-gradient(135deg, #f97316, #ea580c); }
        &.bg-purple { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
        &.bg-red { background: linear-gradient(135deg, #ef4444, #dc2626); }
        &.bg-emerald { background: linear-gradient(135deg, #10b981, #047857); }
        &.bg-amber { background: linear-gradient(135deg, #f59e0b, #d97706); }
        &.bg-cyan { background: linear-gradient(135deg, #06b6d4, #0891b2); }
      }

      .kpi-info {
        flex: 1;
        min-width: 0;

        .kpi-label {
          font-size: 13px;
          color: #64748b;
          font-weight: 500;
        }

        .kpi-value {
          display: flex;
          align-items: baseline;
          gap: 4px;
          margin: 4px 0;

          .unit-prefix {
            font-size: 18px;
            font-weight: 700;
            color: #059669;
          }

          .number {
            font-size: 26px;
            font-weight: 800;
            letter-spacing: -0.5px;
          }

          .unit {
            font-size: 12px;
            color: #94a3b8;
          }

          .text-blue { color: #2563eb; }
          .text-green { color: #10b981; }
          .text-orange { color: #f97316; }
          .text-purple { color: #8b5cf6; }
          .text-red { color: #ef4444; }
          .text-emerald { color: #059669; }
          .text-amber { color: #d97706; }
          .text-cyan { color: #0891b2; }
        }

        .kpi-progress {
          margin: 6px 0;
        }

        .kpi-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 12px;
          color: #94a3b8;
          margin-top: 6px;
        }
      }
    }
  }
}

// 态势图表行
.charts-row {
  .chart-card {
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    min-height: 380px;

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .chart-title-group {
        display: flex;
        align-items: center;
        gap: 8px;

        .title-icon {
          font-size: 18px;
          color: #3b82f6;
        }

        .title-text {
          font-size: 15px;
          font-weight: 700;
          color: #1e293b;
        }
      }
    }
  }
}

// 趋势柱状图
.trend-chart-container {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .chart-legend {
    display: flex;
    justify-content: flex-end;
    gap: 18px;
    font-size: 12px;
    color: #64748b;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .legend-dot {
        width: 10px;
        height: 10px;
        border-radius: 2px;

        &.in { background-color: #3b82f6; }
        &.out { background-color: #94a3b8; }
        &.care-plan { background-color: #e2e8f0; border: 1px solid #cbd5e1; }
        &.care-done { background-color: #8b5cf6; }
      }
    }
  }

  .bar-chart-wrapper {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    height: 240px;
    padding: 10px 10px 0 10px;
    border-bottom: 1px solid #f1f5f9;

    .bar-column-group {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      height: 100%;
      justify-content: flex-end;

      .bar-pair {
        display: flex;
        align-items: flex-end;
        gap: 6px;
        height: 190px;

        .bar-item {
          width: 22px;
          border-radius: 4px 4px 0 0;
          transition: all 0.3s ease;
          position: relative;
          display: flex;
          justify-content: center;

          &:hover {
            filter: brightness(1.1);
          }

          .bar-val {
            position: absolute;
            top: -20px;
            font-size: 11px;
            color: #64748b;
            font-weight: 600;
          }

          &.bar-in {
            background: linear-gradient(180deg, #60a5fa 0%, #2563eb 100%);
          }

          &.bar-out {
            background: linear-gradient(180deg, #cbd5e1 0%, #94a3b8 100%);
          }

          &.bar-plan {
            background: linear-gradient(180deg, #e2e8f0 0%, #cbd5e1 100%);
          }

          &.bar-done {
            background: linear-gradient(180deg, #a78bfa 0%, #7c3aed 100%);
          }
        }
      }

      .column-date {
        font-size: 12px;
        color: #94a3b8;
      }
    }
  }
}

// 风险分布
.risk-distribution-card {
  .risk-distribution-body {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .risk-level-list {
      display: flex;
      flex-direction: column;
      gap: 14px;

      .risk-level-item {
        .level-meta {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 6px;
          font-size: 13px;

          .level-name {
            font-weight: 600;
          }

          .level-count {
            font-weight: 700;
            color: #334155;
          }
        }
      }
    }

    .quick-risk-actions {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: 8px;

      .w-100 {
        width: 100%;
      }
    }
  }
}

// 业务直通车
.quick-nav-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;

  .quick-nav-header {
    display: flex;
    align-items: center;
    gap: 10px;

    .section-title {
      font-size: 15px;
      font-weight: 700;
      color: #1e293b;
    }

    .section-subtitle {
      font-size: 13px;
      color: #94a3b8;
    }
  }

  .quick-nav-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 16px;

    .nav-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px 10px;
      border-radius: 10px;
      background: #f8fafc;
      border: 1px solid #f1f5f9;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #ffffff;
        border-color: #cbd5e1;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
        transform: translateY(-2px);
      }

      &.highlight-nav {
        background: #f5f3ff;
        border-color: #ddd6fe;

        &:hover {
          background: #ede9fe;
        }
      }

      .nav-icon {
        width: 42px;
        height: 42px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-size: 20px;
        margin-bottom: 8px;

        &.bg-blue { background: #3b82f6; }
        &.bg-green { background: #10b981; }
        &.bg-orange { background: #f97316; }
        &.bg-red { background: #ef4444; }
        &.bg-purple { background: #8b5cf6; }
        &.bg-amber { background: #f59e0b; }
        &.bg-emerald { background: #059669; }
        &.bg-cyan { background: #06b6d4; }
        &.bg-indigo { background: #6366f1; }
      }

      .nav-title {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
        margin-bottom: 2px;
      }

      .nav-desc {
        font-size: 11px;
        color: #94a3b8;
      }
    }
  }
}
</style>
