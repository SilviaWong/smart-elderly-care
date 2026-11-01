package com.example.elderlycare.modules.elder;

import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.common.util.SecurityCryptoUtil;
import com.example.elderlycare.modules.elder.dto.ElderCreateDTO;
import com.example.elderlycare.modules.elder.dto.ElderUpdateDTO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderContactMapper;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.elder.service.ElderAssessmentService;
import com.example.elderlycare.modules.elder.service.ElderContactService;
import com.example.elderlycare.modules.elder.service.impl.ElderServiceImpl;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElderServiceTest {

    @Mock
    private ElderMapper elderMapper;

    @Mock
    private ElderContactMapper elderContactMapper;

    @Mock
    private ElderContactService elderContactService;

    @Mock
    private ElderAssessmentService elderAssessmentService;

    @Mock
    private SysUserMapper sysUserMapper;

    @Spy
    private SecurityCryptoUtil securityCryptoUtil = new SecurityCryptoUtil("ElderlyCareAesSecretKey32BytesLong!");

    @InjectMocks
    private ElderServiceImpl elderService;

    @BeforeEach
    void setUp() {
        OrgContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("同一机构内老人编号重复时必须抛出 CONFLICT 业务异常")
    void shouldThrowConflictWhenElderNoAlreadyExistsInSameOrg() {
        OrgContextHolder.setOrgId(1L);

        when(elderMapper.selectCount(any())).thenReturn(1L);

        ElderCreateDTO dto = ElderCreateDTO.builder()
                .elderNo("ELD-001")
                .name("张建国")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            elderService.createElder(dto);
        });

        assertEquals(ErrorCode.CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("在当前机构已存在"));
        verify(elderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("建档时证件号必须自动加密存储且生成 SHA-256 哈希索引")
    void shouldEncryptIdNoAndComputeHashWhenCreatingElder() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(10L);

        when(elderMapper.selectCount(any())).thenReturn(0L);

        ElderCreateDTO dto = ElderCreateDTO.builder()
                .elderNo("ELD-002")
                .name("李淑芬")
                .idType("ID_CARD")
                .idNo("110101194805121234")
                .birthDate(LocalDate.of(1948, 5, 12))
                .build();

        elderService.createElder(dto);

        ArgumentCaptor<Elder> captor = ArgumentCaptor.forClass(Elder.class);
        verify(elderMapper).insert(captor.capture());

        Elder saved = captor.getValue();
        assertEquals("ELD-002", saved.getElderNo());
        assertEquals("李淑芬", saved.getName());
        assertEquals(1L, saved.getOrgId());
        assertEquals("PENDING", saved.getStatus(), "新建老人档案初始状态应为 PENDING");

        assertNotNull(saved.getIdNoCiphertext(), "证件号密文不能为空");
        assertNotEquals("110101194805121234", saved.getIdNoCiphertext(), "证件号绝不能明文存库");
        assertNotNull(saved.getIdNoHash(), "证件号哈希索引不能为空");
        assertEquals(64, saved.getIdNoHash().length(), "SHA-256 哈希应为 64 位字符");
    }

    @Test
    @DisplayName("机构A用户修改机构B的老人档案时必须抛出 ORG_ACCESS_DENIED 异常被拦截")
    void shouldBlockCrossOrgAccessWhenUpdatingElder() {
        OrgContextHolder.setOrgId(1L);

        Elder org2Elder = Elder.builder()
                .id(88L)
                .orgId(2L)
                .elderNo("ELD-ORG2-001")
                .name("王大爷")
                .build();

        when(elderMapper.selectById(88L)).thenReturn(org2Elder);

        ElderUpdateDTO updateDTO = ElderUpdateDTO.builder()
                .name("非法修改")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            elderService.updateElder(88L, updateDTO);
        });

        assertEquals(ErrorCode.ORG_ACCESS_DENIED.getCode(), ex.getCode());
        verify(elderMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("归档操作只变更状态为 ARCHIVED，不执行物理删除或清除历史")
    void shouldArchiveElderWithoutPhysicalDelete() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(10L);

        Elder elder = Elder.builder()
                .id(55L)
                .orgId(1L)
                .elderNo("ELD-005")
                .name("赵长寿")
                .status("IN_HOUSE")
                .build();

        when(elderMapper.selectById(55L)).thenReturn(elder);

        elderService.archiveElder(55L);

        assertEquals("ARCHIVED", elder.getStatus());
        verify(elderMapper).updateById(elder);
        verify(elderMapper, never()).deleteById(anyLong());
    }
}
