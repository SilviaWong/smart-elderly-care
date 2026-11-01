import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

// 1. 健康指标
export interface HealthMetricVO {
  id: number
  orgId: number
  metricCode: string
  metricName: string
  unit?: string
  valueType: string
  description?: string
  status: string
}

export interface HealthMetricDTO {
  metricCode: string
  metricName: string
  unit?: string
  valueType?: string
  description?: string
  status?: string
}

// 2. 预警规则
export interface HealthAlertRuleVO {
  id: number
  orgId: number
  metricId: number
  metricCode?: string
  metricName?: string
  unit?: string
  ruleName: string
  conditionType: string
  minValue?: number
  maxValue?: number
  severity: string
  dedupMinutes: number
  enabled: number
  createdAt?: string
}

export interface HealthAlertRuleDTO {
  metricId: number
  ruleName: string
  conditionType: string
  minValue?: number
  maxValue?: number
  severity?: string
  dedupMinutes?: number
  enabled?: number
}

// 3. 健康记录
export interface HealthRecordVO {
  id: number
  orgId: number
  elderId: number
  elderName?: string
  elderNo?: string
  bedInfo?: string
  metricId: number
  metricCode?: string
  metricName?: string
  metricValue: number
  unitSnapshot?: string
  measuredAt: string
  sourceType: string
  recorderId?: number
  recorderName?: string
  remark?: string
  createdAt?: string
}

export interface HealthRecordCreateDTO {
  elderId: number
  metricId: number
  metricValue: number
  measuredAt: string
  sourceType?: string
  sourceDeviceId?: number
  remark?: string
}

export interface HealthRecordBatchCreateDTO {
  elderId: number
  measuredAt: string
  sourceType?: string
  sourceDeviceId?: number
  remark?: string
  items: Array<{
    metricId: number
    metricValue: number
  }>
}

export interface HealthRecordQueryDTO {
  elderId?: number
  elderName?: string
  metricId?: number
  sourceType?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface HealthTrendPointVO {
  id: number
  measuredAt: string
  metricValue: number
  unitSnapshot?: string
  sourceType: string
}

// 4. 健康预警
export interface HealthAlertVO {
  id: number
  orgId: number
  alertNo: string
  elderId: number
  elderName?: string
  elderNo?: string
  bedInfo?: string
  healthRecordId: number
  metricId?: number
  metricCode?: string
  metricName?: string
  unit?: string
  ruleId: number
  ruleName?: string
  conditionType?: string
  thresholdMin?: number
  thresholdMax?: number
  severity: string
  alertValue: number
  status: string
  assigneeId?: number
  assigneeName?: string
  handledAt?: string
  handleResult?: string
  createdAt: string
}

export interface HealthAlertQueryDTO {
  alertNo?: string
  elderId?: number
  elderName?: string
  metricId?: number
  ruleId?: number
  severity?: string
  status?: string
  assigneeId?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface HealthAlertSummaryVO {
  totalAlerts: number
  openCount: number
  processingCount: number
  resolvedCount: number
  ignoredCount: number
  criticalCount: number
}

export interface HealthAlertAssignDTO {
  assigneeId: number
}

export interface HealthAlertHandleDTO {
  status: 'RESOLVED' | 'IGNORED'
  handleResult: string
}

// 接口方法定义

// 指标配置
export function getHealthMetrics(status?: string): Promise<ApiResponse<HealthMetricVO[]>> {
  return request.get('/health/metrics', { params: { status } })
}

export function createHealthMetric(data: HealthMetricDTO): Promise<ApiResponse<number>> {
  return request.post('/health/metrics', data)
}

export function updateHealthMetric(id: number, data: HealthMetricDTO): Promise<ApiResponse<void>> {
  return request.put(`/health/metrics/${id}`, data)
}

export function toggleHealthMetricStatus(id: number, status: string): Promise<ApiResponse<void>> {
  return request.put(`/health/metrics/${id}/status`, null, { params: { status } })
}

// 预警规则
export function getHealthAlertRules(metricId?: number, enabled?: number): Promise<ApiResponse<HealthAlertRuleVO[]>> {
  return request.get('/health/alert-rules', { params: { metricId, enabled } })
}

export function createHealthAlertRule(data: HealthAlertRuleDTO): Promise<ApiResponse<number>> {
  return request.post('/health/alert-rules', data)
}

export function updateHealthAlertRule(id: number, data: HealthAlertRuleDTO): Promise<ApiResponse<void>> {
  return request.put(`/health/alert-rules/${id}`, data)
}

export function toggleHealthAlertRule(id: number, enabled: number): Promise<ApiResponse<void>> {
  return request.put(`/health/alert-rules/${id}/enabled`, null, { params: { enabled } })
}

// 健康体征记录
export function getHealthRecordPage(params: HealthRecordQueryDTO): Promise<ApiResponse<PageResult<HealthRecordVO>>> {
  return request.get('/health/records', { params })
}

export function getHealthTrend(elderId: number, metricId: number, days?: number): Promise<ApiResponse<HealthTrendPointVO[]>> {
  return request.get('/health/records/trend', { params: { elderId, metricId, days } })
}

export function createHealthRecord(data: HealthRecordCreateDTO): Promise<ApiResponse<number>> {
  return request.post('/health/records', data)
}

export function batchCreateHealthRecords(data: HealthRecordBatchCreateDTO): Promise<ApiResponse<number[]>> {
  return request.post('/health/records/batch', data)
}

// 健康预警
export function getHealthAlertPage(params: HealthAlertQueryDTO): Promise<ApiResponse<PageResult<HealthAlertVO>>> {
  return request.get('/health/alerts', { params })
}

export function getHealthAlertSummary(): Promise<ApiResponse<HealthAlertSummaryVO>> {
  return request.get('/health/alerts/summary')
}

export function getHealthAlertDetail(id: number): Promise<ApiResponse<HealthAlertVO>> {
  return request.get(`/health/alerts/${id}`)
}

export function assignHealthAlert(id: number, data: HealthAlertAssignDTO): Promise<ApiResponse<void>> {
  return request.post(`/health/alerts/${id}/assign`, data)
}

export function handleHealthAlert(id: number, data: HealthAlertHandleDTO): Promise<ApiResponse<void>> {
  return request.post(`/health/alerts/${id}/handle`, data)
}
