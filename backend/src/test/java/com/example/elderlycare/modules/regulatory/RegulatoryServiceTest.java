package com.example.elderlycare.modules.regulatory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.common.util.SecurityCryptoUtil;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.care.mapper.CareTaskMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.entity.ElderContact;
import com.example.elderlycare.modules.elder.mapper.ElderContactMapper;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.regulatory.adapter.impl.DefaultFileExportSubmissionAdapter;
import com.example.elderlycare.modules.regulatory.dto.FieldMappingDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchGenerateDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchSubmitDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryTemplateCreateDTO;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryBatch;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryTemplate;
import com.example.elderlycare.modules.regulatory.mapper.RegulatoryBatchMapper;
import com.example.elderlycare.modules.regulatory.mapper.RegulatoryTemplateMapper;
import com.example.elderlycare.modules.regulatory.service.impl.RegulatoryBatchServiceImpl;
import com.example.elderlycare.modules.regulatory.service.impl.RegulatoryTemplateServiceImpl;
import com.example.elderlycare.modules.regulatory.vo.BatchValidationResultVO;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryBatchVO;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryTemplateVO;
import com.example.elderlycare.modules.risk.mapper.RiskEventMapper;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegulatoryServiceTest {

    @Mock
    private RegulatoryTemplateMapper templateMapper;
    @Mock
    private RegulatoryBatchMapper batchMapper;
    @Mock
    private ElderMapper elderMapper;
    @Mock
    private ElderContactMapper elderContactMapper;
    @Mock
    private AdmissionMapper admissionMapper;
    @Mock
    private BedMapper bedMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private BuildingMapper buildingMapper;
    @Mock
    private CareTaskMapper careTaskMapper;
    @Mock
    private RiskEventMapper riskEventMapper;
    @Mock
    private SysUserMapper sysUserMapper;

    private SecurityCryptoUtil securityCryptoUtil;
    private ObjectMapper objectMapper;
    private DefaultFileExportSubmissionAdapter submissionAdapter;

    private RegulatoryTemplateServiceImpl templateService;
    private RegulatoryBatchServiceImpl batchService;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(1L);

        securityCryptoUtil = new SecurityCryptoUtil("ElderlyCareAesSecretKey32BytesLong!");
        objectMapper = new ObjectMapper();
        submissionAdapter = new DefaultFileExportSubmissionAdapter();

        templateService = new RegulatoryTemplateServiceImpl(templateMapper, objectMapper);
        batchService = new RegulatoryBatchServiceImpl(
                batchMapper,
                templateMapper,
                elderMapper,
                elderContactMapper,
                admissionMapper,
                bedMapper,
                roomMapper,
                buildingMapper,
                careTaskMapper,
                riskEventMapper,
                sysUserMapper,
                securityCryptoUtil,
                submissionAdapter,
                objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("测试创建监管报送模板及重复编码校验")
    void testCreateTemplate() {
        when(templateMapper.selectCount(any())).thenReturn(0L);

        FieldMappingDTO mapping = FieldMappingDTO.builder()
                .targetAuthority("民政局养老处")
                .businessDomain("ELDER")
                .fileFormat("CSV")
                .fields(Arrays.asList(
                        FieldMappingDTO.FieldItem.builder()
                                .sourceField("name")
                                .targetField("长者姓名")
                                .required(true)
                                .build(),
                        FieldMappingDTO.FieldItem.builder()
                                .sourceField("idCard")
                                .targetField("公民身份号码")
                                .required(true)
                                .validationRule("ID_CARD")
                                .build()
                ))
                .build();

        RegulatoryTemplateCreateDTO createDTO = RegulatoryTemplateCreateDTO.builder()
                .templateCode("CIVIL_ELDER_TEST")
                .templateName("民政在住长者备案测试模板")
                .versionNo("v1.0")
                .fieldMapping(mapping)
                .status("ACTIVE")
                .build();

        doAnswer(invocation -> {
            RegulatoryTemplate entity = invocation.getArgument(0);
            entity.setId(10L);
            return 1;
        }).when(templateMapper).insert(any(RegulatoryTemplate.class));

        Long templateId = templateService.createTemplate(createDTO);
        assertNotNull(templateId);
        assertEquals(10L, templateId);

        // 重复创建应抛出异常
        when(templateMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> templateService.createTemplate(createDTO));
    }

    @Test
    @DisplayName("测试报送批次抽取长者数据、AES解密身份证并严格执行合规前置校验")
    void testBatchGenerateAndValidation() throws Exception {
        // 构造有效模板
        FieldMappingDTO mapping = FieldMappingDTO.builder()
                .targetAuthority("民政局养老处")
                .businessDomain("ELDER")
                .fileFormat("CSV")
                .fields(Arrays.asList(
                        FieldMappingDTO.FieldItem.builder()
                                .sourceField("name")
                                .targetField("长者姓名")
                                .required(true)
                                .sortOrder(1)
                                .build(),
                        FieldMappingDTO.FieldItem.builder()
                                .sourceField("idCard")
                                .targetField("公民身份号码")
                                .required(true)
                                .validationRule("ID_CARD")
                                .sortOrder(2)
                                .build(),
                        FieldMappingDTO.FieldItem.builder()
                                .sourceField("gender")
                                .targetField("性别")
                                .required(true)
                                .transformRule("MALE:1-男,FEMALE:2-女")
                                .sortOrder(3)
                                .build(),
                        FieldMappingDTO.FieldItem.builder()
                                .sourceField("phone")
                                .targetField("联系电话")
                                .required(false)
                                .validationRule("PHONE")
                                .sortOrder(4)
                                .build(),
                        FieldMappingDTO.FieldItem.builder()
                                .sourceField("emergencyContact")
                                .targetField("紧急联系人")
                                .required(true)
                                .sortOrder(5)
                                .build(),
                        FieldMappingDTO.FieldItem.builder()
                                .sourceField("emergencyPhone")
                                .targetField("紧急联系人电话")
                                .required(true)
                                .validationRule("PHONE")
                                .sortOrder(6)
                                .build()
                ))
                .build();

        String mappingJson = objectMapper.writeValueAsString(mapping);
        RegulatoryTemplate template = RegulatoryTemplate.builder()
                .id(1L)
                .orgId(1L)
                .templateCode("CIVIL_ELDER_RECORD")
                .templateName("民政在住长者备案台账")
                .versionNo("v1.0")
                .fieldMappingJson(mappingJson)
                .status("ACTIVE")
                .build();
        when(templateMapper.selectById(1L)).thenReturn(template);

        // 模拟 2 名老人数据：
        // 老人1：全部数据合规
        // 老人2：身份证号少于18位 (格式错误)，且缺少紧急联系人电话 (必填缺失)
        String validIdCard = "110101194805121234";
        String validCipher = securityCryptoUtil.encrypt(validIdCard);

        Elder elder1 = Elder.builder()
                .id(1L)
                .orgId(1L)
                .name("张建国")
                .gender("MALE")
                .birthDate(LocalDate.of(1948, 5, 12))
                .phone("13811110001")
                .idNoCiphertext(validCipher)
                .deleted(0)
                .build();

        String invalidIdCard = "11010119500307"; // 14位错误
        String invalidCipher = securityCryptoUtil.encrypt(invalidIdCard);

        Elder elder2 = Elder.builder()
                .id(2L)
                .orgId(1L)
                .name("李淑芬")
                .gender("FEMALE")
                .birthDate(LocalDate.of(1952, 8, 20))
                .phone("13811110002")
                .idNoCiphertext(invalidCipher)
                .deleted(0)
                .build();

        when(elderMapper.selectList(any())).thenReturn(Arrays.asList(elder1, elder2));

        // 模拟紧急联系人
        ElderContact contact1 = ElderContact.builder()
                .elderId(1L)
                .contactName("张志强")
                .phone("13900001111")
                .isEmergency(1)
                .build();
        when(elderContactMapper.selectOne(argThat(w -> w != null))).thenReturn(contact1, (ElderContact) null);

        doAnswer(invocation -> {
            RegulatoryBatch b = invocation.getArgument(0);
            b.setId(88L);
            return 1;
        }).when(batchMapper).insert(any(RegulatoryBatch.class));

        RegulatoryBatchGenerateDTO generateDTO = RegulatoryBatchGenerateDTO.builder()
                .templateId(1L)
                .periodStart(LocalDate.of(2026, 10, 1))
                .periodEnd(LocalDate.of(2026, 10, 31))
                .build();

        Long batchId = batchService.generateBatch(generateDTO);
        assertNotNull(batchId);
        assertEquals(88L, batchId);

        // 验证批次入库参数
        verify(batchMapper).insert(argThat(batch -> {
            assertEquals("VALIDATED", batch.getStatus());
            assertNotNull(batch.getValidationResultJson());
            assertTrue(batch.getValidationResultJson().contains("CONTAINS_ERRORS"));
            assertTrue(batch.getValidationResultJson().contains("MISSING_REQUIRED"));
            assertTrue(batch.getValidationResultJson().contains("FORMAT_INVALID"));
            return true;
        }));
    }

    @Test
    @DisplayName("测试通过预留适配器正式提交报送批次")
    void testSubmitBatchThroughAdapter() {
        RegulatoryBatch batch = RegulatoryBatch.builder()
                .id(1L)
                .orgId(1L)
                .batchNo("REG20261101-001")
                .templateId(1L)
                .status("VALIDATED")
                .fileUrl("/api/v1/regulatory/batches/REG20261101-001/download")
                .build();

        when(batchMapper.selectByIdForUpdate(1L)).thenReturn(batch);

        RegulatoryBatchSubmitDTO submitDTO = RegulatoryBatchSubmitDTO.builder()
                .channel("DEFAULT_FILE_EXPORT")
                .externalReceiptNo("GOV-TEST-8899")
                .submissionNotes("民政平台常规月度备案提交")
                .build();

        batchService.submitBatch(1L, submitDTO);

        assertEquals("SUBMITTED", batch.getStatus());
        assertNotNull(batch.getSubmittedAt());
        assertTrue(batch.getResponseDesc().contains("GOV-TEST-8899"));
        assertTrue(batch.getResponseDesc().contains("民政平台常规月度备案提交"));

        verify(batchMapper).updateById(batch);

        // 已提交批次重复提交应抛出异常
        assertThrows(BusinessException.class, () -> batchService.submitBatch(1L, submitDTO));
    }

    @Test
    @DisplayName("测试跨机构非法访问拦截 (多租户隔离验证)")
    void testCrossOrgAccessDenied() {
        // 当前机构为 1，模板属于机构 2
        RegulatoryTemplate templateOrg2 = RegulatoryTemplate.builder()
                .id(2L)
                .orgId(2L)
                .templateCode("CIVIL_ORG2")
                .build();

        when(templateMapper.selectById(2L)).thenReturn(templateOrg2);

        // 获取详情应拒绝
        assertThrows(BusinessException.class, () -> templateService.getTemplateDetail(2L));

        // 生成批次应拒绝
        RegulatoryBatchGenerateDTO generateDTO = RegulatoryBatchGenerateDTO.builder()
                .templateId(2L)
                .build();
        assertThrows(BusinessException.class, () -> batchService.generateBatch(generateDTO));

        // 批次属于机构 2
        RegulatoryBatch batchOrg2 = RegulatoryBatch.builder()
                .id(99L)
                .orgId(2L)
                .batchNo("REG_ORG2")
                .build();
        when(batchMapper.selectById(99L)).thenReturn(batchOrg2);

        assertThrows(BusinessException.class, () -> batchService.getBatchDetail(99L));
        assertThrows(BusinessException.class, () -> batchService.getValidationResult(99L));
        assertThrows(BusinessException.class, () -> batchService.downloadBatchFile(99L));
    }
}
