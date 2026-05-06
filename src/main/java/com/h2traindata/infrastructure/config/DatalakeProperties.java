package com.h2traindata.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.datalake")
public class DatalakeProperties {

    @NotBlank
    private String rootPath = "datalake";

    @NotBlank
    private String subjectIdSalt = "local-dev-subject-salt";

    @NotBlank
    private String defaultConsentVersion = "v1";

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getSubjectIdSalt() {
        return subjectIdSalt;
    }

    public void setSubjectIdSalt(String subjectIdSalt) {
        this.subjectIdSalt = subjectIdSalt;
    }

    public String getDefaultConsentVersion() {
        return defaultConsentVersion;
    }

    public void setDefaultConsentVersion(String defaultConsentVersion) {
        this.defaultConsentVersion = defaultConsentVersion;
    }

    public Path rootDirectory() {
        return Path.of(rootPath).toAbsolutePath().normalize();
    }
}
