package com.example.elderlycare.modules.admission.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.admission.dto.AdmissionCheckInDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionDischargeDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionQueryDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionTransferDTO;
import com.example.elderlycare.modules.admission.service.AdmissionService;
import com.example.elderlycare.modules.admission.vo.AdmissionPrecheckVO;
import com.example.elderlycare.modules.admission.vo.AdmissionVO;
import com.example.elderlycare.modules.admission.vo.BedChangeLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "入住与床位流转管理", description = "长者入住办理、调床、退住及变动审计日志接口")
@RestController
@RequestMapping("/api/v1/admissions")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionService admissionService;

    @Operation(summary = "分页查询入住记录")
    @GetMapping
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<PageResult<AdmissionVO>> pageAdmissions(AdmissionQueryDTO query) {
        return ApiResponse.success(admissionService.pageAdmissions(query));
    }

    @Operation(summary = "查询入住详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<AdmissionVO> getAdmissionDetail(@PathVariable Long id) {
        return ApiResponse.success(admissionService.getAdmissionDetail(id));
    }

    @Operation(summary = "办理长者入住（排他行锁防并发占床）")
    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Long> checkIn(@Valid @RequestBody AdmissionCheckInDTO dto) {
        return ApiResponse.success(admissionService.checkIn(dto));
    }

    @Operation(summary = "办理调床（升序防死锁双床位原子切换）")
    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Void> transferBed(@PathVariable Long id, @Valid @RequestBody AdmissionTransferDTO dto) {
        admissionService.transferBed(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "退住前置检查")
    @GetMapping("/{id}/precheck-discharge")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<AdmissionPrecheckVO> precheckDischarge(@PathVariable Long id) {
        return ApiResponse.success(admissionService.checkDischargePreconditions(id));
    }

    @Operation(summary = "办理退住（释放床位、更新长者状态并记录审计日志）")
    @PostMapping("/{id}/discharge")
    @PreAuthorize("hasAuthority('admission:operate')")
    public ApiResponse<Void> discharge(@PathVariable Long id, @Valid @RequestBody AdmissionDischargeDTO dto) {
        admissionService.discharge(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "查询床位变更流转历史记录")
    @GetMapping("/{id}/change-logs")
    @PreAuthorize("hasAuthority('admission:view')")
    public ApiResponse<List<BedChangeLogVO>> listChangeLogs(@PathVariable Long id) {
        return ApiResponse.success(admissionService.listChangeLogs(id));
    }
}
