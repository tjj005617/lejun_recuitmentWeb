package com.interview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.domain.po.KgCategory;
import com.interview.domain.po.KgDocument;
import com.interview.domain.po.KgEdge;
import com.interview.domain.po.KgVertex;
import com.interview.domain.vo.KgDocumentVO;
import com.interview.mapper.KgCategoryMapper;
import com.interview.mapper.KgDocumentMapper;
import com.interview.mapper.KgEdgeMapper;
import com.interview.mapper.KgVertexMapper;
import com.interview.service.KgDocumentService;
import com.interview.service.KgProcessingService;
import com.interview.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识图谱文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KgDocumentServiceImpl implements KgDocumentService {

    private final KgDocumentMapper documentMapper;
    private final KgCategoryMapper categoryMapper;
    private final KgVertexMapper vertexMapper;
    private final KgEdgeMapper edgeMapper;
    private final KgProcessingService processingService;
    private final MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KgDocument uploadDocument(MultipartFile file, String categoryName, String title) {
        // 读取文件内容
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }

        // 根据分类名称查找或新建分类
        Long categoryId = null;
        if (categoryName != null && !categoryName.isBlank()) {
            KgCategory matched = findOrCreateCategory(categoryName.trim());
            categoryId = matched.getId();
        }

        // 上传到 MinIO（有分类用分类桶，无分类用默认桶）
        String objectName;
        if (categoryId != null) {
            KgCategory category = categoryMapper.selectById(categoryId);
            objectName = minioUtil.upload(file, category.getBucket());
        } else {
            objectName = minioUtil.upload(file);
        }

        // 保存文档记录
        KgDocument doc = new KgDocument();
        doc.setUuid(UUID.randomUUID().toString());
        doc.setTitle(title != null && !title.isBlank() ? title : file.getOriginalFilename());
        doc.setFileName(file.getOriginalFilename());
        doc.setObjectName(objectName);
        doc.setCategoryId(categoryId);
        doc.setRawContent(content);
        doc.setParseStatus("pending");
        doc.setVertexCount(0);
        doc.setEdgeCount(0);
        documentMapper.insert(doc);

        log.info("文档上传成功: id={}, title={}, category={}", doc.getId(), doc.getTitle(),
                categoryName != null ? categoryName : "AI自动分类");

        // 在事务提交后触发异步处理流水线，确保异步线程能读到文档数据
        Long docId = doc.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processingService.processDocument(docId);
            }
        });

        return doc;
    }

    /**
     * 根据分类名称查找已有分类，不存在则新建
     */
    private KgCategory findOrCreateCategory(String name) {
        // 精确匹配已有分类
        KgCategory existing = categoryMapper.selectOne(
                new LambdaQueryWrapper<KgCategory>().eq(KgCategory::getName, name));
        if (existing != null) {
            return existing;
        }
        // 新建分类（使用默认桶名称）
        KgCategory newCat = new KgCategory();
        newCat.setName(name);
        newCat.setBucket("kg-" + name.toLowerCase().replaceAll("[^a-z0-9]", "-"));
        newCat.setIcon("📁");
        newCat.setSortOrder(99);
        newCat.setStatus("active");
        categoryMapper.insert(newCat);
        log.info("新建知识图谱分类: name={}, bucket={}", name, newCat.getBucket());
        return newCat;
    }

    @Override
    public List<KgDocumentVO> listByCategory(Long categoryId, int page, int size) {
        Page<KgDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KgDocument> wrapper = new LambdaQueryWrapper<KgDocument>()
                .eq(KgDocument::getDeleted, 0);
        if (categoryId != null) {
            wrapper.eq(KgDocument::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(KgDocument::getCreatedAt);

        Page<KgDocument> result = documentMapper.selectPage(pageParam, wrapper);

        // 批量查询分类名称
        List<Long> categoryIds = result.getRecords().stream()
                .map(KgDocument::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, KgCategory> finalCategoryMap;
        if (!categoryIds.isEmpty()) {
            finalCategoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(KgCategory::getId, c -> c));
        } else {
            finalCategoryMap = Map.of();
        }

        Map<Long, KgCategory> categoryMap = finalCategoryMap;
        return result.getRecords().stream().map(doc -> {
            KgDocumentVO vo = new KgDocumentVO();
            BeanUtil.copyProperties(doc, vo);
            KgCategory cat = categoryMap.get(doc.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public KgDocumentVO getDocument(Long id) {
        KgDocument doc = documentMapper.selectById(id);
        if (doc == null) return null;

        KgDocumentVO vo = new KgDocumentVO();
        BeanUtil.copyProperties(doc, vo);

        KgCategory cat = categoryMapper.selectById(doc.getCategoryId());
        if (cat != null) {
            vo.setCategoryName(cat.getName());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        KgDocument doc = documentMapper.selectById(id);
        if (doc == null) return;

        // 软删除文档
        documentMapper.deleteById(id);

        // 删除关联的顶点
        vertexMapper.delete(new LambdaQueryWrapper<KgVertex>()
                .eq(KgVertex::getDocumentId, id));

        // 删除关联的边（涉及这些顶点的边）
        // 先查出所有被删顶点的ID，再删边
        // 由于顶点已软删除，这里用文档ID关联删除
        edgeMapper.delete(new LambdaQueryWrapper<KgEdge>()
                .apply("from_id IN (SELECT id FROM kg_vertex WHERE document_id = {0})", id)
                .or()
                .apply("to_id IN (SELECT id FROM kg_vertex WHERE document_id = {0})", id));

        // 删除 MinIO 文件
        if (doc.getObjectName() != null) {
            KgCategory cat = categoryMapper.selectById(doc.getCategoryId());
            if (cat != null) {
                try {
                    minioUtil.deleteFile(doc.getObjectName(), cat.getBucket());
                } catch (Exception e) {
                    log.warn("删除MinIO文件失败: {}", e.getMessage());
                }
            }
        }

        log.info("文档删除完成: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryProcessing(Long id) {
        KgDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }
        if (!"failed".equals(doc.getParseStatus())) {
            throw new RuntimeException("只能重试处理失败的文档");
        }

        // 重置状态
        doc.setParseStatus("pending");
        doc.setErrorMessage(null);
        documentMapper.updateById(doc);

        // 在事务提交后触发异步处理
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processingService.processDocument(id);
            }
        });
    }
}
