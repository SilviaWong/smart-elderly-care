package com.example.elderlycare.modules.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.finance.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {

    @Select("SELECT * FROM refund_record WHERE id = #{id} FOR UPDATE")
    RefundRecord selectByIdForUpdate(@Param("id") Long id);
}
