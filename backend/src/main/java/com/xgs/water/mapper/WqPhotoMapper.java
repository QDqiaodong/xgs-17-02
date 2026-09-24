package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgs.water.entity.WqPhoto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WqPhotoMapper extends BaseMapper<WqPhoto> {

    /**
     * 唯一键 uk_ref(ref_type, sample_item_id, retest_id, photo_url) 兜底，
     * 网络重试不会产生重复照片引用。
     */
    @Insert("INSERT IGNORE INTO wq_photo(ref_type, sample_item_id, retest_id, device_id, photo_url) " +
            "VALUES(#{refType}, #{sampleItemId}, #{retestId}, #{deviceId}, #{photoUrl})")
    int insertIgnore(WqPhoto photo);

    @Select("<script>" +
            "SELECT * FROM wq_photo WHERE " +
            "(ref_type = 'SAMPLE' AND sample_item_id IN " +
            "<foreach collection='sampleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>) " +
            "OR (ref_type = 'RETEST' AND retest_id IN " +
            "<foreach collection='retestIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>) " +
            "ORDER BY id</script>")
    List<WqPhoto> selectByRefIds(@Param("sampleIds") List<Long> sampleIds,
                                 @Param("retestIds") List<Long> retestIds);
}
