package com.example.elderlycare.modules.system.service.impl;

import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.modules.system.entity.SysOperationLog;
import com.example.elderlycare.modules.system.mapper.SysOperationLogMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl implements SysOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    public void recordLog(String module, String action, String bizType, Long bizId, String requestUri, String requestIp, String detailJson) {
        try {
            SysOperationLog logEntry = SysOperationLog.builder()
                    .orgId(OrgContextHolder.getOrgId())
                    .userId(OrgContextHolder.getUserId())
                    .module(module)
                    .action(action)
                    .bizType(bizType)
                    .bizId(bizId)
                    .requestUri(requestUri)
                    .requestIp(requestIp)
                    .detailJson(detailJson)
                    .createdAt(LocalDateTime.now())
                    .build();
            sysOperationLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("记录系统操作审计日志异常: {}", e.getMessage(), e);
        }
    }
}
