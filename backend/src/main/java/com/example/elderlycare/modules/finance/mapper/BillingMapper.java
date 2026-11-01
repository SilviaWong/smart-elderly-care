package com.example.elderlycare.modules.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.finance.entity.Billing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BillingMapper extends BaseMapper<Billing> {

    @Select("SELECT * FROM billing WHERE id = #{id} FOR UPDATE")
    Billing selectByIdForUpdate(@Param("id") Long id);
}
