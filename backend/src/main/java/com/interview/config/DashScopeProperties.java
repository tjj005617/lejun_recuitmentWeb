package com.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云 DashScope Embedding API 配置
 * 用于文本向量化（text-embedding-v3 模型）
 */
@Data
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {

    /** DashScope API Key */
    private String apiKey;
    /** 模型名称，默认 text-embedding-v3 */
    private String model = "text-embedding-v3";
    /** 向量维度，默认 1024 */
    private int dimension = 1024;
}
