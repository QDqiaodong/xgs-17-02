package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgs.water.entity.BuildingGroupClosure;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BuildingGroupClosureMapper extends BaseMapper<BuildingGroupClosure> {

    /** 取某区域全部下级（含自身）：选中园区/楼栋时检索范围用 */
    @Select("SELECT descendant_id FROM building_group_closure WHERE ancestor_id = #{groupId}")
    List<Long> selectDescendantIds(@Param("groupId") Long groupId);

    /** 某区域的全部祖先（含自身），用于拼完整路径 */
    @Select("SELECT ancestor_id FROM building_group_closure WHERE descendant_id = #{groupId} AND ancestor_id > 0 ORDER BY distance DESC")
    List<Long> selectAncestorIds(@Param("groupId") Long groupId);

    /**
     * 新增节点后挂载闭包关系：
     * 新节点作为所有“父节点祖先”的后代，再加上自身关系。
     */
    @Insert("INSERT INTO building_group_closure (ancestor_id, descendant_id, distance) " +
            "SELECT ancestor_id, #{newNodeId}, distance + 1 FROM building_group_closure WHERE descendant_id = #{parentId} " +
            "UNION ALL SELECT #{newNodeId}, #{newNodeId}, 0")
    int insertForNewNode(@Param("newNodeId") Long newNodeId, @Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM building_group_closure")
    long countAll();

    /** 删除节点（仅叶子可删）：移除该节点参与的全部闭包关系 */
    @Delete("DELETE FROM building_group_closure WHERE descendant_id = #{groupId}")
    int deleteDescendantRefs(@Param("groupId") Long groupId);
}
