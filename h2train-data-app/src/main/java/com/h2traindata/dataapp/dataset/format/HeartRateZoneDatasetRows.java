package com.h2traindata.dataapp.dataset.format;

import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetResponse;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetRow;
import java.util.List;

final class HeartRateZoneDatasetRows {

    private HeartRateZoneDatasetRows() {
    }

    static List<HeartRateZoneDatasetRow> flatten(HeartRateZoneDatasetResponse response) {
        return response.days().stream()
                .flatMap(day -> day.zones().stream().map(zone -> new HeartRateZoneDatasetRow(
                        day.userId(),
                        day.date(),
                        day.provider(),
                        day.trackedMinutes(),
                        day.activeMinutes(),
                        day.totalCalories(),
                        day.activeCalories(),
                        day.highIntensityMinutes(),
                        day.dominantActiveZone(),
                        zone.zone(),
                        zone.minutes(),
                        zone.calories(),
                        zone.percentageOfTrackedTime(),
                        zone.percentageOfActiveTime()
                )))
                .toList();
    }
}
