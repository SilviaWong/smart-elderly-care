import request, { ApiResponse } from '@/utils/request'

export interface ElderItem {
  id: number
  orgId: number
  elderNo: string
  name: string
  gender: string
  birthDate: string
  age: number
  phoneMasked: string
  idNoMasked: string
  primaryContactName: string
  primaryContactPhone: string
  mobilityLevel: string
  status: string
  createdAt: string
}

export interface ElderPageResult {
  list: ElderItem[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface ContactItem {
  id: number
  elderId: number
  contactName: string
  relationship: string
  phoneMasked: string
  phone: string
  isPrimary: number
  isEmergency: number
  address: string
  createdAt: string
}

export interface AssessmentItem {
  id: number
  elderId: number
  assessmentType: string
  assessmentDate: string
  score: number
  resultLevel: string
  detailJson: string
  assessorId: number
  assessorName: string
  remark: string
  createdAt: string
}

export interface ElderDetail {
  id: number
  orgId: number
  elderNo: string
  name: string
  gender: string
  birthDate: string
  age: number
  idType: string
  idNoMasked: string
  phoneMasked: string
  address: string
  photoUrl: string
  bloodType: string
  allergyDesc: string
  chronicDiseaseDesc: string
  medicationDesc: string
  dietTaboo: string
  mobilityLevel: string
  status: string
  remark: string
  creatorName: string
  createdAt: string
  contacts: ContactItem[]
  assessments: AssessmentItem[]
}

// 老人主档案
export function fetchElders(params: any): Promise<ApiResponse<ElderPageResult>> {
  return request.get('/elders', { params })
}

export function getElderDetail(id: number): Promise<ApiResponse<ElderDetail>> {
  return request.get(`/elders/${id}`)
}

export function createElder(data: any): Promise<ApiResponse<number>> {
  return request.post('/elders', data)
}

export function updateElder(id: number, data: any): Promise<ApiResponse<void>> {
  return request.put(`/elders/${id}`, data)
}

export function archiveElder(id: number): Promise<ApiResponse<void>> {
  return request.put(`/elders/${id}/archive`)
}

// 联系人
export function fetchContacts(elderId: number): Promise<ApiResponse<ContactItem[]>> {
  return request.get(`/elders/${elderId}/contacts`)
}

export function createContact(elderId: number, data: any): Promise<ApiResponse<number>> {
  return request.post(`/elders/${elderId}/contacts`, data)
}

export function updateContact(elderId: number, contactId: number, data: any): Promise<ApiResponse<void>> {
  return request.put(`/elders/${elderId}/contacts/${contactId}`, data)
}

export function deleteContact(elderId: number, contactId: number): Promise<ApiResponse<void>> {
  return request.delete(`/elders/${elderId}/contacts/${contactId}`)
}

// 评估记录
export function fetchAssessments(elderId: number): Promise<ApiResponse<AssessmentItem[]>> {
  return request.get(`/elders/${elderId}/assessments`)
}

export function createAssessment(elderId: number, data: any): Promise<ApiResponse<number>> {
  return request.post(`/elders/${elderId}/assessments`, data)
}
