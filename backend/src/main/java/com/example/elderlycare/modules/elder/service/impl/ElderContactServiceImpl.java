package com.example.elderlycare.modules.elder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.common.util.DesensitizeUtil;
import com.example.elderlycare.modules.elder.dto.ContactDTO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.entity.ElderContact;
import com.example.elderlycare.modules.elder.mapper.ElderContactMapper;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.elder.service.ElderContactService;
import com.example.elderlycare.modules.elder.vo.ContactVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElderContactServiceImpl implements ElderContactService {

    private final ElderContactMapper elderContactMapper;
    private final ElderMapper elderMapper;

    @Override
    public List<ContactVO> listContacts(Long elderId) {
        checkElderOrgAccess(elderId);

        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<ElderContact> wrapper = new LambdaQueryWrapper<ElderContact>()
                .eq(ElderContact::getElderId, elderId)
                .eq(currentOrgId != null, ElderContact::getOrgId, currentOrgId)
                .orderByDesc(ElderContact::getIsPrimary)
                .orderByAsc(ElderContact::getId);

        List<ElderContact> contacts = elderContactMapper.selectList(wrapper);
        return contacts.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContact(Long elderId, ContactDTO dto) {
        Elder elder = checkElderOrgAccess(elderId);
        Long currentOrgId = OrgContextHolder.getOrgId() != null ? OrgContextHolder.getOrgId() : elder.getOrgId();

        // 若设置为主要联系人，先将其他联系人的 isPrimary 置为 0，确保主要联系人唯一性约束
        if (Integer.valueOf(1).equals(dto.getIsPrimary())) {
            resetOtherPrimaryContacts(elderId);
        }

        ElderContact contact = ElderContact.builder()
                .orgId(currentOrgId)
                .elderId(elderId)
                .contactName(dto.getContactName())
                .relationship(dto.getRelationship())
                .phone(dto.getPhone())
                .isPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : 0)
                .isEmergency(dto.getIsEmergency() != null ? dto.getIsEmergency() : 0)
                .address(dto.getAddress())
                .createdAt(LocalDateTime.now())
                .build();

        elderContactMapper.insert(contact);
        return contact.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContact(Long elderId, Long contactId, ContactDTO dto) {
        checkElderOrgAccess(elderId);
        ElderContact contact = checkContactBelongsToElder(contactId, elderId);

        if (Integer.valueOf(1).equals(dto.getIsPrimary()) && !Integer.valueOf(1).equals(contact.getIsPrimary())) {
            resetOtherPrimaryContacts(elderId);
        }

        contact.setContactName(dto.getContactName());
        contact.setRelationship(dto.getRelationship());
        contact.setPhone(dto.getPhone());
        contact.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : 0);
        contact.setIsEmergency(dto.getIsEmergency() != null ? dto.getIsEmergency() : 0);
        contact.setAddress(dto.getAddress());

        elderContactMapper.updateById(contact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContact(Long elderId, Long contactId) {
        checkElderOrgAccess(elderId);
        ElderContact contact = checkContactBelongsToElder(contactId, elderId);
        elderContactMapper.deleteById(contact.getId());
    }

    private void resetOtherPrimaryContacts(Long elderId) {
        List<ElderContact> existing = elderContactMapper.selectList(
                new LambdaQueryWrapper<ElderContact>()
                        .eq(ElderContact::getElderId, elderId)
                        .eq(ElderContact::getIsPrimary, 1)
        );
        for (ElderContact c : existing) {
            c.setIsPrimary(0);
            elderContactMapper.updateById(c);
        }
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

    private ElderContact checkContactBelongsToElder(Long contactId, Long elderId) {
        ElderContact contact = elderContactMapper.selectById(contactId);
        if (contact == null || !elderId.equals(contact.getElderId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "联系人不存在或不属于该老人");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(contact.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的联系人数据");
        }
        return contact;
    }

    private ContactVO convertToVO(ElderContact contact) {
        return ContactVO.builder()
                .id(contact.getId())
                .elderId(contact.getElderId())
                .contactName(contact.getContactName())
                .relationship(contact.getRelationship())
                .phone(contact.getPhone())
                .phoneMasked(DesensitizeUtil.maskPhone(contact.getPhone()))
                .isPrimary(contact.getIsPrimary())
                .isEmergency(contact.getIsEmergency())
                .address(contact.getAddress())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}
