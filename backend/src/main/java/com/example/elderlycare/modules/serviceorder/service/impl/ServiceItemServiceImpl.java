package com.example.elderlycare.modules.serviceorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.serviceorder.dto.ServiceItemDTO;
import com.example.elderlycare.modules.serviceorder.entity.ServiceItem;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceItemMapper;
import com.example.elderlycare.modules.serviceorder.service.ServiceItemService;
import com.example.elderlycare.modules.serviceorder.vo.ServiceItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemMapper serviceItemMapper;

    @Override
    public List<ServiceItemVO> listItems(String category, String status) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, ServiceItem::getOrgId, currentOrgId);
        wrapper.eq(StringUtils.hasText(category), ServiceItem::getCategory, category);
        wrapper.eq(StringUtils.hasText(status), ServiceItem::getStatus, status);
        wrapper.orderByAsc(ServiceItem::getItemCode);

        List<ServiceItem> list = serviceItemMapper.selectList(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public ServiceItemVO getItemById(Long id) {
        ServiceItem item = checkOrgAccess(id);
        return convertToVO(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createItem(ServiceItemDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        // 校验机构内项目编码唯一性
        Long count = serviceItemMapper.selectCount(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(currentOrgId != null, ServiceItem::getOrgId, currentOrgId)
                        .eq(ServiceItem::getItemCode, dto.getItemCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "服务项目编码 [" + dto.getItemCode() + "] 在当前机构已存在");
        }

        ServiceItem item = ServiceItem.builder()
                .orgId(currentOrgId)
                .itemCode(dto.getItemCode())
                .itemName(dto.getItemName())
                .category(dto.getCategory())
                .unit(dto.getUnit())
                .standardPrice(dto.getStandardPrice())
                .needConfirm(dto.getNeedConfirm() != null ? dto.getNeedConfirm() : 0)
                .description(dto.getDescription())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        serviceItemMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long id, ServiceItemDTO dto) {
        ServiceItem item = checkOrgAccess(id);
        Long currentOrgId = OrgContextHolder.getOrgId();

        if (!item.getItemCode().equals(dto.getItemCode())) {
            Long count = serviceItemMapper.selectCount(
                    new LambdaQueryWrapper<ServiceItem>()
                            .eq(currentOrgId != null, ServiceItem::getOrgId, currentOrgId)
                            .eq(ServiceItem::getItemCode, dto.getItemCode())
                            .ne(ServiceItem::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "服务项目编码 [" + dto.getItemCode() + "] 在当前机构已存在");
            }
            item.setItemCode(dto.getItemCode());
        }

        item.setItemName(dto.getItemName());
        item.setCategory(dto.getCategory());
        item.setUnit(dto.getUnit());
        item.setStandardPrice(dto.getStandardPrice());
        if (dto.getNeedConfirm() != null) {
            item.setNeedConfirm(dto.getNeedConfirm());
        }
        item.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getStatus())) {
            item.setStatus(dto.getStatus());
        }

        serviceItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleItemStatus(Long id, String status) {
        ServiceItem item = checkOrgAccess(id);
        item.setStatus(status);
        serviceItemMapper.updateById(item);
    }

    private ServiceItem checkOrgAccess(Long id) {
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "服务项目不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(item.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的服务项目");
        }
        return item;
    }

    private ServiceItemVO convertToVO(ServiceItem item) {
        return ServiceItemVO.builder()
                .id(item.getId())
                .orgId(item.getOrgId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .category(item.getCategory())
                .unit(item.getUnit())
                .standardPrice(item.getStandardPrice())
                .needConfirm(item.getNeedConfirm())
                .description(item.getDescription())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
