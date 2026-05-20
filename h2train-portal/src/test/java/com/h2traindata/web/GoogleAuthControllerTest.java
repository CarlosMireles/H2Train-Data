package com.h2traindata.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.strava.client-id=12345",
        "app.strava.client-secret=test-secret",
        "app.strava.redirect-uri=http://localhost:8080/auth/strava/callback",
        "app.fitbit.enabled=true",
        "app.fitbit.client-id=fitbit-client",
        "app.fitbit.client-secret=fitbit-secret",
        "app.fitbit.redirect-uri=http://localhost:8080/auth/fitbit/callback",
        "app.auth.google.enabled=true",
        "app.auth.google.client-id=google-client",
        "app.auth.google.client-secret=google-secret",
        "app.auth.google.redirect-uri=http://localhost:8080/auth/google/callback"
})
@AutoConfigureMockMvc
class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void googleLoginRedirectsToGoogleAuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/auth/google/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("client_id=google-client")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("redirect_uri=http://localhost:8080/auth/google/callback")));
    }
}
