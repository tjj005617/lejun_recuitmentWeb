package com.interview.util;

import com.interview.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    // ==================== 默认桶方法（简历等原有功能） ====================

    /**
     * 上传文件到默认桶
     */
    public String upload(MultipartFile file) {
        return upload(file, minioConfig.getBucket());
    }

    /**
     * 获取文件预签名URL（有效期7天，默认桶）
     */
    public String getFileUrl(String objectName) {
        return getFileUrl(objectName, minioConfig.getBucket());
    }

    /**
     * 删除文件（默认桶）
     */
    public void deleteFile(String objectName) {
        deleteFile(objectName, minioConfig.getBucket());
    }

    // ==================== 指定桶方法（知识图谱等新功能） ====================

    /**
     * 上传文件到指定桶
     */
    public String upload(MultipartFile file, String bucket) {
        try {
            // 确保bucket存在
            ensureBucket(bucket);

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String objectName = UUID.randomUUID() + ext;

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("文件上传成功: bucket={}, object={}", bucket, objectName);
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件预签名URL（指定桶，有效期7天）
     */
    public String getFileUrl(String objectName, String bucket) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("获取文件URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件（指定桶）
     */
    public void deleteFile(String objectName, String bucket) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("文件删除成功: bucket={}, object={}", bucket, objectName);
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取文件内容为字符串（用于读取 MD 文档等文本文件）
     */
    public String getTextContent(String objectName, String bucket) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取文件内容失败: " + e.getMessage(), e);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 确保指定bucket存在，不存在则创建
     */
    private void ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );
                log.info("Bucket创建成功: {}", bucket);
            }
        } catch (Exception e) {
            throw new RuntimeException("检查Bucket失败: " + e.getMessage(), e);
        }
    }
}
