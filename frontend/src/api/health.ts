import request, { ApiResponse } from '@/utils/request'

export interface HealthInfo {
  status: string
  application: string
  version: string
  serverTime: string
}

export function checkBackendHealth(): Promise<ApiResponse<HealthInfo>> {
  return request.get('/health')
}
