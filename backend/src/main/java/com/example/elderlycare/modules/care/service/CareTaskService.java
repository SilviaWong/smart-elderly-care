package com.example.elderlycare.modules.care.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.care.dto.CareTaskActionDTO;
import com.example.elderlycare.modules.care.dto.CareTaskAssignDTO;
import com.example.elderlycare.modules.care.dto.CareTaskGenerateDTO;
import com.example.elderlycare.modules.care.dto.CareTaskQueryDTO;
import com.example.elderlycare.modules.care.vo.CareTaskRecordVO;
import com.example.elderlycare.modules.care.vo.CareTaskSummaryVO;
import com.example.elderlycare.modules.care.vo.CareTaskVO;

import java.time.LocalDate;
import java.util.List;

public interface CareTaskService {

    int generateTasks(CareTaskGenerateDTO dto);

    PageResult<CareTaskVO> pageTasks(CareTaskQueryDTO query);

    CareTaskSummaryVO getTaskSummary(LocalDate taskDate);

    CareTaskVO getTaskDetail(Long id);

    void assignTask(Long id, CareTaskAssignDTO dto);

    void executeTaskAction(Long id, CareTaskActionDTO dto);

    List<CareTaskRecordVO> listTaskRecords(Long taskId);
}
