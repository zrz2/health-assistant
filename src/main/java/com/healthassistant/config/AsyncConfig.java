package com.healthassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean
    public ExecutorService chatExecutor() {
        return Executors.newFixedThreadPool(4);
    }

    @Bean
    public ExecutorService searchExecutor() {
        return Executors.newFixedThreadPool(2);
    }
}
