package com.example.elderlycare.modules.regulatory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.regulatory.dto.FieldMappingDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateCreateDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateQueryDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateUpdateDTO;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryTemplate;
import com.example.elderlycare.modules.regulatory.mapper.RegulatoryTemplateMapper;
import com.example.elderlycare.modules.regulatory.service.RegulatoryTemplateService;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryTemplateVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegulatoryTemplateServiceImpl implements RegulatoryTemplateService {

    private final RegulatoryTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    private Long getCurrentOrgId() {
        Long orgId = OrgContextHolder.getOrgId();
        return orgId != null ? orgId : 1L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(RegulatoryTemplateCreateDTO dto) {
        Long orgId = getCurrentOrgId();

        // 校验机构下 templateCode + versionNo 唯一性
        Long count = templateMapper.selectCount(new LambdaQueryWrapper<RegulatoryTemplate>()
                .eq(RegulatoryTemplate::getOrgId, orgId)
                .eq(RegulatoryTemplate::getTemplateCode, dto.getTemplateCode().trim())
                .eq(RegulatoryTemplate::getVersionNo, dto.getVersionNo().trim()));
        if (count > 0) {
            throw new BusinessException("该监管模板代码与版本号已存在，不可重复创建");
        }

        String fieldMappingJson = serializeFieldMapping(dto.getFieldMapping());

        RegulatoryTemplate entity = RegulatoryTemplate.builder()
                .orgId(orgId)
                .templateCode(dto.getTemplateCode().trim())
                .templateName(dto.getTemplateName().trim())
                .versionNo(dto.getVersionNo().trim())
                .fieldMappingJson(fieldMappingJson)
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, RegulatoryTemplateUpdateDTO dto) {
        Long orgId = getCurrentOrgId();
        RegulatoryTemplate template = templateMapper.selectById(id);
        if (template == null || !template.getOrgId().equals(orgId)) {
            throw new BusinessException("监管模板不存在或无权操作");
        }

        if (StringUtils.hasText(dto.getTemplateName())) {
            template.setTemplateName(dto.getTemplateName().trim());
        }
        if (StringUtils.hasText(dto.getVersionNo())) {
            template.setVersionNo(dto.getVersionNo().trim());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            template.setStatus(dto.getStatus());
        }
        if (dto.getFieldMapping() != null) {
            template.setFieldMappingJson(serializeFieldMapping(dto.getFieldMapping()));
        }

        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        Long orgId = getCurrentOrgId();
        RegulatoryTemplate template = templateMapper.selectById(id);
        if (template == null || !template.getOrgId().equals(orgId)) {
            throw new BusinessException("监管模板不存在或无权操作");
        }
        templateMapper.deleteById(id);
    }

    @Override
    public RegulatoryTemplateVO getTemplateDetail(Long id) {
        Long orgId = getCurrentOrgId();
        RegulatoryTemplate template = templateMapper.selectById(id);
        if (template == null || !template.getOrgId().equals(orgId)) {
            throw new BusinessException("监管模板不存在或无权访问");
        }
        return convertToVO(template);
    }

    @Override
    public PageResult<RegulatoryTemplateVO> pageTemplates(RegulatoryTemplateQueryDTO query) {
        Long orgId = getCurrentOrgId();
        LambdaQueryWrapper<RegulatoryTemplate> wrapper = new LambdaQueryWrapper<RegulatoryTemplate>()
                .eq(RegulatoryTemplate::getOrgId, orgId);

        if (StringUtils.hasText(query.getTemplateCode())) {
            wrapper.eq(RegulatoryTemplate::getTemplateCode, query.getTemplateCode().trim());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(RegulatoryTemplate::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(RegulatoryTemplate::getTemplateName, kw)
                    .or().like(RegulatoryTemplate::getTemplateCode, kw));
        }

        wrapper.orderByDesc(RegulatoryTemplate::getCreatedAt);

        Page<RegulatoryTemplate> pageParam = new Page<>(query.getPageNum(), query.getPageSize());
        Page<RegulatoryTemplate> page = templateMapper.selectPage(pageParam, wrapper);

        List<RegulatoryTemplateVO> list = page.getRecords().stream()
                .map(this::convertToVO)
                .filter(vo -> {
                    if (StringUtils.hasText(query.getBusinessDomain())) {
                        return vo.getFieldMapping() != null && query.getBusinessDomain().equalsIgnoreCase(vo.getFieldMapping().getBusinessDomain());
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return PageResult.of(list, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public List<RegulatoryTemplateVO> listActiveTemplates() {
        Long orgId = getCurrentOrgId();
        List<RegulatoryTemplate> list = templateMapper.selectList(new LambdaQueryWrapper<RegulatoryTemplate>()
                .eq(RegulatoryTemplate::getOrgId, orgId)
                .eq(RegulatoryTemplate::getStatus, "ACTIVE")
                .orderByAsc(RegulatoryTemplate::getId));

        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private RegulatoryTemplateVO convertToVO(RegulatoryTemplate entity) {
        FieldMappingDTO mapping = deserializeFieldMapping(entity.getFieldMappingJson());
        int fieldCount = mapping != null && mapping.getFields() != null ? mapping.getFields().size() : 0;

        return RegulatoryTemplateVO.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .versionNo(entity.getVersionNo())
                .fieldMapping(mapping)
                .fieldCount(fieldCount)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String serializeFieldMapping(FieldMappingDTO dto) {
        if (dto == null) return "{}";
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("字段映射序列化失败", e);
            throw new BusinessException("字段映射配置 JSON 序列化失败");
        }
    }

    private FieldMappingDTO deserializeFieldMapping(String json) {
        if (!StringUtils.hasText(json)) {
            return FieldMappingDTO.builder().fields(Collections.emptyList()).build();
        }
        try {
            return objectMapper.readValue(json, FieldMappingDTO.class);
        } catch (Exception e) {
            log.error("字段映射反序列化失败: {}", json, e);
            return FieldMappingDTO.builder().fields(Collections.emptyList()).build();
        }
    }
}
