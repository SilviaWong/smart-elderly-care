package com.example.elderlycare.modules.report.service;

import com.example.elderlycare.modules.report.vo.DashboardStatsVO;

public interface DashboardService {

    /**
     * 获取机构全景运营看板实时数据指标 (在住人数、床位占用率、工单、护理完成率、健康预警、当月收入、7日流转趋势等)
     */
    DashboardStatsVO getDashboardStats();
}
