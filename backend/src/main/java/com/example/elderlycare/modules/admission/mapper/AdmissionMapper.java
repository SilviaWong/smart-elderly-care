package com.example.elderlycare.modules.admission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.admission.entity.Admission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdmissionMapper extends BaseMapper<Admission> {
}
