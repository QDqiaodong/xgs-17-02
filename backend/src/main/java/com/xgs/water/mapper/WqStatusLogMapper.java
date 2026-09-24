package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgs.water.entity.WqStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WqStatusLogMapper extends BaseMapper<WqStatusLog> {

    @Select("SELECT * FROM wq_status_log WHERE batch_id = #{batchId} ORDER BY id")
    List<WqStatusLog> selectByBatchId(@Param("batchId") Long batchId);
}
