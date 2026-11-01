package com.example.elderlycare.modules.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.health.dto.HealthMetricDTO;
import com.example.elderlycare.modules.health.entity.HealthMetric;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.service.HealthMetricService;
import com.example.elderlycare.modules.health.vo.HealthMetricVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthMetricServiceImpl implements HealthMetricService {

    private final HealthMetricMapper healthMetricMapper;

    @Override
    public List<HealthMetricVO> listMetrics(String status) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<HealthMetric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, HealthMetric::getOrgId, currentOrgId);
        wrapper.eq(StringUtils.hasText(status), HealthMetric::getStatus, status);
        wrapper.orderByAsc(HealthMetric::getId);

        List<HealthMetric> list = healthMetricMapper.selectList(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public HealthMetricVO getMetricById(Long id) {
        HealthMetric metric = checkOrgAccess(id);
        return convertToVO(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMetric(HealthMetricDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        // 校验机构内指标编码唯一性
        Long count = healthMetricMapper.selectCount(
                new LambdaQueryWrapper<HealthMetric>()
                        .eq(currentOrgId != null, HealthMetric::getOrgId, currentOrgId)
                        .eq(HealthMetric::getMetricCode, dto.getMetricCode().trim())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "指标编码 [" + dto.getMetricCode() + "] 在当前机构已存在");
        }

        HealthMetric metric = HealthMetric.builder()
                .orgId(currentOrgId)
                .metricCode(dto.getMetricCode().trim())
                .metricName(dto.getMetricName().trim())
                .unit(dto.getUnit())
                .valueType(StringUtils.hasText(dto.getValueType()) ? dto.getValueType() : "NUMBER")
                .description(dto.getDescription())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ACTIVE")
                .build();

        healthMetricMapper.insert(metric);
        return metric.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMetric(Long id, HealthMetricDTO dto) {
        HealthMetric metric = checkOrgAccess(id);
        Long currentOrgId = OrgContextHolder.getOrgId();

        if (!metric.getMetricCode().equalsIgnoreCase(dto.getMetricCode().trim())) {
            Long count = healthMetricMapper.selectCount(
                    new LambdaQueryWrapper<HealthMetric>()
                            .eq(currentOrgId != null, HealthMetric::getOrgId, currentOrgId)
                            .eq(HealthMetric::getMetricCode, dto.getMetricCode().trim())
                            .ne(HealthMetric::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "指标编码 [" + dto.getMetricCode() + "] 在当前机构已存在");
            }
            metric.setMetricCode(dto.getMetricCode().trim());
        }

        metric.setMetricName(dto.getMetricName().trim());
        metric.setUnit(dto.getUnit());
        if (StringUtils.hasText(dto.getValueType())) {
            metric.setValueType(dto.getValueType());
        }
        metric.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getStatus())) {
            metric.setStatus(dto.getStatus());
        }

        healthMetricMapper.updateById(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleMetricStatus(Long id, String status) {
        HealthMetric metric = checkOrgAccess(id);
        metric.setStatus(status);
        healthMetricMapper.updateById(metric);
    }

    private HealthMetric checkOrgAccess(Long id) {
        HealthMetric metric = healthMetricMapper.selectById(id);
        if (metric == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康指标不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(metric.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的健康指标");
        }
        return metric;
    }

    private HealthMetricVO convertToVO(HealthMetric metric) {
        return HealthMetricVO.builder()
                .id(metric.getId())
                .orgId(metric.getOrgId())
                .metricCode(metric.getMetricCode())
                .metricName(metric.getMetricName())
                .unit(metric.getUnit())
                .valueType(metric.getValueType())
                .description(metric.getDescription())
                .status(metric.getStatus())
                .build();
    }
}
