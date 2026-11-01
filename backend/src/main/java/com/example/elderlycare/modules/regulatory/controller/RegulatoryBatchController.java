package com.example.elderlycare.modules.regulatory.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchGenerateDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchQueryDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchSubmitDTO;
import com.example.elderlycare.modules.regulatory.service.RegulatoryBatchService;
import com.example.elderlycare.modules.regulatory.vo.BatchValidationResultVO;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryBatchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "监管报送 - 批次生成、数据校验与报送提交", description = "监管报送批次抽取、前置数据合规校验、流式文件导出与适配器提交接口")
@RestController
@RequestMapping("/api/v1/regulatory/batches")
@RequiredArgsConstructor
public class RegulatoryBatchController {

    private final RegulatoryBatchService batchService;

    @Operation(summary = "生成监管报送批次 (数据抽取、合规校验与文件打包)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('regulatory')")
    public ApiResponse<Long> generateBatch(@Valid @RequestBody RegulatoryBatchGenerateDTO dto) {
        return ApiResponse.success(batchService.generateBatch(dto));
    }

    @Operation(summary = "分页查询监管报送批次列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('regulatory')")
    public ApiResponse<PageResult<RegulatoryBatchVO>> pageBatches(RegulatoryBatchQueryDTO query) {
        return ApiResponse.success(batchService.pageBatches(query));
    }

    @Operation(summary = "获取指定报送批次详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('regulatory')")
    public ApiResponse<RegulatoryBatchVO> getBatchDetail(@PathVariable Long id) {
        return ApiResponse.success(batchService.getBatchDetail(id));
    }

    @Operation(summary = "查看报送批次的前置数据校验与定位报告")
    @GetMapping("/{id}/validation-result")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('regulatory')")
    public ApiResponse<BatchValidationResultVO> getValidationResult(@PathVariable Long id) {
        return ApiResponse.success(batchService.getValidationResult(id));
    }

    @Operation(summary = "下载合规报送 CSV 文件")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadBatchFile(@PathVariable Long id) {
        RegulatoryBatchVO batch = batchService.getBatchDetail(id);
        Resource resource = batchService.downloadBatchFile(id);

        String encodedFileName = URLEncoder.encode(batch.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @Operation(summary = "登记并正式提交监管报送批次")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('regulatory')")
    public ApiResponse<Void> submitBatch(@PathVariable Long id, @RequestBody RegulatoryBatchSubmitDTO dto) {
        batchService.submitBatch(id, dto);
        return ApiResponse.success();
    }
}
