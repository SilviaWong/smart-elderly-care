import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

// 风险事件视图对象
export interface RiskEventVO {
  id: number
  orgId: number
  eventNo: string
  elderId?: number
  elderName?: string
  elderNo?: string
  bedInfo?: string
  eventType: string
  eventTypeName: string
  severity: string
  severityName: string
  occurredAt: string
  locationDesc?: string
  description: string
  reporterId?: number
  reporterName?: string
  assigneeId?: number
  assigneeName?: string
  status: string
  statusName: string
  conclusion?: string
  closedAt?: string
  createdAt: string
  requiresReview: boolean
}

// 风险事件轨迹流转日志视图对象
export interface RiskEventLogVO {
  id: number
  riskEventId: number
  action: string
  actionName: string
  fromStatus?: string
  fromStatusName?: string
  toStatus?: string
  toStatusName?: string
  operatorId: number
  operatorName: string
  content: string
  createdAt: string
}

// 风险事件详情（包含事件基本信息与流转日志时间线）
export interface RiskEventDetailVO {
  event: RiskEventVO
  logs: RiskEventLogVO[]
}

// 工作台统计看板数据
export interface RiskEventSummaryVO {
  totalCount: number
  reportedCount: number
  assignedCount: number
  processingCount: number
  pendingReviewCount: number
  closedCount: number
  highCriticalCount: number
}

// 上报登记 DTO
export interface RiskEventReportDTO {
  elderId?: number
  eventType: string
  severity: string
  occurredAt: string
  locationDesc?: string
  description: string
  assigneeId?: number
}

// 指派责任人 DTO
export interface RiskEventAssignDTO {
  assigneeId: number
  remark?: string
}

// 处置登记 / 进展记录 DTO
export interface RiskEventProcessDTO {
  content: string
  action?: string
}

// 提交复核 DTO
export interface RiskEventSubmitReviewDTO {
  content: string
}

// 主管复核 DTO
export interface RiskEventReviewDTO {
  approved: boolean
  conclusion?: string
  rejectReason?: string
}

// 结案关闭 DTO
export interface RiskEventCloseDTO {
  conclusion: string
}

// 列表查询条件 DTO
export interface RiskEventQueryDTO {
  pageNum?: number
  pageSize?: number
  keyword?: string
  eventType?: string
  severity?: string
  status?: string
  elderId?: number
  assigneeId?: number
  reporterId?: number
  startTime?: string
  endTime?: string
}

// 接口方法定义

/**
 * 分页查询风险事件
 */
export function getRiskEventPage(params: RiskEventQueryDTO): Promise<ApiResponse<PageResult<RiskEventVO>>> {
  return request({
    url: '/risk-events',
    method: 'get',
    params
  })
}

/**
 * 获取风险事件看板汇总统计
 */
export function getRiskEventSummary(): Promise<ApiResponse<RiskEventSummaryVO>> {
  return request({
    url: '/risk-events/summary',
    method: 'get'
  })
}

/**
 * 获取风险事件详情与日志轨迹时间线
 */
export function getRiskEventDetail(id: number): Promise<ApiResponse<RiskEventDetailVO>> {
  return request({
    url: `/risk-events/${id}`,
    method: 'get'
  })
}

/**
 * 登记上报安全风险事件
 */
export function reportRiskEvent(data: RiskEventReportDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/risk-events',
    method: 'post',
    data
  })
}

/**
 * 指派处理负责人
 */
export function assignRiskEvent(id: number, data: RiskEventAssignDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/risk-events/${id}/assign`,
    method: 'put',
    data
  })
}

/**
 * 接单开始处置
 */
export function startProcessRiskEvent(id: number, data: RiskEventProcessDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/risk-events/${id}/start-process`,
    method: 'put',
    data
  })
}

/**
 * 记录处置进展
 */
export function updateRiskEventProgress(id: number, data: RiskEventProcessDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/risk-events/${id}/progress`,
    method: 'post',
    data
  })
}

/**
 * 提交主管复核申请
 */
export function submitRiskEventReview(id: number, data: RiskEventSubmitReviewDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/risk-events/${id}/submit-review`,
    method: 'put',
    data
  })
}

/**
 * 主管审核复核（通过或驳回）
 */
export function reviewRiskEvent(id: number, data: RiskEventReviewDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/risk-events/${id}/review`,
    method: 'put',
    data
  })
}

/**
 * 结案关闭风险事件
 */
export function closeRiskEvent(id: number, data: RiskEventCloseDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/risk-events/${id}/close`,
    method: 'put',
    data
  })
}
