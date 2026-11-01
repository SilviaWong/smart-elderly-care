import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

// 1. 菜品接口模型
export interface DishVO {
  id: number
  orgId: number
  dishCode: string
  dishName: string
  category: string
  categoryName: string
  price: number
  allergens?: string
  description?: string
  status: string
  statusName: string
}

export interface DishDTO {
  dishCode: string
  dishName: string
  category: string
  price: number
  allergens?: string
  description?: string
  status?: string
}

export interface DishQueryDTO {
  category?: string
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 2. 菜单接口模型
export interface MealMenuItemVO {
  id: number
  dishId: number
  dishCode: string
  dishName: string
  category: string
  categoryName: string
  priceSnapshot: number
  allergens?: string
  description?: string
}

export interface MealMenuVO {
  id: number
  orgId: number
  menuDate: string
  mealType: string
  mealTypeName: string
  status: string
  statusName: string
  createdBy?: number
  createdByName?: string
  createdAt: string
  dishCount: number
  items: MealMenuItemVO[]
}

export interface MealMenuCreateDTO {
  menuDate: string
  mealType: string
  status?: string
  dishIds: number[]
}

export interface MealMenuQueryDTO {
  menuDate?: string
  mealType?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

// 3. 订餐订单接口模型
export interface MealOrderItemVO {
  id: number
  dishId?: number
  dishNameSnapshot: string
  quantity: number
  unitPrice: number
  lineAmount: number
}

export interface MealOrderVO {
  id: number
  orgId: number
  orderNo: string
  elderId: number
  elderName: string
  elderNo: string
  bedInfo?: string
  orderDate: string
  mealType: string
  mealTypeName: string
  totalAmount: number
  status: string
  statusName: string
  orderedBy?: number
  orderedByName?: string
  createdAt: string
  isPastCancelCutoff: boolean
  items: MealOrderItemVO[]
}

export interface MealOrderItemDTO {
  dishId: number
  quantity: number
}

export interface MealOrderCreateDTO {
  elderId: number
  orderDate: string
  mealType: string
  items: MealOrderItemDTO[]
  overrideWarning?: boolean
}

export interface MealOrderQueryDTO {
  elderId?: number
  orderDate?: string
  mealType?: string
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 4. 膳食过敏与禁忌检查模型
export interface ElderDietaryProfileVO {
  elderId: number
  elderName: string
  elderNo: string
  allergyDesc?: string
  dietTaboo?: string
  chronicDiseaseDesc?: string
}

export interface DietaryCheckResultVO {
  hasWarning: boolean
  warnings: string[]
  elderProfile: ElderDietaryProfileVO
}

// 5. 餐饮看板模型
export interface DiningSummaryVO {
  activeDishCount: number
  todayMenuCount: number
  todayOrderCount: number
  pendingDeliverCount: number
  deliveredCount: number
  todayRevenue: number
}

// 接口调用方法

// 菜品相关
export function pageDishes(params: DishQueryDTO): Promise<ApiResponse<PageResult<DishVO>>> {
  return request({
    url: '/dining/dishes',
    method: 'get',
    params
  })
}

export function listActiveDishes(): Promise<ApiResponse<DishVO[]>> {
  return request({
    url: '/dining/dishes/active',
    method: 'get'
  })
}

export function getDish(id: number): Promise<ApiResponse<DishVO>> {
  return request({
    url: `/dining/dishes/${id}`,
    method: 'get'
  })
}

export function createDish(data: DishDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/dining/dishes',
    method: 'post',
    data
  })
}

export function updateDish(id: number, data: DishDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/dining/dishes/${id}`,
    method: 'put',
    data
  })
}

export function toggleDishStatus(id: number, status: string): Promise<ApiResponse<void>> {
  return request({
    url: `/dining/dishes/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 菜单相关
export function pageMenus(params: MealMenuQueryDTO): Promise<ApiResponse<PageResult<MealMenuVO>>> {
  return request({
    url: '/dining/menus',
    method: 'get',
    params
  })
}

export function getMenuByDateAndType(menuDate: string, mealType: string): Promise<ApiResponse<MealMenuVO>> {
  return request({
    url: '/dining/menus/by-date-type',
    method: 'get',
    params: { menuDate, mealType }
  })
}

export function getMenuDetail(id: number): Promise<ApiResponse<MealMenuVO>> {
  return request({
    url: `/dining/menus/${id}`,
    method: 'get'
  })
}

export function saveOrUpdateMenu(data: MealMenuCreateDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/dining/menus',
    method: 'post',
    data
  })
}

export function deleteMenu(id: number): Promise<ApiResponse<void>> {
  return request({
    url: `/dining/menus/${id}`,
    method: 'delete'
  })
}

// 订单相关
export function pageOrders(params: MealOrderQueryDTO): Promise<ApiResponse<PageResult<MealOrderVO>>> {
  return request({
    url: '/dining/orders',
    method: 'get',
    params
  })
}

export function getDiningSummary(): Promise<ApiResponse<DiningSummaryVO>> {
  return request({
    url: '/dining/orders/summary',
    method: 'get'
  })
}

export function getOrderDetail(id: number): Promise<ApiResponse<MealOrderVO>> {
  return request({
    url: `/dining/orders/${id}`,
    method: 'get'
  })
}

export function checkDietaryWarning(elderId: number, dishIds: number[]): Promise<ApiResponse<DietaryCheckResultVO>> {
  return request({
    url: '/dining/orders/check-dietary',
    method: 'post',
    params: { elderId },
    data: dishIds
  })
}

export function createOrder(data: MealOrderCreateDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/dining/orders',
    method: 'post',
    data
  })
}

export function cancelOrder(id: number): Promise<ApiResponse<void>> {
  return request({
    url: `/dining/orders/${id}/cancel`,
    method: 'put'
  })
}

export function deliverOrder(id: number): Promise<ApiResponse<void>> {
  return request({
    url: `/dining/orders/${id}/deliver`,
    method: 'put'
  })
}
