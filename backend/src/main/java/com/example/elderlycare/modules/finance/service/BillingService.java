package com.example.elderlycare.modules.finance.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.finance.dto.BillingCreateDTO;
import com.example.elderlycare.modules.finance.dto.BillingQueryDTO;
import com.example.elderlycare.modules.finance.vo.BillingVO;

public interface BillingService {

    /**
     * 生成月度账单（计算总额并保存收费项目与价格快照）
     */
    Long createBilling(BillingCreateDTO dto);

    /**
     * 查询账单详情（包含明细快照、长者与床位、实收与待付差额）
     */
    BillingVO getBillingDetail(Long id);

    /**
     * 分页查询账单列表
     */
    PageResult<BillingVO> pageBillings(BillingQueryDTO query);

    /**
     * 作废账单（仅限未产生有效收款的账单）
     */
    void cancelBilling(Long id);

    /**
     * 重算并更新账单金额与状态（由收款与退款驱动）
     */
    void updateBillingPaidAmountAndStatus(Long billingId);
}
