package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xgs.water.entity.WaterDispenser;
import com.xgs.water.vo.WaterDispenserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WaterDispenserMapper extends BaseMapper<WaterDispenser> {

    @Select("<script>" +
            "SELECT wd.*, " +
            "CASE wd.status WHEN 1 THEN '正常' WHEN 0 THEN '停用' ELSE '未知' END as statusName " +
            "FROM water_dispenser wd " +
            "<where> " +
            "<if test='groupId != null'>AND wd.group_id = #{groupId}</if> " +
            "<if test='groupIds != null and groupIds.size() > 0'>AND wd.group_id IN " +
            "<foreach collection='groupIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</if> " +
            "<if test='keyword != null and keyword != \"\"'>AND (wd.device_no LIKE CONCAT('%',#{keyword},'%') OR wd.model LIKE CONCAT('%',#{keyword},'%'))</if> " +
            "<if test='status != null'>AND wd.status = #{status}</if> " +
            "</where> " +
            "ORDER BY wd.create_time DESC" +
            "</script>")
    IPage<WaterDispenserVO> selectPageList(Page<WaterDispenserVO> page,
                                           @Param("groupId") Long groupId,
                                           @Param("groupIds") List<Long> groupIds,
                                           @Param("keyword") String keyword,
                                           @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM water_dispenser WHERE group_id = #{groupId} AND status = 1")
    Integer countByGroupId(@Param("groupId") Long groupId);
}
