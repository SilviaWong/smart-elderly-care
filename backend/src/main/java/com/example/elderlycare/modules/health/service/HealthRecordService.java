package com.example.elderlycare.modules.health.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.health.dto.HealthRecordBatchCreateDTO;
import com.example.elderlycare.modules.health.dto.HealthRecordCreateDTO;
import com.example.elderlycare.modules.health.dto.HealthRecordQueryDTO;
import com.example.elderlycare.modules.health.vo.HealthRecordVO;
import com.example.elderlycare.modules.health.vo.HealthTrendPointVO;

import java.util.List;

public interface HealthRecordService {

    PageResult<HealthRecordVO> pageRecords(HealthRecordQueryDTO query);

    List<HealthTrendPointVO> listTrend(Long elderId, Long metricId, Integer days);

    Long createRecord(HealthRecordCreateDTO dto);

    List<Long> batchCreateRecords(HealthRecordBatchCreateDTO dto);
}
