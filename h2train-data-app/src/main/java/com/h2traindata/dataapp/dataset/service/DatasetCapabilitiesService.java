package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.dataset.aggregation.DatasetAggregation;
import com.h2traindata.dataapp.dataset.dto.DatasetCapabilitiesResponse;
import com.h2traindata.dataapp.dataset.filter.DatasetDimension;
import com.h2traindata.dataapp.dataset.filter.DatasetOperator;
import com.h2traindata.dataapp.dataset.format.DatasetFormat;
import org.springframework.stereotype.Service;

@Service
public class DatasetCapabilitiesService {

    private final LongitudinalDatasetReader reader;

    public DatasetCapabilitiesService(LongitudinalDatasetReader reader) {
        this.reader = reader;
    }

    public DatasetCapabilitiesResponse capabilities() {
        return new DatasetCapabilitiesResponse(
                reader.availableMetrics().stream().sorted().toList(),
                DatasetOperator.supportedValues(),
                DatasetAggregation.supportedValues(),
                DatasetFormat.supportedValues(),
                DatasetDimension.supportedValues()
        );
    }
}
