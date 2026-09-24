package com.xgs.water.vo;

import java.util.List;

/**
 * 筛选面板可选项（型号、安装规格、出水类型）。
 * 区域树仍走 /building-group/tree；状态与待复检为固定枚举。
 */
public class DeviceFacetVO {

    private List<String> models;
    private List<String> specs;
    private List<String> waterTypes;
    /** db / cache，仅用于观测缓存降级 */
    private String source;

    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }

    public List<String> getSpecs() { return specs; }
    public void setSpecs(List<String> specs) { this.specs = specs; }

    public List<String> getWaterTypes() { return waterTypes; }
    public void setWaterTypes(List<String> waterTypes) { this.waterTypes = waterTypes; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
