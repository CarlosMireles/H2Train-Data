package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.dataset.aggregation.DatasetAggregationService;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryRequest;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetSubjectMatch;
import com.h2traindata.dataapp.dataset.filter.DatasetFilter;
import com.h2traindata.dataapp.domain.SubjectInfo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DatasetQueryService {

    private final LongitudinalDatasetReader reader;
    private final DatasetRequestValidator validator;
    private final DatasetAggregationService aggregationService;

    public DatasetQueryService(LongitudinalDatasetReader reader,
                               DatasetRequestValidator validator,
                               DatasetAggregationService aggregationService) {
        this.reader = reader;
        this.validator = validator;
        this.aggregationService = aggregationService;
    }

    public DatasetQueryResult query(DatasetQueryRequest request) {
        DatasetRequestValidator.ValidatedQuery query = validator.validate(request);
        DatasetFilter filter = query.filter();
        List<DatasetSubjectMatch> matches = new ArrayList<>();
        reader.subjects().stream()
                .sorted(Comparator.comparing(SubjectInfo::userId))
                .forEach(subject -> aggregationService.aggregate(
                                filter.aggregation(),
                                filter.dimensions().filter(reader.readPoints(
                                        subject.userId(),
                                        filter.metric(),
                                        query.from(),
                                        query.to()
                                )))
                        .filter(filter::matches)
                        .map(BigDecimal::stripTrailingZeros)
                        .ifPresent(value -> matches.add(new DatasetSubjectMatch(
                                subject.userId(),
                                filter.metric(),
                                value,
                                query.from(),
                                query.to()
                        ))));
        return new DatasetQueryResult(
                query.format(),
                new DatasetQueryResponse(query.normalizedRequest(), List.copyOf(matches))
        );
    }
}
