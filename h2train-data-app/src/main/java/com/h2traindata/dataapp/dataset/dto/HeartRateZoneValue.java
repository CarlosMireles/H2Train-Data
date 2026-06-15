package com.h2traindata.dataapp.dataset.dto;

import java.math.BigDecimal;

public record HeartRateZoneValue(
        String zone,
        BigDecimal minutes,
        BigDecimal calories,
        BigDecimal percentageOfTrackedTime,
        BigDecimal percentageOfActiveTime
) {
}
