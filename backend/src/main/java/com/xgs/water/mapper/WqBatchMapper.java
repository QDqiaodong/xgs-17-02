package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xgs.water.entity.WqBatch;
import com.xgs.water.vo.WqBatchListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WqBatchMapper extends BaseMapper<WqBatch> {

    /** 悲观行锁：两名复核人/复检人并发处理时，事务串行化，只允许一人成功 */
    @Select("SELECT * FROM wq_batch WHERE id = #{id} FOR UPDATE")
    WqBatch selectByIdForUpdate(@Param("id") Long id);

    @Select("<script>" +
            "SELECT b.*, " +
            " (SELECT COUNT(1) FROM wq_sample_item i WHERE i.batch_id = b.id) AS deviceCount, " +
            " (SELECT COUNT(1) FROM wq_sample_item i WHERE i.batch_id = b.id AND i.item_result = 'FAIL') AS failCount, " +
            " (SELECT COUNT(1) FROM wq_fail_item f WHERE f.batch_id = b.id AND f.status = 'OPEN') AS openFailCount " +
            "FROM wq_batch b " +
            "<where> " +
            "<if test='groupIds != null and groupIds.size() > 0'>AND b.scope_group_id IN " +
            "<foreach collection='groupIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if> " +
            "<if test='status != null and status != \"\"'>AND b.status = #{status}</if> " +
            "<if test='startDate != null and startDate != \"\"'>AND COALESCE(b.submit_time, b.create_time) &gt;= #{startDate}</if> " +
            "<if test='endDate != null and endDate != \"\"'>AND COALESCE(b.submit_time, b.create_time) &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if> " +
            "<if test='deviceNo != null and deviceNo != \"\"'>AND EXISTS (" +
            "  SELECT 1 FROM wq_sample_item i WHERE i.batch_id = b.id AND i.device_no_snapshot LIKE CONCAT('%',#{deviceNo},'%'))</if> " +
            "</where> " +
            "ORDER BY b.create_time DESC" +
            "</script>")
    IPage<WqBatchListVO> selectBatchPage(Page<WqBatchListVO> page,
                                         @Param("groupIds") List<Long> groupIds,
                                         @Param("status") String status,
                                         @Param("startDate") String startDate,
                                         @Param("endDate") String endDate,
                                         @Param("deviceNo") String deviceNo);

    /**
     * 乐观锁条件更新：version 变化说明已被他人处理，返回 0 行，调用方抛 409。
     * 与 selectByIdForUpdate 组成双保险（行锁防同时进入，乐观锁防陈旧写入）。
     */
    @Update("UPDATE wq_batch SET status = #{toStatus}, version = version + 1, update_time = NOW() " +
            "WHERE id = #{id} AND status = #{expectStatus} AND version = #{version}")
    int casStatus(@Param("id") Long id,
                  @Param("expectStatus") String expectStatus,
                  @Param("toStatus") String toStatus,
                  @Param("version") Integer version);
}
