package com.example.elderlycare.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.report.entity.ReportExportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportExportTaskMapper extends BaseMapper<ReportExportTask> {

    @Select("SELECT * FROM report_export_task WHERE id = #{id} FOR UPDATE")
    ReportExportTask selectByIdForUpdate(@Param("id") Long id);
}
