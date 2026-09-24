package com.xgs.water.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xgs.water.dto.DeviceSearchParams;
import com.xgs.water.entity.WaterDispenser;
import com.xgs.water.vo.DeviceSearchItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceSearchMapper extends BaseMapper<WaterDispenser> {

    /**
     * 全园设备键集检索：
     * - 区域路径固定三级 JOIN 一次性产出，不逐行查树；
     * - WHERE 全部走参数化，排序列来自白名单；
     * - 键集谓词 (主排序值, id) 保证主排序值相同也不重不漏。
     * 多取 1 条（limit = pageSize+1）由 Service 判断 hasMore。
     */
    List<DeviceSearchItem> search(@Param("p") DeviceSearchParams params);

    /** 型号可选项：精确去重 */
    List<String> distinctModels();

    /** 安装规格可选项：非空去重 */
    List<String> distinctSpecs();
}
