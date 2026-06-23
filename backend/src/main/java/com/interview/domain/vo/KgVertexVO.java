package com.interview.domain.vo;

import lombok.Data;

/**
 * 知识点顶点 VO
 */
@Data
public class KgVertexVO {

    private Long id;
    private String uuid;
    /** 知识点名称 */
    private String name;
    /** 类型：concept/technology/api/theory/example/keyword */
    private String vertexType;
    /** 子类型 */
    private String subType;
    /** 简要描述 */
    private String description;
    /** 来源文档标题 */
    private String documentTitle;
    /** 所属分类名称 */
    private String categoryName;
    /** 扩展属性JSON */
    private String properties;
    /** 关联边数量（用于计算节点大小） */
    private Integer edgeCount;
}
