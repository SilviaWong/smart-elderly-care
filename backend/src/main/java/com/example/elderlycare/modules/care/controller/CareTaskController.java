package com.example.elderlycare.modules.care.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.care.dto.CareTaskActionDTO;
import com.example.elderlycare.modules.care.dto.CareTaskAssignDTO;
import com.example.elderlycare.modules.care.dto.CareTaskGenerateDTO;
import com.example.elderlycare.modules.care.dto.CareTaskQueryDTO;
import com.example.elderlycare.modules.care.service.CareTaskService;
import com.example.elderlycare.modules.care.vo.CareTaskRecordVO;
import com.example.elderlycare.modules.care.vo.CareTaskSummaryVO;
import com.example.elderlycare.modules.care.vo.CareTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "护理任务调度与执行记录", description = "按计划幂等生成日常护理任务、任务指派、执行状态闭环与不可篡改记录接口")
@RestController
@RequestMapping("/api/v1/care/tasks")
@RequiredArgsConstructor
public class CareTaskController {

    private final CareTaskService careTaskService;

    @Operation(summary = "按有效护理计划幂等生成日常护理任务（支持手动触发与定时任务）")
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Integer> generateTasks(@Valid @RequestBody CareTaskGenerateDTO dto) {
        return ApiResponse.success(careTaskService.generateTasks(dto));
    }

    @Operation(summary = "分页查询护理任务")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<PageResult<CareTaskVO>> pageTasks(CareTaskQueryDTO query) {
        return ApiResponse.success(careTaskService.pageTasks(query));
    }

    @Operation(summary = "查询指定日期的护理任务汇总数据（待执行/执行中/已完成/漏执行）")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<CareTaskSummaryVO> getTaskSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate) {
        return ApiResponse.success(careTaskService.getTaskSummary(taskDate));
    }

    @Operation(summary = "查询护理任务详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<CareTaskVO> getTaskDetail(@PathVariable Long id) {
        return ApiResponse.success(careTaskService.getTaskDetail(id));
    }

    @Operation(summary = "指派护理任务责任人")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Void> assignTask(@PathVariable Long id, @Valid @RequestBody CareTaskAssignDTO dto) {
        careTaskService.assignTask(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "执行任务流转操作（START开始 / COMPLETE完成 / MISS漏执行 / CANCEL取消）")
    @PostMapping("/{id}/action")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<Void> executeTaskAction(@PathVariable Long id, @Valid @RequestBody CareTaskActionDTO dto) {
        careTaskService.executeTaskAction(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "查询护理任务的不可无痕修改执行记录")
    @GetMapping("/{id}/records")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<List<CareTaskRecordVO>> listTaskRecords(@PathVariable Long id) {
        return ApiResponse.success(careTaskService.listTaskRecords(id));
    }
}
