package com.example.elderlycare.modules.risk.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.risk.dto.*;
import com.example.elderlycare.modules.risk.vo.RiskEventDetailVO;
import com.example.elderlycare.modules.risk.vo.RiskEventSummaryVO;
import com.example.elderlycare.modules.risk.vo.RiskEventVO;

public interface RiskEventService {

    /**
     * 上报/登记安全风险事件
     */
    Long reportEvent(RiskEventReportDTO dto);

    /**
     * 分页查询风险事件
     */
    PageResult<RiskEventVO> pageEvents(RiskEventQueryDTO query);

    /**
     * 获取事件统计看板数据
     */
    RiskEventSummaryVO getSummary();

    /**
     * 获取风险事件详情及流转轨迹时间线
     */
    RiskEventDetailVO getEventDetail(Long id);

    /**
     * 指派/转派处理负责人
     */
    void assignEvent(Long id, RiskEventAssignDTO dto);

    /**
     * 开始处置
     */
    void startProcess(Long id, RiskEventProcessDTO dto);

    /**
     * 处置进展更新
     */
    void updateProgress(Long id, RiskEventProcessDTO dto);

    /**
     * 提交复核申请
     */
    void submitReview(Long id, RiskEventSubmitReviewDTO dto);

    /**
     * 主管审核复核 (通过或驳回)
     */
    void reviewEvent(Long id, RiskEventReviewDTO dto);

    /**
     * 结案关闭事件
     */
    void closeEvent(Long id, RiskEventCloseDTO dto);
}
