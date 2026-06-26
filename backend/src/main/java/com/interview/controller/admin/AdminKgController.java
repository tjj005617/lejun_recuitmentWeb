package com.interview.controller.admin;

import com.interview.common.Result;
import com.interview.domain.po.KgCategory;
import com.interview.service.KgCategoryService;
import com.interview.service.KgDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理后台 - 知识图谱文档管理控制器
 */
@RestController
@RequestMapping("/api/admin/kg")
@RequiredArgsConstructor
public class AdminKgController {

    private final KgDocumentService documentService;
    private final KgCategoryService categoryService;

    /**
     * 上传文档并触发异步处理（可选指定分类名称，不填则 AI 自动分类）
     */
    @PostMapping("/upload")
    public Result<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String title) {
        try {
            var doc = documentService.uploadDocument(file, categoryName, title);
            return Result.ok(doc);
        } catch (Exception e) {
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文档（可选指定分类名称，不填则 AI 自动分类）
     */
    @PostMapping("/batch-upload")
    public Result<?> batchUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false) String categoryName) {
        try {
            var docs = documentService.batchUploadDocuments(files, categoryName);
            return Result.ok(docs);
        } catch (Exception e) {
            return Result.fail("批量上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有分类（包括禁用的）
     */
    @GetMapping("/categories")
    public Result<?> listCategories() {
        return Result.ok(categoryService.listAll());
    }

    /**
     * 重试处理失败的文档
     */
    @PostMapping("/retry/{documentId}")
    public Result<?> retryProcessing(@PathVariable Long documentId) {
        try {
            documentService.retryProcessing(documentId);
            return Result.ok("已重新提交处理");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 删除文档及其关联的图谱数据
     */
    @DeleteMapping("/document/{id}")
    public Result<?> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.ok("删除成功");
    }
}
