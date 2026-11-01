import request, { ApiResponse } from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
  orgId?: number
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  realName: string
  orgId: number
  orgName: string
  roles: string[]
  permissions: string[]
}

export function loginApi(data: LoginParams): Promise<ApiResponse<LoginResult>> {
  return request.post('/auth/login', data)
}

export function fetchCurrentUser(): Promise<ApiResponse<any>> {
  return request.get('/auth/me')
}

export function logoutApi(): Promise<ApiResponse<void>> {
  return request.post('/auth/logout')
}
