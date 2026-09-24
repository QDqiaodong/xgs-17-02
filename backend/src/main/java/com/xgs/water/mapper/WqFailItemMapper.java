package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgs.water.entity.WqFailItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WqFailItemMapper extends BaseMapper<WqFailItem> {

    /** 复检前锁定不合格项行，防止两个复检请求同时读到 OPEN 各写一条复检记录 */
    @Select("SELECT * FROM wq_fail_item WHERE id = #{id} FOR UPDATE")
    WqFailItem selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM wq_fail_item WHERE batch_id = #{batchId} ORDER BY id")
    List<WqFailItem> selectByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT * FROM wq_fail_item WHERE sample_item_id = #{sampleItemId} ORDER BY id")
    List<WqFailItem> selectBySampleItemId(@Param("sampleItemId") Long sampleItemId);

    @Select("SELECT * FROM wq_fail_item WHERE device_id = #{deviceId} AND status = 'OPEN' ORDER BY id")
    List<WqFailItem> selectOpenByDeviceId(@Param("deviceId") Long deviceId);
}
