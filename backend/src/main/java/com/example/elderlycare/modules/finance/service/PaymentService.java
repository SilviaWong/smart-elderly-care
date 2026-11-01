package com.example.elderlycare.modules.finance.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.finance.dto.PaymentCreateDTO;
import com.example.elderlycare.modules.finance.dto.PaymentQueryDTO;
import com.example.elderlycare.modules.finance.vo.PaymentRecordVO;

import java.math.BigDecimal;

public interface PaymentService {

    /**
     * 登记收款/模拟支付（严格保证支付幂等性与事务安全）
     */
    Long registerPayment(PaymentCreateDTO dto);

    /**
     * 获取指定收款记录当前剩余可退款金额
     */
    BigDecimal getRefundableAmount(Long paymentId);

    /**
     * 查询收款详情
     */
    PaymentRecordVO getPaymentDetail(Long id);

    /**
     * 分页查询收款记录
     */
    PageResult<PaymentRecordVO> pagePayments(PaymentQueryDTO query);
}
