<template>
  <div class="dining-page">
    <!-- 顶部 KPI 统计看板 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card total-orders">
          <div class="kpi-icon"><el-icon><Tickets /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">今日订餐总量</span>
            <span class="kpi-num">{{ summary.todayOrderCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card pending-deliver">
          <div class="kpi-icon"><el-icon><Clock /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">待配餐/待配送</span>
            <span class="kpi-num warning-num">{{ summary.pendingDeliverCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card delivered">
          <div class="kpi-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">今日已完成配送</span>
            <span class="kpi-num success-num">{{ summary.deliveredCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card today-revenue">
          <div class="kpi-icon"><el-icon><Money /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">今日订餐流水 (元)</span>
            <span class="kpi-num primary-num">¥{{ Number(summary.todayRevenue || 0).toFixed(2) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card active-dishes">
          <div class="kpi-icon"><el-icon><DishIcon /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">在售营养菜品</span>
            <span class="kpi-num">{{ summary.activeDishCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="kpi-card today-menus">
          <div class="kpi-icon"><el-icon><Calendar /></el-icon></div>
          <div class="kpi-content">
            <span class="kpi-title">今日已编排菜单</span>
            <span class="kpi-num purple-num">{{ summary.todayMenuCount }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 选项卡主内容区 -->
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 选项卡 1：订餐全流程工作台 -->
        <el-tab-pane label="长者订餐订单工作台" name="orders">
          <div class="tab-header">
            <div class="header-text">
              <h3 class="tab-title">订餐订单与配餐配送管理</h3>
              <span class="tab-subtitle">支持长者按日期餐次点餐、自动过敏禁忌筛查、餐次截止时间控制、菜品名称与单价快照审计</span>
            </div>
            <div class="header-btns">
              <el-button type="primary" :icon="Plus" @click="handleOpenCreateOrder">
                为长者订餐
              </el-button>
              <el-button :icon="Refresh" @click="loadOrders">刷新</el-button>
            </div>
          </div>

          <!-- 订单查询筛选 -->
          <el-form :inline="true" :model="orderQuery" class="filter-form">
            <el-form-item label="就餐日期">
              <el-date-picker
                v-model="orderQuery.orderDate"
                type="date"
                placeholder="选择就餐日期"
                value-format="YYYY-MM-DD"
                clearable
                style="width: 150px"
              />
            </el-form-item>
            <el-form-item label="餐次">
              <el-select v-model="orderQuery.mealType" placeholder="全部餐次" clearable style="width: 120px">
                <el-option label="早餐 (BREAKFAST)" value="BREAKFAST" />
                <el-option label="午餐 (LUNCH)" value="LUNCH" />
                <el-option label="晚餐 (DINNER)" value="DINNER" />
              </el-select>
            </el-form-item>
            <el-form-item label="订单状态">
              <el-select v-model="orderQuery.status" placeholder="全部状态" clearable style="width: 140px">
                <el-option label="已确认/待配送" value="CONFIRMED" />
                <el-option label="已配送/完成" value="DELIVERED" />
                <el-option label="已取消" value="CANCELLED" />
              </el-select>
            </el-form-item>
            <el-form-item label="就餐长者">
              <el-select v-model="orderQuery.elderId" placeholder="选择长者" filterable clearable style="width: 160px">
                <el-option
                  v-for="elder in elderOptions"
                  :key="elder.id"
                  :label="elder.name + ' (' + elder.elderNo + ')'"
                  :value="elder.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="订单编号">
              <el-input v-model="orderQuery.keyword" placeholder="搜索订单编号" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleQueryOrders">查询</el-button>
              <el-button :icon="RefreshLeft" @click="handleResetOrders">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 订单数据表格 -->
          <el-table v-loading="ordersLoading" :data="orderList" border stripe style="width: 100%" class="custom-table">
            <el-table-column prop="orderNo" label="订单编号" width="160">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" class="mono-link" @click="handleViewOrderDetail(row.id)">
                  {{ row.orderNo }}
                </el-link>
              </template>
            </el-table-column>

            <el-table-column label="就餐长者 / 床位" width="180">
              <template #default="{ row }">
                <div class="elder-cell">
                  <span class="elder-name">{{ row.elderName }}</span>
                  <span class="elder-bed">{{ row.bedInfo || '-' }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="就餐时间与餐次" width="160">
              <template #default="{ row }">
                <div><strong>{{ row.orderDate }}</strong></div>
                <el-tag size="small" :type="getMealTypeTag(row.mealType)" effect="light">
                  {{ row.mealTypeName }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="订购菜品明细 (快照)" min-width="240">
              <template #default="{ row }">
                <div class="items-cell">
                  <span
                    v-for="(item, idx) in row.items"
                    :key="idx"
                    class="dish-snapshot-badge"
                  >
                    {{ item.dishNameSnapshot }} ×{{ item.quantity }}
                    <span class="price-hint">(¥{{ Number(item.unitPrice).toFixed(2) }})</span>
                  </span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="totalAmount" label="总金额" width="120" align="right">
              <template #default="{ row }">
                <span class="amount-text">¥{{ Number(row.totalAmount).toFixed(2) }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="statusName" label="订单状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="getOrderStatusTag(row.status)" effect="plain">
                  {{ row.statusName }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="下单信息" width="160">
              <template #default="{ row }">
                <div class="small-text">{{ row.orderedByName || '系统' }}</div>
                <div class="text-muted small-text">{{ row.createdAt }}</div>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button link type="primary" size="small" @click="handleViewOrderDetail(row.id)">
                    详情
                  </el-button>

                  <el-button
                    v-if="row.status === 'CONFIRMED'"
                    link
                    type="success"
                    size="small"
                    @click="handleDeliverOrder(row.id)"
                  >
                    配送核销
                  </el-button>

                  <el-popconfirm
                    v-if="row.status === 'CONFIRMED'"
                    title="确定取消该订餐订单？(需在餐次截止时间前)"
                    confirm-button-text="确认取消"
                    cancel-button-text="保留"
                    @confirm="handleCancelOrder(row.id)"
                  >
                    <template #reference>
                      <el-button link type="danger" size="small">
                        取消
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="orderQuery.pageNum"
              v-model:page-size="orderQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="ordersTotal"
              @size-change="loadOrders"
              @current-change="loadOrders"
            />
          </div>
        </el-tab-pane>

        <!-- 选项卡 2：每日餐次菜单编排 -->
        <el-tab-pane label="每日餐次菜单编排" name="menus">
          <div class="tab-header">
            <div class="header-text">
              <h3 class="tab-title">餐次菜单计划编排</h3>
              <span class="tab-subtitle">每日早、中、晚餐次菜品上架，菜单发布时自动固化菜品价格快照</span>
            </div>
            <div class="header-btns">
              <el-button type="primary" :icon="Plus" @click="handleOpenCreateMenu">
                编排餐次菜单
              </el-button>
              <el-button :icon="Refresh" @click="loadMenus">刷新</el-button>
            </div>
          </div>

          <!-- 菜单筛选 -->
          <el-form :inline="true" :model="menuQuery" class="filter-form">
            <el-form-item label="菜单日期">
              <el-date-picker
                v-model="menuQuery.menuDate"
                type="date"
                placeholder="选择菜单日期"
                value-format="YYYY-MM-DD"
                clearable
                style="width: 150px"
              />
            </el-form-item>
            <el-form-item label="餐次">
              <el-select v-model="menuQuery.mealType" placeholder="全部餐次" clearable style="width: 130px">
                <el-option label="早餐" value="BREAKFAST" />
                <el-option label="午餐" value="LUNCH" />
                <el-option label="晚餐" value="DINNER" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="menuQuery.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="草稿" value="DRAFT" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="loadMenus">查询</el-button>
            </el-form-item>
          </el-form>

          <!-- 菜单表格 -->
          <el-table v-loading="menusLoading" :data="menuList" border stripe style="width: 100%" class="custom-table">
            <el-table-column prop="menuDate" label="菜单就餐日期" width="150" />
            <el-table-column prop="mealTypeName" label="餐次" width="120">
              <template #default="{ row }">
                <el-tag :type="getMealTypeTag(row.mealType)">{{ row.mealTypeName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="包含菜品明细" min-width="300">
              <template #default="{ row }">
                <div class="menu-dishes-wrap">
                  <el-tag
                    v-for="dish in row.items"
                    :key="dish.id"
                    class="mr-2 mb-1"
                    effect="plain"
                  >
                    {{ dish.dishName }} (¥{{ Number(dish.priceSnapshot).toFixed(2) }})
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="dishCount" label="菜品总数" width="100" align="center" />
            <el-table-column prop="statusName" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="light">
                  {{ row.statusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="编排人员" width="160">
              <template #default="{ row }">
                <div>{{ row.createdByName }}</div>
                <div class="text-muted small-text">{{ row.createdAt }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditMenu(row)">
                  修改菜单
                </el-button>
                <el-popconfirm title="确认删除该菜单配置？" @confirm="handleDeleteMenu(row.id)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="menuQuery.pageNum"
              v-model:page-size="menuQuery.pageSize"
              layout="total, prev, pager, next"
              :total="menusTotal"
              @current-change="loadMenus"
            />
          </div>
        </el-tab-pane>

        <!-- 选项卡 3：营养菜品台账 -->
        <el-tab-pane label="营养菜品台账" name="dishes">
          <div class="tab-header">
            <div class="header-text">
              <h3 class="tab-title">全院菜品与营养食谱库</h3>
              <span class="tab-subtitle">维护菜品编码、分类、单价、常见过敏原成分与适老营养烹饪描述</span>
            </div>
            <div class="header-btns">
              <el-button type="primary" :icon="Plus" @click="handleOpenCreateDish">
                新建菜品
              </el-button>
              <el-button :icon="Refresh" @click="loadDishes">刷新</el-button>
            </div>
          </div>

          <!-- 菜品筛选 -->
          <el-form :inline="true" :model="dishQuery" class="filter-form">
            <el-form-item label="分类">
              <el-select v-model="dishQuery.category" placeholder="全部分类" clearable style="width: 140px">
                <el-option label="主食碳水" value="STAPLE" />
                <el-option label="荤菜优质蛋白" value="MEAT" />
                <el-option label="素菜膳食纤维" value="VEGETABLE" />
                <el-option label="滋补汤羹" value="SOUP" />
                <el-option label="水果点心" value="FRUIT" />
                <el-option label="特需食疗" value="SPECIAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="dishQuery.status" placeholder="全部状态" clearable style="width: 110px">
                <el-option label="在售" value="ACTIVE" />
                <el-option label="停售" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键字">
              <el-input v-model="dishQuery.keyword" placeholder="编码 / 名称 / 过敏原" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="loadDishes">查询</el-button>
            </el-form-item>
          </el-form>

          <!-- 菜品表格 -->
          <el-table v-loading="dishesLoading" :data="dishList" border stripe style="width: 100%" class="custom-table">
            <el-table-column prop="dishCode" label="菜品编码" width="130" class-name="mono-link" />
            <el-table-column prop="dishName" label="菜品名称" width="180">
              <template #default="{ row }">
                <strong>{{ row.dishName }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="categoryName" label="分类" width="130">
              <template #default="{ row }">
                <el-tag effect="light">{{ row.categoryName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="单价" width="110" align="right">
              <template #default="{ row }">
                <span class="price-val">¥{{ Number(row.price).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="allergens" label="常见过敏原成分" width="180">
              <template #default="{ row }">
                <span v-if="row.allergens" class="allergen-pill">
                  <el-icon class="mr-1 text-danger"><Warning /></el-icon>
                  {{ row.allergens }}
                </span>
                <span v-else class="text-muted">无已知常见过敏原</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="营养特点与描述" min-width="220" show-overflow-tooltip />
            <el-table-column prop="statusName" label="售卖状态" width="110" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 'ACTIVE'"
                  active-text="在售"
                  inactive-text="停售"
                  inline-prompt
                  @change="(val: boolean) => handleToggleDishStatus(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditDish(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="dishQuery.pageNum"
              v-model:page-size="dishQuery.pageSize"
              layout="total, prev, pager, next"
              :total="dishesTotal"
              @current-change="loadDishes"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 弹窗 1：订餐下单弹窗 (包含长者档案过敏原/禁忌智能核验) -->
    <el-dialog
      v-model="orderDialogVisible"
      title="为长者办理点餐订餐"
      width="780px"
      destroy-on-close
      class="custom-dialog"
    >
      <el-form label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="就餐长者" required>
              <el-select
                v-model="createOrderForm.elderId"
                placeholder="请选择就餐长者"
                filterable
                style="width: 100%"
                @change="handleSelectElder"
              >
                <el-option
                  v-for="elder in elderOptions"
                  :key="elder.id"
                  :label="elder.name + ' (' + elder.elderNo + ')'"
                  :value="elder.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="就餐日期" required>
              <el-date-picker
                v-model="createOrderForm.orderDate"
                type="date"
                placeholder="选择就餐日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="就餐餐次" required>
          <el-radio-group v-model="createOrderForm.mealType">
            <el-radio label="BREAKFAST">早餐 (07:00截止取消)</el-radio>
            <el-radio label="LUNCH">午餐 (10:30截止取消)</el-radio>
            <el-radio label="DINNER">晚餐 (16:30截止取消)</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 长者膳食健康警示档案 -->
        <div v-if="selectedElderProfile" class="elder-profile-banner mb-3">
          <div class="banner-title">
            <el-icon class="mr-1 text-primary"><User /></el-icon>
            <strong>{{ selectedElderProfile.name }}</strong>
            <span class="text-muted ml-2">慢病情况: {{ selectedElderProfile.chronicDiseaseDesc || '无特殊' }}</span>
          </div>
          <div class="banner-body">
            <div class="tag-row">
              <span class="label">过敏史:</span>
              <el-tag v-if="selectedElderProfile.allergyDesc" type="danger" size="small" effect="dark">
                {{ selectedElderProfile.allergyDesc }}
              </el-tag>
              <span v-else class="text-muted">无已知过敏记录</span>
            </div>
            <div class="tag-row mt-1">
              <span class="label">饮食禁忌:</span>
              <el-tag v-if="selectedElderProfile.dietTaboo" type="warning" size="small">
                {{ selectedElderProfile.dietTaboo }}
              </el-tag>
              <span v-else class="text-muted">无特殊禁忌</span>
            </div>
          </div>
        </div>

        <!-- 菜品选购列表 -->
        <el-form-item label="选购菜品" required>
          <div class="dish-picker-box">
            <el-table :data="availableDishes" size="small" border max-height="240">
              <el-table-column label="选择" width="60" align="center">
                <template #default="{ row }">
                  <el-checkbox
                    :model-value="isDishSelected(row.id)"
                    @change="(val: boolean) => handleToggleSelectDish(row, val)"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="dishName" label="菜品名称" width="180">
                <template #default="{ row }">
                  <strong>{{ row.dishName }}</strong>
                  <span v-if="row.allergens" class="tag-allergen">({{ row.allergens }})</span>
                </template>
              </el-table-column>
              <el-table-column prop="categoryName" label="分类" width="110" />
              <el-table-column prop="price" label="单价" width="100" align="right">
                <template #default="{ row }">
                  ¥{{ Number(row.price).toFixed(2) }}
                </template>
              </el-table-column>
              <el-table-column label="份数" width="140" align="center">
                <template #default="{ row }">
                  <el-input-number
                    v-if="isDishSelected(row.id)"
                    v-model="getDishOrderItem(row.id).quantity"
                    :min="1"
                    :max="10"
                    size="small"
                  />
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>

        <!-- 智能风险核验警示框 -->
        <el-alert
          v-if="dietaryWarningResult && dietaryWarningResult.hasWarning"
          title="长者饮食安全预警提示 (触发过敏或饮食禁忌)"
          type="error"
          show-icon
          :closable="false"
          class="mb-3"
        >
          <template #default>
            <ul class="warning-list">
              <li v-for="(warn, idx) in dietaryWarningResult.warnings" :key="idx">
                {{ warn }}
              </li>
            </ul>
            <div class="override-checkbox mt-2">
              <el-checkbox v-model="createOrderForm.overrideWarning">
                我已与长者家属及照护人员充分沟通确认无碍，确认忽略提示强行下单
              </el-checkbox>
            </div>
          </template>
        </el-alert>

        <!-- 结算明细汇总栏 -->
        <div class="order-summary-bar">
          <div class="summary-left">
            已选菜品: <strong>{{ createOrderForm.items.length }}</strong> 种
          </div>
          <div class="summary-right">
            合计应收: <span class="total-price-highlight">¥{{ calcOrderTotalAmount.toFixed(2) }}</span>
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitCreateOrder">
          确认下单
        </el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2：编排每日菜单弹窗 -->
    <el-dialog
      v-model="menuDialogVisible"
      :title="(menuForm.id ? '编辑' : '编排') + '餐次菜单'"
      width="640px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜单日期" required>
              <el-date-picker
                v-model="menuForm.menuDate"
                type="date"
                placeholder="选择就餐日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="就餐餐次" required>
              <el-select v-model="menuForm.mealType" placeholder="选择餐次" style="width: 100%">
                <el-option label="早餐" value="BREAKFAST" />
                <el-option label="午餐" value="LUNCH" />
                <el-option label="晚餐" value="DINNER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="发布状态">
          <el-radio-group v-model="menuForm.status">
            <el-radio label="PUBLISHED">立即发布在售</el-radio>
            <el-radio label="DRAFT">存为草稿待审</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="上架菜品" required>
          <el-select
            v-model="menuForm.dishIds"
            multiple
            filterable
            placeholder="从在售菜品库中选择多道菜品"
            style="width: 100%"
          >
            <el-option
              v-for="dish in allDishes"
              :key="dish.id"
              :label="dish.dishName + ' (' + dish.categoryName + ' - ¥' + Number(dish.price).toFixed(2) + ')'"
              :value="dish.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitMenu">保存菜单</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 3：新建/编辑菜品弹窗 -->
    <el-dialog
      v-model="dishDialogVisible"
      :title="(dishForm.id ? '编辑' : '新建') + '菜品'"
      width="540px"
      destroy-on-close
    >
      <el-form ref="dishFormRef" :model="dishForm" :rules="dishRules" label-width="110px">
        <el-form-item label="菜品编码" prop="dishCode">
          <el-input v-model="dishForm.dishCode" placeholder="如: DISH010" :disabled="!!dishForm.id" />
        </el-form-item>
        <el-form-item label="菜品名称" prop="dishName">
          <el-input v-model="dishForm.dishName" placeholder="如: 铁棍山药炖排骨" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="dishForm.category" style="width: 100%">
            <el-option label="主食碳水 (STAPLE)" value="STAPLE" />
            <el-option label="荤菜优质蛋白 (MEAT)" value="MEAT" />
            <el-option label="素菜膳食纤维 (VEGETABLE)" value="VEGETABLE" />
            <el-option label="滋补汤羹 (SOUP)" value="SOUP" />
            <el-option label="水果点心 (FRUIT)" value="FRUIT" />
            <el-option label="特需食疗 (SPECIAL)" value="SPECIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜品单价(元)" prop="price">
          <el-input-number v-model="dishForm.price" :precision="2" :step="1" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="常见过敏原">
          <el-input v-model="dishForm.allergens" placeholder="如: 鱼类/海鲜、蛋类、坚果、乳制品、大豆、麸质" />
        </el-form-item>
        <el-form-item label="烹饪营养描述">
          <el-input v-model="dishForm.description" type="textarea" :rows="3" placeholder="适老化软烂程度、少盐少油等营养特点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitDish">保存菜品</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 4：订单详情与快照查看抽屉 -->
    <el-drawer v-model="orderDrawerVisible" title="订餐订单与菜品快照详情" size="560px" destroy-on-close>
      <div v-if="orderDetail" class="order-detail-wrap">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单单号" :span="2">
            <span class="mono-link">{{ orderDetail.orderNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="就餐长者">
            {{ orderDetail.elderName }} ({{ orderDetail.elderNo }})
          </el-descriptions-item>
          <el-descriptions-item label="床位位置">
            {{ orderDetail.bedInfo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="就餐日期">
            {{ orderDetail.orderDate }}
          </el-descriptions-item>
          <el-descriptions-item label="就餐餐次">
            <el-tag :type="getMealTypeTag(orderDetail.mealType)">{{ orderDetail.mealTypeName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getOrderStatusTag(orderDetail.status)">{{ orderDetail.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总金额">
            <strong class="text-danger">¥{{ Number(orderDetail.totalAmount).toFixed(2) }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="下单人">
            {{ orderDetail.orderedByName || '系统' }}
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">
            {{ orderDetail.createdAt }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="mt-4 mb-2">订单菜品明细快照 (不受后续改价改名影响)</h4>
        <el-table :data="orderDetail.items" border stripe size="small">
          <el-table-column prop="dishNameSnapshot" label="菜品名称(下单时)" />
          <el-table-column prop="unitPrice" label="单价快照" width="100" align="right">
            <template #default="{ row }">¥{{ Number(row.unitPrice).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="份数" width="70" align="center" />
          <el-table-column prop="lineAmount" label="小计" width="100" align="right">
            <template #default="{ row }"><strong>¥{{ Number(row.lineAmount).toFixed(2) }}</strong></template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Tickets,
  Clock,
  CircleCheckFilled,
  Money,
  Calendar,
  Search,
  Refresh,
  RefreshLeft,
  Plus,
  Warning,
  User,
  CoffeeCup as DishIcon
} from '@element-plus/icons-vue'
import {
  getDiningSummary,
  pageOrders,
  createOrder,
  cancelOrder,
  deliverOrder,
  getOrderDetail,
  checkDietaryWarning,
  pageMenus,
  saveOrUpdateMenu,
  deleteMenu,
  pageDishes,
  listActiveDishes,
  createDish,
  updateDish,
  toggleDishStatus,
  DiningSummaryVO,
  MealOrderVO,
  MealMenuVO,
  DishVO,
  DietaryCheckResultVO
} from '@/api/dining'
import { fetchElders, ElderItem } from '@/api/elder'

const activeTab = ref('orders')
const submitLoading = ref(false)

// 顶部 KPI 数据
const summary = reactive<DiningSummaryVO>({
  activeDishCount: 0,
  todayMenuCount: 0,
  todayOrderCount: 0,
  pendingDeliverCount: 0,
  deliveredCount: 0,
  todayRevenue: 0
})

// ----------------- Tab 1: 订单 -----------------
const ordersLoading = ref(false)
const orderList = ref<MealOrderVO[]>([])
const ordersTotal = ref(0)
const orderQuery = reactive({
  orderDate: new Date().toISOString().substring(0, 10),
  mealType: '',
  status: '',
  elderId: undefined as number | undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const elderOptions = ref<ElderItem[]>([])
const allDishes = ref<DishVO[]>([])
const availableDishes = ref<DishVO[]>([])

// 订餐弹窗
const orderDialogVisible = ref(false)
const createOrderForm = reactive({
  elderId: undefined as number | undefined,
  orderDate: new Date().toISOString().substring(0, 10),
  mealType: 'LUNCH',
  items: [] as { dishId: number; quantity: number }[],
  overrideWarning: false
})
const selectedElderProfile = ref<ElderItem | null>(null)
const dietaryWarningResult = ref<DietaryCheckResultVO | null>(null)

// 订单详情抽屉
const orderDrawerVisible = ref(false)
const orderDetail = ref<MealOrderVO | null>(null)

// ----------------- Tab 2: 菜单 -----------------
const menusLoading = ref(false)
const menuList = ref<MealMenuVO[]>([])
const menusTotal = ref(0)
const menuQuery = reactive({
  menuDate: undefined as string | undefined,
  mealType: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})
const menuDialogVisible = ref(false)
const menuForm = reactive({
  id: undefined as number | undefined,
  menuDate: new Date().toISOString().substring(0, 10),
  mealType: 'LUNCH',
  status: 'PUBLISHED',
  dishIds: [] as number[]
})

// ----------------- Tab 3: 菜品 -----------------
const dishesLoading = ref(false)
const dishList = ref<DishVO[]>([])
const dishesTotal = ref(0)
const dishQuery = reactive({
  category: '',
  status: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})
const dishDialogVisible = ref(false)
const dishFormRef = ref()
const dishForm = reactive({
  id: undefined as number | undefined,
  dishCode: '',
  dishName: '',
  category: 'STAPLE',
  price: 10.00,
  allergens: '',
  description: '',
  status: 'ACTIVE'
})
const dishRules = {
  dishCode: [{ required: true, message: '请输入菜品编码', trigger: 'blur' }],
  dishName: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

// ----------------- 生命周期与加载 -----------------
onMounted(() => {
  loadSummaryData()
  loadElderList()
  loadActiveDishes()
  loadOrders()
})

const handleTabChange = (tabName: string) => {
  if (tabName === 'orders') loadOrders()
  else if (tabName === 'menus') loadMenus()
  else if (tabName === 'dishes') loadDishes()
}

const loadSummaryData = async () => {
  try {
    const res = await getDiningSummary()
    if (res.code === 200 && res.data) {
      Object.assign(summary, res.data)
    }
  } catch (err) {
    console.error('加载餐饮统计失败', err)
  }
}

const loadElderList = async () => {
  try {
    const res = await fetchElders({ pageNum: 1, pageSize: 200 })
    if (res.code === 200 && res.data?.list) {
      elderOptions.value = res.data.list
    }
  } catch (err) {
    console.warn('加载长者列表失败', err)
  }
}

const loadActiveDishes = async () => {
  try {
    const res = await listActiveDishes()
    if (res.code === 200 && res.data) {
      allDishes.value = res.data
      availableDishes.value = res.data
    }
  } catch (err) {
    console.warn('加载在售菜品库失败', err)
  }
}

// ----------------- 订单相关逻辑 -----------------
const loadOrders = async () => {
  ordersLoading.value = true
  try {
    const res = await pageOrders(orderQuery)
    if (res.code === 200 && res.data) {
      orderList.value = res.data.list
      ordersTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载订餐订单失败', err)
  } finally {
    ordersLoading.value = false
  }
}

const handleQueryOrders = () => {
  orderQuery.pageNum = 1
  loadOrders()
}

const handleResetOrders = () => {
  orderQuery.orderDate = ''
  orderQuery.mealType = ''
  orderQuery.status = ''
  orderQuery.elderId = undefined
  orderQuery.keyword = ''
  handleQueryOrders()
}

const handleOpenCreateOrder = () => {
  createOrderForm.elderId = undefined
  createOrderForm.orderDate = new Date().toISOString().substring(0, 10)
  createOrderForm.mealType = 'LUNCH'
  createOrderForm.items = []
  createOrderForm.overrideWarning = false
  selectedElderProfile.value = null
  dietaryWarningResult.value = null
  orderDialogVisible.value = true
}

const handleSelectElder = async (elderId: number) => {
  const found = elderOptions.value.find(e => e.id === elderId)
  selectedElderProfile.value = found || null
  checkDietary()
}

const isDishSelected = (dishId: number) => {
  return createOrderForm.items.some(i => i.dishId === dishId)
}

const getDishOrderItem = (dishId: number) => {
  let item = createOrderForm.items.find(i => i.dishId === dishId)
  if (!item) {
    item = { dishId, quantity: 1 }
    createOrderForm.items.push(item)
  }
  return item
}

const handleToggleSelectDish = (dish: DishVO, selected: boolean) => {
  if (selected) {
    if (!createOrderForm.items.some(i => i.dishId === dish.id)) {
      createOrderForm.items.push({ dishId: dish.id, quantity: 1 })
    }
  } else {
    createOrderForm.items = createOrderForm.items.filter(i => i.dishId !== dish.id)
  }
  checkDietary()
}

const checkDietary = async () => {
  if (!createOrderForm.elderId || createOrderForm.items.length === 0) {
    dietaryWarningResult.value = null
    return
  }
  try {
    const dishIds = createOrderForm.items.map(i => i.dishId)
    const res = await checkDietaryWarning(createOrderForm.elderId, dishIds)
    if (res.code === 200 && res.data) {
      dietaryWarningResult.value = res.data
    }
  } catch (err) {
    console.error('过敏禁忌检查失败', err)
  }
}

const calcOrderTotalAmount = computed(() => {
  return createOrderForm.items.reduce((acc, item) => {
    const dish = availableDishes.value.find(d => d.id === item.dishId)
    const price = dish ? Number(dish.price) : 0
    return acc + price * (item.quantity || 1)
  }, 0)
})

const submitCreateOrder = async () => {
  if (!createOrderForm.elderId) {
    ElMessage.warning('请选择就餐长者')
    return
  }
  if (createOrderForm.items.length === 0) {
    ElMessage.warning('请至少选购一道菜品')
    return
  }
  if (dietaryWarningResult.value?.hasWarning && !createOrderForm.overrideWarning) {
    ElMessage.error('所选菜品触碰长者饮食禁忌或过敏原，请阅读警示并确认勾选强行下单')
    return
  }

  submitLoading.value = true
  try {
    const res = await createOrder({
      elderId: createOrderForm.elderId,
      orderDate: createOrderForm.orderDate,
      mealType: createOrderForm.mealType,
      items: createOrderForm.items,
      overrideWarning: createOrderForm.overrideWarning
    })
    if (res.code === 200) {
      ElMessage.success('订餐成功，菜品名称与价格快照已锁定保存')
      orderDialogVisible.value = false
      loadOrders()
      loadSummaryData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '订餐失败')
  } finally {
    submitLoading.value = false
  }
}

const handleCancelOrder = async (id: number) => {
  try {
    const res = await cancelOrder(id)
    if (res.code === 200) {
      ElMessage.success('订单已成功取消')
      loadOrders()
      loadSummaryData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '取消订单失败')
  }
}

const handleDeliverOrder = async (id: number) => {
  try {
    const res = await deliverOrder(id)
    if (res.code === 200) {
      ElMessage.success('订单已标记为配送完成')
      loadOrders()
      loadSummaryData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleViewOrderDetail = async (id: number) => {
  try {
    const res = await getOrderDetail(id)
    if (res.code === 200 && res.data) {
      orderDetail.value = res.data
      orderDrawerVisible.value = true
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取订单详情失败')
  }
}

// ----------------- 菜单相关逻辑 -----------------
const loadMenus = async () => {
  menusLoading.value = true
  try {
    const res = await pageMenus(menuQuery)
    if (res.code === 200 && res.data) {
      menuList.value = res.data.list
      menusTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载餐次菜单失败', err)
  } finally {
    menusLoading.value = false
  }
}

const handleOpenCreateMenu = () => {
  menuForm.id = undefined
  menuForm.menuDate = new Date().toISOString().substring(0, 10)
  menuForm.mealType = 'LUNCH'
  menuForm.status = 'PUBLISHED'
  menuForm.dishIds = []
  menuDialogVisible.value = true
}

const handleEditMenu = (row: MealMenuVO) => {
  menuForm.id = row.id
  menuForm.menuDate = row.menuDate
  menuForm.mealType = row.mealType
  menuForm.status = row.status
  menuForm.dishIds = row.items?.map(i => i.dishId) || []
  menuDialogVisible.value = true
}

const submitMenu = async () => {
  if (menuForm.dishIds.length === 0) {
    ElMessage.warning('请至少添加一道菜品至菜单')
    return
  }
  submitLoading.value = true
  try {
    const res = await saveOrUpdateMenu({
      menuDate: menuForm.menuDate,
      mealType: menuForm.mealType,
      status: menuForm.status,
      dishIds: menuForm.dishIds
    })
    if (res.code === 200) {
      ElMessage.success('餐次菜单保存发布成功')
      menuDialogVisible.value = false
      loadMenus()
      loadSummaryData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '保存菜单失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDeleteMenu = async (id: number) => {
  try {
    const res = await deleteMenu(id)
    if (res.code === 200) {
      ElMessage.success('菜单已删除')
      loadMenus()
      loadSummaryData()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '删除菜单失败')
  }
}

// ----------------- 菜品相关逻辑 -----------------
const loadDishes = async () => {
  dishesLoading.value = true
  try {
    const res = await pageDishes(dishQuery)
    if (res.code === 200 && res.data) {
      dishList.value = res.data.list
      dishesTotal.value = res.data.total
    }
  } catch (err) {
    console.error('加载菜品失败', err)
  } finally {
    dishesLoading.value = false
  }
}

const handleOpenCreateDish = () => {
  dishForm.id = undefined
  dishForm.dishCode = 'DISH' + String(Math.floor(Math.random() * 900 + 100))
  dishForm.dishName = ''
  dishForm.category = 'STAPLE'
  dishForm.price = 12.00
  dishForm.allergens = ''
  dishForm.description = ''
  dishForm.status = 'ACTIVE'
  dishDialogVisible.value = true
}

const handleEditDish = (row: DishVO) => {
  dishForm.id = row.id
  dishForm.dishCode = row.dishCode
  dishForm.dishName = row.dishName
  dishForm.category = row.category
  dishForm.price = row.price
  dishForm.allergens = row.allergens || ''
  dishForm.description = row.description || ''
  dishForm.status = row.status
  dishDialogVisible.value = true
}

const submitDish = async () => {
  if (!dishFormRef.value) return
  await dishFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (dishForm.id) {
        await updateDish(dishForm.id, dishForm)
        ElMessage.success('菜品更新成功')
      } else {
        await createDish(dishForm)
        ElMessage.success('菜品创建成功')
      }
      dishDialogVisible.value = false
      loadDishes()
      loadActiveDishes()
      loadSummaryData()
    } catch (err: any) {
      ElMessage.error(err.message || '保存菜品失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleToggleDishStatus = async (row: DishVO, val: boolean) => {
  const newStatus = val ? 'ACTIVE' : 'INACTIVE'
  try {
    await toggleDishStatus(row.id, newStatus)
    row.status = newStatus
    row.statusName = val ? '在售' : '停售'
    ElMessage.success(`菜品【${row.dishName}】状态已更新为${row.statusName}`)
    loadActiveDishes()
    loadSummaryData()
  } catch (err: any) {
    ElMessage.error(err.message || '更新状态失败')
  }
}

// ----------------- 样式辅助函数 -----------------
const getMealTypeTag = (type: string) => {
  switch (type?.toUpperCase()) {
    case 'BREAKFAST': return 'warning'
    case 'LUNCH': return 'primary'
    case 'DINNER': return 'success'
    default: return 'info'
  }
}

const getOrderStatusTag = (st: string) => {
  switch (st?.toUpperCase()) {
    case 'CONFIRMED': return 'primary'
    case 'DELIVERED': return 'success'
    case 'CANCELLED': return 'info'
    default: return 'info'
  }
}
</script>

<style scoped>
.dining-page {
  padding: 16px;
  background-color: #f6f8fb;
  min-height: calc(100vh - 84px);
}

/* KPI 统计卡片 */
.kpi-row {
  margin-bottom: 16px;
}
.kpi-card {
  border-radius: 8px;
  display: flex;
  align-items: center;
  border: 1px solid #e2e8f0;
  transition: all 0.25s ease;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
:deep(.kpi-card .el-card__body) {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  width: 100%;
}
.kpi-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-right: 12px;
}
.kpi-content {
  display: flex;
  flex-direction: column;
}
.kpi-title {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 2px;
}
.kpi-num {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

.total-orders .kpi-icon { background: #e0f2fe; color: #0284c7; }
.pending-deliver .kpi-icon { background: #fef3c7; color: #d97706; }
.delivered .kpi-icon { background: #dcfce7; color: #16a34a; }
.today-revenue .kpi-icon { background: #ede9fe; color: #7c3aed; }
.active-dishes .kpi-icon { background: #ffedd5; color: #ea580c; }
.today-menus .kpi-icon { background: #fce7f3; color: #db2777; }

.warning-num { color: #d97706; }
.success-num { color: #16a34a; }
.primary-num { color: #7c3aed; }
.purple-num { color: #db2777; }

/* 主卡片与选项卡 */
.main-card {
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}
.tab-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 4px 0;
}
.tab-subtitle {
  font-size: 13px;
  color: #64748b;
}
.header-btns {
  display: flex;
  gap: 10px;
}

/* 筛选表单 */
.filter-form {
  margin-bottom: 12px;
}
.custom-table {
  border-radius: 6px;
  overflow: hidden;
}

.mono-link {
  font-family: Menlo, Monaco, Consolas, monospace;
  font-weight: 600;
}
.elder-cell {
  display: flex;
  flex-direction: column;
}
.elder-name {
  font-weight: 600;
  color: #1e293b;
}
.elder-bed {
  font-size: 12px;
  color: #64748b;
}
.items-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.dish-snapshot-badge {
  font-size: 12px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 2px 8px;
  border-radius: 4px;
  color: #334155;
}
.price-hint {
  color: #64748b;
  margin-left: 2px;
}
.amount-text {
  font-weight: 700;
  color: #e11d48;
  font-size: 15px;
}
.text-muted {
  color: #94a3b8;
}
.small-text {
  font-size: 12px;
}
.price-val {
  font-weight: 600;
  color: #0f172a;
}
.allergen-pill {
  color: #dc2626;
  font-size: 12px;
  font-weight: 500;
  background: #fef2f2;
  padding: 2px 6px;
  border-radius: 4px;
}
.menu-dishes-wrap {
  display: flex;
  flex-wrap: wrap;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 点餐弹窗长者膳食名片 */
.elder-profile-banner {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px 14px;
}
.banner-title {
  display: flex;
  align-items: center;
  font-size: 14px;
  margin-bottom: 6px;
}
.banner-body {
  font-size: 13px;
}
.tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tag-row .label {
  color: #64748b;
  width: 70px;
}
.dish-picker-box {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}
.tag-allergen {
  color: #dc2626;
  font-size: 12px;
  margin-left: 4px;
}
.warning-list {
  margin: 6px 0 0 16px;
  padding: 0;
  color: #991b1b;
  font-size: 13px;
  line-height: 1.6;
}
.order-summary-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f1f5f9;
  border-radius: 6px;
  margin-top: 10px;
}
.total-price-highlight {
  font-size: 20px;
  font-weight: 700;
  color: #e11d48;
}
.order-detail-wrap {
  padding: 4px;
}
</style>
