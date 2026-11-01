package com.example.elderlycare.modules.regulatory.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchGenerateDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchQueryDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchSubmitDTO;
import com.example.elderlycare.modules.regulatory.vo.BatchValidationResultVO;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryBatchVO;
import org.springframework.core.io.Resource;

public interface RegulatoryBatchService {

    /**
     * 生成监管报送批次 (抽取业务数据、执行前置校验并生成合规报送文件)
     */
    Long generateBatch(RegulatoryBatchGenerateDTO dto);

    /**
     * 分页查询监管报送批次列表
     */
    PageResult<RegulatoryBatchVO> pageBatches(RegulatoryBatchQueryDTO query);

    /**
     * 获取指定批次详情
     */
    RegulatoryBatchVO getBatchDetail(Long id);

    /**
     * 获取批次的数据校验与错误明细定位报告
     */
    BatchValidationResultVO getValidationResult(Long id);

    /**
     * 下载已生成的合规报送文件
     */
    Resource downloadBatchFile(Long id);

    /**
     * 登记并正式提交监管报送批次 (调用预留 Adapter)
     */
    void submitBatch(Long id, RegulatoryBatchSubmitDTO dto);
}
