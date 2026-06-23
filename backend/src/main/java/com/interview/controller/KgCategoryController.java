package com.interview.controller;

import com.interview.common.Result;
import com.interview.service.KgCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识图谱分类控制器（公开接口）
 */
@RestController
@RequestMapping("/api/kg/category")
@RequiredArgsConstructor
public class KgCategoryController {

    private final KgCategoryService categoryService;

    /**
     * 获取所有启用的分类
     */
    @GetMapping("/list")
    public Result<?> listEnabled() {
        return Result.ok(categoryService.listEnabled());
    }

    /**
     * 获取所有分类（含禁用，管理员用）
     */
    @GetMapping("/list/all")
    public Result<?> listAll() {
        return Result.ok(categoryService.listAll());
    }
}
