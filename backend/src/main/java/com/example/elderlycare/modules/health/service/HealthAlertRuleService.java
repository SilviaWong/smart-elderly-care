package com.example.elderlycare.modules.health.service;

import com.example.elderlycare.modules.health.dto.HealthAlertRuleDTO;
import com.example.elderlycare.modules.health.vo.HealthAlertRuleVO;

import java.util.List;

public interface HealthAlertRuleService {

    List<HealthAlertRuleVO> listRules(Long metricId, Integer enabled);

    HealthAlertRuleVO getRuleById(Long id);

    Long createRule(HealthAlertRuleDTO dto);

    void updateRule(Long id, HealthAlertRuleDTO dto);

    void toggleRuleEnabled(Long id, Integer enabled);
}
