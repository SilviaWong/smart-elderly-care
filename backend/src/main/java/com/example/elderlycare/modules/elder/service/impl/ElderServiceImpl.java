package com.example.elderlycare.modules.elder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.common.util.DesensitizeUtil;
import com.example.elderlycare.common.util.SecurityCryptoUtil;
import com.example.elderlycare.modules.elder.dto.ContactDTO;
import com.example.elderlycare.modules.elder.dto.ElderCreateDTO;
import com.example.elderlycare.modules.elder.dto.ElderQueryDTO;
import com.example.elderlycare.modules.elder.dto.ElderUpdateDTO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.entity.ElderContact;
import com.example.elderlycare.modules.elder.mapper.ElderContactMapper;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.elder.service.ElderAssessmentService;
import com.example.elderlycare.modules.elder.service.ElderContactService;
import com.example.elderlycare.modules.elder.service.ElderService;
import com.example.elderlycare.modules.elder.vo.AssessmentVO;
import com.example.elderlycare.modules.elder.vo.ContactVO;
import com.example.elderlycare.modules.elder.vo.ElderDetailVO;
import com.example.elderlycare.modules.elder.vo.ElderListVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElderServiceImpl implements ElderService {

    private final ElderMapper elderMapper;
    private final ElderContactMapper elderContactMapper;
    private final ElderContactService elderContactService;
    private final ElderAssessmentService elderAssessmentService;
    private final SysUserMapper sysUserMapper;
    private final SecurityCryptoUtil securityCryptoUtil;

    @Override
    public PageResult<ElderListVO> pageElders(ElderQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<Elder> wrapper = new LambdaQueryWrapper<>();
        // 强制机构多租户数据隔离
        wrapper.eq(currentOrgId != null, Elder::getOrgId, currentOrgId);
        wrapper.eq(StringUtils.hasText(query.getElderNo()), Elder::getElderNo, query.getElderNo());
        wrapper.like(StringUtils.hasText(query.getName()), Elder::getName, query.getName());
        wrapper.like(StringUtils.hasText(query.getPhone()), Elder::getPhone, query.getPhone());
        wrapper.eq(StringUtils.hasText(query.getStatus()), Elder::getStatus, query.getStatus());
        wrapper.eq(StringUtils.hasText(query.getMobilityLevel()), Elder::getMobilityLevel, query.getMobilityLevel());

        // 证件号精确检索（基于 SHA-256 哈希列）
        if (StringUtils.hasText(query.getIdNo())) {
            String idHash = securityCryptoUtil.sha256Hex(query.getIdNo());
            wrapper.eq(Elder::getIdNoHash, idHash);
        }

        wrapper.orderByDesc(Elder::getId);

        Page<Elder> page = elderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper
        );

        List<ElderListVO> list = page.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ElderDetailVO getElderDetail(Long id) {
        Elder elder = checkElderOrgAccess(id);

        List<ContactVO> contacts = elderContactService.listContacts(id);
        List<AssessmentVO> assessments = elderAssessmentService.listAssessments(id);

        String creatorName = "系统管理员";
        if (elder.getCreatedBy() != null) {
            SysUser creator = sysUserMapper.selectById(elder.getCreatedBy());
            if (creator != null) {
                creatorName = creator.getRealName();
            }
        }

        String plainIdNo = securityCryptoUtil.decrypt(elder.getIdNoCiphertext());
        String maskedIdNo = DesensitizeUtil.maskIdNo(plainIdNo);

        Integer age = null;
        if (elder.getBirthDate() != null) {
            age = Period.between(elder.getBirthDate(), LocalDate.now()).getYears();
        }

        return ElderDetailVO.builder()
                .id(elder.getId())
                .orgId(elder.getOrgId())
                .elderNo(elder.getElderNo())
                .name(elder.getName())
                .gender(elder.getGender())
                .birthDate(elder.getBirthDate())
                .age(age)
                .idType(elder.getIdType())
                .idNoMasked(maskedIdNo)
                .phoneMasked(DesensitizeUtil.maskPhone(elder.getPhone()))
                .address(elder.getAddress())
                .photoUrl(elder.getPhotoUrl())
                .bloodType(elder.getBloodType())
                .allergyDesc(elder.getAllergyDesc())
                .chronicDiseaseDesc(elder.getChronicDiseaseDesc())
                .medicationDesc(elder.getMedicationDesc())
                .dietTaboo(elder.getDietTaboo())
                .mobilityLevel(elder.getMobilityLevel())
                .status(elder.getStatus())
                .remark(elder.getRemark())
                .creatorName(creatorName)
                .createdAt(elder.getCreatedAt())
                .contacts(contacts)
                .assessments(assessments)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createElder(ElderCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        // 校验老人编号在机构内唯一性
        Long count = elderMapper.selectCount(
                new LambdaQueryWrapper<Elder>()
                        .eq(Elder::getElderNo, dto.getElderNo())
                        .eq(currentOrgId != null, Elder::getOrgId, currentOrgId)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "老人档案编号 [" + dto.getElderNo() + "] 在当前机构已存在");
        }

        // 敏感数据加密与哈希处理
        String cipher = null;
        String hash = null;
        if (StringUtils.hasText(dto.getIdNo())) {
            cipher = securityCryptoUtil.encrypt(dto.getIdNo());
            hash = securityCryptoUtil.sha256Hex(dto.getIdNo());
        }

        Elder elder = Elder.builder()
                .orgId(currentOrgId)
                .elderNo(dto.getElderNo())
                .name(dto.getName())
                .gender(dto.getGender())
                .birthDate(dto.getBirthDate())
                .idType(dto.getIdType())
                .idNoCiphertext(cipher)
                .idNoHash(hash)
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .photoUrl(dto.getPhotoUrl())
                .bloodType(dto.getBloodType())
                .allergyDesc(dto.getAllergyDesc())
                .chronicDiseaseDesc(dto.getChronicDiseaseDesc())
                .medicationDesc(dto.getMedicationDesc())
                .dietTaboo(dto.getDietTaboo())
                .mobilityLevel(dto.getMobilityLevel())
                .status("PENDING") // 初始状态为待入住
                .remark(dto.getRemark())
                .createdBy(currentUserId)
                .createdAt(LocalDateTime.now())
                .build();

        elderMapper.insert(elder);

        // 如果建档时附带了联系人信息，一并创建
        if (dto.getContacts() != null && !dto.getContacts().isEmpty()) {
            for (ContactDTO contactDTO : dto.getContacts()) {
                elderContactService.createContact(elder.getId(), contactDTO);
            }
        }

        return elder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateElder(Long id, ElderUpdateDTO dto) {
        Elder elder = checkElderOrgAccess(id);
        Long currentUserId = OrgContextHolder.getUserId();

        elder.setName(dto.getName());
        elder.setGender(dto.getGender());
        elder.setBirthDate(dto.getBirthDate());
        elder.setIdType(dto.getIdType());
        if (StringUtils.hasText(dto.getIdNo())) {
            elder.setIdNoCiphertext(securityCryptoUtil.encrypt(dto.getIdNo()));
            elder.setIdNoHash(securityCryptoUtil.sha256Hex(dto.getIdNo()));
        }
        elder.setPhone(dto.getPhone());
        elder.setAddress(dto.getAddress());
        elder.setPhotoUrl(dto.getPhotoUrl());
        elder.setBloodType(dto.getBloodType());
        elder.setAllergyDesc(dto.getAllergyDesc());
        elder.setChronicDiseaseDesc(dto.getChronicDiseaseDesc());
        elder.setMedicationDesc(dto.getMedicationDesc());
        elder.setDietTaboo(dto.getDietTaboo());
        elder.setMobilityLevel(dto.getMobilityLevel());
        if (StringUtils.hasText(dto.getStatus())) {
            elder.setStatus(dto.getStatus());
        }
        elder.setRemark(dto.getRemark());
        elder.setUpdatedBy(currentUserId);
        elder.setUpdatedAt(LocalDateTime.now());

        elderMapper.updateById(elder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveElder(Long id) {
        Elder elder = checkElderOrgAccess(id);
        // PRD 规则：退住或归档后保留档案历史，禁止物理删除老人主档案
        elder.setStatus("ARCHIVED");
        elder.setUpdatedBy(OrgContextHolder.getUserId());
        elder.setUpdatedAt(LocalDateTime.now());
        elderMapper.updateById(elder);
    }

    @Override
    public Elder getElderEntityById(Long id) {
        return checkElderOrgAccess(id);
    }

    private Elder checkElderOrgAccess(Long id) {
        Elder elder = elderMapper.selectById(id);
        if (elder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "老人档案不存在");
        }

        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问或修改其他机构的老人档案");
        }

        return elder;
    }

    private ElderListVO convertToListVO(Elder elder) {
        Integer age = null;
        if (elder.getBirthDate() != null) {
            age = Period.between(elder.getBirthDate(), LocalDate.now()).getYears();
        }

        String plainIdNo = securityCryptoUtil.decrypt(elder.getIdNoCiphertext());
        String maskedIdNo = DesensitizeUtil.maskIdNo(plainIdNo);

        // 获取主要联系人
        ElderContact primaryContact = elderContactMapper.selectOne(
                new LambdaQueryWrapper<ElderContact>()
                        .eq(ElderContact::getElderId, elder.getId())
                        .eq(ElderContact::getIsPrimary, 1)
                        .last("LIMIT 1")
        );

        String contactName = primaryContact != null ? primaryContact.getContactName() : "--";
        String contactPhone = primaryContact != null ? DesensitizeUtil.maskPhone(primaryContact.getPhone()) : "--";

        return ElderListVO.builder()
                .id(elder.getId())
                .orgId(elder.getOrgId())
                .elderNo(elder.getElderNo())
                .name(elder.getName())
                .gender(elder.getGender())
                .birthDate(elder.getBirthDate())
                .age(age)
                .phoneMasked(DesensitizeUtil.maskPhone(elder.getPhone()))
                .idNoMasked(maskedIdNo)
                .primaryContactName(contactName)
                .primaryContactPhone(contactPhone)
                .mobilityLevel(elder.getMobilityLevel())
                .status(elder.getStatus())
                .createdAt(elder.getCreatedAt())
                .build();
    }
}
