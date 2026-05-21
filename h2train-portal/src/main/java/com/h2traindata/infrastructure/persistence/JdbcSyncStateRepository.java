package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.SyncStateRepository;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.domain.SyncState;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "type", havingValue = "jdbc", matchIfMissing = true)
public class JdbcSyncStateRepository implements SyncStateRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSyncStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<SyncState> findByProviderAndAthleteAndEventType(String providerId,
                                                                    String athleteId,
                                                                    EventType eventType) {
        return jdbcTemplate.query(
                """
                        SELECT provider_id, athlete_id, event_type, last_cursor, last_synced_at
                        FROM sync_states
                        WHERE provider_id = ? AND athlete_id = ? AND event_type = ?
                        """,
                this::mapSyncState,
                providerId,
                athleteId,
                eventType.name()
        ).stream().findFirst();
    }

    @Override
    public void save(SyncState syncState) {
        int updatedRows = jdbcTemplate.update(
                """
                        UPDATE sync_states
                        SET last_cursor = ?,
                            last_synced_at = ?,
                            updated_at = ?
                        WHERE provider_id = ? AND athlete_id = ? AND event_type = ?
                        """,
                fromCursor(syncState.lastCursor()),
                fromInstant(syncState.lastSyncedAt()),
                Instant.now().toString(),
                syncState.providerId(),
                syncState.athleteId(),
                syncState.eventType().name()
        );

        if (updatedRows == 0) {
            jdbcTemplate.update(
                    """
                            INSERT INTO sync_states (
                                provider_id,
                                athlete_id,
                                event_type,
                                last_cursor,
                                last_synced_at,
                                updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    syncState.providerId(),
                    syncState.athleteId(),
                    syncState.eventType().name(),
                    fromCursor(syncState.lastCursor()),
                    fromInstant(syncState.lastSyncedAt()),
                    Instant.now().toString()
            );
        }
    }

    private SyncState mapSyncState(ResultSet rs, int rowNum) throws SQLException {
        return new SyncState(
                rs.getString("provider_id"),
                rs.getString("athlete_id"),
                EventType.valueOf(rs.getString("event_type")),
                toCursor(rs.getString("last_cursor")),
                toInstant(rs.getString("last_synced_at"))
        );
    }

    private String fromInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    private Instant toInstant(String value) {
        return StringUtils.hasText(value) ? Instant.parse(value) : null;
    }

    private String fromCursor(SyncCursor cursor) {
        return cursor != null ? cursor.value() : null;
    }

    private SyncCursor toCursor(String value) {
        return StringUtils.hasText(value) ? new SyncCursor(value) : null;
    }
}
