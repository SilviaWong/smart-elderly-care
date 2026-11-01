package com.example.elderlycare.modules.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.care.dto.CareItemDTO;
import com.example.elderlycare.modules.care.dto.CareLevelDTO;
import com.example.elderlycare.modules.care.entity.CareItem;
import com.example.elderlycare.modules.care.entity.CareLevel;
import com.example.elderlycare.modules.care.mapper.CareItemMapper;
import com.example.elderlycare.modules.care.mapper.CareLevelMapper;
import com.example.elderlycare.modules.care.service.CareConfigService;
import com.example.elderlycare.modules.care.vo.CareItemVO;
import com.example.elderlycare.modules.care.vo.CareLevelVO;
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
public class CareConfigServiceImpl implements CareConfigService {

    private final CareLevelMapper careLevelMapper;
    private final CareItemMapper careItemMapper;

    @Override
    public List<CareLevelVO> listLevels(String status) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<CareLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, CareLevel::getOrgId, currentOrgId);
        wrapper.eq(StringUtils.hasText(status), CareLevel::getStatus, status);
        wrapper.orderByAsc(CareLevel::getId);

        List<CareLevel> levels = careLevelMapper.selectList(wrapper);
        return levels.stream().map(l -> CareLevelVO.builder()
                .id(l.getId())
                .orgId(l.getOrgId())
                .levelCode(l.getLevelCode())
                .levelName(l.getLevelName())
                .description(l.getDescription())
                .status(l.getStatus())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLevel(CareLevelDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long count = careLevelMapper.selectCount(
                new LambdaQueryWrapper<CareLevel>()
                        .eq(currentOrgId != null, CareLevel::getOrgId, currentOrgId)
                        .eq(CareLevel::getLevelCode, dto.getLevelCode().trim())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "护理等级编码 [" + dto.getLevelCode() + "] 在当前机构已存在");
        }

        CareLevel level = CareLevel.builder()
                .orgId(currentOrgId)
                .levelCode(dto.getLevelCode().trim())
                .levelName(dto.getLevelName().trim())
                .description(dto.getDescription())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ACTIVE")
                .build();

        careLevelMapper.insert(level);
        return level.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLevel(Long id, CareLevelDTO dto) {
        CareLevel level = checkLevelOrgAccess(id);
        Long currentOrgId = OrgContextHolder.getOrgId();

        if (!level.getLevelCode().equalsIgnoreCase(dto.getLevelCode().trim())) {
            Long count = careLevelMapper.selectCount(
                    new LambdaQueryWrapper<CareLevel>()
                            .eq(currentOrgId != null, CareLevel::getOrgId, currentOrgId)
                            .eq(CareLevel::getLevelCode, dto.getLevelCode().trim())
                            .ne(CareLevel::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "护理等级编码 [" + dto.getLevelCode() + "] 在当前机构已存在");
            }
            level.setLevelCode(dto.getLevelCode().trim());
        }

        level.setLevelName(dto.getLevelName().trim());
        level.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getStatus())) {
            level.setStatus(dto.getStatus());
        }
        careLevelMapper.updateById(level);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleLevelStatus(Long id, String status) {
        CareLevel level = checkLevelOrgAccess(id);
        level.setStatus(status);
        careLevelMapper.updateById(level);
    }

    @Override
    public List<CareItemVO> listItems(String category, String status) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<CareItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, CareItem::getOrgId, currentOrgId);
        wrapper.eq(StringUtils.hasText(category), CareItem::getCategory, category);
        wrapper.eq(StringUtils.hasText(status), CareItem::getStatus, status);
        wrapper.orderByAsc(CareItem::getId);

        List<CareItem> items = careItemMapper.selectList(wrapper);
        return items.stream().map(i -> CareItemVO.builder()
                .id(i.getId())
                .orgId(i.getOrgId())
                .itemCode(i.getItemCode())
                .itemName(i.getItemName())
                .category(i.getCategory())
                .description(i.getDescription())
                .status(i.getStatus())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createItem(CareItemDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long count = careItemMapper.selectCount(
                new LambdaQueryWrapper<CareItem>()
                        .eq(currentOrgId != null, CareItem::getOrgId, currentOrgId)
                        .eq(CareItem::getItemCode, dto.getItemCode().trim())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "护理项目编码 [" + dto.getItemCode() + "] 在当前机构已存在");
        }

        CareItem item = CareItem.builder()
                .orgId(currentOrgId)
                .itemCode(dto.getItemCode().trim())
                .itemName(dto.getItemName().trim())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ACTIVE")
                .build();

        careItemMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long id, CareItemDTO dto) {
        CareItem item = checkItemOrgAccess(id);
        Long currentOrgId = OrgContextHolder.getOrgId();

        if (!item.getItemCode().equalsIgnoreCase(dto.getItemCode().trim())) {
            Long count = careItemMapper.selectCount(
                    new LambdaQueryWrapper<CareItem>()
                            .eq(currentOrgId != null, CareItem::getOrgId, currentOrgId)
                            .eq(CareItem::getItemCode, dto.getItemCode().trim())
                            .ne(CareItem::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "护理项目编码 [" + dto.getItemCode() + "] 在当前机构已存在");
            }
            item.setItemCode(dto.getItemCode().trim());
        }

        item.setItemName(dto.getItemName().trim());
        item.setCategory(dto.getCategory());
        item.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getStatus())) {
            item.setStatus(dto.getStatus());
        }
        careItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleItemStatus(Long id, String status) {
        CareItem item = checkItemOrgAccess(id);
        item.setStatus(status);
        careItemMapper.updateById(item);
    }

    private CareLevel checkLevelOrgAccess(Long id) {
        CareLevel level = careLevelMapper.selectById(id);
        if (level == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理等级不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(level.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的护理等级");
        }
        return level;
    }

    private CareItem checkItemOrgAccess(Long id) {
        CareItem item = careItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理项目不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(item.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的护理项目");
        }
        return item;
    }
}
