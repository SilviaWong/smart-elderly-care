import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

// 1. 设备模型
export interface DeviceVO {
  id: number
  orgId: number
  deviceCode: string
  deviceName: string
  deviceType: string
  deviceTypeName: string
  vendor?: string
  model?: string
  serialNo?: string
  installLocation?: string
  status: string
  statusName: string
  lastOnlineAt?: string
  createdAt: string
  bindStatus: string
  bindType?: string
  boundElderId?: number
  boundElderName?: string
  boundBedId?: number
  boundBedInfo?: string
  boundAt?: string
}

export interface DeviceDTO {
  deviceCode: string
  deviceName: string
  deviceType: string
  vendor?: string
  model?: string
  serialNo?: string
  installLocation?: string
  status?: string
}

export interface DeviceQueryDTO {
  deviceType?: string
  status?: string
  bindStatus?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 2. 设备绑定模型
export interface DeviceBindDTO {
  deviceId: number
  elderId?: number
  bedId?: number
  bindType: string
}

export interface DeviceUnbindDTO {
  deviceId: number
  remark?: string
}

export interface DeviceBindingVO {
  id: number
  orgId: number
  deviceId: number
  deviceCode: string
  deviceName: string
  elderId?: number
  elderName?: string
  elderNo?: string
  bedId?: number
  bedInfo?: string
  bindType: string
  bindTypeName: string
  boundAt: string
  unboundAt?: string
  status: string
  statusName: string
}

// 3. 遥测数据模型
export interface DeviceDataVO {
  id: number
  orgId: number
  deviceId: number
  deviceCode: string
  deviceName: string
  deviceType: string
  elderId?: number
  elderName?: string
  metricCode: string
  metricName: string
  metricValue: number
  unit: string
  rawPayload?: string
  collectedAt: string
  receivedAt: string
}

export interface MetricItemDTO {
  metricCode: string
  metricValue: number
  unit?: string
}

export interface EventItemDTO {
  eventType: string
  severity?: string
  message?: string
}

export interface DeviceReportDTO {
  deviceCode: string
  protocol?: string
  rawPayload?: string
  collectedAt?: string
  metrics?: MetricItemDTO[]
  events?: EventItemDTO[]
  battery?: number
  sos?: boolean
  fallDetected?: boolean
}

export interface DeviceDataQueryDTO {
  deviceId?: number
  elderId?: number
  metricCode?: string
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

// 4. 设备事件模型
export interface DeviceEventVO {
  id: number
  orgId: number
  deviceId: number
  deviceCode: string
  deviceName: string
  deviceType: string
  installLocation?: string
  elderId?: number
  elderName?: string
  eventType: string
  eventTypeName: string
  severity: string
  severityName: string
  eventPayload?: string
  eventStatus: string
  eventStatusName: string
  occurredAt: string
  handledBy?: number
  handledByName?: string
  handledAt?: string
  handleResult?: string
  createdAt: string
}

export interface DeviceEventHandleDTO {
  eventStatus: string
  handleResult: string
}

export interface DeviceEventQueryDTO {
  deviceId?: number
  elderId?: number
  eventType?: string
  severity?: string
  eventStatus?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 5. 物联网综合统计指标模型
export interface IotSummaryVO {
  totalDevices: number
  activeDevices: number
  offlineDevices: number
  faultDevices: number
  boundDevices: number
  unboundDevices: number
  openEventCount: number
  todayTelemetryCount: number
}

/* ================== API 请求方法 ================== */

// 设备台账与绑定接口
export function pageDevices(params: DeviceQueryDTO): Promise<ApiResponse<PageResult<DeviceVO>>> {
  return request({
    url: '/iot/devices',
    method: 'get',
    params
  })
}

export function listActiveDevices(deviceType?: string): Promise<ApiResponse<DeviceVO[]>> {
  return request({
    url: '/iot/devices/active',
    method: 'get',
    params: { deviceType }
  })
}

export function getDeviceDetail(id: number): Promise<ApiResponse<DeviceVO>> {
  return request({
    url: `/iot/devices/${id}`,
    method: 'get'
  })
}

export function createDevice(data: DeviceDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/iot/devices',
    method: 'post',
    data
  })
}

export function updateDevice(id: number, data: DeviceDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/iot/devices/${id}`,
    method: 'put',
    data
  })
}

export function bindDevice(data: DeviceBindDTO): Promise<ApiResponse<void>> {
  return request({
    url: '/iot/devices/bind',
    method: 'post',
    data
  })
}

export function unbindDevice(data: DeviceUnbindDTO): Promise<ApiResponse<void>> {
  return request({
    url: '/iot/devices/unbind',
    method: 'post',
    data
  })
}

export function pageBindings(params: {
  deviceId?: number
  elderId?: number
  pageNum?: number
  pageSize?: number
}): Promise<ApiResponse<PageResult<DeviceBindingVO>>> {
  return request({
    url: '/iot/devices/bindings',
    method: 'get',
    params
  })
}

export function getIotSummary(): Promise<ApiResponse<IotSummaryVO>> {
  return request({
    url: '/iot/devices/summary',
    method: 'get'
  })
}

// 遥测数据上报与查询接口
export function reportTelemetry(data: DeviceReportDTO): Promise<ApiResponse<void>> {
  return request({
    url: '/iot/telemetry/report',
    method: 'post',
    data
  })
}

export function pageTelemetryData(params: DeviceDataQueryDTO): Promise<ApiResponse<PageResult<DeviceDataVO>>> {
  return request({
    url: '/iot/telemetry/data',
    method: 'get',
    params
  })
}

// 设备告警事件处理接口
export function pageEvents(params: DeviceEventQueryDTO): Promise<ApiResponse<PageResult<DeviceEventVO>>> {
  return request({
    url: '/iot/events',
    method: 'get',
    params
  })
}

export function getEventDetail(id: number): Promise<ApiResponse<DeviceEventVO>> {
  return request({
    url: `/iot/events/${id}`,
    method: 'get'
  })
}

export function handleEvent(id: number, data: DeviceEventHandleDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/iot/events/${id}/handle`,
    method: 'put',
    data
  })
}
