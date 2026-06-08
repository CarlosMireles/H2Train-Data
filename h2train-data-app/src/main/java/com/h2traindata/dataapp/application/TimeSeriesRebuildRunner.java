package com.h2traindata.dataapp.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.timeseries", name = "rebuild-on-startup", havingValue = "true")
public class TimeSeriesRebuildRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TimeSeriesRebuildRunner.class);

    private final TimeSeriesRebuilder rebuilder;

    public TimeSeriesRebuildRunner(TimeSeriesRebuilder rebuilder) {
        this.rebuilder = rebuilder;
    }

    @Override
    public void run(ApplicationArguments args) {
        int eventCount = rebuilder.rebuild();
        log.info("Rebuilt longitudinal time-series datamart from datalake events count={}", eventCount);
    }
}
