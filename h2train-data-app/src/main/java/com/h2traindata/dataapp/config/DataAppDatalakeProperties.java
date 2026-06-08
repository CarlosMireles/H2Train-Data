package com.h2traindata.dataapp.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.datalake")
public class DataAppDatalakeProperties {

    private Path rootPath = Path.of("datalake");
    private Path longitudinalDatamartPath = Path.of("datamarts").resolve("longitudinal");

    public Path getRootPath() {
        return rootPath;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    public Path getLongitudinalDatamartPath() {
        return longitudinalDatamartPath;
    }

    public void setLongitudinalDatamartPath(Path longitudinalDatamartPath) {
        this.longitudinalDatamartPath = longitudinalDatamartPath;
    }

    public Path effectiveRootPath() {
        if (Files.exists(rootPath)) {
            return rootPath;
        }
        Path parentRelative = Path.of("..").resolve(rootPath).normalize();
        if (!rootPath.isAbsolute() && Files.exists(parentRelative)) {
            return parentRelative;
        }
        return rootPath;
    }

    public Path effectiveLongitudinalDatamartPath() {
        Path root = effectiveRootPath();
        if (longitudinalDatamartPath.isAbsolute()) {
            return longitudinalDatamartPath;
        }
        return root.resolve(longitudinalDatamartPath);
    }
}
