package com.h2traindata.infrastructure.datalake;

import com.h2traindata.infrastructure.config.DatalakeProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DatalakeStructureInitializer implements ApplicationRunner {

    private final DatalakeProperties datalakeProperties;

    public DatalakeStructureInitializer(DatalakeProperties datalakeProperties) {
        this.datalakeProperties = datalakeProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path root = datalakeProperties.rootDirectory();
        List<Path> directories = List.of(
                root,
                root.resolve("bronze"),
                root.resolve("bronze").resolve("zone=restricted"),
                root.resolve("silver"),
                root.resolve("silver").resolve("zone=curated"),
                root.resolve("silver").resolve("zone=curated").resolve("domain=activity_session"),
                root.resolve("silver").resolve("zone=curated").resolve("domain=activity_trackpoint"),
                root.resolve("silver").resolve("zone=curated").resolve("domain=activity_lap"),
                root.resolve("silver").resolve("zone=curated").resolve("domain=health_snapshot"),
                root.resolve("silver").resolve("zone=curated").resolve("domain=profile_snapshot"),
                root.resolve("gold"),
                root.resolve("gold").resolve("zone=analytics"),
                root.resolve("gold").resolve("zone=analytics").resolve("mart=subject_daily"),
                root.resolve("gold").resolve("zone=analytics").resolve("mart=cohort_daily"),
                root.resolve("gold").resolve("zone=analytics").resolve("mart=training_load"),
                root.resolve("gold").resolve("zone=analytics").resolve("mart=population_baselines"),
                root.resolve("meta"),
                root.resolve("meta").resolve("manifests"),
                root.resolve("meta").resolve("quality"),
                root.resolve("meta").resolve("dead_letter")
        );

        try {
            for (Path directory : directories) {
                Files.createDirectories(directory);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to initialize datalake directory structure", exception);
        }
    }
}
