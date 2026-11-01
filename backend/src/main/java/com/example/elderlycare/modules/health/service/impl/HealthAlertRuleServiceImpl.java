package com.example.elderlycare.modules.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.health.dto.HealthAlertRuleDTO;
import com.example.elderlycare.modules.health.entity.HealthAlertRule;
import com.example.elderlycare.modules.health.entity.HealthMetric;
import com.example.elderlycare.modules.health.mapper.HealthAlertRuleMapper;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.service.HealthAlertRuleService;
import com.example.elderlycare.modules.health.vo.HealthAlertRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthAlertRuleServiceImpl implements HealthAlertRuleService {

    private final HealthAlertRuleMapper healthAlertRuleMapper;
    private final HealthMetricMapper healthMetricMapper;

    @Override
    public List<HealthAlertRuleVO> listRules(Long metricId, Integer enabled) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<HealthAlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, HealthAlertRule::getOrgId, currentOrgId);
        wrapper.eq(metricId != null, HealthAlertRule::getMetricId, metricId);
        wrapper.eq(enabled != null, HealthAlertRule::getEnabled, enabled);
        wrapper.orderByDesc(HealthAlertRule::getId);

        List<HealthAlertRule> rules = healthAlertRuleMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(rules)) {
            return Collections.emptyList();
        }

        Set<Long> metricIds = rules.stream().map(HealthAlertRule::getMetricId).collect(Collectors.toSet());
        Map<Long, HealthMetric> metricMap = metricIds.isEmpty() ? Collections.emptyMap() :
                healthMetricMapper.selectBatchIds(metricIds).stream().collect(Collectors.toMap(HealthMetric::getId, m -> m));

        return rules.stream().map(rule -> {
            HealthMetric metric = metricMap.get(rule.getMetricId());
            return convertToVO(rule, metric);
        }).collect(Collectors.toList());
    }

    @Override
    public HealthAlertRuleVO getRuleById(Long id) {
        HealthAlertRule rule = checkOrgAccess(id);
        HealthMetric metric = healthMetricMapper.selectById(rule.getMetricId());
        return convertToVO(rule, metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(HealthAlertRuleDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        // 校验关联指标有效性
        HealthMetric metric = healthMetricMapper.selectById(dto.getMetricId());
        if (metric == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联的健康指标不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(metric.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权使用其他机构的健康指标");
        }

        // 校验阈值有效性
        validateThresholds(dto.getConditionType(), dto.getMinValue(), dto.getMaxValue());

        HealthAlertRule rule = HealthAlertRule.builder()
                .orgId(currentOrgId != null ? currentOrgId : metric.getOrgId())
                .metricId(dto.getMetricId())
                .ruleName(dto.getRuleName().trim())
                .conditionType(dto.getConditionType().toUpperCase().trim())
                .minValue(dto.getMinValue())
                .maxValue(dto.getMaxValue())
                .severity(StringUtils.hasText(dto.getSeverity()) ? dto.getSeverity() : "MEDIUM")
                .dedupMinutes(dto.getDedupMinutes() != null && dto.getDedupMinutes() > 0 ? dto.getDedupMinutes() : 30)
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : 1)
                .createdAt(LocalDateTime.now())
                .build();

        healthAlertRuleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(Long id, HealthAlertRuleDTO dto) {
        HealthAlertRule rule = checkOrgAccess(id);

        if (dto.getMetricId() != null && !dto.getMetricId().equals(rule.getMetricId())) {
            HealthMetric metric = healthMetricMapper.selectById(dto.getMetricId());
            if (metric == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联的健康指标不存在");
            }
            rule.setMetricId(dto.getMetricId());
        }

        validateThresholds(dto.getConditionType(), dto.getMinValue(), dto.getMaxValue());

        rule.setRuleName(dto.getRuleName().trim());
        rule.setConditionType(dto.getConditionType().toUpperCase().trim());
        rule.setMinValue(dto.getMinValue());
        rule.setMaxValue(dto.getMaxValue());
        if (StringUtils.hasText(dto.getSeverity())) {
            rule.setSeverity(dto.getSeverity());
        }
        if (dto.getDedupMinutes() != null && dto.getDedupMinutes() > 0) {
            rule.setDedupMinutes(dto.getDedupMinutes());
        }
        if (dto.getEnabled() != null) {
            rule.setEnabled(dto.getEnabled());
        }

        healthAlertRuleMapper.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleRuleEnabled(Long id, Integer enabled) {
        HealthAlertRule rule = checkOrgAccess(id);
        rule.setEnabled(enabled != null && enabled == 1 ? 1 : 0);
        healthAlertRuleMapper.updateById(rule);
    }

    private void validateThresholds(String conditionType, java.math.BigDecimal min, java.math.BigDecimal max) {
        String ct = conditionType.toUpperCase().trim();
        if ("GT".equals(ct) || "GREATER_THAN".equals(ct)) {
            if (max == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "高于上限条件必须设置 maxValue（上限阈值）");
            }
        } else if ("LT".equals(ct) || "LESS_THAN".equals(ct)) {
            if (min == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "低于下限条件必须设置 minValue（下限阈值）");
            }
        } else if ("OUT_OF_RANGE".equals(ct)) {
            if (min == null && max == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "超出区间条件必须至少设置下限或上限阈值");
            }
            if (min != null && max != null && min.compareTo(max) > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "下限阈值不能大于上限阈值");
            }
        }
    }

    private HealthAlertRule checkOrgAccess(Long id) {
        HealthAlertRule rule = healthAlertRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "预警规则不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(rule.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的预警规则");
        }
        return rule;
    }

    private HealthAlertRuleVO convertToVO(HealthAlertRule rule, HealthMetric metric) {
        return HealthAlertRuleVO.builder()
                .id(rule.getId())
                .orgId(rule.getOrgId())
                .metricId(rule.getMetricId())
                .metricCode(metric != null ? metric.getMetricCode() : null)
                .metricName(metric != null ? metric.getMetricName() : null)
                .unit(metric != null ? metric.getUnit() : null)
                .ruleName(rule.getRuleName())
                .conditionType(rule.getConditionType())
                .minValue(rule.getMinValue())
                .maxValue(rule.getMaxValue())
                .severity(rule.getSeverity())
                .dedupMinutes(rule.getDedupMinutes())
                .enabled(rule.getEnabled())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
