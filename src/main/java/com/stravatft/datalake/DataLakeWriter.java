package com.stravatft.datalake;

import com.stravatft.web.dto.StravaActivity;
import java.util.List;

public interface DataLakeWriter {

    void writeActivities(long athleteId, List<StravaActivity> activities);
}
