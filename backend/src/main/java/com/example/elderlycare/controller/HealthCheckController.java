package com.example.elderlycare.controller;

import com.example.elderlycare.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "基础监控与健康检查", description = "服务可用性与运行状态探测")
@RestController
@RequestMapping("/api/v1")
public class HealthCheckController {

    @Operation(summary = "服务健康检查接口")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("application", "smart-elderly-care-backend");
        healthInfo.put("version", "1.0.0");
        healthInfo.put("serverTime", LocalDateTime.now());
        return ApiResponse.success(healthInfo);
    }
}
