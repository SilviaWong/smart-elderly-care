package com.example.elderlycare.modules.elder.service;

import com.example.elderlycare.modules.elder.dto.AssessmentCreateDTO;
import com.example.elderlycare.modules.elder.vo.AssessmentVO;

import java.util.List;

public interface ElderAssessmentService {

    List<AssessmentVO> listAssessments(Long elderId);

    Long createAssessment(Long elderId, AssessmentCreateDTO dto);
}
