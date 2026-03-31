package com.stravatft.datalake;

import com.stravatft.web.dto.StravaActivity;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingDataLakeWriter implements DataLakeWriter {

    private static final Logger log = LoggerFactory.getLogger(LoggingDataLakeWriter.class);

    @Override
    public void writeActivities(long athleteId, List<StravaActivity> activities) {
        log.info("Persisting {} activities for athlete {} to the configured datalake sink", activities.size(), athleteId);
    }
}
