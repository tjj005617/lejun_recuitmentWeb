package com.interview.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 * 使用 Milvus SDK 3.0.x 的 MilvusClientV2
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {

    /** Milvus 服务地址 */
    private String uri = "http://localhost:19530";
    /** 集合名称 */
    private String collection = "job_vectors";
    /** 向量维度 */
    private int dimension = 1024;

    private MilvusClientV2 client;

    @Bean
    public MilvusClientV2 milvusClientV2() {
        try {
            log.info("连接 Milvus: uri={}", uri);
            ConnectConfig connectConfig = ConnectConfig.builder()
                    .uri(uri)
                    .build();
            client = new MilvusClientV2(connectConfig);
            return client;
        } catch (Exception e) {
            log.warn("Milvus 连接失败，岗位匹配功能将不可用: {}", e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
            log.info("Milvus 连接已关闭");
        }
    }
}
