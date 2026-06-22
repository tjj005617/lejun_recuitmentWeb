package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 简历表
 * 存储用户上传的简历信息，文件保存在 MinIO，解析后的纯文本保存在 rawContent
 * AI 面试时根据 rawContent 生成针对性的面试问题
 */
@Data
@TableName("resume")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String objectName;

    private String rawContent;

    private String name;

    private String education;

    private String skills;

    private String workExperience;

    private String projectExperience;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    private Integer deleted;
}
