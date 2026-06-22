package com.interview.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 职位详情VO（包含公司信息）
 */
@Data
public class JobDetailVO {

    // ---- 职位字段 ----
    private Long id;
    private Long companyId;
    /** 负责该职位的HR用户ID */
    private Long userId;
    private String title;
    private String city;
    private String address;
    private Integer salaryMin;
    private Integer salaryMax;
    private String jobType;
    private String experience;
    private String education;
    private String description;
    private String requirements;
    /** 职位福利标签名称列表 */
    private List<String> benefits;
    private String status;
    private Integer viewCount;
    private Integer applyCount;
    private LocalDateTime publishedAt;

    // ---- 公司信息 ----
    private CompanyVO company;

    // ---- HR信息 ----
    private HrVO hr;

    /**
     * HR简要信息
     */
    @Data
    public static class HrVO {
        private Long id;
        private String nickname;
        private String username;
        private String avatar;
    }

    /**
     * 公司简要信息
     */
    @Data
    public static class CompanyVO {
        private Long id;
        private String name;
        private String logo;
        private String industry;
        private String scale;
        private String type;
        private String city;
        private String address;
        private String website;
        private String description;
        /** 公司福利标签名称列表 */
        private List<String> benefits;
    }
}
