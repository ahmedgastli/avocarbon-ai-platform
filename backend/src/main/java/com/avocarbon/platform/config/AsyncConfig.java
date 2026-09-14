package com.avocarbon.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for background generation tasks.
 *
 * Defines the "generatorExecutor" thread pool used exclusively by
 * {@link com.avocarbon.platform.module.generator.GenerationJobExecutor}.
 *
 * Pool is intentionally small (core=2, max=10) to avoid exhausting
 * system resources on concurrent generation requests.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "generatorExecutor")
    public Executor generatorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("generator-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
