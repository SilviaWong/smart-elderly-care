import request from '../utils/request'

export interface BuildingVO {
  id: number
  orgId: number
  buildingName: string
  buildingCode: string
  address?: string
  status: string
  roomCount: number
  bedCount: number
  createdAt?: string
}

export interface BuildingDTO {
  buildingName: string
  buildingCode: string
  address?: string
  status?: string
}

export interface RoomVO {
  id: number
  orgId: number
  buildingId: number
  buildingName?: string
  floorNo?: string
  roomNo: string
  roomType?: string
  status: string
  bedCount: number
  availableBedCount: number
  createdAt?: string
}

export interface RoomDTO {
  buildingId: number
  floorNo?: string
  roomNo: string
  roomType?: string
  status?: string
}

export interface BedVO {
  id: number
  orgId: number
  roomId: number
  roomNo?: string
  buildingId?: number
  buildingName?: string
  floorNo?: string
  bedNo: string
  bedType?: string
  status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'DISABLED'
  version: number
  currentAdmissionId?: number
  currentElderId?: number
  currentElderName?: string
  currentElderNo?: string
  admissionDate?: string
  createdAt?: string
}

export interface BedDTO {
  roomId: number
  bedNo: string
  bedType?: string
  status?: string
}

export interface SpaceTreeVO {
  id: number
  name: string
  code: string
  address?: string
  status: string
  totalRooms: number
  totalBeds: number
  occupiedBeds: number
  availableBeds: number
  rooms: {
    id: number
    buildingId: number
    floorNo?: string
    roomNo: string
    roomType?: string
    status: string
    beds: BedVO[]
  }[]
}

export interface BedBoardSummaryVO {
  totalBeds: number
  occupiedBeds: number
  availableBeds: number
  maintenanceBeds: number
  disabledBeds: number
  occupancyRate: number
}

export interface AdmissionVO {
  id: number
  orgId: number
  admissionNo: string
  elderId: number
  elderName: string
  elderNo: string
  elderGender?: string
  elderAge?: number
  elderPhone?: string
  bedId: number
  bedNo: string
  roomId?: number
  roomNo?: string
  buildingId?: number
  buildingName?: string
  contractNo?: string
  admissionDate: string
  plannedDischargeDate?: string
  actualDischargeDate?: string
  careLevelCode?: string
  status: 'ACTIVE' | 'DISCHARGED'
  depositAmount: number
  dischargeReason?: string
  operatorName?: string
  createdAt: string
  updatedAt: string
}

export interface AdmissionCheckInDTO {
  elderId: number
  bedId: number
  admissionDate: string
  plannedDischargeDate?: string
  careLevelCode?: string
  depositAmount?: number
  contractNo?: string
  remark?: string
}

export interface AdmissionTransferDTO {
  newBedId: number
  reason?: string
}

export interface AdmissionDischargeDTO {
  actualDischargeDate: string
  dischargeReason?: string
  forceDischarge?: boolean
}

export interface AdmissionPrecheckVO {
  canDischarge: boolean
  warnings: string[]
  blockingReasons: string[]
}

export interface BedChangeLogVO {
  id: number
  admissionId: number
  elderId: number
  elderName: string
  fromBedId?: number
  fromBedNo?: string
  toBedId?: number
  toBedNo?: string
  changeType: 'CHECK_IN' | 'TRANSFER' | 'DISCHARGE'
  changeReason?: string
  changedByName?: string
  changedAt: string
}

export interface AdmissionQueryDTO {
  admissionNo?: string
  elderId?: number
  elderName?: string
  elderNo?: string
  bedNo?: string
  status?: string
  buildingId?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

// 空间资源 API
export function getBuildings() {
  return request.get<{ code: number; message: string; data: BuildingVO[] }>('/api/v1/spaces/buildings')
}

export function createBuilding(data: BuildingDTO) {
  return request.post<{ code: number; message: string; data: number }>('/api/v1/spaces/buildings', data)
}

export function updateBuilding(id: number, data: BuildingDTO) {
  return request.put<{ code: number; message: string; data: void }>(`/api/v1/spaces/buildings/${id}`, data)
}

export function getRooms(buildingId?: number) {
  return request.get<{ code: number; message: string; data: RoomVO[] }>('/api/v1/spaces/rooms', {
    params: { buildingId },
  })
}

export function createRoom(data: RoomDTO) {
  return request.post<{ code: number; message: string; data: number }>('/api/v1/spaces/rooms', data)
}

export function updateRoom(id: number, data: RoomDTO) {
  return request.put<{ code: number; message: string; data: void }>(`/api/v1/spaces/rooms/${id}`, data)
}

export function getBeds(roomId?: number, status?: string) {
  return request.get<{ code: number; message: string; data: BedVO[] }>('/api/v1/spaces/beds', {
    params: { roomId, status },
  })
}

export function getAvailableBeds() {
  return request.get<{ code: number; message: string; data: BedVO[] }>('/api/v1/spaces/beds/available')
}

export function createBed(data: BedDTO) {
  return request.post<{ code: number; message: string; data: number }>('/api/v1/spaces/beds', data)
}

export function updateBed(id: number, data: BedDTO) {
  return request.put<{ code: number; message: string; data: void }>(`/api/v1/spaces/beds/${id}`, data)
}

export function updateBedStatus(id: number, status: string) {
  return request.put<{ code: number; message: string; data: void }>(`/api/v1/spaces/beds/${id}/status`, { status })
}

export function getSpaceTree() {
  return request.get<{ code: number; message: string; data: SpaceTreeVO[] }>('/api/v1/spaces/tree')
}

export function getBedBoardSummary() {
  return request.get<{ code: number; message: string; data: BedBoardSummaryVO }>('/api/v1/spaces/bed-board/summary')
}

// 入住及流转 API
export function getAdmissions(params: AdmissionQueryDTO) {
  return request.get<{
    code: number
    message: string
    data: { list: AdmissionVO[]; total: number; pageNum: number; pageSize: number }
  }>('/api/v1/admissions', { params })
}

export function getAdmissionDetail(id: number) {
  return request.get<{ code: number; message: string; data: AdmissionVO }>(`/api/v1/admissions/${id}`)
}

export function checkIn(data: AdmissionCheckInDTO) {
  return request.post<{ code: number; message: string; data: number }>('/api/v1/admissions/check-in', data)
}

export function transferBed(id: number, data: AdmissionTransferDTO) {
  return request.post<{ code: number; message: string; data: void }>(`/api/v1/admissions/${id}/transfer`, data)
}

export function precheckDischarge(id: number) {
  return request.get<{ code: number; message: string; data: AdmissionPrecheckVO }>(
    `/api/v1/admissions/${id}/precheck-discharge`
  )
}

export function discharge(id: number, data: AdmissionDischargeDTO) {
  return request.post<{ code: number; message: string; data: void }>(`/api/v1/admissions/${id}/discharge`, data)
}

export function getChangeLogs(admissionId: number) {
  return request.get<{ code: number; message: string; data: BedChangeLogVO[] }>(
    `/api/v1/admissions/${admissionId}/change-logs`
  )
}
