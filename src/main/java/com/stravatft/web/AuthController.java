package com.stravatft.web;

import com.stravatft.service.ActivityIngestionService;
import com.stravatft.service.StravaConnectionService;
import com.stravatft.service.StravaConnectionService.StoredAthleteConnection;
import com.stravatft.web.dto.ConnectAthleteResponse;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/strava")
public class AuthController {

    private final StravaConnectionService stravaConnectionService;
    private final ActivityIngestionService activityIngestionService;

    public AuthController(StravaConnectionService stravaConnectionService,
                          ActivityIngestionService activityIngestionService) {
        this.stravaConnectionService = stravaConnectionService;
        this.activityIngestionService = activityIngestionService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        URI authorizationUri = stravaConnectionService.buildAuthorizationUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizationUri.toString())
                .build();
    }

    @GetMapping("/callback")
    public ConnectAthleteResponse callback(@RequestParam("code") String code) {
        StoredAthleteConnection connection = stravaConnectionService.connectAthlete(code);
        int importedActivities = activityIngestionService
                .ingestRecentActivities(connection.athleteId(), connection.accessToken())
                .size();

        return new ConnectAthleteResponse(
                connection.athleteId(),
                connection.username(),
                importedActivities,
                "Athlete connected and recent activities sent to the datalake writer"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
