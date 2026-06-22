package com.interview.controller;

import com.interview.common.Result;
import com.interview.domain.po.JobCategory;
import com.interview.service.JobCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 职位分类控制器
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class JobCategoryController {

    private final JobCategoryService jobCategoryService;

    /**
     * 获取分类树形结构（公开接口）
     */
    @GetMapping("/tree")
    public Result<?> getCategoryTree() {
        List<Map<String, Object>> tree = jobCategoryService.getCategoryTree();
        return Result.ok(tree);
    }

    /**
     * 获取分类扁平列表（公开接口）
     */
    @GetMapping("/list")
    public Result<?> getCategoryList() {
        List<JobCategory> list = jobCategoryService.getCategoryList();
        return Result.ok(list);
    }
}
