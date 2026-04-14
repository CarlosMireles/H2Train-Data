package com.h2traindata.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.ProviderConnection;
import java.time.Instant;
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
        "app.fitbit.redirect-uri=http://localhost:8080/auth/fitbit/callback"
})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Test
    void loginRedirectsToStravaAuthorizeEndpoint() throws Exception {
        mockMvc.perform(get("/auth/strava/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("https://www.strava.com/oauth/authorize")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("client_id=12345")));
    }

    @Test
    void loginRedirectsToFitbitAuthorizeEndpoint() throws Exception {
        mockMvc.perform(get("/auth/fitbit/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("https://www.fitbit.com/oauth2/authorize")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("client_id=fitbit-client")));
    }

    @Test
    void syncSettingsCanBeReadAndUpdated() throws Exception {
        connectionRepository.save(new ProviderConnection(
                "strava",
                new AthleteProfile("7", "runner"),
                "access-token",
                "refresh-token",
                Instant.parse("2026-04-10T19:00:00Z")
        ));

        mockMvc.perform(get("/auth/strava/athletes/7/sync-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("strava"))
                .andExpect(jsonPath("$.athleteId").value("7"))
                .andExpect(jsonPath("$.syncEnabled").value(true))
                .andExpect(jsonPath("$.syncInterval").value("EVERY_24_HOURS"));

        mockMvc.perform(put("/auth/strava/athletes/7/sync-settings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false,
                                  "interval": "EVERY_5_HOURS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncEnabled").value(false))
                .andExpect(jsonPath("$.syncInterval").value("EVERY_5_HOURS"))
                .andExpect(jsonPath("$.syncIntervalLabel").value("Every 5 hours"));
    }
}
