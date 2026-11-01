package com.example.elderlycare.modules.admission.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.admission.dto.AdmissionCheckInDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionDischargeDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionQueryDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionTransferDTO;
import com.example.elderlycare.modules.admission.vo.AdmissionPrecheckVO;
import com.example.elderlycare.modules.admission.vo.AdmissionVO;
import com.example.elderlycare.modules.admission.vo.BedChangeLogVO;

import java.util.List;

public interface AdmissionService {

    PageResult<AdmissionVO> pageAdmissions(AdmissionQueryDTO query);

    AdmissionVO getAdmissionDetail(Long id);

    /**
     * 办理入住（事务 + SELECT ... FOR UPDATE 悲观锁保证并发占床安全）
     *
     * @param dto 入住参数
     * @return 入住记录ID
     */
    Long checkIn(AdmissionCheckInDTO dto);

    /**
     * 办理调床（事务 + 升序防死锁双床位排他锁）
     *
     * @param admissionId 入住记录ID
     * @param dto 调床参数
     */
    void transferBed(Long admissionId, AdmissionTransferDTO dto);

    /**
     * 办理退住（事务 + 床位排他锁释放与状态更新）
     *
     * @param admissionId 入住记录ID
     * @param dto 退住参数
     */
    void discharge(Long admissionId, AdmissionDischargeDTO dto);

    /**
     * 退住前置检查
     *
     * @param admissionId 入住记录ID
     * @return 前置检查结果
     */
    AdmissionPrecheckVO checkDischargePreconditions(Long admissionId);

    /**
     * 获取床位变动日志历史
     *
     * @param admissionId 入住记录ID
     * @return 变动历史列表
     */
    List<BedChangeLogVO> listChangeLogs(Long admissionId);
}
