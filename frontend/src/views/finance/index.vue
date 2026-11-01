<template>
  <div class="finance-page">
    <!-- 顶部 KPI 财务统计看板 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card total-billed">
          <div class="kpi-icon"><el-icon><Tickets /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">应收账单总额</span>
            <span class="kpi-num primary-num">¥{{ Number(summary.totalBilledAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card total-paid">
          <div class="kpi-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">实际收款总额</span>
            <span class="kpi-num success-num">¥{{ Number(summary.totalPaidAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card total-unpaid">
          <div class="kpi-icon"><el-icon><WarningFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待缴欠费差额</span>
            <span class="kpi-num danger-num">¥{{ Number(summary.totalUnpaidAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card total-refund">
          <div class="kpi-icon"><el-icon><RefreshLeft /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">累计成功退款</span>
            <span class="kpi-num warning-num">¥{{ Number(summary.totalRefundAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card unpaid-count">
          <div class="kpi-icon"><el-icon><Clock /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待付/部分支付账单</span>
            <span class="kpi-num">{{ (summary.unpaidBillCount || 0) + (summary.partialBillCount || 0) }} 张</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card paid-count">
          <div class="kpi-icon"><el-icon><DocumentChecked /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">已全部结清账单</span>
            <span class="kpi-num purple-num">{{ summary.paidBillCount || 0 }} 张</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 选项卡主内容区 -->
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 选项卡 1：月度账单与缴费工作台 -->
        <el-tab-pane label="月度账单与缴费工作台" name="billings">
          <div class="tab-header">
            <div class="header-text">
              <h3 class="tab-title">长者月度账单与收款平账</h3>
              <span class="tab-subtitle">支持床位费、护理费、餐费等周期性账单按项生成，价格与项目快照固化；严格保障支付幂等与实收自动平账</span>
            </div>
            <div class="header-btns">
              <el-button type="primary" :icon="Plus" @click="handleOpenCreateBilling">
                生成月度账单
              </el-button>
              <el-button :icon="Refresh" @click="loadBillings">刷新</el-button>
            </div>
          </div>

          <!-- 账单筛选表单 -->
          <el-form :inline="true" :model="billingQuery" class="filter-form">
            <el-form-item label="账单月份">
              <el-date-picker
                v-model="billingQuery.billMonth"
                type="month"
                placeholder="选择账单月份"
                value-format="YYYY-MM"
                clearable
                style="width: 140px"
              />
            </el-form-item>
            <el-form-item label="账单状态">
              <el-select v-model="billingQuery.status" placeholder="全部状态" clearable style="width: 130px">
                <el-option label="待支付 (UNPAID)" value="UNPAID" />
                <el-option label="部分支付 (PARTIAL)" value="PARTIAL" />
                <el-option label="已付清 (PAID)" value="PAID" />
                <el-option label="已逾期 (OVERDUE)" value="OVERDUE" />
                <el-option label="已作废 (CANCELLED)" value="CANCELLED" />
              </el-select>
            </el-form-item>
            <el-form-item label="综合搜索">
              <el-input
                v-model="billingQuery.keyword"
                placeholder="搜索单号 / 备注..."
                clearable
                :prefix-icon="Search"
                style="width: 200px"
                @keyup.enter="handleBillingSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleBillingSearch">查询</el-button>
              <el-button :icon="RefreshRight" @click="handleBillingReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 账单列表表格 -->
          <el-table :data="billingList" v-loading="billingLoading" stripe border style="width: 100%">
            <el-table-column prop="billNo" label="账单编号" min-width="170">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="handleViewBilling(row)">
                  <b>{{ row.billNo }}</b>
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="elderName" label="长者姓名" min-width="120">
              <template #default="{ row }">
                <span class="elder-cell-name">{{ row.elderName }}</span>
                <span class="sub-bed-tag">{{ row.bedInfo || '未分配' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="billMonth" label="账单月份" width="110" align="center">
              <template #default="{ row }">
                <el-tag effect="plain" type="info">{{ row.billMonth }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalAmount" label="应收总额" width="120" align="right">
              <template #default="{ row }">
                <span class="amount-val">¥{{ Number(row.totalAmount).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="paidAmount" label="已收金额" width="120" align="right">
              <template #default="{ row }">
                <span class="paid-val">¥{{ Number(row.paidAmount).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="unpaidAmount" label="待付欠额" width="120" align="right">
              <template #default="{ row }">
                <span :class="{'unpaid-val': Number(row.unpaidAmount) > 0, 'zero-val': Number(row.unpaidAmount) === 0}">
                  ¥{{ Number(row.unpaidAmount).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="账单状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getBillingTagType(row.status)">
                  {{ row.statusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dueDate" label="缴费截止日" width="120" align="center">
              <template #default="{ row }">
                <span :class="{ 'overdue-text': row.isOverdue }">
                  {{ row.dueDate || '-' }}
                </span>
                <div v-if="row.isOverdue" class="overdue-badge">已逾期</div>
              </template>
            </el-table-column>
            <el-table-column prop="createdByName" label="创建人" width="110" align="center" />
            <el-table-column label="操作" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleViewBilling(row)">
                  明细
                </el-button>
                <el-button
                  v-if="['UNPAID', 'PARTIAL', 'OVERDUE'].includes(row.status)"
                  type="success"
                  size="small"
                  @click="handleOpenPayment(row)"
                >
                  登记缴费
                </el-button>
                <el-popconfirm
                  v-if="row.status !== 'CANCELLED' && Number(row.paidAmount) === 0"
                  title="确认作废该未缴费账单吗？作废后不可恢复。"
                  @confirm="handleCancelBilling(row)"
                >
                  <template #reference>
                    <el-button link type="danger" size="small">作废</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <!-- 账单分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="billingQuery.pageNum"
              v-model:page-size="billingQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="billingTotal"
              @size-change="loadBillings"
              @current-change="loadBillings"
            />
          </div>
        </el-tab-pane>

        <!-- 选项卡 2：收款流水与退款审核 -->
        <el-tab-pane label="收款与退款流水明细" name="ledger">
          <div class="sub-tab-switch">
            <el-radio-group v-model="ledgerType" size="default">
              <el-radio-button label="payments">收款流水记录 (Payment)</el-radio-button>
              <el-radio-button label="refunds">退款记录与审批 (Refund)</el-radio-button>
            </el-radio-group>
            <el-button :icon="Refresh" @click="loadLedger">刷新流水</el-button>
          </div>

          <!-- 收款流水子视图 -->
          <div v-if="ledgerType === 'payments'" class="ledger-block">
            <el-form :inline="true" :model="paymentQuery" class="filter-form">
              <el-form-item label="支付渠道">
                <el-select v-model="paymentQuery.paymentMethod" placeholder="全部渠道" clearable style="width: 140px">
                  <el-option label="现金 (CASH)" value="CASH" />
                  <el-option label="银行转账 (BANK_TRANSFER)" value="BANK_TRANSFER" />
                  <el-option label="支付宝 (ALIPAY)" value="ALIPAY" />
                  <el-option label="微信支付 (WECHAT_PAY)" value="WECHAT_PAY" />
                  <el-option label="POS刷卡 (POS)" value="POS" />
                  <el-option label="模拟在线支付 (MOCK_ONLINE)" value="MOCK_ONLINE" />
                </el-select>
              </el-form-item>
              <el-form-item label="综合搜索">
                <el-input
                  v-model="paymentQuery.keyword"
                  placeholder="收款流水 / 交易单号 / 幂等键..."
                  clearable
                  :prefix-icon="Search"
                  style="width: 240px"
                  @keyup.enter="loadPayments"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadPayments">查询</el-button>
                <el-button :icon="RefreshRight" @click="handlePaymentReset">重置</el-button>
              </el-form-item>
            </el-form>

            <el-table :data="paymentList" v-loading="paymentLoading" stripe border style="width: 100%">
              <el-table-column prop="paymentNo" label="收款流水号" min-width="170">
                <template #default="{ row }">
                  <b>{{ row.paymentNo }}</b>
                </template>
              </el-table-column>
              <el-table-column prop="billNo" label="关联账单号" min-width="160" />
              <el-table-column prop="elderName" label="长者" width="110">
                <template #default="{ row }">
                  <span>{{ row.elderName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="实收金额" width="120" align="right">
                <template #default="{ row }">
                  <span class="amount-val">¥{{ Number(row.amount).toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="refundableAmount" label="剩余可退额度" width="130" align="right">
                <template #default="{ row }">
                  <span class="refundable-val">¥{{ Number(row.refundableAmount).toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="paymentMethodName" label="支付方式" width="130" align="center">
                <template #default="{ row }">
                  <el-tag effect="light">{{ row.paymentMethodName }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="idempotencyKey" label="支付幂等键" min-width="170" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="mono-code">{{ row.idempotencyKey }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="statusName" label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'info'">{{ row.statusName }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="paidAt" label="收款时间" width="160" align="center" />
              <el-table-column prop="operatorName" label="经办人" width="110" align="center" />
              <el-table-column label="操作" width="120" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button
                    type="warning"
                    size="small"
                    :disabled="Number(row.refundableAmount) <= 0 || row.status !== 'SUCCESS'"
                    @click="handleOpenRefund(row)"
                  >
                    申请退款
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="paymentQuery.pageNum"
                v-model:page-size="paymentQuery.pageSize"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="paymentTotal"
                @size-change="loadPayments"
                @current-change="loadPayments"
              />
            </div>
          </div>

          <!-- 退款记录与审核子视图 -->
          <div v-if="ledgerType === 'refunds'" class="ledger-block">
            <el-form :inline="true" :model="refundQuery" class="filter-form">
              <el-form-item label="退款状态">
                <el-select v-model="refundQuery.status" placeholder="全部状态" clearable style="width: 140px">
                  <el-option label="待审批 (PENDING)" value="PENDING" />
                  <el-option label="退款成功 (SUCCESS)" value="SUCCESS" />
                  <el-option label="已驳回 (REJECTED)" value="REJECTED" />
                </el-select>
              </el-form-item>
              <el-form-item label="综合搜索">
                <el-input
                  v-model="refundQuery.keyword"
                  placeholder="退款单号 / 退款原因..."
                  clearable
                  :prefix-icon="Search"
                  style="width: 220px"
                  @keyup.enter="loadRefunds"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadRefunds">查询</el-button>
                <el-button :icon="RefreshRight" @click="handleRefundReset">重置</el-button>
              </el-form-item>
            </el-form>

            <el-table :data="refundList" v-loading="refundLoading" stripe border style="width: 100%">
              <el-table-column prop="refundNo" label="退款流水号" min-width="170">
                <template #default="{ row }">
                  <b>{{ row.refundNo }}</b>
                </template>
              </el-table-column>
              <el-table-column prop="paymentNo" label="原收款流水号" min-width="160" />
              <el-table-column prop="billNo" label="关联账单号" min-width="150" />
              <el-table-column prop="elderName" label="长者" width="110" />
              <el-table-column prop="amount" label="退款金额" width="120" align="right">
                <template #default="{ row }">
                  <span class="refund-val">-¥{{ Number(row.amount).toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="退款原因" min-width="160" show-overflow-tooltip />
              <el-table-column prop="status" label="退款状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="getRefundTagType(row.status)">{{ row.statusName }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="申请时间" width="160" align="center" />
              <el-table-column prop="createdByName" label="申请人" width="110" align="center" />
              <el-table-column prop="approvedByName" label="审批人" width="110" align="center" />
              <el-table-column label="操作" width="120" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 'PENDING'"
                    type="primary"
                    size="small"
                    @click="handleOpenAudit(row)"
                  >
                    审批处理
                  </el-button>
                  <span v-else class="text-muted">已办结</span>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="refundQuery.pageNum"
                v-model:page-size="refundQuery.pageSize"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="refundTotal"
                @size-change="loadRefunds"
                @current-change="loadRefunds"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 选项卡 3：收费项目配置 -->
        <el-tab-pane label="收费项目标准配置" name="feeItems">
          <div class="tab-header">
            <div class="header-text">
              <h3 class="tab-title">养老机构收费标准维护</h3>
              <span class="tab-subtitle">集中维护床位、护理、膳食、增值服务等基础计费标准与计费周期，月度出账时自动生成项目价格快照</span>
            </div>
            <div class="header-btns">
              <el-button type="primary" :icon="Plus" @click="handleOpenCreateFeeItem">
                新增收费项目
              </el-button>
              <el-button :icon="Refresh" @click="loadFeeItems">刷新</el-button>
            </div>
          </div>

          <el-form :inline="true" :model="feeItemQuery" class="filter-form">
            <el-form-item label="收费大类">
              <el-select v-model="feeItemQuery.category" placeholder="全部类别" clearable style="width: 140px">
                <el-option label="床位费 (BED)" value="BED" />
                <el-option label="护理费 (CARE)" value="CARE" />
                <el-option label="餐饮费 (MEAL)" value="MEAL" />
                <el-option label="增值服务费 (SERVICE)" value="SERVICE" />
                <el-option label="医疗耗材 (MEDICAL)" value="MEDICAL" />
                <el-option label="入住押金 (DEPOSIT)" value="DEPOSIT" />
                <el-option label="其他费用 (OTHER)" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="feeItemQuery.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option label="启用 (ACTIVE)" value="ACTIVE" />
                <el-option label="停用 (INACTIVE)" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item label="综合搜索">
              <el-input
                v-model="feeItemQuery.keyword"
                placeholder="编码 / 名称 / 描述..."
                clearable
                :prefix-icon="Search"
                style="width: 200px"
                @keyup.enter="loadFeeItems"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="loadFeeItems">查询</el-button>
              <el-button :icon="RefreshRight" @click="handleFeeItemReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="feeItemList" v-loading="feeItemLoading" stripe border style="width: 100%">
            <el-table-column prop="itemCode" label="项目编码" width="120">
              <template #default="{ row }">
                <span class="mono-code">{{ row.itemCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="itemName" label="收费项目名称" min-width="160">
              <template #default="{ row }">
                <b>{{ row.itemName }}</b>
              </template>
            </el-table-column>
            <el-table-column prop="categoryName" label="费用类别" width="130" align="center">
              <template #default="{ row }">
                <el-tag effect="light">{{ row.categoryName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="billingCycleName" label="计费周期" width="110" align="center" />
            <el-table-column prop="price" label="标准单价" width="120" align="right">
              <template #default="{ row }">
                <span class="amount-val">¥{{ Number(row.price).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="计量单位" width="100" align="center" />
            <el-table-column prop="status" label="启用状态" width="110" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 'ACTIVE'"
                  active-text="启用"
                  inactive-text="停用"
                  inline-prompt
                  @change="(val: any) => handleToggleFeeStatus(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="description" label="项目说明" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="100" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleOpenEditFeeItem(row)">
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="feeItemQuery.pageNum"
              v-model:page-size="feeItemQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="feeItemTotal"
              @size-change="loadFeeItems"
              @current-change="loadFeeItems"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 对话框 1：生成月度账单 -->
    <el-dialog
      v-model="createBillingVisible"
      title="生成长者月度账单"
      width="780px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="billingFormRef" :model="billingForm" :rules="billingRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="就养长者" prop="elderId">
              <el-select
                v-model="billingForm.elderId"
                placeholder="搜索并选择入住长者"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="elder in elderOptions"
                  :key="elder.id"
                  :label="`${elder.name} (${elder.elderNo})`"
                  :value="elder.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账单月份" prop="billMonth">
              <el-date-picker
                v-model="billingForm.billMonth"
                type="month"
                placeholder="选择账单月份"
                value-format="YYYY-MM"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出账日期" prop="billDate">
              <el-date-picker
                v-model="billingForm.billDate"
                type="date"
                placeholder="选择出账日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="缴费截止日" prop="dueDate">
              <el-date-picker
                v-model="billingForm.dueDate"
                type="date"
                placeholder="选择最晚付款截止日"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="账单备注">
          <el-input v-model="billingForm.remark" placeholder="如：含11月床位费与基础护理费" />
        </el-form-item>

        <div class="billing-items-section">
          <div class="items-bar">
            <span class="items-title"><b>账单收费明细清单</b> (支持多项计费，价格实时计算并快照留痕)</span>
            <el-button type="primary" link :icon="Plus" @click="handleAddBillingItem">
              添加收费明细项
            </el-button>
          </div>
          <el-table :data="billingForm.items" border size="small" style="width: 100%; margin-top: 8px">
            <el-table-column label="收费项目" min-width="180">
              <template #default="{ row }">
                <el-select
                  v-model="row.feeItemId"
                  placeholder="选择收费项目"
                  size="small"
                  style="width: 100%"
                  @change="(val: any) => handleFeeItemSelect(row, val)"
                >
                  <el-option
                    v-for="item in activeFeeItems"
                    :key="item.id"
                    :label="`${item.itemName} (¥${item.price}/${item.unit})`"
                    :value="item.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="项目名称快照" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.itemNameSnapshot" size="small" placeholder="快照留存名称" />
              </template>
            </el-table-column>
            <el-table-column label="单价 (元)" width="110">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.unitPrice"
                  :min="0"
                  :precision="2"
                  :step="10"
                  size="small"
                  controls-position="right"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="90">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  :precision="0"
                  size="small"
                  controls-position="right"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="小计 (元)" width="110" align="right">
              <template #default="{ row }">
                <span class="subtotal-val">¥{{ (row.quantity * row.unitPrice).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="handleRemoveBillingItem($index)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="billing-total-preview">
            <span>账单总金额合计：</span>
            <b class="total-preview-num">¥{{ calcFormTotalAmount.toFixed(2) }}</b>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createBillingVisible = false">取消</el-button>
        <el-button type="primary" :loading="createBillingSubmitting" @click="handleSubmitCreateBilling">
          确认生成账单
        </el-button>
      </template>
    </el-dialog>

    <!-- 对话框 2：登记缴费 (具备幂等键控制与模拟支付) -->
    <el-dialog
      v-model="paymentVisible"
      title="登记账单收款 / 模拟支付"
      width="540px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="currentBilling" class="payment-summary-box">
        <div class="summary-line">
          <span>账单单号：</span><b>{{ currentBilling.billNo }}</b>
        </div>
        <div class="summary-line">
          <span>就养长者：</span><span>{{ currentBilling.elderName }} ({{ currentBilling.bedInfo }})</span>
        </div>
        <div class="summary-line">
          <span>账单月份：</span><span>{{ currentBilling.billMonth }}</span>
        </div>
        <div class="summary-line highlight-line">
          <span>应收总额：¥{{ Number(currentBilling.totalAmount).toFixed(2) }}</span>
          <span>已收：¥{{ Number(currentBilling.paidAmount).toFixed(2) }}</span>
          <span class="danger-text"><b>当前待缴差额：¥{{ Number(currentBilling.unpaidAmount).toFixed(2) }}</b></span>
        </div>
      </div>

      <el-form ref="paymentFormRef" :model="paymentForm" :rules="paymentRules" label-width="110px" style="margin-top: 16px">
        <el-form-item label="本次缴费金额" prop="amount">
          <el-input-number
            v-model="paymentForm.amount"
            :min="0.01"
            :max="currentBilling ? Number(currentBilling.unpaidAmount) : 99999"
            :precision="2"
            :step="100"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select v-model="paymentForm.paymentMethod" style="width: 100%">
            <el-option label="模拟在线支付 (MOCK_ONLINE)" value="MOCK_ONLINE" />
            <el-option label="微信支付 (WECHAT_PAY)" value="WECHAT_PAY" />
            <el-option label="支付宝 (ALIPAY)" value="ALIPAY" />
            <el-option label="银行转账 (BANK_TRANSFER)" value="BANK_TRANSFER" />
            <el-option label="现金收款 (CASH)" value="CASH" />
            <el-option label="POS刷卡 (POS)" value="POS" />
          </el-select>
        </el-form-item>
        <el-form-item label="外部流水号">
          <el-input v-model="paymentForm.transactionNo" placeholder="如支付宝/银行流水交易号（选填）" />
        </el-form-item>
        <el-form-item label="支付幂等键" prop="idempotencyKey">
          <el-input v-model="paymentForm.idempotencyKey" placeholder="客户端防重全局唯一键">
            <template #append>
              <el-button @click="generateNewIdemKey">换一个</el-button>
            </template>
          </el-input>
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon> 提示：相同幂等键发起多次请求将被后端拦截去重，仅记入一笔有效收款。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paymentVisible = false">取消</el-button>
        <el-button type="success" :loading="paymentSubmitting" @click="handleSubmitPayment">
          确认收款入账
        </el-button>
      </template>
    </el-dialog>

    <!-- 对话框 3：申请退款 -->
    <el-dialog
      v-model="refundVisible"
      title="申请收款退款"
      width="520px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="currentPayment" class="payment-summary-box">
        <div class="summary-line">
          <span>原收款流水号：</span><b>{{ currentPayment.paymentNo }}</b>
        </div>
        <div class="summary-line">
          <span>就养长者：</span><span>{{ currentPayment.elderName }}</span>
        </div>
        <div class="summary-line">
          <span>原收款金额：</span><span>¥{{ Number(currentPayment.amount).toFixed(2) }}</span>
        </div>
        <div class="summary-line highlight-line">
          <span class="warning-text"><b>当前剩余最大可退金额：¥{{ Number(currentPayment.refundableAmount).toFixed(2) }}</b></span>
        </div>
      </div>

      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="110px" style="margin-top: 16px">
        <el-form-item label="申请退款金额" prop="amount">
          <el-input-number
            v-model="refundForm.amount"
            :min="0.01"
            :max="currentPayment ? Number(currentPayment.refundableAmount) : 0"
            :precision="2"
            :step="100"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="退款原因说明" prop="reason">
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请详细说明退款原因（如老人中途请假离院退伙食费、提前退住平账等）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="handleSubmitRefund">
          提交退款申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 对话框 4：审核退款 -->
    <el-dialog
      v-model="auditVisible"
      title="审批退款申请"
      width="480px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="currentRefund" class="payment-summary-box">
        <div class="summary-line">
          <span>退款流水号：</span><b>{{ currentRefund.refundNo }}</b>
        </div>
        <div class="summary-line">
          <span>长者姓名：</span><span>{{ currentRefund.elderName }}</span>
        </div>
        <div class="summary-line">
          <span>申请退款金额：</span><b class="danger-text">¥{{ Number(currentRefund.amount).toFixed(2) }}</b>
        </div>
        <div class="summary-line">
          <span>退款具体原因：</span><span>{{ currentRefund.reason }}</span>
        </div>
      </div>

      <el-form :model="auditForm" label-width="100px" style="margin-top: 16px">
        <el-form-item label="审批结论" required>
          <el-radio-group v-model="auditForm.approved">
            <el-radio :label="true">同意退款并冲减账单</el-radio>
            <el-radio :label="false">驳回退款申请</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批批注">
          <el-input v-model="auditForm.remark" placeholder="填写审批批复说明或驳回理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="handleSubmitAudit">
          确定审批
        </el-button>
      </template>
    </el-dialog>

    <!-- 对话框 5：收费项目新增/编辑 -->
    <el-dialog
      v-model="feeItemDialogVisible"
      :title="feeItemForm.id ? '编辑收费项目' : '新增收费标准项目'"
      width="560px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="feeItemFormRef" :model="feeItemForm" :rules="feeItemRules" label-width="110px">
        <el-form-item label="项目编码" prop="itemCode">
          <el-input v-model="feeItemForm.itemCode" placeholder="如 FEE_BED_DELUXE" :disabled="!!feeItemForm.id" />
        </el-form-item>
        <el-form-item label="项目名称" prop="itemName">
          <el-input v-model="feeItemForm.itemName" placeholder="如 豪华双人间床位费" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="费用大类" prop="category">
              <el-select v-model="feeItemForm.category" style="width: 100%">
                <el-option label="床位费 (BED)" value="BED" />
                <el-option label="护理费 (CARE)" value="CARE" />
                <el-option label="餐饮费 (MEAL)" value="MEAL" />
                <el-option label="增值服务费 (SERVICE)" value="SERVICE" />
                <el-option label="医疗耗材 (MEDICAL)" value="MEDICAL" />
                <el-option label="入住押金 (DEPOSIT)" value="DEPOSIT" />
                <el-option label="其他 (OTHER)" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计费周期" prop="billingCycle">
              <el-select v-model="feeItemForm.billingCycle" style="width: 100%">
                <el-option label="按月计费 (MONTHLY)" value="MONTHLY" />
                <el-option label="按日计费 (DAILY)" value="DAILY" />
                <el-option label="按次计费 (ONCE)" value="ONCE" />
                <el-option label="一次性收取 (ONE_TIME)" value="ONE_TIME" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标准单价(元)" prop="price">
              <el-input-number v-model="feeItemForm.price" :min="0" :precision="2" :step="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位" prop="unit">
              <el-input v-model="feeItemForm.unit" placeholder="如 月、天、次、件" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="项目说明">
          <el-input v-model="feeItemForm.description" type="textarea" :rows="2" placeholder="费用项目涵盖内容或计费规则说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feeItemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="feeItemSubmitting" @click="handleSubmitFeeItem">
          保存标准
        </el-button>
      </template>
    </el-dialog>

    <!-- 抽屉：账单全景详情与项目快照 -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="月度账单全景详情"
      size="620px"
      destroy-on-close
    >
      <div v-if="selectedBilling" class="billing-detail-view">
        <el-descriptions title="基础信息" :column="2" border size="small">
          <el-descriptions-item label="账单编号">{{ selectedBilling.billNo }}</el-descriptions-item>
          <el-descriptions-item label="账单月份">{{ selectedBilling.billMonth }}</el-descriptions-item>
          <el-descriptions-item label="就养长者">{{ selectedBilling.elderName }} ({{ selectedBilling.elderNo }})</el-descriptions-item>
          <el-descriptions-item label="当前床位">{{ selectedBilling.bedInfo }}</el-descriptions-item>
          <el-descriptions-item label="出账日期">{{ selectedBilling.billDate }}</el-descriptions-item>
          <el-descriptions-item label="付款截止日">{{ selectedBilling.dueDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="账单状态">
            <el-tag :type="getBillingTagType(selectedBilling.status)">
              {{ selectedBilling.statusName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建经办人">{{ selectedBilling.createdByName }}</el-descriptions-item>
        </el-descriptions>

        <div class="amount-card-grid">
          <div class="amt-box">
            <span class="label">应收总额</span>
            <span class="val">¥{{ Number(selectedBilling.totalAmount).toFixed(2) }}</span>
          </div>
          <div class="amt-box success">
            <span class="label">已收金额</span>
            <span class="val">¥{{ Number(selectedBilling.paidAmount).toFixed(2) }}</span>
          </div>
          <div class="amt-box danger">
            <span class="label">待付欠额</span>
            <span class="val">¥{{ Number(selectedBilling.unpaidAmount).toFixed(2) }}</span>
          </div>
        </div>

        <h4 class="sub-sec-title">收费明细快照清单</h4>
        <el-table :data="selectedBilling.items" border size="small" style="width: 100%">
          <el-table-column prop="itemNameSnapshot" label="项目名称快照" min-width="160" />
          <el-table-column label="计费周期" width="170" align="center">
            <template #default="{ row }">
              <span v-if="row.periodStart && row.periodEnd" class="period-text">
                {{ row.periodStart }} ~ {{ row.periodEnd }}
              </span>
              <span v-else class="text-muted">当期结算</span>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="70" align="center" />
          <el-table-column prop="unitPrice" label="单价" width="100" align="right">
            <template #default="{ row }">¥{{ Number(row.unitPrice).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="100" align="right">
            <template #default="{ row }"><b>¥{{ Number(row.amount).toFixed(2) }}</b></template>
          </el-table-column>
        </el-table>

        <div v-if="selectedBilling.remark" class="remark-block">
          <b>备注：</b>{{ selectedBilling.remark }}
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus,
  Refresh,
  Search,
  RefreshRight,
  Tickets,
  CircleCheckFilled,
  WarningFilled,
  RefreshLeft,
  Clock,
  DocumentChecked,
  InfoFilled
} from '@element-plus/icons-vue'
import {
  BillingVO,
  BillingQueryDTO,
  BillingCreateDTO,
  BillingItemDTO,
  PaymentRecordVO,
  PaymentQueryDTO,
  PaymentCreateDTO,
  RefundRecordVO,
  RefundQueryDTO,
  RefundApplyDTO,
  RefundAuditDTO,
  FeeItemVO,
  FeeItemQueryDTO,
  FeeItemDTO,
  FinanceSummaryVO,
  pageBillings,
  getBillingDetail,
  createBilling,
  cancelBilling,
  getFinanceSummary,
  pagePayments,
  registerPayment,
  pageRefunds,
  applyRefund,
  auditRefund,
  pageFeeItems,
  listActiveFeeItems,
  createFeeItem,
  updateFeeItem,
  toggleFeeItemStatus
} from '@/api/finance'
import { fetchElders } from '@/api/elder'

// 活跃选项卡
const activeTab = ref('billings')
const ledgerType = ref<'payments' | 'refunds'>('payments')

// 顶部 KPI 指标
const summary = ref<FinanceSummaryVO>({
  totalBilledAmount: 0,
  totalPaidAmount: 0,
  totalUnpaidAmount: 0,
  unpaidBillCount: 0,
  partialBillCount: 0,
  paidBillCount: 0,
  overdueBillCount: 0,
  totalRefundAmount: 0
})

const loadSummary = async () => {
  try {
    const res = await getFinanceSummary()
    if (res.data) {
      summary.value = res.data
    }
  } catch (err) {
    console.error('加载财务统计指标失败', err)
  }
}

/* ================= 1. 账单列表与管理 ================= */
const billingLoading = ref(false)
const billingList = ref<BillingVO[]>([])
const billingTotal = ref(0)
const billingQuery = reactive<BillingQueryDTO>({
  billMonth: '',
  status: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const loadBillings = async () => {
  billingLoading.value = true
  try {
    const res = await pageBillings(billingQuery)
    if (res.data) {
      billingList.value = res.data.list
      billingTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载账单失败', err)
  } finally {
    billingLoading.value = false
  }
}

const handleBillingSearch = () => {
  billingQuery.pageNum = 1
  loadBillings()
}

const handleBillingReset = () => {
  billingQuery.billMonth = ''
  billingQuery.status = ''
  billingQuery.keyword = ''
  billingQuery.pageNum = 1
  loadBillings()
}

const handleCancelBilling = async (row: BillingVO) => {
  try {
    await cancelBilling(row.id)
    ElMessage.success('账单已成功作废')
    loadBillings()
    loadSummary()
  } catch (err: any) {
    ElMessage.error(err.message || '作废账单失败')
  }
}

// 账单详情抽屉
const detailDrawerVisible = ref(false)
const selectedBilling = ref<BillingVO | null>(null)
const handleViewBilling = async (row: BillingVO) => {
  try {
    const res = await getBillingDetail(row.id)
    selectedBilling.value = res.data
    detailDrawerVisible.value = true
  } catch (err: any) {
    ElMessage.error(err.message || '获取账单详情失败')
  }
}

/* ================= 2. 生成账单弹窗 ================= */
const createBillingVisible = ref(false)
const createBillingSubmitting = ref(false)
const elderOptions = ref<any[]>([])
const activeFeeItems = ref<FeeItemVO[]>([])
const billingFormRef = ref()

interface BillingFormState {
  elderId?: number
  billMonth: string
  billDate: string
  dueDate: string
  remark: string
  items: BillingItemDTO[]
}

const billingForm = reactive<BillingFormState>({
  elderId: undefined,
  billMonth: '',
  billDate: '',
  dueDate: '',
  remark: '',
  items: []
})

const billingRules = {
  elderId: [{ required: true, message: '请选择就养长者', trigger: 'change' }],
  billMonth: [{ required: true, message: '请选择账单月份', trigger: 'change' }],
  billDate: [{ required: true, message: '请选择出账日期', trigger: 'change' }]
}

const calcFormTotalAmount = computed(() => {
  return billingForm.items.reduce((acc, cur) => {
    return acc + (cur.quantity || 0) * (cur.unitPrice || 0)
  }, 0)
})

const handleOpenCreateBilling = async () => {
  // 默认当月
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const curMonth = `${y}-${m}`
  const today = `${y}-${m}-${d}`

  // 默认到期日 15 天后
  const due = new Date(now.getTime() + 14 * 24 * 3600 * 1000)
  const dy = due.getFullYear()
  const dm = String(due.getMonth() + 1).padStart(2, '0')
  const dd = String(due.getDate()).padStart(2, '0')

  billingForm.elderId = undefined
  billingForm.billMonth = curMonth
  billingForm.billDate = today
  billingForm.dueDate = `${dy}-${dm}-${dd}`
  billingForm.remark = ''
  billingForm.items = []

  // 预加载长者与收费项目
  try {
    const elderRes = await fetchElders({ pageSize: 100 })
    if (elderRes.data) {
      elderOptions.value = elderRes.data.list
    }
    const feeRes = await listActiveFeeItems()
    if (feeRes.data) {
      activeFeeItems.value = feeRes.data
    }
  } catch (err) {
    console.error(err)
  }

  // 默认添加一项
  handleAddBillingItem()
  createBillingVisible.value = true
}

const handleAddBillingItem = () => {
  billingForm.items.push({
    feeItemId: 0,
    itemNameSnapshot: '',
    periodStart: '',
    periodEnd: '',
    quantity: 1,
    unitPrice: 0
  })
}

const handleRemoveBillingItem = (idx: number) => {
  billingForm.items.splice(idx, 1)
}

const handleFeeItemSelect = (row: BillingItemDTO, feeItemId: number) => {
  const found = activeFeeItems.value.find(item => item.id === feeItemId)
  if (found) {
    row.itemNameSnapshot = found.itemName
    row.unitPrice = Number(found.price)
  }
}

const handleSubmitCreateBilling = async () => {
  if (!billingFormRef.value) return
  await billingFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (!billingForm.items || billingForm.items.length === 0) {
      ElMessage.warning('请至少添加一项收费明细')
      return
    }
    for (const it of billingForm.items) {
      if (!it.itemNameSnapshot) {
        ElMessage.warning('收费明细的项目名称不能为空')
        return
      }
    }

    createBillingSubmitting.value = true
    try {
      const payload: BillingCreateDTO = {
        elderId: billingForm.elderId!,
        billMonth: billingForm.billMonth,
        billDate: billingForm.billDate,
        dueDate: billingForm.dueDate,
        remark: billingForm.remark,
        items: billingForm.items
      }
      await createBilling(payload)
      ElMessage.success('月度账单生成成功')
      createBillingVisible.value = false
      loadBillings()
      loadSummary()
    } catch (err: any) {
      ElMessage.error(err.message || '生成账单失败')
    } finally {
      createBillingSubmitting.value = false
    }
  })
}

/* ================= 3. 登记缴费 (模拟支付 & 幂等控制) ================= */
const paymentVisible = ref(false)
const paymentSubmitting = ref(false)
const currentBilling = ref<BillingVO | null>(null)
const paymentFormRef = ref()

const paymentForm = reactive<PaymentCreateDTO>({
  billingId: 0,
  amount: 0,
  paymentMethod: 'MOCK_ONLINE',
  transactionNo: '',
  idempotencyKey: ''
})

const paymentRules = {
  amount: [{ required: true, message: '请输入收款金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
  idempotencyKey: [{ required: true, message: '幂等键不能为空', trigger: 'blur' }]
}

const generateNewIdemKey = () => {
  paymentForm.idempotencyKey = 'IDEM_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8).toUpperCase()
}

const handleOpenPayment = (row: BillingVO) => {
  currentBilling.value = row
  paymentForm.billingId = row.id
  paymentForm.amount = Number(row.unpaidAmount)
  paymentForm.paymentMethod = 'MOCK_ONLINE'
  paymentForm.transactionNo = ''
  generateNewIdemKey()
  paymentVisible.value = true
}

const handleSubmitPayment = async () => {
  if (!paymentFormRef.value) return
  await paymentFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    paymentSubmitting.value = true
    try {
      await registerPayment(paymentForm)
      ElMessage.success('缴费收款入账成功！已实时平账与更新账单状态')
      paymentVisible.value = false
      loadBillings()
      loadSummary()
      if (ledgerType.value === 'payments') {
        loadPayments()
      }
    } catch (err: any) {
      ElMessage.error(err.message || '登记缴费失败')
    } finally {
      paymentSubmitting.value = false
    }
  })
}

/* ================= 4. 收款与退款流水明细 ================= */
const paymentLoading = ref(false)
const paymentList = ref<PaymentRecordVO[]>([])
const paymentTotal = ref(0)
const paymentQuery = reactive<PaymentQueryDTO>({
  paymentMethod: '',
  status: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const loadPayments = async () => {
  paymentLoading.value = true
  try {
    const res = await pagePayments(paymentQuery)
    if (res.data) {
      paymentList.value = res.data.list
      paymentTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载收款流水失败', err)
  } finally {
    paymentLoading.value = false
  }
}

const handlePaymentReset = () => {
  paymentQuery.paymentMethod = ''
  paymentQuery.status = ''
  paymentQuery.keyword = ''
  paymentQuery.pageNum = 1
  loadPayments()
}

// 申请退款弹窗
const refundVisible = ref(false)
const refundSubmitting = ref(false)
const currentPayment = ref<PaymentRecordVO | null>(null)
const refundFormRef = ref()

const refundForm = reactive<RefundApplyDTO>({
  paymentId: 0,
  amount: 0,
  reason: ''
})

const refundRules = {
  amount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写退款原因', trigger: 'blur' }]
}

const handleOpenRefund = (row: PaymentRecordVO) => {
  currentPayment.value = row
  refundForm.paymentId = row.id
  refundForm.amount = Number(row.refundableAmount)
  refundForm.reason = ''
  refundVisible.value = true
}

const handleSubmitRefund = async () => {
  if (!refundFormRef.value) return
  await refundFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    refundSubmitting.value = true
    try {
      await applyRefund(refundForm)
      ElMessage.success('退款申请提交成功，请在退款审批列表中审核通过')
      refundVisible.value = false
      ledgerType.value = 'refunds'
      loadRefunds()
      loadPayments()
    } catch (err: any) {
      ElMessage.error(err.message || '申请退款失败')
    } finally {
      refundSubmitting.value = false
    }
  })
}

// 退款审核视图
const refundLoading = ref(false)
const refundList = ref<RefundRecordVO[]>([])
const refundTotal = ref(0)
const refundQuery = reactive<RefundQueryDTO>({
  status: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const loadRefunds = async () => {
  refundLoading.value = true
  try {
    const res = await pageRefunds(refundQuery)
    if (res.data) {
      refundList.value = res.data.list
      refundTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载退款列表失败', err)
  } finally {
    refundLoading.value = false
  }
}

const handleRefundReset = () => {
  refundQuery.status = ''
  refundQuery.keyword = ''
  refundQuery.pageNum = 1
  loadRefunds()
}

// 审批退款弹窗
const auditVisible = ref(false)
const auditSubmitting = ref(false)
const currentRefund = ref<RefundRecordVO | null>(null)
const auditForm = reactive<RefundAuditDTO>({
  approved: true,
  remark: ''
})

const handleOpenAudit = (row: RefundRecordVO) => {
  currentRefund.value = row
  auditForm.approved = true
  auditForm.remark = ''
  auditVisible.value = true
}

const handleSubmitAudit = async () => {
  if (!currentRefund.value) return
  auditSubmitting.value = true
  try {
    await auditRefund(currentRefund.value.id, auditForm)
    ElMessage.success(auditForm.approved ? '退款审批通过！已自动冲减账单已实收金额' : '已驳回该退款申请')
    auditVisible.value = false
    loadRefunds()
    loadBillings()
    loadSummary()
  } catch (err: any) {
    ElMessage.error(err.message || '退款审批处理失败')
  } finally {
    auditSubmitting.value = false
  }
}

const loadLedger = () => {
  if (ledgerType.value === 'payments') {
    loadPayments()
  } else {
    loadRefunds()
  }
}

/* ================= 5. 收费项目标准配置 ================= */
const feeItemLoading = ref(false)
const feeItemList = ref<FeeItemVO[]>([])
const feeItemTotal = ref(0)
const feeItemQuery = reactive<FeeItemQueryDTO>({
  category: '',
  status: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const loadFeeItems = async () => {
  feeItemLoading.value = true
  try {
    const res = await pageFeeItems(feeItemQuery)
    if (res.data) {
      feeItemList.value = res.data.list
      feeItemTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载收费项目失败', err)
  } finally {
    feeItemLoading.value = false
  }
}

const handleFeeItemReset = () => {
  feeItemQuery.category = ''
  feeItemQuery.status = ''
  feeItemQuery.keyword = ''
  feeItemQuery.pageNum = 1
  loadFeeItems()
}

const handleToggleFeeStatus = async (row: FeeItemVO, val: boolean) => {
  const newStatus = val ? 'ACTIVE' : 'INACTIVE'
  try {
    await toggleFeeItemStatus(row.id, newStatus)
    ElMessage.success(`收费项目已${val ? '启用' : '停用'}`)
    row.status = newStatus
  } catch (err: any) {
    ElMessage.error(err.message || '更新状态失败')
  }
}

// 收费项目新增/编辑弹窗
const feeItemDialogVisible = ref(false)
const feeItemSubmitting = ref(false)
const feeItemFormRef = ref()

interface FeeItemFormState {
  id?: number
  itemCode: string
  itemName: string
  category: string
  billingCycle: string
  price: number
  unit: string
  description: string
}

const feeItemForm = reactive<FeeItemFormState>({
  id: undefined,
  itemCode: '',
  itemName: '',
  category: 'BED',
  billingCycle: 'MONTHLY',
  price: 0,
  unit: '月',
  description: ''
})

const feeItemRules = {
  itemCode: [{ required: true, message: '请输入项目唯一编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入收费项目名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择费用大类', trigger: 'change' }],
  billingCycle: [{ required: true, message: '请选择计费周期', trigger: 'change' }],
  price: [{ required: true, message: '请输入标准单价', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }]
}

const handleOpenCreateFeeItem = () => {
  feeItemForm.id = undefined
  feeItemForm.itemCode = 'FEE_' + Math.random().toString(36).substring(2, 8).toUpperCase()
  feeItemForm.itemName = ''
  feeItemForm.category = 'BED'
  feeItemForm.billingCycle = 'MONTHLY'
  feeItemForm.price = 1000
  feeItemForm.unit = '月'
  feeItemForm.description = ''
  feeItemDialogVisible.value = true
}

const handleOpenEditFeeItem = (row: FeeItemVO) => {
  feeItemForm.id = row.id
  feeItemForm.itemCode = row.itemCode
  feeItemForm.itemName = row.itemName
  feeItemForm.category = row.category
  feeItemForm.billingCycle = row.billingCycle
  feeItemForm.price = Number(row.price)
  feeItemForm.unit = row.unit
  feeItemForm.description = row.description || ''
  feeItemDialogVisible.value = true
}

const handleSubmitFeeItem = async () => {
  if (!feeItemFormRef.value) return
  await feeItemFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    feeItemSubmitting.value = true
    try {
      const payload: FeeItemDTO = {
        itemCode: feeItemForm.itemCode,
        itemName: feeItemForm.itemName,
        category: feeItemForm.category,
        billingCycle: feeItemForm.billingCycle,
        price: feeItemForm.price,
        unit: feeItemForm.unit,
        description: feeItemForm.description
      }
      if (feeItemForm.id) {
        await updateFeeItem(feeItemForm.id, payload)
        ElMessage.success('收费标准修改成功')
      } else {
        await createFeeItem(payload)
        ElMessage.success('收费标准新增成功')
      }
      feeItemDialogVisible.value = false
      loadFeeItems()
    } catch (err: any) {
      ElMessage.error(err.message || '保存收费项目失败')
    } finally {
      feeItemSubmitting.value = false
    }
  })
}

// 标签页切换响应
const handleTabChange = (tabName: string) => {
  if (tabName === 'billings') {
    loadBillings()
  } else if (tabName === 'ledger') {
    loadLedger()
  } else if (tabName === 'feeItems') {
    loadFeeItems()
  }
}

// 标签样式工具函数
const getBillingTagType = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'PAID':
      return 'success'
    case 'PARTIAL':
      return 'warning'
    case 'OVERDUE':
      return 'danger'
    case 'CANCELLED':
      return 'info'
    default:
      return 'info'
  }
}

const getRefundTagType = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'SUCCESS':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

onMounted(() => {
  loadSummary()
  loadBillings()
})
</script>

<style scoped>
.finance-page {
  padding: 16px;
  background-color: #f6f8fb;
  min-height: calc(100vh - 64px);
}

/* 顶部 KPI 卡片 */
.kpi-row {
  margin-bottom: 16px;
}
.kpi-card {
  border-radius: 8px;
  transition: all 0.25s ease;
  border: 1px solid #eef2f8;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
}
.kpi-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  padding: 16px;
}
.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 14px;
  flex-shrink: 0;
}
.total-billed .kpi-icon { background: #e8f3ff; color: #1677ff; }
.total-paid .kpi-icon { background: #e6f8ea; color: #52c41a; }
.total-unpaid .kpi-icon { background: #fff1f0; color: #ff4d4f; }
.total-refund .kpi-icon { background: #fffbe6; color: #faad14; }
.unpaid-count .kpi-icon { background: #f0f5ff; color: #2f54eb; }
.paid-count .kpi-icon { background: #f9f0ff; color: #722ed1; }

.kpi-content {
  display: flex;
  flex-direction: column;
}
.kpi-title {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
}
.kpi-num {
  font-size: 20px;
  font-weight: 700;
  color: #262626;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
.primary-num { color: #1677ff; }
.success-num { color: #52c41a; }
.danger-num { color: #ff4d4f; }
.warning-num { color: #faad14; }
.purple-num { color: #722ed1; }

/* 主工作台选项卡 */
.main-card {
  border-radius: 8px;
  border: 1px solid #eef2f8;
}
.tab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.tab-title {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
}
.tab-subtitle {
  font-size: 12px;
  color: #8c8c8c;
}

.filter-form {
  margin-bottom: 12px;
}

.elder-cell-name {
  font-weight: 600;
  display: block;
  color: #1f1f1f;
}
.sub-bed-tag {
  font-size: 12px;
  color: #8c8c8c;
}

.amount-val {
  font-weight: 600;
  color: #262626;
}
.paid-val {
  font-weight: 600;
  color: #52c41a;
}
.unpaid-val {
  font-weight: 700;
  color: #ff4d4f;
}
.zero-val {
  color: #8c8c8c;
}
.refundable-val {
  font-weight: 600;
  color: #faad14;
}
.refund-val {
  font-weight: 600;
  color: #ff4d4f;
}

.overdue-text {
  color: #ff4d4f;
  font-weight: 600;
}
.overdue-badge {
  font-size: 11px;
  color: #ff4d4f;
  background: #fff1f0;
  border-radius: 2px;
  padding: 0 4px;
  display: inline-block;
  margin-top: 2px;
}

.mono-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #595959;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 流水子切换 */
.sub-tab-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.ledger-block {
  margin-top: 8px;
}

/* 账单生成明细区 */
.billing-items-section {
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  padding: 12px;
  margin-top: 12px;
}
.items-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.items-title {
  font-size: 13px;
  color: #595959;
}
.subtotal-val {
  font-weight: 600;
  color: #1677ff;
}
.billing-total-preview {
  margin-top: 12px;
  text-align: right;
  font-size: 14px;
}
.total-preview-num {
  font-size: 20px;
  color: #ff4d4f;
  margin-left: 8px;
}

/* 支付摘要区 */
.payment-summary-box {
  background: #f8fafc;
  border-radius: 6px;
  padding: 12px 16px;
  border-left: 4px solid #1677ff;
}
.summary-line {
  font-size: 13px;
  line-height: 24px;
  color: #595959;
}
.summary-line b {
  color: #1f1f1f;
}
.highlight-line {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #e2e8f0;
  display: flex;
  justify-content: space-between;
}
.danger-text { color: #ff4d4f; }
.warning-text { color: #faad14; }
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
  line-height: 16px;
}

/* 详情抽屉 */
.billing-detail-view {
  padding: 4px;
}
.amount-card-grid {
  display: flex;
  gap: 12px;
  margin: 16px 0;
}
.amt-box {
  flex: 1;
  background: #f0f7ff;
  border: 1px solid #bae0ff;
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
}
.amt-box.success {
  background: #f6ffed;
  border-color: #b7eb8f;
}
.amt-box.danger {
  background: #fff1f0;
  border-color: #ffa39e;
}
.amt-box .label {
  font-size: 12px;
  color: #8c8c8c;
}
.amt-box .val {
  font-size: 18px;
  font-weight: 700;
  color: #1677ff;
  margin-top: 4px;
}
.amt-box.success .val { color: #52c41a; }
.amt-box.danger .val { color: #ff4d4f; }

.sub-sec-title {
  margin: 16px 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
}
.period-text {
  font-size: 12px;
  color: #595959;
}
.text-muted {
  color: #bfbfbf;
}
.remark-block {
  margin-top: 16px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 13px;
  color: #595959;
}
</style>
