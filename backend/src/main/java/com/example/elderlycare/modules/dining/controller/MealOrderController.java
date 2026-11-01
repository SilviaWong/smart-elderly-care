package com.example.elderlycare.modules.dining.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.dining.dto.MealOrderCreateDTO;
import com.example.elderlycare.modules.dining.dto.MealOrderQueryDTO;
import com.example.elderlycare.modules.dining.service.MealOrderService;
import com.example.elderlycare.modules.dining.vo.DietaryCheckResultVO;
import com.example.elderlycare.modules.dining.vo.DiningSummaryVO;
import com.example.elderlycare.modules.dining.vo.MealOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "餐饮管理 - 订餐订单全流程", description = "长者订餐下单、过敏禁忌筛查、取消规则、订单配送与统计接口")
@RestController
@RequestMapping("/api/v1/dining/orders")
@RequiredArgsConstructor
public class MealOrderController {

    private final MealOrderService mealOrderService;

    @Operation(summary = "分页查询订餐订单")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<PageResult<MealOrderVO>> pageOrders(MealOrderQueryDTO query) {
        return ApiResponse.success(mealOrderService.pageOrders(query));
    }

    @Operation(summary = "餐饮运营看板统计指标数据")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<DiningSummaryVO> getSummary() {
        return ApiResponse.success(mealOrderService.getSummary());
    }

    @Operation(summary = "查询订餐订单详情（包含菜品价格与名称快照）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<MealOrderVO> getOrderDetail(@PathVariable Long id) {
        return ApiResponse.success(mealOrderService.getOrderDetail(id));
    }

    @Operation(summary = "订餐前长者过敏原与饮食禁忌风险检查")
    @PostMapping("/check-dietary")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<DietaryCheckResultVO> checkDietaryWarning(
            @RequestParam Long elderId,
            @RequestBody List<Long> dishIds) {
        return ApiResponse.success(mealOrderService.checkDietaryWarning(elderId, dishIds));
    }

    @Operation(summary = "提交订餐订单（自动计算金额并写入菜品名称与价格快照）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<Long> createOrder(@Valid @RequestBody MealOrderCreateDTO dto) {
        return ApiResponse.success(mealOrderService.createOrder(dto));
    }

    @Operation(summary = "取消订餐订单（严格校验餐次取消截止时间）")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<Void> cancelOrder(@PathVariable Long id) {
        mealOrderService.cancelOrder(id);
        return ApiResponse.success();
    }

    @Operation(summary = "标记订单为已配送/已完成")
    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dining')")
    public ApiResponse<Void> deliverOrder(@PathVariable Long id) {
        mealOrderService.deliverOrder(id);
        return ApiResponse.success();
    }
}
