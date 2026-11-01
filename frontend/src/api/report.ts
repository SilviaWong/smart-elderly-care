import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface ReportColumnDef {
  prop: string
  label: string
  width?: number
  align?: string
}

export interface BusinessReportResultVO {
  reportType: string
  reportTypeName: string
  columns: ReportColumnDef[]
  data: PageResult<Record<string, any>>
}

export interface ReportQueryDTO {
  reportType: string
  startDate?: string
  endDate?: string
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface ExportTaskCreateDTO {
  reportType: string
  filterJson?: string
  customFileName?: string
}

export interface ExportTaskQueryDTO {
  reportType?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

export interface ReportExportTaskVO {
  id: number
  orgId: number
  taskNo: string
  reportType: string
  reportTypeName: string
  filterJson?: string
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED'
  statusName: string
  fileUrl?: string
  fileName?: string
  totalRows?: number
  errorMessage?: string
  createdBy?: number
  createdByName?: string
  startedAt?: string
  finishedAt?: string
  expireAt?: string
  isExpired: boolean
  createdAt: string
}

/**
 * 查询业务报表数据
 */
export function queryBusinessReport(params: ReportQueryDTO): Promise<ApiResponse<BusinessReportResultVO>> {
  return request.get('/reports/business-data', { params })
}

/**
 * 提交异步报表导出任务
 */
export function createExportTask(data: ExportTaskCreateDTO): Promise<ApiResponse<number>> {
  return request.post('/reports/export-tasks', data)
}

/**
 * 分页查询导出任务列表
 */
export function pageExportTasks(params: ExportTaskQueryDTO): Promise<ApiResponse<PageResult<ReportExportTaskVO>>> {
  return request.get('/reports/export-tasks', { params })
}

/**
 * 获取指定导出任务详情
 */
export function getExportTaskDetail(id: number): Promise<ApiResponse<ReportExportTaskVO>> {
  return request.get(`/reports/export-tasks/${id}`)
}

/**
 * 下载导出的报表文件
 */
export function downloadExportFile(id: number): Promise<any> {
  return request.get(`/reports/export-tasks/${id}/download`, {
    responseType: 'blob'
  })
}

/**
 * 手动触发过期导出文件清理
 */
export function cleanExpiredExportFiles(): Promise<ApiResponse<number>> {
  return request.post('/reports/export-tasks/clean-expired')
}
