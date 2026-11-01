import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface FieldItemDTO {
  sourceField: string
  targetField: string
  fieldType?: string
  required?: boolean
  validationRule?: string
  transformRule?: string
  defaultValue?: string
  sortOrder?: number
}

export interface FieldMappingDTO {
  targetAuthority?: string
  businessDomain?: string
  fileFormat?: string
  description?: string
  fields: FieldItemDTO[]
}

export interface RegulatoryTemplateVO {
  id: number
  orgId: number
  templateCode: string
  templateName: string
  versionNo: string
  fieldMapping: FieldMappingDTO
  fieldCount: number
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

export interface RegulatoryTemplateCreateDTO {
  templateCode: string
  templateName: string
  versionNo: string
  fieldMapping: FieldMappingDTO
  status?: string
}

export interface RegulatoryTemplateUpdateDTO {
  templateName?: string
  versionNo?: string
  fieldMapping?: FieldMappingDTO
  status?: string
}

export interface RegulatoryTemplateQueryDTO {
  templateCode?: string
  keyword?: string
  businessDomain?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

export interface ValidationErrorItemVO {
  rowNum: number
  recordId: number
  fieldName: string
  fieldLabel: string
  rejectedValue?: any
  errorType: 'MISSING_REQUIRED' | 'FORMAT_INVALID' | 'RULE_VIOLATION' | 'BIZ_CONFLICT'
  errorMessage: string
}

export interface BatchValidationResultVO {
  totalRows: number
  passedRows: number
  failedRows: number
  validationStatus: 'ALL_PASSED' | 'CONTAINS_ERRORS' | 'EMPTY_DATA'
  validatedAt: string
  errors: ValidationErrorItemVO[]
}

export interface RegulatoryBatchVO {
  id: number
  orgId: number
  batchNo: string
  templateId: number
  templateCode: string
  templateName: string
  businessDomain: string
  targetAuthority: string
  periodStart?: string
  periodEnd?: string
  status: 'GENERATED' | 'VALIDATED' | 'SUBMITTED' | 'REJECTED' | 'CONFIRMED'
  statusName: string
  fileUrl?: string
  fileName?: string
  validationResult?: BatchValidationResultVO
  submittedAt?: string
  responseDesc?: string
  createdBy?: number
  createdByName?: string
  createdAt: string
}

export interface RegulatoryBatchGenerateDTO {
  templateId: number
  periodStart?: string
  periodEnd?: string
  remark?: string
}

export interface RegulatoryBatchQueryDTO {
  templateId?: number
  batchNo?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

export interface RegulatoryBatchSubmitDTO {
  channel?: string
  externalReceiptNo?: string
  submissionNotes?: string
}

/**
 * 模板管理 API
 */
export function createTemplate(data: RegulatoryTemplateCreateDTO): Promise<ApiResponse<number>> {
  return request.post('/regulatory/templates', data)
}

export function updateTemplate(id: number, data: RegulatoryTemplateUpdateDTO): Promise<ApiResponse<void>> {
  return request.put(`/regulatory/templates/${id}`, data)
}

export function deleteTemplate(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/regulatory/templates/${id}`)
}

export function getTemplateDetail(id: number): Promise<ApiResponse<RegulatoryTemplateVO>> {
  return request.get(`/regulatory/templates/${id}`)
}

export function pageTemplates(params: RegulatoryTemplateQueryDTO): Promise<ApiResponse<PageResult<RegulatoryTemplateVO>>> {
  return request.get('/regulatory/templates', { params })
}

export function listActiveTemplates(): Promise<ApiResponse<RegulatoryTemplateVO[]>> {
  return request.get('/regulatory/templates/active-list')
}

/**
 * 报送批次与数据校验 API
 */
export function generateBatch(data: RegulatoryBatchGenerateDTO): Promise<ApiResponse<number>> {
  return request.post('/regulatory/batches', data)
}

export function pageBatches(params: RegulatoryBatchQueryDTO): Promise<ApiResponse<PageResult<RegulatoryBatchVO>>> {
  return request.get('/regulatory/batches', { params })
}

export function getBatchDetail(id: number): Promise<ApiResponse<RegulatoryBatchVO>> {
  return request.get(`/regulatory/batches/${id}`)
}

export function getValidationResult(id: number): Promise<ApiResponse<BatchValidationResultVO>> {
  return request.get(`/regulatory/batches/${id}/validation-result`)
}

export function downloadBatchFile(id: number): Promise<any> {
  return request.get(`/regulatory/batches/${id}/download`, {
    responseType: 'blob'
  })
}

export function submitBatch(id: number, data: RegulatoryBatchSubmitDTO): Promise<ApiResponse<void>> {
  return request.post(`/regulatory/batches/${id}/submit`, data)
}
