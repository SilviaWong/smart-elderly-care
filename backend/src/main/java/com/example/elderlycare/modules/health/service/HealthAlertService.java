package com.example.elderlycare.modules.health.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.health.dto.HealthAlertAssignDTO;
import com.example.elderlycare.modules.health.dto.HealthAlertHandleDTO;
import com.example.elderlycare.modules.health.dto.HealthAlertQueryDTO;
import com.example.elderlycare.modules.health.entity.HealthRecord;
import com.example.elderlycare.modules.health.vo.HealthAlertSummaryVO;
import com.example.elderlycare.modules.health.vo.HealthAlertVO;

public interface HealthAlertService {

    PageResult<HealthAlertVO> pageAlerts(HealthAlertQueryDTO query);

    HealthAlertSummaryVO getAlertSummary();

    HealthAlertVO getAlertDetail(Long id);

    void evaluateAndTriggerAlert(HealthRecord record);

    void assignAlert(Long id, HealthAlertAssignDTO dto);

    void handleAlert(Long id, HealthAlertHandleDTO dto);
}
