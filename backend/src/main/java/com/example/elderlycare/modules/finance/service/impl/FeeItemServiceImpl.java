package com.example.elderlycare.modules.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.finance.dto.FeeItemDTO;
import com.example.elderlycare.modules.finance.dto.FeeItemQueryDTO;
import com.example.elderlycare.modules.finance.entity.FeeItem;
import com.example.elderlycare.modules.finance.mapper.FeeItemMapper;
import com.example.elderlycare.modules.finance.service.FeeItemService;
import com.example.elderlycare.modules.finance.vo.FeeItemVO;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeItemServiceImpl implements FeeItemService {

    private final FeeItemMapper feeItemMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFeeItem(FeeItemDTO dto) {
        Long orgId = OrgContextHolder.getOrgId() != null ? OrgContextHolder.getOrgId() : 1L;

        Long count = feeItemMapper.selectCount(
                new LambdaQueryWrapper<FeeItem>()
                        .eq(FeeItem::getOrgId, orgId)
                        .eq(FeeItem::getFeeCode, dto.getFeeCode().trim())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "费用编码已存在: " + dto.getFeeCode());
        }

        FeeItem item = FeeItem.builder()
                .orgId(orgId)
                .feeCode(dto.getFeeCode().trim().toUpperCase())
                .feeName(dto.getFeeName().trim())
                .feeCategory(dto.getFeeCategory().trim().toUpperCase())
                .billingUnit(dto.getBillingUnit().trim())
                .defaultPrice(dto.getDefaultPrice())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim().toUpperCase() : "ACTIVE")
                .build();

        feeItemMapper.insert(item);

        sysOperationLogService.recordLog(
                "FINANCE_FEE_ITEM",
                "CREATE",
                "FEE_ITEM",
                item.getId(),
                "/api/v1/finance/fee-items",
                "127.0.0.1",
                String.format("创建收费项目: 编码=%s, 名称=%s, 单价=%s", item.getFeeCode(), item.getFeeName(), item.getDefaultPrice())
        );

        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeeItem(Long id, FeeItemDTO dto) {
        FeeItem item = getAndCheckFeeItem(id);

        if (!item.getFeeCode().equalsIgnoreCase(dto.getFeeCode().trim())) {
            Long count = feeItemMapper.selectCount(
                    new LambdaQueryWrapper<FeeItem>()
                            .eq(FeeItem::getOrgId, item.getOrgId())
                            .eq(FeeItem::getFeeCode, dto.getFeeCode().trim())
                            .ne(FeeItem::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "费用编码已被使用: " + dto.getFeeCode());
            }
        }

        item.setFeeCode(dto.getFeeCode().trim().toUpperCase());
        item.setFeeName(dto.getFeeName().trim());
        item.setFeeCategory(dto.getFeeCategory().trim().toUpperCase());
        item.setBillingUnit(dto.getBillingUnit().trim());
        item.setDefaultPrice(dto.getDefaultPrice());
        if (StringUtils.hasText(dto.getStatus())) {
            item.setStatus(dto.getStatus().trim().toUpperCase());
        }

        feeItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleFeeItemStatus(Long id, String status) {
        FeeItem item = getAndCheckFeeItem(id);
        item.setStatus(status.trim().toUpperCase());
        feeItemMapper.updateById(item);
    }

    @Override
    public FeeItemVO getFeeItem(Long id) {
        FeeItem item = getAndCheckFeeItem(id);
        return convertToVO(item);
    }

    @Override
    public PageResult<FeeItemVO> pageFeeItems(FeeItemQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<FeeItem> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(FeeItem::getOrgId, currentOrgId);
        }
        if (StringUtils.hasText(query.getFeeCategory())) {
            qw.eq(FeeItem::getFeeCategory, query.getFeeCategory().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(FeeItem::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(FeeItem::getFeeCode, kw).or().like(FeeItem::getFeeName, kw));
        }

        qw.orderByAsc(FeeItem::getFeeCategory).orderByAsc(FeeItem::getFeeCode);

        Page<FeeItem> page = feeItemMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<FeeItemVO> voList = page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<FeeItemVO> listActiveFeeItems() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<FeeItem> qw = new LambdaQueryWrapper<FeeItem>()
                .eq(currentOrgId != null, FeeItem::getOrgId, currentOrgId)
                .eq(FeeItem::getStatus, "ACTIVE")
                .orderByAsc(FeeItem::getFeeCategory)
                .orderByAsc(FeeItem::getFeeCode);

        List<FeeItem> list = feeItemMapper.selectList(qw);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private FeeItem getAndCheckFeeItem(Long id) {
        FeeItem item = feeItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "收费项目不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(item.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构收费项目");
        }
        return item;
    }

    private FeeItemVO convertToVO(FeeItem item) {
        return FeeItemVO.builder()
                .id(item.getId())
                .orgId(item.getOrgId())
                .feeCode(item.getFeeCode())
                .feeName(item.getFeeName())
                .feeCategory(item.getFeeCategory())
                .feeCategoryName(getCategoryName(item.getFeeCategory()))
                .billingUnit(item.getBillingUnit())
                .defaultPrice(item.getDefaultPrice())
                .status(item.getStatus())
                .statusName("ACTIVE".equalsIgnoreCase(item.getStatus()) ? "启用" : "停用")
                .build();
    }

    private String getCategoryName(String cat) {
        if (cat == null) return "其他";
        return switch (cat.toUpperCase()) {
            case "BED" -> "床位费";
            case "CARE" -> "护理费";
            case "MEAL" -> "餐饮膳食费";
            case "SERVICE" -> "日常照护服务费";
            case "MEDICAL" -> "医疗耗材费";
            case "DEPOSIT" -> "综合押金/备用金";
            default -> cat;
        };
    }
}
