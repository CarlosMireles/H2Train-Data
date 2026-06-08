package com.h2traindata.dataapp.application;

import com.h2traindata.dataapp.application.port.DatalakeEventRepository;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TimeSeriesRebuilder {

    private final DatalakeEventRepository eventRepository;
    private final TimeSeriesProjectionService projectionService;
    private final com.h2traindata.dataapp.infrastructure.LongitudinalDatamartRepository datamartRepository;

    public TimeSeriesRebuilder(DatalakeEventRepository eventRepository,
                               TimeSeriesProjectionService projectionService,
                               com.h2traindata.dataapp.infrastructure.LongitudinalDatamartRepository datamartRepository) {
        this.eventRepository = eventRepository;
        this.projectionService = projectionService;
        this.datamartRepository = datamartRepository;
    }

    public int rebuild() {
        datamartRepository.clear();
        List<NormalizedDatalakeEvent> events = eventRepository.readEvents().stream()
                .sorted(Comparator
                        .comparing(NormalizedDatalakeEvent::eventTimestamp)
                        .thenComparing(event -> event.userId() == null ? "" : event.userId())
                        .thenComparing(event -> event.eventId() == null ? "" : event.eventId()))
                .toList();
        events.forEach(projectionService::process);
        return events.size();
    }
}
