import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface ServiceItemVO {
  id: number
  orgId: number
  itemCode: string
  itemName: string
  category: string
  unit: string
  standardPrice: number
  needConfirm: number
  description: string
  status: string
  createdAt?: string
}

export interface ServiceItemDTO {
  itemCode: string
  itemName: string
  category: string
  unit: string
  standardPrice: number
  needConfirm: number
  description?: string
  status?: string
}

export interface ServiceOrderVO {
  id: number
  orgId: number
  orderNo: string
  elderId: number
  elderName?: string
  elderNo?: string
  bedInfo?: string
  serviceItemId: number
  itemName?: string
  itemCode?: string
  category?: string
  standardPrice?: number
  needConfirm?: number
  sourceType: string
  priority: string
  expectedStartAt?: string
  assigneeId?: number
  assigneeName?: string
  creatorId?: number
  creatorName?: string
  status: string
  requestDesc?: string
  executionResult?: string
  completedAt?: string
  createdAt: string
  updatedAt?: string
}

export interface ServiceOrderCreateDTO {
  elderId: number
  admissionId?: number
  serviceItemId: number
  sourceType?: string
  priority?: string
  expectedStartAt?: string
  assigneeId?: number
  requestDesc?: string
}

export interface ServiceOrderAssignDTO {
  assigneeId: number
  remark?: string
}

export interface ServiceOrderActionDTO {
  action: 'ACCEPT' | 'START' | 'COMPLETE' | 'CONFIRM' | 'REJECT' | 'RETURN' | 'CANCEL'
  executionResult?: string
  remark?: string
}

export interface ServiceOrderLogVO {
  id: number
  orderId: number
  fromStatus?: string
  toStatus: string
  action: string
  operatorId?: number
  operatorName?: string
  remark?: string
  createdAt: string
}

export interface ServiceOrderSummaryVO {
  totalOrders: number
  pendingAssignCount: number
  assignedCount: number
  inProgressCount: number
  pendingConfirmCount: number
  completedCount: number
}

export interface ServiceOrderQueryDTO {
  orderNo?: string
  elderId?: number
  elderName?: string
  serviceItemId?: number
  assigneeId?: number
  status?: string
  priority?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

// 服务项目 API
export function getServiceItemList(category?: string, status?: string): Promise<ApiResponse<ServiceItemVO[]>> {
  return request.get('/service-items', { params: { category, status } })
}

export function createServiceItem(data: ServiceItemDTO): Promise<ApiResponse<number>> {
  return request.post('/service-items', data)
}

export function updateServiceItem(id: number, data: ServiceItemDTO): Promise<ApiResponse<void>> {
  return request.put(`/service-items/${id}`, data)
}

export function toggleServiceItemStatus(id: number, status: string): Promise<ApiResponse<void>> {
  return request.put(`/service-items/${id}/status`, null, { params: { status } })
}

// 服务工单 API
export function getServiceOrderPage(params: ServiceOrderQueryDTO): Promise<ApiResponse<PageResult<ServiceOrderVO>>> {
  return request.get('/service-orders', { params })
}

export function getServiceOrderSummary(): Promise<ApiResponse<ServiceOrderSummaryVO>> {
  return request.get('/service-orders/summary')
}

export function getServiceOrderDetail(id: number): Promise<ApiResponse<ServiceOrderVO>> {
  return request.get(`/service-orders/${id}`)
}

export function createServiceOrder(data: ServiceOrderCreateDTO): Promise<ApiResponse<number>> {
  return request.post('/service-orders', data)
}

export function updateServiceOrder(id: number, data: ServiceOrderCreateDTO): Promise<ApiResponse<void>> {
  return request.put(`/service-orders/${id}`, data)
}

export function assignServiceOrder(id: number, data: ServiceOrderAssignDTO): Promise<ApiResponse<void>> {
  return request.post(`/service-orders/${id}/assign`, data)
}

export function executeServiceOrderAction(id: number, data: ServiceOrderActionDTO): Promise<ApiResponse<void>> {
  return request.post(`/service-orders/${id}/action`, data)
}

export function getServiceOrderLogs(id: number): Promise<ApiResponse<ServiceOrderLogVO[]>> {
  return request.get(`/service-orders/${id}/logs`)
}
