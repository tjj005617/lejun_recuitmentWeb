package com.interview.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 简历详情VO
 */
@Data
public class ResumeDetailVO {

    private Long id;
    private String fileName;
    private String name;
    private String education;
    private String skills;
    private String workExperience;
    private String projectExperience;
    private String rawContent;
    private LocalDateTime createdAt;
    private String fileUrl;
}
