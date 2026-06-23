package com.interview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    /**
     * AI 调用线程池
     * 10个线程并行生成10个问题
     */
    @Bean("aiExecutor")
    public ExecutorService aiExecutor() {
        return Executors.newFixedThreadPool(10);
    }

    /**
     * 知识图谱处理线程池
     * 3个线程用于文档解析、知识点提取、关系分析
     */
    @Bean("kgExecutor")
    public ExecutorService kgExecutor() {
        return Executors.newFixedThreadPool(3);
    }
}
