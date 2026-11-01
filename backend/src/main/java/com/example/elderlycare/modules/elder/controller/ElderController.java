package com.example.elderlycare.modules.elder.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.elder.dto.AssessmentCreateDTO;
import com.example.elderlycare.modules.elder.dto.ContactDTO;
import com.example.elderlycare.modules.elder.dto.ElderCreateDTO;
import com.example.elderlycare.modules.elder.dto.ElderQueryDTO;
import com.example.elderlycare.modules.elder.dto.ElderUpdateDTO;
import com.example.elderlycare.modules.elder.service.ElderAssessmentService;
import com.example.elderlycare.modules.elder.service.ElderContactService;
import com.example.elderlycare.modules.elder.service.ElderService;
import com.example.elderlycare.modules.elder.vo.AssessmentVO;
import com.example.elderlycare.modules.elder.vo.ContactVO;
import com.example.elderlycare.modules.elder.vo.ElderDetailVO;
import com.example.elderlycare.modules.elder.vo.ElderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "老人档案管理", description = "老人主档案、紧急联系人、能力评估与健康概况")
@RestController
@RequestMapping("/api/v1/elders")
@RequiredArgsConstructor
public class ElderController {

    private final ElderService elderService;
    private final ElderContactService elderContactService;
    private final ElderAssessmentService elderAssessmentService;

    @Operation(summary = "分页查询老人档案列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('elder:view')")
    public ApiResponse<PageResult<ElderListVO>> pageElders(ElderQueryDTO query) {
        PageResult<ElderListVO> page = elderService.pageElders(query);
        return ApiResponse.success(page);
    }

    @Operation(summary = "查看老人全景档案详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('elder:view')")
    public ApiResponse<ElderDetailVO> getElderDetail(@PathVariable Long id) {
        ElderDetailVO detail = elderService.getElderDetail(id);
        return ApiResponse.success(detail);
    }

    @Operation(summary = "老人建档录入")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('elder:edit')")
    public ApiResponse<Long> createElder(@Valid @RequestBody ElderCreateDTO dto) {
        Long elderId = elderService.createElder(dto);
        return ApiResponse.success("建档成功", elderId);
    }

    @Operation(summary = "编辑老人主档案")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('elder:edit')")
    public ApiResponse<Void> updateElder(@PathVariable Long id, @Valid @RequestBody ElderUpdateDTO dto) {
        elderService.updateElder(id, dto);
        return ApiResponse.success("档案更新成功", null);
    }

    @Operation(summary = "老人档案归档")
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('elder:edit')")
    public ApiResponse<Void> archiveElder(@PathVariable Long id) {
        elderService.archiveElder(id);
        return ApiResponse.success("档案已归档", null);
    }

    // --- 联系人接口 ---

    @Operation(summary = "获取老人联系人列表")
    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('elder:view')")
    public ApiResponse<List<ContactVO>> listContacts(@PathVariable Long id) {
        List<ContactVO> list = elderContactService.listContacts(id);
        return ApiResponse.success(list);
    }

    @Operation(summary = "新增老人联系人")
    @PostMapping("/{id}/contacts")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('elder:edit')")
    public ApiResponse<Long> createContact(@PathVariable Long id, @Valid @RequestBody ContactDTO dto) {
        Long contactId = elderContactService.createContact(id, dto);
        return ApiResponse.success("新增联系人成功", contactId);
    }

    @Operation(summary = "更新老人联系人")
    @PutMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('elder:edit')")
    public ApiResponse<Void> updateContact(@PathVariable Long id,
                                          @PathVariable Long contactId,
                                          @Valid @RequestBody ContactDTO dto) {
        elderContactService.updateContact(id, contactId, dto);
        return ApiResponse.success("联系人更新成功", null);
    }

    @Operation(summary = "删除老人联系人")
    @DeleteMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('elder:edit')")
    public ApiResponse<Void> deleteContact(@PathVariable Long id, @PathVariable Long contactId) {
        elderContactService.deleteContact(id, contactId);
        return ApiResponse.success("联系人已删除", null);
    }

    // --- 评估记录接口 ---

    @Operation(summary = "查询老人评估记录历史")
    @GetMapping("/{id}/assessments")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('elder:view')")
    public ApiResponse<List<AssessmentVO>> listAssessments(@PathVariable Long id) {
        List<AssessmentVO> list = elderAssessmentService.listAssessments(id);
        return ApiResponse.success(list);
    }

    @Operation(summary = "新增老人能力评估记录")
    @PostMapping("/{id}/assessments")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('elder:edit')")
    public ApiResponse<Long> createAssessment(@PathVariable Long id, @Valid @RequestBody AssessmentCreateDTO dto) {
        Long assessmentId = elderAssessmentService.createAssessment(id, dto);
        return ApiResponse.success("评估记录添加成功", assessmentId);
    }
}
