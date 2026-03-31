package com.stravatft.service;

import com.stravatft.client.StravaApiClient;
import com.stravatft.datalake.DataLakeWriter;
import com.stravatft.web.dto.StravaActivity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ActivityIngestionService {

    private final StravaApiClient stravaApiClient;
    private final DataLakeWriter dataLakeWriter;

    public ActivityIngestionService(StravaApiClient stravaApiClient, DataLakeWriter dataLakeWriter) {
        this.stravaApiClient = stravaApiClient;
        this.dataLakeWriter = dataLakeWriter;
    }

    public List<StravaActivity> ingestRecentActivities(long athleteId, String accessToken) {
        List<StravaActivity> activities = stravaApiClient.fetchActivities(accessToken, 50);
        dataLakeWriter.writeActivities(athleteId, activities);
        return activities;
    }
}
