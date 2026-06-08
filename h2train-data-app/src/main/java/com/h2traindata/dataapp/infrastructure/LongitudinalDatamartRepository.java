package com.h2traindata.dataapp.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.dataapp.config.DataAppDatalakeProperties;
import com.h2traindata.dataapp.domain.ActivityRecord;
import com.h2traindata.dataapp.domain.AggregationType;
import com.h2traindata.dataapp.domain.DatasetMetadata;
import com.h2traindata.dataapp.domain.DatasetExport;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import com.h2traindata.dataapp.domain.ProviderSyncStatus;
import com.h2traindata.dataapp.domain.ProcessedTimeSeriesEvent;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.SyncHistoryEntry;
import com.h2traindata.dataapp.domain.TimeSeriesContribution;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import com.h2traindata.dataapp.domain.TimeSeriesProjection;
import com.h2traindata.dataapp.domain.TimeSeriesProjectionResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class LongitudinalDatamartRepository {

    private static final String SUBJECT_INFO_FILE = "subject-info.csv";

    private final DataAppDatalakeProperties properties;
    private final ObjectMapper objectMapper;

    public LongitudinalDatamartRepository(DataAppDatalakeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public synchronized SubjectInfo updateSubjectFromEvent(NormalizedDatalakeEvent event) {
        Map<String, SubjectInfo> subjects = readSubjectsByUser();
        SubjectInfo current = subjects.get(event.userId());
        Set<String> providers = new LinkedHashSet<>();
        if (current != null) {
            providers.addAll(current.providers());
        }
        if (StringUtils.hasText(event.providerId())) {
            providers.add(event.providerId());
        }

        String timezone = current != null ? current.timezone() : "Z";
        String gender = current != null ? current.gender() : null;
        BigDecimal height = current != null ? current.height() : null;
        BigDecimal weight = current != null ? current.weight() : null;

        if (isEvent(event, "USER_STATE", "UserProfile")) {
            timezone = textField(event, "timezone").orElse(timezone);
            gender = textField(event, "gender").orElse(gender);
            height = numberField(event, "height").orElse(height);
            weight = numberField(event, "weight").orElse(weight);
        }
        if (isEvent(event, "BODY_COMPOSITION", "BodyComposition")) {
            weight = numberField(event, "weight").orElse(weight);
        }

        Instant firstRecord = current == null
                || current.firstRecord() == null
                || event.eventTimestamp().isBefore(current.firstRecord())
                ? event.eventTimestamp()
                : current.firstRecord();
        Instant lastRecord = current == null
                || current.lastRecord() == null
                || event.eventTimestamp().isAfter(current.lastRecord())
                ? event.eventTimestamp()
                : current.lastRecord();

        SubjectInfo updated = new SubjectInfo(
                event.userId(),
                event.userId(),
                Set.copyOf(providers),
                StringUtils.hasText(timezone) ? timezone : "Z",
                gender,
                height,
                weight,
                firstRecord,
                lastRecord
        );
        subjects.put(event.userId(), updated);
        writeSubjects(subjects.values().stream()
                .sorted(Comparator.comparing(SubjectInfo::subjectId))
                .toList());
        return updated;
    }

    public Optional<SubjectInfo> findSubject(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(readSubjectsByUser().get(userId));
    }

    public List<SubjectInfo> listSubjects() {
        return readSubjectsByUser().values().stream()
                .sorted(Comparator.comparing(SubjectInfo::subjectId))
                .toList();
    }

    public List<String> metrics(String userId) {
        Path userRoot = timeseriesRoot().resolve(segment(userId));
        if (!Files.exists(userRoot)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(userRoot)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".jsonl"))
                    .map(name -> name.substring(0, name.length() - ".jsonl".length()))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list metrics for user " + userId, exception);
        }
    }

    public List<TimeSeriesPoint> readPoints(String userId, String metricName) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(metricName)) {
            return List.of();
        }
        return readMetricFile(metricFile(userId, metricName));
    }

    public List<TimeSeriesPoint> readAllPoints(String userId) {
        return metrics(userId).stream()
                .flatMap(metric -> readPoints(userId, metric).stream())
                .sorted(pointComparator())
                .toList();
    }

    public DatasetExport describeExport(String userId) {
        List<String> metrics = metrics(userId);
        int pointCount = metrics.stream()
                .mapToInt(metric -> readPoints(userId, metric).size())
                .sum();
        int subjectCount = findSubject(userId).isPresent() ? 1 : 0;
        return new DatasetExport(
                longitudinalRoot(),
                subjectInfoFile(),
                timeseriesRoot(),
                subjectCount,
                pointCount,
                metrics
        );
    }

    public DatasetMetadata datasetMetadata() {
        List<SubjectInfo> subjects = listSubjects();
        Set<String> metrics = new LinkedHashSet<>();
        int pointCount = 0;
        for (SubjectInfo subject : subjects) {
            List<String> userMetrics = metrics(subject.userId());
            metrics.addAll(userMetrics);
            for (String metric : userMetrics) {
                pointCount += readPoints(subject.userId(), metric).size();
            }
        }
        Instant firstRecord = subjects.stream()
                .map(SubjectInfo::firstRecord)
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        Instant lastRecord = subjects.stream()
                .map(SubjectInfo::lastRecord)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new DatasetMetadata(
                subjects.size(),
                metrics.size(),
                pointCount,
                firstRecord,
                lastRecord,
                metrics.stream().sorted().toList()
        );
    }

    public synchronized void updateActivityFromEvent(NormalizedDatalakeEvent event) {
        if (!isEvent(event, "ACTIVITY", "Workout")) {
            return;
        }
        ActivityRecord activity = new ActivityRecord(
                eventIdentity(event, "activity"),
                event.userId(),
                event.providerId(),
                textField(event, "activityType").orElse("workout"),
                instantField(event, "startTime").orElse(event.eventTimestamp()),
                instantField(event, "endTime").orElse(null),
                numberField(event, "duration").orElse(null),
                numberField(event, "distanceMeters").orElse(null),
                numberField(event, "calories").orElse(null),
                event.eventTimestamp()
        );
        List<ActivityRecord> activities = new ArrayList<>(readActivities(event.userId()));
        activities.removeIf(existing -> sameActivity(existing, activity));
        activities.add(activity);
        writeActivities(event.userId(), activities);
    }

    public List<ActivityRecord> readActivities(String userId) {
        Path file = activitiesFile(userId);
        if (!Files.exists(file)) {
            return List.of();
        }
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.filter(StringUtils::hasText)
                    .map(line -> readJson(line, ActivityRecord.class))
                    .sorted(activityComparator())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read activities for user " + userId, exception);
        }
    }

    public Optional<ActivityRecord> findActivity(String userId, String activityId) {
        if (!StringUtils.hasText(activityId)) {
            return Optional.empty();
        }
        return readActivities(userId).stream()
                .filter(activity -> activityId.equals(activity.activityId()))
                .findFirst();
    }

    public synchronized void updateSyncFromEvent(NormalizedDatalakeEvent event) {
        if (!isEvent(event, "ACCOUNT_SYNC", "provider_account_synced")) {
            return;
        }
        Set<String> providers = providerIds(event);
        updateSubjectProvidersFromSync(event, providers);

        String provider = textField(event, "linkedProviderId")
                .orElseGet(() -> providers.stream().findFirst().orElse(null));
        if (!StringUtils.hasText(provider)) {
            return;
        }
        Boolean syncEnabled = booleanField(event, "syncEnabled").orElse(null);
        String syncInterval = textField(event, "syncInterval").orElse(null);
        SyncHistoryEntry historyEntry = new SyncHistoryEntry(
                eventIdentity(event, "sync"),
                event.userId(),
                provider,
                syncEnabled,
                syncInterval,
                event.eventTimestamp(),
                "synced"
        );
        upsertSyncHistory(historyEntry);
        upsertSyncStatus(new ProviderSyncStatus(
                provider,
                syncEnabled,
                syncInterval,
                event.eventTimestamp(),
                "synced"
        ), event.userId());
    }

    public List<ProviderSyncStatus> readSyncStatus(String userId) {
        Path file = syncStatusFile(userId);
        if (!Files.exists(file)) {
            return List.of();
        }
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.filter(StringUtils::hasText)
                    .map(line -> readJson(line, ProviderSyncStatus.class))
                    .sorted(Comparator.comparing(status -> status.provider() == null ? "" : status.provider()))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read sync status for user " + userId, exception);
        }
    }

    public List<SyncHistoryEntry> readSyncHistory(String userId) {
        Path file = syncHistoryFile(userId);
        if (!Files.exists(file)) {
            return List.of();
        }
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.filter(StringUtils::hasText)
                    .map(line -> readJson(line, SyncHistoryEntry.class))
                    .sorted(Comparator.comparing(SyncHistoryEntry::syncedAt).reversed())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read sync history for user " + userId, exception);
        }
    }

    public synchronized TimeSeriesProjectionResult apply(TimeSeriesProjection projection) {
        if (projection.contributions().isEmpty()) {
            return TimeSeriesProjectionResult.processed(0);
        }

        Path stateFile = eventStateFile(projection.sourceEvent().userId(), projection.sourceKey());
        ProcessedTimeSeriesEvent previous = readProcessedEvent(stateFile).orElse(null);
        if (previous != null && projection.payloadHash().equals(previous.payloadHash())) {
            return TimeSeriesProjectionResult.duplicate(previous.contributions().size());
        }

        Set<PointKey> lastKeysToRecompute = new LinkedHashSet<>();
        if (previous != null) {
            removePreviousContributions(previous, stateFile, lastKeysToRecompute);
        }

        List<TimeSeriesContribution> appliedContributions = new ArrayList<>();
        for (TimeSeriesContribution contribution : projection.contributions()) {
            PointKey key = PointKey.from(contribution.point());
            if (contribution.point().aggregationType() == AggregationType.LAST) {
                appliedContributions.add(contribution.asApplied(true));
                lastKeysToRecompute.add(key);
                continue;
            }

            boolean applied = true;
            if (contribution.primaryDailyMetric()) {
                deactivateFallbacks(key, stateFile);
            }
            if (contribution.fallbackDailyMetric() && hasAppliedPrimary(key, stateFile)) {
                applied = false;
            }
            TimeSeriesContribution finalContribution = contribution.asApplied(applied);
            appliedContributions.add(finalContribution);
            if (applied) {
                applyDelta(finalContribution.point(), finalContribution.point().value());
            }
        }

        writeProcessedEvent(stateFile, new ProcessedTimeSeriesEvent(
                projection.sourceKey(),
                projection.payloadHash(),
                projection.sourceEvent().userId(),
                List.copyOf(appliedContributions)
        ));

        for (PointKey key : lastKeysToRecompute) {
            recomputeLastPoint(key);
        }

        return TimeSeriesProjectionResult.processed(appliedContributions.size());
    }

    public synchronized void clear() {
        deleteDirectory(longitudinalRoot());
    }

    private void removePreviousContributions(ProcessedTimeSeriesEvent previous,
                                             Path currentStateFile,
                                             Set<PointKey> lastKeysToRecompute) {
        for (TimeSeriesContribution contribution : previous.contributions()) {
            PointKey key = PointKey.from(contribution.point());
            if (contribution.point().aggregationType() == AggregationType.LAST) {
                lastKeysToRecompute.add(key);
                continue;
            }
            if (contribution.applied()) {
                applyDelta(contribution.point(), contribution.point().value().negate());
            }
        }
        for (TimeSeriesContribution contribution : previous.contributions()) {
            if (contribution.primaryDailyMetric()) {
                activateFallbacksIfNoPrimary(PointKey.from(contribution.point()), currentStateFile);
            }
        }
    }

    private void deactivateFallbacks(PointKey key, Path currentStateFile) {
        for (Path stateFile : eventStateFiles(key.userId())) {
            if (stateFile.equals(currentStateFile)) {
                continue;
            }
            ProcessedTimeSeriesEvent event = readProcessedEvent(stateFile).orElse(null);
            if (event == null) {
                continue;
            }
            boolean changed = false;
            List<TimeSeriesContribution> updated = new ArrayList<>();
            for (TimeSeriesContribution contribution : event.contributions()) {
                if (contribution.fallbackDailyMetric()
                        && contribution.applied()
                        && key.equals(PointKey.from(contribution.point()))) {
                    applyDelta(contribution.point(), contribution.point().value().negate());
                    updated.add(contribution.asApplied(false));
                    changed = true;
                } else {
                    updated.add(contribution);
                }
            }
            if (changed) {
                writeProcessedEvent(stateFile, new ProcessedTimeSeriesEvent(
                        event.sourceKey(),
                        event.payloadHash(),
                        event.userId(),
                        List.copyOf(updated)
                ));
            }
        }
    }

    private void activateFallbacksIfNoPrimary(PointKey key, Path currentStateFile) {
        if (hasAppliedPrimary(key, currentStateFile)) {
            return;
        }
        for (Path stateFile : eventStateFiles(key.userId())) {
            if (stateFile.equals(currentStateFile)) {
                continue;
            }
            ProcessedTimeSeriesEvent event = readProcessedEvent(stateFile).orElse(null);
            if (event == null) {
                continue;
            }
            boolean changed = false;
            List<TimeSeriesContribution> updated = new ArrayList<>();
            for (TimeSeriesContribution contribution : event.contributions()) {
                if (contribution.fallbackDailyMetric()
                        && !contribution.applied()
                        && key.equals(PointKey.from(contribution.point()))) {
                    applyDelta(contribution.point(), contribution.point().value());
                    updated.add(contribution.asApplied(true));
                    changed = true;
                } else {
                    updated.add(contribution);
                }
            }
            if (changed) {
                writeProcessedEvent(stateFile, new ProcessedTimeSeriesEvent(
                        event.sourceKey(),
                        event.payloadHash(),
                        event.userId(),
                        List.copyOf(updated)
                ));
            }
        }
    }

    private boolean hasAppliedPrimary(PointKey key, Path currentStateFile) {
        for (Path stateFile : eventStateFiles(key.userId())) {
            if (stateFile.equals(currentStateFile)) {
                continue;
            }
            ProcessedTimeSeriesEvent event = readProcessedEvent(stateFile).orElse(null);
            if (event == null) {
                continue;
            }
            boolean found = event.contributions().stream()
                    .anyMatch(contribution -> contribution.primaryDailyMetric()
                            && contribution.applied()
                            && key.equals(PointKey.from(contribution.point())));
            if (found) {
                return true;
            }
        }
        return false;
    }

    private void recomputeLastPoint(PointKey key) {
        TimeSeriesContribution latest = null;
        for (Path stateFile : eventStateFiles(key.userId())) {
            ProcessedTimeSeriesEvent event = readProcessedEvent(stateFile).orElse(null);
            if (event == null) {
                continue;
            }
            for (TimeSeriesContribution contribution : event.contributions()) {
                if (contribution.applied()
                        && contribution.point().aggregationType() == AggregationType.LAST
                        && key.equals(PointKey.from(contribution.point()))
                        && (latest == null || !contribution.sourceEventTimestamp().isBefore(latest.sourceEventTimestamp()))) {
                    latest = contribution;
                }
            }
        }
        if (latest == null) {
            removePoint(key);
        } else {
            upsertPoint(latest.point());
        }
    }

    private void applyDelta(TimeSeriesPoint point, BigDecimal delta) {
        PointKey key = PointKey.from(point);
        List<TimeSeriesPoint> points = new ArrayList<>(readMetricFile(metricFile(point.userId(), point.metricName())));
        boolean updated = false;
        for (int index = 0; index < points.size(); index++) {
            TimeSeriesPoint existing = points.get(index);
            if (key.equals(PointKey.from(existing))) {
                points.set(index, copyPoint(point, existing.value().add(delta)));
                updated = true;
                break;
            }
        }
        if (!updated) {
            points.add(copyPoint(point, delta));
        }
        writeMetricFile(point.userId(), point.metricName(), points);
    }

    private void upsertPoint(TimeSeriesPoint point) {
        PointKey key = PointKey.from(point);
        List<TimeSeriesPoint> points = new ArrayList<>(readMetricFile(metricFile(point.userId(), point.metricName())));
        boolean updated = false;
        for (int index = 0; index < points.size(); index++) {
            TimeSeriesPoint existing = points.get(index);
            if (key.equals(PointKey.from(existing))) {
                points.set(index, point);
                updated = true;
                break;
            }
        }
        if (!updated) {
            points.add(point);
        }
        writeMetricFile(point.userId(), point.metricName(), points);
    }

    private void removePoint(PointKey key) {
        List<TimeSeriesPoint> points = new ArrayList<>(readMetricFile(metricFile(key.userId(), key.metricName())));
        points.removeIf(point -> key.equals(PointKey.from(point)));
        writeMetricFile(key.userId(), key.metricName(), points);
    }

    private List<TimeSeriesPoint> readMetricFile(Path file) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.filter(StringUtils::hasText)
                    .map(this::readPoint)
                    .sorted(pointComparator())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read metric file " + file, exception);
        }
    }

    private TimeSeriesPoint readPoint(String json) {
        try {
            return objectMapper.readValue(json, TimeSeriesPoint.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Time-series point JSON is invalid", exception);
        }
    }

    private void writeMetricFile(String userId, String metricName, List<TimeSeriesPoint> points) {
        Path file = metricFile(userId, metricName);
        try {
            Files.createDirectories(file.getParent());
            List<String> lines = points.stream()
                    .sorted(pointComparator())
                    .map(this::json)
                    .toList();
            Files.write(file, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write metric file " + file, exception);
        }
    }

    private void writeActivities(String userId, List<ActivityRecord> activities) {
        Path file = activitiesFile(userId);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, activities.stream()
                            .sorted(activityComparator())
                            .map(this::json)
                            .toList(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write activities for user " + userId, exception);
        }
    }

    private void upsertSyncHistory(SyncHistoryEntry entry) {
        List<SyncHistoryEntry> history = new ArrayList<>(readSyncHistory(entry.userId()));
        history.removeIf(existing -> entry.syncId() != null && entry.syncId().equals(existing.syncId()));
        history.add(entry);
        Path file = syncHistoryFile(entry.userId());
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, history.stream()
                            .sorted(Comparator.comparing(SyncHistoryEntry::syncedAt).reversed())
                            .map(this::json)
                            .toList(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write sync history for user " + entry.userId(), exception);
        }
    }

    private void upsertSyncStatus(ProviderSyncStatus status, String userId) {
        List<ProviderSyncStatus> statuses = new ArrayList<>(readSyncStatus(userId));
        statuses.removeIf(existing -> status.provider() != null && status.provider().equals(existing.provider()));
        statuses.add(status);
        Path file = syncStatusFile(userId);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, statuses.stream()
                            .sorted(Comparator.comparing(item -> item.provider() == null ? "" : item.provider()))
                            .map(this::json)
                            .toList(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write sync status for user " + userId, exception);
        }
    }

    private void updateSubjectProvidersFromSync(NormalizedDatalakeEvent event, Set<String> providersFromEvent) {
        if (providersFromEvent.isEmpty()) {
            return;
        }
        Map<String, SubjectInfo> subjects = readSubjectsByUser();
        SubjectInfo current = subjects.get(event.userId());
        Set<String> providers = new LinkedHashSet<>();
        if (current != null) {
            providers.addAll(current.providers());
        }
        providers.addAll(providersFromEvent);
        SubjectInfo updated = new SubjectInfo(
                event.userId(),
                event.userId(),
                Set.copyOf(providers),
                current != null && StringUtils.hasText(current.timezone()) ? current.timezone() : "Z",
                current != null ? current.gender() : null,
                current != null ? current.height() : null,
                current != null ? current.weight() : null,
                current == null || current.firstRecord() == null || event.eventTimestamp().isBefore(current.firstRecord())
                        ? event.eventTimestamp()
                        : current.firstRecord(),
                current == null || current.lastRecord() == null || event.eventTimestamp().isAfter(current.lastRecord())
                        ? event.eventTimestamp()
                        : current.lastRecord()
        );
        subjects.put(event.userId(), updated);
        writeSubjects(subjects.values().stream()
                .sorted(Comparator.comparing(SubjectInfo::subjectId))
                .toList());
    }

    private Optional<ProcessedTimeSeriesEvent> readProcessedEvent(Path file) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(Files.readString(file, StandardCharsets.UTF_8),
                    ProcessedTimeSeriesEvent.class));
        } catch (IOException exception) {
            throw new IllegalStateException("Processed event state is invalid: " + file, exception);
        }
    }

    private void writeProcessedEvent(Path file, ProcessedTimeSeriesEvent event) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json(event), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write processed event state " + file, exception);
        }
    }

    private List<Path> eventStateFiles(String userId) {
        Path directory = stateEventsRoot().resolve(segment(userId));
        if (!Files.exists(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list processed event state files for " + userId, exception);
        }
    }

    private Path eventStateFile(String userId, String sourceKey) {
        return stateEventsRoot()
                .resolve(segment(userId))
                .resolve(sha256(sourceKey) + ".json");
    }

    private Map<String, SubjectInfo> readSubjectsByUser() {
        Path file = subjectInfoFile();
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            Map<String, SubjectInfo> subjects = new LinkedHashMap<>();
            lines.skip(1)
                    .filter(StringUtils::hasText)
                    .map(this::parseSubject)
                    .forEach(subject -> subjects.put(subject.userId(), subject));
            return subjects;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read subject-info.csv", exception);
        }
    }

    private SubjectInfo parseSubject(String line) {
        String[] columns = line.split(",", -1);
        Set<String> providers = new LinkedHashSet<>();
        if (columns.length > 2 && StringUtils.hasText(columns[2])) {
            providers.addAll(List.of(columns[2].split("\\|")));
        }
        return new SubjectInfo(
                column(columns, 0),
                column(columns, 1),
                Set.copyOf(providers),
                column(columns, 3),
                column(columns, 4),
                decimal(column(columns, 5)),
                decimal(column(columns, 6)),
                instant(column(columns, 7)),
                instant(column(columns, 8))
        );
    }

    private void writeSubjects(List<SubjectInfo> subjects) {
        Path file = subjectInfoFile();
        List<String> lines = new ArrayList<>();
        lines.add("subjectId,userId,providers,timezone,gender,height,weight,firstRecord,lastRecord");
        subjects.stream()
                .sorted(Comparator.comparing(SubjectInfo::subjectId))
                .map(this::subjectCsvLine)
                .forEach(lines::add);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write subject-info.csv", exception);
        }
    }

    private String subjectCsvLine(SubjectInfo subject) {
        return String.join(",",
                value(subject.subjectId()),
                value(subject.userId()),
                value(String.join("|", subject.providers().stream().sorted().toList())),
                value(subject.timezone()),
                value(subject.gender()),
                value(decimal(subject.height())),
                value(decimal(subject.weight())),
                value(instant(subject.firstRecord())),
                value(instant(subject.lastRecord()))
        );
    }

    private Optional<String> textField(NormalizedDatalakeEvent event, String fieldName) {
        String value = event.event().path(fieldName).asText(null);
        return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    private Optional<Boolean> booleanField(NormalizedDatalakeEvent event, String fieldName) {
        if (!event.event().hasNonNull(fieldName)) {
            return Optional.empty();
        }
        if (event.event().path(fieldName).isBoolean()) {
            return Optional.of(event.event().path(fieldName).booleanValue());
        }
        String value = event.event().path(fieldName).asText(null);
        return StringUtils.hasText(value) ? Optional.of(Boolean.parseBoolean(value.trim())) : Optional.empty();
    }

    private Optional<Instant> instantField(NormalizedDatalakeEvent event, String fieldName) {
        if (!event.event().hasNonNull(fieldName)) {
            return Optional.empty();
        }
        if (event.event().path(fieldName).isNumber()) {
            return Optional.of(Instant.ofEpochSecond(event.event().path(fieldName).longValue()));
        }
        String value = event.event().path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(value.trim()));
        } catch (DateTimeParseException ignored) {
            try {
                return Optional.of(OffsetDateTime.parse(value.trim()).toInstant());
            } catch (DateTimeParseException ignoredAgain) {
                return Optional.empty();
            }
        }
    }

    private Set<String> providerIds(NormalizedDatalakeEvent event) {
        Set<String> providers = new LinkedHashSet<>();
        textField(event, "linkedProviderId").ifPresent(providers::add);
        if (event.event().has("providerIds") && event.event().path("providerIds").isArray()) {
            event.event().path("providerIds").forEach(provider -> {
                String value = provider.asText(null);
                if (StringUtils.hasText(value)) {
                    providers.add(value.trim());
                }
            });
        }
        return providers;
    }

    private Optional<BigDecimal> numberField(NormalizedDatalakeEvent event, String fieldName) {
        if (!event.event().hasNonNull(fieldName)) {
            return Optional.empty();
        }
        if (event.event().path(fieldName).isNumber()) {
            return Optional.of(event.event().path(fieldName).decimalValue());
        }
        String value = event.event().path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value.trim()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private boolean isEvent(NormalizedDatalakeEvent event, String eventType, String eventName) {
        return eventType.equalsIgnoreCase(event.eventType()) && eventName.equalsIgnoreCase(event.eventName());
    }

    private TimeSeriesPoint copyPoint(TimeSeriesPoint source, BigDecimal value) {
        return new TimeSeriesPoint(
                source.userId(),
                source.metricName(),
                source.period(),
                source.periodStart(),
                source.periodEnd(),
                value.stripTrailingZeros(),
                source.unit(),
                source.provider(),
                source.sourceEventType(),
                source.sourceEventName(),
                source.aggregationType(),
                source.activityType(),
                source.zone(),
                source.generatedAt()
        );
    }

    private Comparator<TimeSeriesPoint> pointComparator() {
        return Comparator
                .comparing(TimeSeriesPoint::periodStart)
                .thenComparing(TimeSeriesPoint::metricName)
                .thenComparing(point -> point.provider() == null ? "" : point.provider())
                .thenComparing(point -> point.activityType() == null ? "" : point.activityType())
                .thenComparing(point -> point.zone() == null ? "" : point.zone());
    }

    private Comparator<ActivityRecord> activityComparator() {
        return Comparator
                .comparing(ActivityRecord::startTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(activity -> activity.activityId() == null ? "" : activity.activityId());
    }

    private Path longitudinalRoot() {
        return properties.effectiveLongitudinalDatamartPath();
    }

    private Path subjectInfoFile() {
        return longitudinalRoot().resolve(SUBJECT_INFO_FILE);
    }

    private Path timeseriesRoot() {
        return longitudinalRoot().resolve("timeseries");
    }

    private Path stateEventsRoot() {
        return longitudinalRoot().resolve("state").resolve("events");
    }

    private Path activitiesFile(String userId) {
        return longitudinalRoot()
                .resolve("activities")
                .resolve(segment(userId))
                .resolve("activities.jsonl");
    }

    private Path syncStatusFile(String userId) {
        return longitudinalRoot()
                .resolve("sync")
                .resolve(segment(userId))
                .resolve("status.jsonl");
    }

    private Path syncHistoryFile(String userId) {
        return longitudinalRoot()
                .resolve("sync")
                .resolve(segment(userId))
                .resolve("history.jsonl");
    }

    private Path metricFile(String userId, String metricName) {
        return timeseriesRoot()
                .resolve(segment(userId))
                .resolve(segment(metricName) + ".jsonl");
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize datamart value", exception);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Datamart JSON is invalid for " + type.getSimpleName(), exception);
        }
    }

    private boolean sameActivity(ActivityRecord left, ActivityRecord right) {
        return left.activityId() != null
                && left.activityId().equals(right.activityId())
                && ((left.provider() == null && right.provider() == null)
                || (left.provider() != null && left.provider().equals(right.provider())));
    }

    private String eventIdentity(NormalizedDatalakeEvent event, String prefix) {
        if (StringUtils.hasText(event.eventId())) {
            return event.eventId();
        }
        for (String fieldName : List.of("sourceId", "aggregateId", "id", "externalId")) {
            Optional<String> value = textField(event, fieldName);
            if (value.isPresent()) {
                return value.get();
            }
        }
        String stableKey = String.join("|",
                prefix,
                value(event.userId()),
                value(event.providerId()),
                value(event.eventType()),
                value(event.eventName()),
                value(instant(event.eventTimestamp())),
                textField(event, "startTime").orElse(""),
                textField(event, "date").orElse(""));
        return prefix + "-" + sha256(stableKey).substring(0, 16);
    }

    private String segment(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : bytes) {
                builder.append("%02x".formatted(current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void deleteDirectory(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> existing = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : existing) {
                Files.delete(path);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to clear longitudinal datamart " + root, exception);
        }
    }

    private String column(String[] columns, int index) {
        return index < columns.length && StringUtils.hasText(columns[index]) ? columns[index] : null;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal decimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return new BigDecimal(value);
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private Instant instant(String value) {
        return StringUtils.hasText(value) ? Instant.parse(value) : null;
    }

    private String instant(Instant value) {
        return value == null ? "" : value.toString();
    }

    private record PointKey(
            String userId,
            String metricName,
            String period,
            Instant periodStart,
            String provider,
            String activityType,
            String zone
    ) {
        private static PointKey from(TimeSeriesPoint point) {
            return new PointKey(
                    point.userId(),
                    point.metricName(),
                    point.period(),
                    point.periodStart(),
                    point.provider(),
                    point.activityType(),
                    point.zone()
            );
        }
    }
}
