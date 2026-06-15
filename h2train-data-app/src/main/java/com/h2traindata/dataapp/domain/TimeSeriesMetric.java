package com.h2traindata.dataapp.domain;

public enum TimeSeriesMetric {
    DAILY_STEPS("daily_steps"),
    DAILY_DISTANCE("daily_distance"),
    DAILY_CALORIES("daily_calories"),
    DAILY_CALORIES_INGESTED("daily_calories_ingested"),
    DAILY_WATER_CONSUMED("daily_water_consumed"),
    DAILY_SLEEP_DURATION("daily_sleep_duration"),
    DAILY_BLOOD_GLUCOSE("daily_blood_glucose"),
    DAILY_WEIGHT("daily_weight"),
    DAILY_BMI("daily_bmi"),
    DAILY_BODY_FAT_PERCENTAGE("daily_body_fat_percentage"),
    DAILY_ACTIVITY_COUNT("daily_activity_count"),
    DAILY_WORKOUT_DURATION("daily_workout_duration"),
    DAILY_WORKOUT_DISTANCE("daily_workout_distance"),
    DAILY_WORKOUT_CALORIES("daily_workout_calories"),
    WEEKLY_ACTIVITY_COUNT("weekly_activity_count"),
    WEEKLY_WORKOUT_DURATION("weekly_workout_duration"),
    WEEKLY_WORKOUT_DISTANCE_BY_SPORT("weekly_workout_distance_by_sport"),
    WEEKLY_WORKOUT_CALORIES_BY_SPORT("weekly_workout_calories_by_sport"),
    HEART_RATE_ZONE_MINUTES("heart_rate_zone_minutes"),
    HEART_RATE_ZONE_CALORIES("heart_rate_zone_calories");

    private final String metricName;

    TimeSeriesMetric(String metricName) {
        this.metricName = metricName;
    }

    public String metricName() {
        return metricName;
    }
}
