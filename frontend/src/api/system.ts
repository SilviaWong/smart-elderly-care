import request, { ApiResponse } from '@/utils/request'

export interface UserItem {
  id: number
  orgId: number
  username: string
  realName: string
  phone: string
  email: string
  status: string
  lastLoginAt: string
  createdAt: string
  roles: string[]
}

export interface UserPageResult {
  list: UserItem[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface RoleItem {
  id: number
  orgId: number
  roleCode: string
  roleName: string
  description: string
  permissionIds: number[]
  createdAt: string
}

export interface PermissionNode {
  id: number
  parentId: number | null
  permissionCode: string
  permissionName: string
  permissionType: string
  path: string
  children: PermissionNode[]
}

// 用户相关 API
export function fetchUsers(params: any): Promise<ApiResponse<UserPageResult>> {
  return request.get('/system/users', { params })
}

export function createUser(data: any): Promise<ApiResponse<number>> {
  return request.post('/system/users', data)
}

export function updateUser(id: number, data: any): Promise<ApiResponse<void>> {
  return request.put(`/system/users/${id}`, data)
}

export function changeUserStatus(id: number, status: string): Promise<ApiResponse<void>> {
  return request.put(`/system/users/${id}/status`, null, { params: { status } })
}

// 角色相关 API
export function fetchRoles(): Promise<ApiResponse<RoleItem[]>> {
  return request.get('/system/roles')
}

export function createRole(data: any): Promise<ApiResponse<number>> {
  return request.post('/system/roles', data)
}

export function updateRole(id: number, data: any): Promise<ApiResponse<void>> {
  return request.put(`/system/roles/${id}`, data)
}

export function getRolePermissions(roleId: number): Promise<ApiResponse<number[]>> {
  return request.get(`/system/roles/${roleId}/permissions`)
}

export function deleteRole(roleId: number): Promise<ApiResponse<void>> {
  return request.delete(`/system/roles/${roleId}`)
}

// 权限树 API
export function fetchPermissionTree(): Promise<ApiResponse<PermissionNode[]>> {
  return request.get('/system/permissions')
}
