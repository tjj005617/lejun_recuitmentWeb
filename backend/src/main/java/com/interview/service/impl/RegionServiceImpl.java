package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.Region;
import com.interview.mapper.RegionMapper;
import com.interview.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 省市区服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;

    /**
     * 获取省市区树形结构（三级）
     */
    @Override
    public List<Map<String, Object>> getRegionTree() {
        // 查询所有区域
        List<Region> allRegions = regionMapper.selectList(
            new LambdaQueryWrapper<Region>()
                .orderByAsc(Region::getAreaCode)
        );

        // 按 parentId 分组
        Map<Long, List<Region>> groupByParent = allRegions.stream()
            .collect(Collectors.groupingBy(r -> r.getParentId() != null ? r.getParentId() : 0L));

        // 构建省级列表，每省带市级，每市带区级
        List<Region> provinces = groupByParent.getOrDefault(0L, Collections.emptyList());
        List<Map<String, Object>> tree = new ArrayList<>();

        for (Region province : provinces) {
            Map<String, Object> provinceNode = new LinkedHashMap<>();
            provinceNode.put("value", province.getId());
            provinceNode.put("label", province.getName());

            // 查找该省下的市
            List<Region> cities = groupByParent.getOrDefault(province.getId(), Collections.emptyList());
            List<Map<String, Object>> cityList = new ArrayList<>();

            for (Region city : cities) {
                Map<String, Object> cityNode = new LinkedHashMap<>();
                cityNode.put("value", city.getId());
                cityNode.put("label", city.getName());

                // 查找该市下的区
                List<Region> districts = groupByParent.getOrDefault(city.getId(), Collections.emptyList());
                List<Map<String, Object>> districtList = new ArrayList<>();

                for (Region district : districts) {
                    Map<String, Object> districtNode = new LinkedHashMap<>();
                    districtNode.put("value", district.getId());
                    districtNode.put("label", district.getName());
                    districtList.add(districtNode);
                }

                if (!districtList.isEmpty()) {
                    cityNode.put("children", districtList);
                }
                cityList.add(cityNode);
            }

            if (!cityList.isEmpty()) {
                provinceNode.put("children", cityList);
            }
            tree.add(provinceNode);
        }

        return tree;
    }

    /**
     * 根据父级ID获取下级区域
     */
    @Override
    public List<Region> getByParentId(Long parentId) {
        return regionMapper.selectList(
            new LambdaQueryWrapper<Region>()
                .eq(Region::getParentId, parentId)
                .orderByAsc(Region::getAreaCode)
        );
    }

    /**
     * 根据区级ID获取完整省市区路径（如：广东省 深圳市 南山区）
     */
    @Override
    public String getFullPath(Long regionId) {
        if (regionId == null) {
            return null;
        }
        // 依次向上查询：区 → 市 → 省
        List<String> names = new ArrayList<>();
        Long currentId = regionId;
        for (int i = 0; i < 3; i++) {
            Region region = regionMapper.selectById(currentId);
            if (region == null) break;
            names.add(0, region.getName());
            if (region.getParentId() == null || region.getParentId() == 0) break;
            currentId = region.getParentId();
        }
        return String.join(" ", names);
    }

    /**
     * 根据区级ID获取完整路径ID数组 [省ID, 市ID, 区ID]
     */
    @Override
    public List<Long> getPathIds(Long regionId) {
        if (regionId == null) {
            return Collections.emptyList();
        }
        // 依次向上查询：区 → 市 → 省
        List<Long> ids = new ArrayList<>();
        Long currentId = regionId;
        for (int i = 0; i < 3; i++) {
            Region region = regionMapper.selectById(currentId);
            if (region == null) break;
            ids.add(0, region.getId());
            if (region.getParentId() == null || region.getParentId() == 0) break;
            currentId = region.getParentId();
        }
        return ids;
    }
}
