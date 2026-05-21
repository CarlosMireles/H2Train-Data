package com.h2traindata.infrastructure.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SyncExecutionConfiguration {

    @Bean(name = "connectionSyncExecutor")
    public Executor connectionSyncExecutor(SyncProperties properties) {
        return executor("connection-sync-", properties.getConnectionParallelism());
    }

    @Bean(name = "activityCollectorExecutor")
    public Executor activityCollectorExecutor(SyncProperties properties) {
        return executor("activity-sync-", properties.getActivityParallelism());
    }

    @Bean(name = "metricsCollectorExecutor")
    public Executor metricsCollectorExecutor(SyncProperties properties) {
        return executor("metrics-sync-", properties.getMetricsParallelism());
    }

    private Executor executor(String threadNamePrefix, int parallelism) {
        int normalizedParallelism = Math.max(1, parallelism);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(normalizedParallelism);
        executor.setMaxPoolSize(normalizedParallelism);
        executor.setQueueCapacity(Math.max(64, normalizedParallelism * 16));
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
