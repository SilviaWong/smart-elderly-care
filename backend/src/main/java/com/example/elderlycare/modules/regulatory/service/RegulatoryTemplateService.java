package com.example.elderlycare.modules.regulatory.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateCreateDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateQueryDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateUpdateDTO;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryTemplateVO;

import java.util.List;

public interface RegulatoryTemplateService {

    /**
     * 创建监管报送模板
     */
    Long createTemplate(RegulatoryTemplateCreateDTO dto);

    /**
     * 修改监管报送模板及字段映射
     */
    void updateTemplate(Long id, RegulatoryTemplateUpdateDTO dto);

    /**
     * 删除监管报送模板
     */
    void deleteTemplate(Long id);

    /**
     * 获取指定模板详情
     */
    RegulatoryTemplateVO getTemplateDetail(Long id);

    /**
     * 分页查询监管报送模板
     */
    PageResult<RegulatoryTemplateVO> pageTemplates(RegulatoryTemplateQueryDTO query);

    /**
     * 查询所有启用的有效模板列表
     */
    List<RegulatoryTemplateVO> listActiveTemplates();
}
