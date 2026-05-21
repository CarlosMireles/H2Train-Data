package com.h2traindata.dataapp.application.port;

import com.h2traindata.dataapp.domain.ProjectionCheckpoint;
import java.util.Optional;

public interface CheckpointRepository {

    Optional<ProjectionCheckpoint> findLatest(String projectionName);

    void save(ProjectionCheckpoint checkpoint);
}
