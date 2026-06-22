package com.interview.service.impl;

import com.interview.domain.po.Resume;
import com.interview.mapper.ResumeMapper;
import com.interview.service.ResumeService;
import com.interview.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历服务实现类
 * 负责简历上传、解析和存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final MinioUtil minioUtil;
    private final Tika tika = new Tika();

    /**
     * 上传简历
     * 流程：上传文件到MinIO → 解析文件内容 → 保存到数据库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Resume uploadResume(MultipartFile file) {
        try {
            // 1. 上传文件到MinIO
            String objectName = minioUtil.upload(file);

            // 2. 用 Tika 解析文件，提取纯文本内容
            String content = tika.parseToString(file.getInputStream());

            // 3. 构建简历对象
            Resume resume = new Resume();
            resume.setFileName(file.getOriginalFilename());
            resume.setObjectName(objectName);
            resume.setRawContent(content);
            resume.setName(extractName(content));
            resume.setSkills(extractSkills(content));

            // 4. 保存到数据库
            resumeMapper.insert(resume);
            log.info("简历上传成功: id={}, objectName={}", resume.getId(), objectName);
            return resume;
        } catch (Exception e) {
            log.error("简历上传失败", e);
            throw new RuntimeException("简历上传失败", e);
        }
    }

    @Override
    public Resume getResumeById(Long id) {
        return resumeMapper.selectById(id);
    }

    /**
     * 获取简历文件的访问URL
     */
    public String getResumeFileUrl(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || resume.getObjectName() == null) {
            return null;
        }
        return minioUtil.getFileUrl(resume.getObjectName());
    }

    private String extractName(String content) {
        String[] lines = content.split("\n");
        return lines.length > 0 ? lines[0].trim() : "Unknown";
    }

    private String extractSkills(String content) {
        return "[]";
    }
}
