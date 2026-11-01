package com.example.elderlycare.modules.report.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.report.dto.ExportTaskCreateDTO;
import com.example.elderlycare.modules.report.dto.ExportTaskQueryDTO;
import com.example.elderlycare.modules.report.dto.ReportQueryDTO;
import com.example.elderlycare.modules.report.vo.BusinessReportResultVO;
import com.example.elderlycare.modules.report.vo.ReportExportTaskVO;
import org.springframework.core.io.Resource;

public interface ReportService {

    /**
     * 查询多业务领域综合报表数据 (支持入住退住、床位、工单、护理、健康、账单、设备、风险等)
     */
    BusinessReportResultVO queryBusinessReport(ReportQueryDTO query);

    /**
     * 创建异步报表导出任务 (后台分批流式导出，防止大量数据加载造成 JVM 内存峰值)
     */
    Long createExportTask(ExportTaskCreateDTO dto);

    /**
     * 分页查询导出任务状态及下载清单
     */
    PageResult<ReportExportTaskVO> pageExportTasks(ExportTaskQueryDTO query);

    /**
     * 获取指定导出任务详情
     */
    ReportExportTaskVO getExportTaskDetail(Long id);

    /**
     * 安全下载导出文件 (严格校验机构隔离、发起人授权及文件过期时间)
     */
    Resource downloadExportFile(Long id);

    /**
     * 安全清理已过期的物理导出文件（可配置清理，不擅自删除有效文件）
     */
    int cleanExpiredExportFiles();
}
