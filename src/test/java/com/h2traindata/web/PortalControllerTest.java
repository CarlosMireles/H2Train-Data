package com.h2traindata.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        "app.fitbit.redirect-uri=http://localhost:8080/auth/fitbit/callback"
})
@AutoConfigureMockMvc
class PortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeRendersLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("H2Train Data")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fitbit")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Strava")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/h2train-logo.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Automatic sync")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Every 5 hours")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Every 7 days")));
    }
}
