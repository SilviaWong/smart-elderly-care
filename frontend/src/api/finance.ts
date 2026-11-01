import request, { ApiResponse } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

// 1. 收费项目模型
export interface FeeItemVO {
  id: number
  orgId: number
  itemCode: string
  itemName: string
  category: string
  categoryName: string
  billingCycle: string
  billingCycleName: string
  price: number
  unit: string
  status: string
  statusName: string
  description?: string
  createdAt: string
}

export interface FeeItemDTO {
  itemCode: string
  itemName: string
  category: string
  billingCycle: string
  price: number
  unit: string
  status?: string
  description?: string
}

export interface FeeItemQueryDTO {
  category?: string
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 2. 账单明细与账单模型
export interface BillingItemVO {
  id: number
  billingId: number
  feeItemId: number
  itemNameSnapshot: string
  periodStart?: string
  periodEnd?: string
  quantity: number
  unitPrice: number
  amount: number
}

export interface BillingItemDTO {
  feeItemId: number
  itemNameSnapshot: string
  periodStart?: string
  periodEnd?: string
  quantity: number
  unitPrice: number
}

export interface BillingVO {
  id: number
  orgId: number
  billNo: string
  elderId: number
  elderName: string
  elderNo: string
  bedInfo: string
  admissionId?: number
  billMonth: string
  billDate: string
  dueDate?: string
  totalAmount: number
  paidAmount: number
  unpaidAmount: number
  status: string
  statusName: string
  remark?: string
  createdBy?: number
  createdByName: string
  createdAt: string
  isOverdue: boolean
  items: BillingItemVO[]
}

export interface BillingCreateDTO {
  elderId: number
  admissionId?: number
  billMonth: string
  billDate: string
  dueDate?: string
  remark?: string
  items: BillingItemDTO[]
}

export interface BillingQueryDTO {
  elderId?: number
  billMonth?: string
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 3. 收款记录模型
export interface PaymentRecordVO {
  id: number
  orgId: number
  paymentNo: string
  billingId: number
  billNo: string
  elderId: number
  elderName: string
  bedInfo: string
  amount: number
  paymentMethod: string
  paymentMethodName: string
  transactionNo?: string
  idempotencyKey: string
  status: string
  statusName: string
  paidAt: string
  operatorId?: number
  operatorName: string
  createdAt: string
  refundableAmount: number
}

export interface PaymentCreateDTO {
  billingId: number
  amount: number
  paymentMethod: string
  transactionNo?: string
  idempotencyKey: string
}

export interface PaymentQueryDTO {
  billingId?: number
  paymentMethod?: string
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 4. 退款记录模型
export interface RefundRecordVO {
  id: number
  orgId: number
  refundNo: string
  paymentId: number
  paymentNo: string
  billingId: number
  billNo: string
  elderId: number
  elderName: string
  amount: number
  reason: string
  status: string
  statusName: string
  approvedBy?: number
  approvedByName: string
  refundedAt?: string
  createdBy?: number
  createdByName: string
  createdAt: string
}

export interface RefundApplyDTO {
  paymentId: number
  amount: number
  reason: string
}

export interface RefundAuditDTO {
  approved: boolean
  remark?: string
}

export interface RefundQueryDTO {
  paymentId?: number
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 5. 财务统计指标模型
export interface FinanceSummaryVO {
  totalBilledAmount: number
  totalPaidAmount: number
  totalUnpaidAmount: number
  unpaidBillCount: number
  partialBillCount: number
  paidBillCount: number
  overdueBillCount: number
  totalRefundAmount: number
}

/* ================== API 请求函数 ================== */

// 收费项目接口
export function pageFeeItems(params: FeeItemQueryDTO): Promise<ApiResponse<PageResult<FeeItemVO>>> {
  return request({
    url: '/finance/fee-items',
    method: 'get',
    params
  })
}

export function listActiveFeeItems(): Promise<ApiResponse<FeeItemVO[]>> {
  return request({
    url: '/finance/fee-items/active',
    method: 'get'
  })
}

export function getFeeItem(id: number): Promise<ApiResponse<FeeItemVO>> {
  return request({
    url: `/finance/fee-items/${id}`,
    method: 'get'
  })
}

export function createFeeItem(data: FeeItemDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/finance/fee-items',
    method: 'post',
    data
  })
}

export function updateFeeItem(id: number, data: FeeItemDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/finance/fee-items/${id}`,
    method: 'put',
    data
  })
}

export function toggleFeeItemStatus(id: number, status: string): Promise<ApiResponse<void>> {
  return request({
    url: `/finance/fee-items/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 账单接口
export function pageBillings(params: BillingQueryDTO): Promise<ApiResponse<PageResult<BillingVO>>> {
  return request({
    url: '/finance/billings',
    method: 'get',
    params
  })
}

export function getBillingDetail(id: number): Promise<ApiResponse<BillingVO>> {
  return request({
    url: `/finance/billings/${id}`,
    method: 'get'
  })
}

export function createBilling(data: BillingCreateDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/finance/billings',
    method: 'post',
    data
  })
}

export function cancelBilling(id: number): Promise<ApiResponse<void>> {
  return request({
    url: `/finance/billings/${id}/cancel`,
    method: 'put'
  })
}

export function getFinanceSummary(): Promise<ApiResponse<FinanceSummaryVO>> {
  return request({
    url: '/finance/billings/summary',
    method: 'get'
  })
}

// 收款接口
export function pagePayments(params: PaymentQueryDTO): Promise<ApiResponse<PageResult<PaymentRecordVO>>> {
  return request({
    url: '/finance/payments',
    method: 'get',
    params
  })
}

export function getPaymentDetail(id: number): Promise<ApiResponse<PaymentRecordVO>> {
  return request({
    url: `/finance/payments/${id}`,
    method: 'get'
  })
}

export function registerPayment(data: PaymentCreateDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/finance/payments',
    method: 'post',
    data
  })
}

export function getRefundableAmount(paymentId: number): Promise<ApiResponse<number>> {
  return request({
    url: `/finance/payments/${paymentId}/refundable-amount`,
    method: 'get'
  })
}

// 退款接口
export function pageRefunds(params: RefundQueryDTO): Promise<ApiResponse<PageResult<RefundRecordVO>>> {
  return request({
    url: '/finance/refunds',
    method: 'get',
    params
  })
}

export function getRefundDetail(id: number): Promise<ApiResponse<RefundRecordVO>> {
  return request({
    url: `/finance/refunds/${id}`,
    method: 'get'
  })
}

export function applyRefund(data: RefundApplyDTO): Promise<ApiResponse<number>> {
  return request({
    url: '/finance/refunds',
    method: 'post',
    data
  })
}

export function auditRefund(id: number, data: RefundAuditDTO): Promise<ApiResponse<void>> {
  return request({
    url: `/finance/refunds/${id}/audit`,
    method: 'put',
    data
  })
}
