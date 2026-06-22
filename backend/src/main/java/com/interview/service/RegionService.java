package com.interview.service;

import com.interview.domain.po.Region;

import java.util.List;
import java.util.Map;

/**
 * 省市区服务接口
 */
public interface RegionService {

    /**
     * 获取省市区树形结构
     */
    List<Map<String, Object>> getRegionTree();

    /**
     * 根据父级ID获取下级区域
     */
    List<Region> getByParentId(Long parentId);

    /**
     * 根据区级ID获取完整省市区路径（如：广东省 深圳市 南山区）
     * @param regionId 区级ID（level=3）
     * @return 完整路径字符串
     */
    String getFullPath(Long regionId);

    /**
     * 根据区级ID获取完整路径ID数组 [省ID, 市ID, 区ID]
     * @param regionId 区级ID（level=3）
     * @return ID数组
     */
    List<Long> getPathIds(Long regionId);
}
