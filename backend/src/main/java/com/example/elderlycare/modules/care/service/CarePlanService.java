package com.example.elderlycare.modules.care.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.care.dto.CarePlanCreateDTO;
import com.example.elderlycare.modules.care.dto.CarePlanQueryDTO;
import com.example.elderlycare.modules.care.vo.CarePlanDetailVO;
import com.example.elderlycare.modules.care.vo.CarePlanVO;

public interface CarePlanService {

    PageResult<CarePlanVO> pagePlans(CarePlanQueryDTO query);

    CarePlanDetailVO getPlanDetail(Long id);

    Long createPlan(CarePlanCreateDTO dto);

    void updatePlan(Long id, CarePlanCreateDTO dto);

    void togglePlanStatus(Long id, String status);
}
