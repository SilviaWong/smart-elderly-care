package com.example.elderlycare.modules.elder.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.elder.dto.ElderCreateDTO;
import com.example.elderlycare.modules.elder.dto.ElderQueryDTO;
import com.example.elderlycare.modules.elder.dto.ElderUpdateDTO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.vo.ElderDetailVO;
import com.example.elderlycare.modules.elder.vo.ElderListVO;

public interface ElderService {

    PageResult<ElderListVO> pageElders(ElderQueryDTO query);

    ElderDetailVO getElderDetail(Long id);

    Long createElder(ElderCreateDTO dto);

    void updateElder(Long id, ElderUpdateDTO dto);

    void archiveElder(Long id);

    Elder getElderEntityById(Long id);
}
