package com.interview.service;

import com.interview.domain.po.Resume;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历服务接口
 * 定义简历的上传、查询和文件访问能力
 * 简历文件存储在 MinIO，解析后的内容保存到数据库
 */
public interface ResumeService {
    Resume uploadResume(MultipartFile file);
    Resume getResumeById(Long id);
    String getResumeFileUrl(Long id);
}
