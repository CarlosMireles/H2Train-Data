package com.h2traindata.dataapp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesContribution;
import com.h2traindata.dataapp.domain.TimeSeriesProjection;
import com.h2traindata.dataapp.domain.TimeSeriesProjectionResult;
import com.h2traindata.dataapp.infrastructure.LongitudinalDatamartRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TimeSeriesProjectionService {

    private final TimeSeriesBuilderService builderService;
    private final LongitudinalDatamartRepository datamartRepository;
    private final ObjectMapper objectMapper;

    public TimeSeriesProjectionService(TimeSeriesBuilderService builderService,
                                       LongitudinalDatamartRepository datamartRepository,
                                       ObjectMapper objectMapper) {
        this.builderService = builderService;
        this.datamartRepository = datamartRepository;
        this.objectMapper = objectMapper;
    }

    public TimeSeriesProjectionResult process(NormalizedDatalakeEvent event) {
        datamartRepository.updateSyncFromEvent(event);
        datamartRepository.updateActivityFromEvent(event);

        SubjectInfo subject = shouldUpdateSubject(event)
                ? datamartRepository.updateSubjectFromEvent(event)
                : datamartRepository.findSubject(event.userId()).orElse(null);
        List<TimeSeriesContribution> contributions = builderService.projectEvent(
                event,
                zone(subject != null ? subject.timezone() : null)
        );
        if (contributions.isEmpty()) {
            return TimeSeriesProjectionResult.processed(0);
        }
        return datamartRepository.apply(new TimeSeriesProjection(
                event,
                sourceKey(event),
                payloadHash(event),
                contributions
        ));
    }

    private boolean shouldUpdateSubject(NormalizedDatalakeEvent event) {
        if (!StringUtils.hasText(event.providerId()) || "h2train".equalsIgnoreCase(event.providerId())) {
            return false;
        }
        return "USER_STATE".equalsIgnoreCase(event.eventType())
                || "ACTIVITY".equalsIgnoreCase(event.eventType())
                || "PHYSIOLOGICAL".equalsIgnoreCase(event.eventType())
                || "BODY_COMPOSITION".equalsIgnoreCase(event.eventType())
                || "HEALTH".equalsIgnoreCase(event.eventType());
    }

    private ZoneId zone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ignored) {
            return ZoneOffset.UTC;
        }
    }

    private String sourceKey(NormalizedDatalakeEvent event) {
        String stableId = StringUtils.hasText(event.eventId())
                ? event.eventId()
                : event.eventTimestamp().toString();
        return String.join("|",
                event.userId(),
                nullToEmpty(event.providerId()),
                nullToEmpty(event.eventType()),
                nullToEmpty(event.eventName()),
                stableId
        );
    }

    private String payloadHash(NormalizedDatalakeEvent event) {
        try {
            return sha256(objectMapper.writeValueAsString(event.event()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to canonicalize event payload for idempotency", exception);
        }
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
