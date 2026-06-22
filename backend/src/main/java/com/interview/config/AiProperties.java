package com.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MiMo 模型配置属性
 * 读取 application.yml 中 ai.* 前缀的配置项
 * 用于创建 OpenAI 兼容格式的 ChatClient
 */
@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String apiKey;
    private String apiUrl;
    private String model;
    private int timeout = 30;
}
