import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

// 1. 护理等级
export interface CareLevelVO {
  id: number
  orgId: number
  levelCode: string
  levelName: string
  description?: string
  status: string
}

export interface CareLevelDTO {
  levelCode: string
  levelName: string
  description?: string
  status?: string
}

// 2. 护理项目
export interface CareItemVO {
  id: number
  orgId: number
  itemCode: string
  itemName: string
  category?: string
  description?: string
  status: string
}

export interface CareItemDTO {
  itemCode: string
  itemName: string
  category?: string
  description?: string
  status?: string
}

// 3. 护理计划明细
export interface CarePlanItemVO {
  id: number
  planId: number
  careItemId: number
  itemCode?: string
  itemName?: string
  category?: string
  frequencyType: string
  frequencyValue?: string
  timeWindowStart?: string
  timeWindowEnd?: string
  assigneeId?: number
  assigneeName?: string
  enabled: number
}

export interface CarePlanItemDTO {
  id?: number
  careItemId: number
  frequencyType: string
  frequencyValue?: string
  timeWindowStart?: string
  timeWindowEnd?: string
  assigneeId?: number
  enabled?: number
}

// 4. 护理计划
export interface CarePlanVO {
  id: number
  orgId: number
  planNo: string
  elderId: number
  elderName?: string
  elderNo?: string
  bedInfo?: string
  careLevelId: number
  careLevelName?: string
  careLevelCode?: string
  startDate: string
  endDate?: string
  status: string
  creatorId: number
  creatorName?: string
  remark?: string
  itemCount: number
  createdAt: string
}

export interface CarePlanCreateDTO {
  elderId: number
  careLevelId: number
  startDate: string
  endDate?: string
  remark?: string
  items: CarePlanItemDTO[]
}

export interface CarePlanDetailVO {
  plan: CarePlanVO
  items: CarePlanItemVO[]
}

export interface CarePlanQueryDTO {
  planNo?: string
  elderId?: number
  elderName?: string
  careLevelId?: number
  status?: string
  pageNum?: number
  pageSize?: number
}

// 5. 护理任务
export interface CareTaskVO {
  id: number
  orgId: number
  taskNo: string
  planId: number
  planNo?: string
  planItemId: number
  careItemId?: number
  careItemCode?: string
  careItemName?: string
  category?: string
  elderId: number
  elderName?: string
  elderNo?: string
  bedInfo?: string
  taskDate: string
  scheduledAt?: string
  assigneeId?: number
  assigneeName?: string
  status: string
  idempotencyKey: string
  createdAt: string
  updatedAt?: string
}

export interface CareTaskGenerateDTO {
  taskDate: string
  elderId?: number
}

export interface CareTaskQueryDTO {
  taskNo?: string
  planId?: number
  elderId?: number
  elderName?: string
  assigneeId?: number
  status?: string
  taskDate?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface CareTaskSummaryVO {
  totalTasks: number
  pendingCount: number
  inProgressCount: number
  completedCount: number
  missedCount: number
  cancelledCount: number
}

export interface CareTaskAssignDTO {
  assigneeId: number
}

export interface CareTaskActionDTO {
  action: 'START' | 'COMPLETE' | 'MISS' | 'CANCEL'
  resultCode?: string
  resultDesc?: string
  attachmentJson?: string
}

export interface CareTaskRecordVO {
  id: number
  taskId: number
  executorId: number
  executorName?: string
  executedAt: string
  resultCode: string
  resultDesc?: string
  attachmentJson?: string
  createdAt: string
}

// 接口方法定义

// 护理等级 API
export function getCareLevels(status?: string): Promise<ApiResponse<CareLevelVO[]>> {
  return request.get('/care/config/levels', { params: { status } })
}

export function createCareLevel(data: CareLevelDTO): Promise<ApiResponse<number>> {
  return request.post('/care/config/levels', data)
}

export function updateCareLevel(id: number, data: CareLevelDTO): Promise<ApiResponse<void>> {
  return request.put(`/care/config/levels/${id}`, data)
}

export function toggleCareLevelStatus(id: number, status: string): Promise<ApiResponse<void>> {
  return request.put(`/care/config/levels/${id}/status`, null, { params: { status } })
}

// 护理项目 API
export function getCareItems(category?: string, status?: string): Promise<ApiResponse<CareItemVO[]>> {
  return request.get('/care/config/items', { params: { category, status } })
}

export function createCareItem(data: CareItemDTO): Promise<ApiResponse<number>> {
  return request.post('/care/config/items', data)
}

export function updateCareItem(id: number, data: CareItemDTO): Promise<ApiResponse<void>> {
  return request.put(`/care/config/items/${id}`, data)
}

export function toggleCareItemStatus(id: number, status: string): Promise<ApiResponse<void>> {
  return request.put(`/care/config/items/${id}/status`, null, { params: { status } })
}

// 护理计划 API
export function getCarePlanPage(params: CarePlanQueryDTO): Promise<ApiResponse<PageResult<CarePlanVO>>> {
  return request.get('/care/plans', { params })
}

export function getCarePlanDetail(id: number): Promise<ApiResponse<CarePlanDetailVO>> {
  return request.get(`/care/plans/${id}`)
}

export function createCarePlan(data: CarePlanCreateDTO): Promise<ApiResponse<number>> {
  return request.post('/care/plans', data)
}

export function updateCarePlan(id: number, data: CarePlanCreateDTO): Promise<ApiResponse<void>> {
  return request.put(`/care/plans/${id}`, data)
}

export function toggleCarePlanStatus(id: number, status: string): Promise<ApiResponse<void>> {
  return request.put(`/care/plans/${id}/status`, null, { params: { status } })
}

// 护理任务 API
export function generateCareTasks(data: CareTaskGenerateDTO): Promise<ApiResponse<number>> {
  return request.post('/care/tasks/generate', data)
}

export function getCareTaskPage(params: CareTaskQueryDTO): Promise<ApiResponse<PageResult<CareTaskVO>>> {
  return request.get('/care/tasks', { params })
}

export function getCareTaskSummary(taskDate?: string): Promise<ApiResponse<CareTaskSummaryVO>> {
  return request.get('/care/tasks/summary', { params: { taskDate } })
}

export function getCareTaskDetail(id: number): Promise<ApiResponse<CareTaskVO>> {
  return request.get(`/care/tasks/${id}`)
}

export function assignCareTask(id: number, data: CareTaskAssignDTO): Promise<ApiResponse<void>> {
  return request.post(`/care/tasks/${id}/assign`, data)
}

export function executeCareTaskAction(id: number, data: CareTaskActionDTO): Promise<ApiResponse<void>> {
  return request.post(`/care/tasks/${id}/action`, data)
}

export function getCareTaskRecords(id: number): Promise<ApiResponse<CareTaskRecordVO[]>> {
  return request.get(`/care/tasks/${id}/records`)
}
