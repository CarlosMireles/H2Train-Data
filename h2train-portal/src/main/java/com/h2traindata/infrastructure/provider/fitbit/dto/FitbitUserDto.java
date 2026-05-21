package com.h2traindata.infrastructure.provider.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FitbitUserDto(
        @JsonProperty("encodedId") String encodedId,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("fullName") String fullName
) {
}
