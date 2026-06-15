package com.h2traindata.dataapp.dataset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HeartRateZoneDay(
        String userId,
        LocalDate date,
        String provider,
        BigDecimal trackedMinutes,
        BigDecimal activeMinutes,
        BigDecimal totalCalories,
        BigDecimal activeCalories,
        BigDecimal highIntensityMinutes,
        String dominantActiveZone,
        List<HeartRateZoneValue> zones
) {
}
