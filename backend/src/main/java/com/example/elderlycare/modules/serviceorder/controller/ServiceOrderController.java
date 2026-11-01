package com.example.elderlycare.modules.serviceorder.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderActionDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderAssignDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderCreateDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderQueryDTO;
import com.example.elderlycare.modules.serviceorder.service.ServiceOrderService;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderLogVO;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderSummaryVO;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "服务工单全流程管理", description = "工单创建、派单、接单、执行、验收、取消及流转审计日志接口")
@RestController
@RequestMapping("/api/v1/service-orders")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    @Operation(summary = "分页查询服务工单")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('serviceorder')")
    public ApiResponse<PageResult<ServiceOrderVO>> pageOrders(ServiceOrderQueryDTO query) {
        return ApiResponse.success(serviceOrderService.pageOrders(query));
    }

    @Operation(summary = "工单统计看板数据（各状态数量汇总）")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('serviceorder')")
    public ApiResponse<ServiceOrderSummaryVO> getOrderSummary() {
        return ApiResponse.success(serviceOrderService.getOrderSummary());
    }

    @Operation(summary = "查询服务工单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('serviceorder')")
    public ApiResponse<ServiceOrderVO> getOrderDetail(@PathVariable Long id) {
        return ApiResponse.success(serviceOrderService.getOrderDetail(id));
    }

    @Operation(summary = "创建服务工单")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('serviceorder')")
    public ApiResponse<Long> createOrder(@Valid @RequestBody ServiceOrderCreateDTO dto) {
        return ApiResponse.success(serviceOrderService.createOrder(dto));
    }

    @Operation(summary = "修改工单核心内容（仅限未完成且未取消工单）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('serviceorder')")
    public ApiResponse<Void> updateOrder(@PathVariable Long id, @Valid @RequestBody ServiceOrderCreateDTO dto) {
        serviceOrderService.updateOrder(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "派发/转派服务工单")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('serviceorder')")
    public ApiResponse<Void> assignOrder(@PathVariable Long id, @Valid @RequestBody ServiceOrderAssignDTO dto) {
        serviceOrderService.assignOrder(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "工单状态流转操作（接单/开始/完成/验收/拒单/退回/取消）")
    @PostMapping("/{id}/action")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('serviceorder')")
    public ApiResponse<Void> executeAction(@PathVariable Long id, @Valid @RequestBody ServiceOrderActionDTO dto) {
        serviceOrderService.executeAction(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "查询工单全生命周期操作日志轨迹")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('serviceorder')")
    public ApiResponse<List<ServiceOrderLogVO>> listLogs(@PathVariable Long id) {
        return ApiResponse.success(serviceOrderService.listLogs(id));
    }
}
