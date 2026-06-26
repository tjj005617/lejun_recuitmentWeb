package com.interview.service;

import com.interview.domain.po.KgDocument;
import com.interview.domain.vo.KgDocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱文档服务接口
 */
public interface KgDocumentService {

    /**
     * 上传文档并触发异步处理
     *
     * @param file         上传的文件
     * @param categoryName 可选分类名称（如 "Java"），为空时 AI 自动分类
     * @param title        可选文档标题
     */
    KgDocument uploadDocument(MultipartFile file, String categoryName, String title);

    /**
     * 批量上传文档并触发异步处理
     *
     * @param files        上传的文件数组
     * @param categoryName 可选分类名称，为空时 AI 自动分类
     */
    List<KgDocument> batchUploadDocuments(MultipartFile[] files, String categoryName);

    /**
     * 根据分类分页查询文档列表
     * 返回 Map: { records: List<KgDocumentVO>, total: long }
     */
    Map<String, Object> listByCategory(Long categoryId, int page, int size);

    /**
     * 获取文档详情
     */
    KgDocumentVO getDocument(Long id);

    /**
     * 删除文档（级联删除顶点和边）
     */
    void deleteDocument(Long id);

    /**
     * 重试处理失败的文档
     */
    void retryProcessing(Long id);
}
