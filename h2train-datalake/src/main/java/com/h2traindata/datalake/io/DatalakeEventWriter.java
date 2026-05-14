package com.h2traindata.datalake.io;

import com.h2traindata.datalake.domain.DatalakeEventRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.stereotype.Component;

@Component
public class DatalakeEventWriter {

    private final DatalakePathResolver pathResolver;

    public DatalakeEventWriter(DatalakePathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    public synchronized Path write(DatalakeEventRecord eventRecord) {
        Path target = pathResolver.eventsFile(eventRecord);
        appendLine(target, eventRecord.rawJson());
        return target;
    }

    private void appendLine(Path target, String line) {
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(
                    target,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write event to datalake file " + target, exception);
        }
    }
}
