package com.example.elderlycare.modules.system.service;

public interface SysOperationLogService {

    void recordLog(String module, String action, String bizType, Long bizId, String requestUri, String requestIp, String detailJson);
}
