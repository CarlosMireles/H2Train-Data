package com.h2traindata.dataapp.dataset.format;

import com.h2traindata.dataapp.dataset.dto.DatasetExportResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetExportRow;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetSubjectMatch;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetResponse;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetRow;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CsvDatasetWriter implements DatasetWriter {

    @Override
    public DatasetFormat format() {
        return DatasetFormat.CSV;
    }

    @Override
    public byte[] writeQuery(DatasetQueryResponse response) {
        StringBuilder output = new StringBuilder("userId,metric,aggregatedValue,from,to\n");
        response.subjects().forEach(subject -> appendQueryRow(output, subject));
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] writeExport(DatasetExportResponse response) {
        StringBuilder output = new StringBuilder(
                "userId,metric,value,periodStart,periodEnd,unit,period,provider,activityType,zone\n");
        response.rows().forEach(row -> appendExportRow(output, row));
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] writeHeartRateZones(HeartRateZoneDatasetResponse response) {
        StringBuilder output = new StringBuilder(
                "userId,date,provider,trackedMinutes,activeMinutes,totalCalories,activeCalories,"
                        + "highIntensityMinutes,dominantActiveZone,zone,minutes,calories,"
                        + "percentageOfTrackedTime,percentageOfActiveTime\n");
        HeartRateZoneDatasetRows.flatten(response).forEach(row -> appendHeartRateZoneRow(output, row));
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendQueryRow(StringBuilder output, DatasetSubjectMatch subject) {
        append(output,
                subject.userId(),
                subject.metric(),
                subject.aggregatedValue(),
                subject.from(),
                subject.to());
    }

    private void appendExportRow(StringBuilder output, DatasetExportRow row) {
        append(output,
                row.userId(),
                row.metric(),
                row.value(),
                row.periodStart(),
                row.periodEnd(),
                row.unit(),
                row.period(),
                row.provider(),
                row.activityType(),
                row.zone());
    }

    private void appendHeartRateZoneRow(StringBuilder output, HeartRateZoneDatasetRow row) {
        append(output,
                row.userId(),
                row.date(),
                row.provider(),
                row.trackedMinutes(),
                row.activeMinutes(),
                row.totalCalories(),
                row.activeCalories(),
                row.highIntensityMinutes(),
                row.dominantActiveZone(),
                row.zone(),
                row.minutes(),
                row.calories(),
                row.percentageOfTrackedTime(),
                row.percentageOfActiveTime());
    }

    private void append(StringBuilder output, Object... values) {
        output.append(Arrays.stream(values)
                        .map(this::escape)
                        .collect(Collectors.joining(",")))
                .append('\n');
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
