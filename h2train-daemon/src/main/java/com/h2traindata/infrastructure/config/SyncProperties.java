package com.h2traindata.infrastructure.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.sync")
public class SyncProperties {

    @Min(1)
    private long pollIntervalMs = 60_000L;

    @Min(1)
    private int connectionParallelism = 4;

    @Min(1)
    private int activityParallelism = 2;

    @Min(1)
    private int metricsParallelism = 2;

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getConnectionParallelism() {
        return connectionParallelism;
    }

    public void setConnectionParallelism(int connectionParallelism) {
        this.connectionParallelism = connectionParallelism;
    }

    public int getActivityParallelism() {
        return activityParallelism;
    }

    public void setActivityParallelism(int activityParallelism) {
        this.activityParallelism = activityParallelism;
    }

    public int getMetricsParallelism() {
        return metricsParallelism;
    }

    public void setMetricsParallelism(int metricsParallelism) {
        this.metricsParallelism = metricsParallelism;
    }
}
