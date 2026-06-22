package com.interview.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * 公司搜索文档（ES company_index）
 */
@Data
@Document(indexName = "company_index")
public class CompanyDocument {

    /** 公司ID（主键） */
    @Id
    private Long id;

    /** 公司名称 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /** 所属行业 */
    @Field(type = FieldType.Keyword)
    private String industry;

    /** 公司规模 */
    @Field(type = FieldType.Keyword)
    private String scale;

    /** 公司类型 */
    @Field(type = FieldType.Keyword)
    private String type;

    /** 所在城市 */
    @Field(type = FieldType.Keyword)
    private String city;

    /** 详细地址 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String address;

    /** 公司介绍 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /** 福利待遇标签名称列表 */
    @Field(type = FieldType.Keyword)
    private List<String> benefits;

    /** 浏览量 */
    @Field(type = FieldType.Integer)
    private Long viewCount;

    /** 关注数 */
    @Field(type = FieldType.Integer)
    private Long followCount;

    /** 在招职位数（冗余，定时更新） */
    @Field(type = FieldType.Integer)
    private Integer jobCount;
}
