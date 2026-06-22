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
}
