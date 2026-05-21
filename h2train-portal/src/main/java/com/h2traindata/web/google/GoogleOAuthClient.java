package com.h2traindata.web.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.h2traindata.web.identity.ExternalIdentityProfile;
import com.h2traindata.web.identity.ExternalIdentityProvider;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuthClient implements ExternalIdentityProvider {

    private static final String SCOPE = "openid email profile";
    private static final String PROVIDER_ID = "google";

    private final GoogleAuthProperties properties;
    private final RestClient restClient;

    public GoogleOAuthClient(GoogleAuthProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public URI authorizationUri(String state) {
        return UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("prompt", "select_account")
                .queryParam("state", state)
                .build()
                .toUri();
    }

    @Override
    public ExternalIdentityProfile fetchProfile(String code) {
        GoogleTokenResponse tokenResponse = exchangeCode(code);
        GoogleUserInfoResponse userInfo = restClient.get()
                .uri(properties.getUserInfoUri())
                .headers(headers -> headers.setBearerAuth(tokenResponse.accessToken()))
                .retrieve()
                .body(GoogleUserInfoResponse.class);

        if (userInfo == null || !Boolean.TRUE.equals(userInfo.emailVerified())) {
            throw new IllegalStateException("Google account email is not verified");
        }
        return new ExternalIdentityProfile(userInfo.email(), userInfo.name());
    }

    private GoogleTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("grant_type", "authorization_code");

        GoogleTokenResponse tokenResponse = restClient.post()
                .uri(properties.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);

        if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new IllegalStateException("Google did not return an access token");
        }
        return tokenResponse;
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("id_token") String idToken
    ) {
    }

    private record GoogleUserInfoResponse(
            String email,
            String name,
            @JsonProperty("email_verified") Boolean emailVerified
    ) {
    }
}
