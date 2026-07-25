package com.example.csvdemo.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "fixedPoolExecutor")
    public ExecutorService cscLoaderExecutor() {

        return Executors.newFixedThreadPool(4);
    }

    @Bean(name= "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

