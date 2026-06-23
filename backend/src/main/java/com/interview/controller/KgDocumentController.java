package com.interview.controller;

import com.interview.common.Result;
import com.interview.service.KgDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 知识图谱文档控制器（公开接口）
 */
@RestController
@RequestMapping("/api/kg/document")
@RequiredArgsConstructor
public class KgDocumentController {

    private final KgDocumentService documentService;

    /**
     * 按分类分页查询文档列表
     */
    @GetMapping("/list")
    public Result<?> listByCategory(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(documentService.listByCategory(categoryId, page, size));
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public Result<?> getDocument(@PathVariable Long id) {
        return Result.ok(documentService.getDocument(id));
    }
}
