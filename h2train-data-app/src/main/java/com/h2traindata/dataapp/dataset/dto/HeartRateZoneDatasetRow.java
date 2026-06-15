package com.h2traindata.dataapp.dataset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HeartRateZoneDatasetRow(
        String userId,
        LocalDate date,
        String provider,
        BigDecimal trackedMinutes,
        BigDecimal activeMinutes,
        BigDecimal totalCalories,
        BigDecimal activeCalories,
        BigDecimal highIntensityMinutes,
        String dominantActiveZone,
        String zone,
        BigDecimal minutes,
        BigDecimal calories,
        BigDecimal percentageOfTrackedTime,
        BigDecimal percentageOfActiveTime
) {
}
