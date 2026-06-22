package com.interview.controller;

import com.interview.common.Result;
import com.interview.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 省市区控制器
 */
@RestController
@RequestMapping("/api/region")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    /**
     * 获取省市区树形结构（公开接口）
     */
    @GetMapping("/tree")
    public Result<?> getRegionTree() {
        List<Map<String, Object>> tree = regionService.getRegionTree();
        return Result.ok(tree);
    }

    /**
     * 根据父级ID获取下级区域（公开接口）
     */
    @GetMapping("/children/{parentId}")
    public Result<?> getChildren(@PathVariable Long parentId) {
        return Result.ok(regionService.getByParentId(parentId));
    }

    /**
     * 根据区级ID获取完整路径ID数组（公开接口）
     * 返回 [省ID, 市ID, 区ID]，用于前端级联选择器回显
     */
    @GetMapping("/path/{regionId}")
    public Result<?> getPathIds(@PathVariable Long regionId) {
        return Result.ok(regionService.getPathIds(regionId));
    }
}
