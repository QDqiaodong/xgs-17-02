package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgs.water.entity.WqSampleItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WqSampleItemMapper extends BaseMapper<WqSampleItem> {

    @Select("SELECT * FROM wq_sample_item WHERE batch_id = #{batchId} ORDER BY sort_order, id")
    List<WqSampleItem> selectByBatchId(@Param("batchId") Long batchId);
}
