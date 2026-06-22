package com.interview.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * 职位搜索文档（ES job_index）
 */
@Data
@Document(indexName = "job_index")
public class JobDocument {

    /** 职位ID（主键） */
    @Id
    private Long id;

    /** HR用户ID（null表示无人认领，搜索时过滤掉） */
    @Field(type = FieldType.Long)
    private Long userId;

    /** 公司ID */
    private Long companyId;

    /** 公司名称（冗余，搜索展示用） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String companyName;

    /** 职位分类ID */
    private Long categoryId;

    /** 分类名称（冗余） */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /** 职位名称 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /** 城市（精确匹配） */
    @Field(type = FieldType.Keyword)
    private String city;

    /** 工作地址 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String address;

    /** 薪资下限（元/月） */
    @Field(type = FieldType.Integer)
    private Integer salaryMin;

    /** 薪资上限（元/月） */
    @Field(type = FieldType.Integer)
    private Integer salaryMax;

    /** 工作类型：full_time/part_time/intern */
    @Field(type = FieldType.Keyword)
    private String jobType;

    /** 经验要求 */
    @Field(type = FieldType.Keyword)
    private String experience;

    /** 学历要求 */
    @Field(type = FieldType.Keyword)
    private String education;

    /** 职位描述 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /** 任职要求 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String requirements;

    /** 职位福利标签名称列表 */
    @Field(type = FieldType.Keyword)
    private List<String> benefits;

    /** 状态：active/paused/closed */
    @Field(type = FieldType.Keyword)
    private String status;

    /** 浏览次数 */
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    /** 申请次数 */
    @Field(type = FieldType.Integer)
    private Integer applyCount;

    /** 发布时间 */
    @Field(type = FieldType.Date, format = DateFormat.date, pattern = "uuuu-MM-dd")
    private String publishedAt;
}
