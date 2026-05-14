package com.h2traindata.datalake.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.datalake.domain.IncomingBusMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DatalakeDeadLetterWriter {

    private final DatalakePathResolver pathResolver;
    private final ObjectMapper objectMapper;

    public DatalakeDeadLetterWriter(DatalakePathResolver pathResolver, ObjectMapper objectMapper) {
        this.pathResolver = pathResolver;
        this.objectMapper = objectMapper;
    }

    public synchronized Path write(IncomingBusMessage message, RuntimeException exception) {
        ZonedDateTime failedAt = ZonedDateTime.now(java.time.ZoneOffset.UTC);
        Path target = pathResolver.deadLetterFile(failedAt);
        appendLine(target, deadLetterLine(message, exception, failedAt));
        return target;
    }

    private String deadLetterLine(IncomingBusMessage message,
                                  RuntimeException exception,
                                  ZonedDateTime failedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("failedAt", failedAt.toInstant().toString());
        payload.put("source", message.source());
        payload.put("channel", message.channel());
        payload.put("partition", message.partition());
        payload.put("offset", message.offset());
        payload.put("key", message.key());
        payload.put("reason", exception.getMessage());
        payload.put("rawPayload", message.payload());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException jsonException) {
            throw new IllegalStateException("Failed to serialize dead-letter payload", jsonException);
        }
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
            throw new IllegalStateException("Failed to write dead-letter event to datalake file " + target, exception);
        }
    }
}
