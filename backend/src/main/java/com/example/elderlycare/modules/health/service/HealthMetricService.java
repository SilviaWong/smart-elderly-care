package com.example.elderlycare.modules.health.service;

import com.example.elderlycare.modules.health.dto.HealthMetricDTO;
import com.example.elderlycare.modules.health.vo.HealthMetricVO;

import java.util.List;

public interface HealthMetricService {

    List<HealthMetricVO> listMetrics(String status);

    HealthMetricVO getMetricById(Long id);

    Long createMetric(HealthMetricDTO dto);

    void updateMetric(Long id, HealthMetricDTO dto);

    void toggleMetricStatus(Long id, String status);
}
