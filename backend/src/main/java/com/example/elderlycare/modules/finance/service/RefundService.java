package com.example.elderlycare.modules.finance.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.finance.dto.RefundApplyDTO;
import com.example.elderlycare.modules.finance.dto.RefundAuditDTO;
import com.example.elderlycare.modules.finance.dto.RefundQueryDTO;
import com.example.elderlycare.modules.finance.vo.FinanceSummaryVO;
import com.example.elderlycare.modules.finance.vo.RefundRecordVO;

public interface RefundService {

    /**
     * 发起退款申请（严格校验原收款可退额度，不可超额退款）
     */
    Long applyRefund(RefundApplyDTO dto);

    /**
     * 审核退款（审批通过后执行退款并自动回退账单已收金额与账单状态）
     */
    void auditRefund(Long id, RefundAuditDTO dto);

    /**
     * 查询退款详情
     */
    RefundRecordVO getRefundDetail(Long id);

    /**
     * 分页查询退款记录
     */
    PageResult<RefundRecordVO> pageRefunds(RefundQueryDTO query);

    /**
     * 获取机构财务综合看板指标（应收、实收、欠费、退款、账单状态分布）
     */
    FinanceSummaryVO getFinanceSummary();
}
