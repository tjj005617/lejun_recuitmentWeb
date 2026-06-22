package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 省市区表
 */
@Data
@TableName("region")
public class Region {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名称 */
    private String name;

    /** 父级ID（0=省级） */
    private Long parentId;

    /** 层级：1=省 2=市 3=区 */
    private Integer level;

    /** 行政区划代码 */
    private String areaCode;
}
