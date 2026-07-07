package com.h2traindata.dataapp.infrastructure;

import com.h2traindata.dataapp.application.port.DatalakeEventRepository;
import com.h2traindata.dataapp.config.DataAppDatalakeProperties;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class LocalDatalakeEventRepository implements DatalakeEventRepository {

    private final DataAppDatalakeProperties properties;
    private final DatalakeEventJsonParser eventJsonParser;

    public LocalDatalakeEventRepository(DataAppDatalakeProperties properties, DatalakeEventJsonParser eventJsonParser) {
        this.properties = properties;
        this.eventJsonParser = eventJsonParser;
    }

    @Override
    public List<NormalizedDatalakeEvent> readEvents() {
        Path eventsRoot = properties.effectiveRootPath().resolve("events");
        if (!Files.exists(eventsRoot)) {
            return List.of();
        }
        List<NormalizedDatalakeEvent> events = new ArrayList<>();
        try (Stream<Path> files = Files.walk(eventsRoot)) {
            List<Path> eventFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path file : eventFiles) {
                readFile(file, events);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read datalake events from " + eventsRoot, exception);
        }
        return List.copyOf(events);
    }

    @Override
    public List<NormalizedDatalakeEvent> readEvents(String userId) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }
        return readEvents().stream()
                .filter(event -> userId.equals(event.userId()))
                .toList();
    }

    private void readFile(Path file, List<NormalizedDatalakeEvent> events) {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            lines.filter(StringUtils::hasText)
                    .map(eventJsonParser::parse)
                    .forEach(event -> event.ifPresent(events::add));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read datalake event file " + file, exception);
        }
    }
}
