package com.stravatft.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.strava.client-id=test-client",
        "app.strava.client-secret=test-secret",
        "app.strava.redirect-uri=http://localhost:8080/auth/strava/callback"
})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginRedirectsToStravaAuthorizeEndpoint() throws Exception {
        mockMvc.perform(get("/auth/strava/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("https://www.strava.com/oauth/authorize")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("client_id=test-client")));
    }
}
