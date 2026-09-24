package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgs.water.entity.WqIdempotentRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WqIdempotentRecordMapper extends BaseMapper<WqIdempotentRecord> {

    /**
     * 抢占幂等键：返回 1 表示本次请求获得执行权；
     * 返回 0（唯一键冲突）说明同一 requestId 已执行或正在执行，调用方按重复请求拒绝。
     */
    @Insert("INSERT IGNORE INTO wq_idempotent_record(request_id, action, batch_id) " +
            "VALUES(#{requestId}, #{action}, #{batchId})")
    int tryAcquire(WqIdempotentRecord record);
}
