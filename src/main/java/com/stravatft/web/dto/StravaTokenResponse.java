package com.stravatft.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StravaTokenResponse(
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_at") long expiresAt,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        StravaAthlete athlete
) {
}
