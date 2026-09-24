package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgs.water.entity.WqRetest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WqRetestMapper extends BaseMapper<WqRetest> {

    @Select("SELECT * FROM wq_retest WHERE fail_item_id IN " +
            "<foreach collection='failItemIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY id")
    List<WqRetest> selectByFailItemIds(@Param("failItemIds") List<Long> failItemIds);

    @Select("SELECT * FROM wq_retest WHERE batch_id = #{batchId} ORDER BY id")
    List<WqRetest> selectByBatchId(@Param("batchId") Long batchId);
}
