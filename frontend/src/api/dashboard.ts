import request, { ApiResponse } from '@/utils/request'

export interface DailyTrendItem {
  date: string
  checkInCount: number
  dischargeCount: number
}

export interface CareTaskTrendItem {
  date: string
  totalCount: number
  completedCount: number
}

export interface DashboardStatsVO {
  activeElderCount: number
  totalBedCount: number
  occupiedBedCount: number
  bedOccupancyRate: number
  pendingOrderCount: number
  careCompletionRate: number
  healthAlertCount: number
  currentMonthRevenue: number
  openRiskEventCount: number
  todayTelemetryCount: number
  admissionTrend: DailyTrendItem[]
  careTaskTrend: CareTaskTrendItem[]
  riskDistribution: Record<string, number>
}

/**
 * 获取全景运营看板综合指标
 */
export function getDashboardStats(): Promise<ApiResponse<DashboardStatsVO>> {
  return request.get('/dashboard/stats')
}
