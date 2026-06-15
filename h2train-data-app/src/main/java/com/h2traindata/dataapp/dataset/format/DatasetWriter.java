package com.h2traindata.dataapp.dataset.format;

import com.h2traindata.dataapp.dataset.dto.DatasetExportResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryResponse;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetResponse;

public interface DatasetWriter {

    DatasetFormat format();

    byte[] writeQuery(DatasetQueryResponse response);

    byte[] writeExport(DatasetExportResponse response);

    byte[] writeHeartRateZones(HeartRateZoneDatasetResponse response);
}
