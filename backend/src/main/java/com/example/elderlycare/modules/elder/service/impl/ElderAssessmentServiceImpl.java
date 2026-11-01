package com.example.elderlycare.modules.elder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.elder.dto.AssessmentCreateDTO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.entity.ElderAssessment;
import com.example.elderlycare.modules.elder.mapper.ElderAssessmentMapper;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.elder.service.ElderAssessmentService;
import com.example.elderlycare.modules.elder.vo.AssessmentVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElderAssessmentServiceImpl implements ElderAssessmentService {

    private final ElderAssessmentMapper elderAssessmentMapper;
    private final ElderMapper elderMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<AssessmentVO> listAssessments(Long elderId) {
        checkElderOrgAccess(elderId);

        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<ElderAssessment> wrapper = new LambdaQueryWrapper<ElderAssessment>()
                .eq(ElderAssessment::getElderId, elderId)
                .eq(currentOrgId != null, ElderAssessment::getOrgId, currentOrgId)
                .orderByDesc(ElderAssessment::getAssessmentDate)
                .orderByDesc(ElderAssessment::getId);

        List<ElderAssessment> list = elderAssessmentMapper.selectList(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAssessment(Long elderId, AssessmentCreateDTO dto) {
        Elder elder = checkElderOrgAccess(elderId);
        Long currentOrgId = OrgContextHolder.getOrgId() != null ? OrgContextHolder.getOrgId() : elder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        ElderAssessment assessment = ElderAssessment.builder()
                .orgId(currentOrgId)
                .elderId(elderId)
                .assessmentType(dto.getAssessmentType())
                .assessmentDate(dto.getAssessmentDate())
                .score(dto.getScore())
                .resultLevel(dto.getResultLevel())
                .detailJson(dto.getDetailJson())
                .assessorId(currentUserId)
                .remark(dto.getRemark())
                .createdAt(LocalDateTime.now())
                .build();

        elderAssessmentMapper.insert(assessment);
        return assessment.getId();
    }

    private Elder checkElderOrgAccess(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "老人档案不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的老人数据");
        }
        return elder;
    }

    private AssessmentVO convertToVO(ElderAssessment a) {
        String assessorName = "系统录入";
        if (a.getAssessorId() != null) {
            SysUser assessor = sysUserMapper.selectById(a.getAssessorId());
            if (assessor != null) {
                assessorName = assessor.getRealName();
            }
        }

        return AssessmentVO.builder()
                .id(a.getId())
                .elderId(a.getElderId())
                .assessmentType(a.getAssessmentType())
                .assessmentDate(a.getAssessmentDate())
                .score(a.getScore())
                .resultLevel(a.getResultLevel())
                .detailJson(a.getDetailJson())
                .assessorId(a.getAssessorId())
                .assessorName(assessorName)
                .remark(a.getRemark())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
