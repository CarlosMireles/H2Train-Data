package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.domain.SyncInterval;
import com.h2traindata.domain.SyncPreferences;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "type", havingValue = "jdbc", matchIfMissing = true)
public class JdbcConnectionRepository implements ConnectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcConnectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ProviderConnection connection) {
        int updatedRows = jdbcTemplate.update(
                """
                        UPDATE provider_connections
                        SET athlete_username = ?,
                            access_token = ?,
                            refresh_token = ?,
                            expires_at = ?,
                            sync_enabled = ?,
                            sync_interval = ?,
                            last_sync_cursor = ?,
                            last_synced_at = ?,
                            user_id = ?,
                            updated_at = ?
                        WHERE provider_id = ? AND athlete_id = ?
                        """,
                connection.athlete().username(),
                connection.accessToken(),
                connection.refreshToken(),
                fromInstant(connection.expiresAt()),
                connection.syncPreferences().enabled(),
                connection.syncPreferences().interval().name(),
                fromCursor(connection.lastSyncCursor()),
                fromInstant(connection.lastSyncedAt()),
                connection.userId(),
                Instant.now().toString(),
                connection.providerId(),
                connection.athlete().id()
        );

        if (updatedRows == 0) {
            jdbcTemplate.update(
                    """
                            INSERT INTO provider_connections (
                                provider_id,
                                athlete_id,
                                athlete_username,
                                access_token,
                                refresh_token,
                                expires_at,
                                sync_enabled,
                                sync_interval,
                                last_sync_cursor,
                                last_synced_at,
                                user_id,
                                updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    connection.providerId(),
                    connection.athlete().id(),
                    connection.athlete().username(),
                    connection.accessToken(),
                    connection.refreshToken(),
                    fromInstant(connection.expiresAt()),
                    connection.syncPreferences().enabled(),
                    connection.syncPreferences().interval().name(),
                    fromCursor(connection.lastSyncCursor()),
                    fromInstant(connection.lastSyncedAt()),
                    connection.userId(),
                    Instant.now().toString()
            );
        }
    }

    @Override
    public Optional<ProviderConnection> findByProviderAndAthlete(String providerId, String athleteId) {
        return jdbcTemplate.query(
                """
                        SELECT provider_id,
                               athlete_id,
                               athlete_username,
                               access_token,
                               refresh_token,
                               expires_at,
                               sync_enabled,
                               sync_interval,
                               last_sync_cursor,
                               last_synced_at,
                               user_id
                        FROM provider_connections
                        WHERE provider_id = ? AND athlete_id = ?
                        """,
                this::mapConnection,
                providerId,
                athleteId
        ).stream().findFirst();
    }

    @Override
    public List<ProviderConnection> findByUserId(String userId) {
        return jdbcTemplate.query(
                """
                        SELECT provider_id,
                               athlete_id,
                               athlete_username,
                               access_token,
                               refresh_token,
                               expires_at,
                               sync_enabled,
                               sync_interval,
                               last_sync_cursor,
                               last_synced_at,
                               user_id
                        FROM provider_connections
                        WHERE user_id = ?
                        ORDER BY provider_id, athlete_id
                        """,
                this::mapConnection,
                userId
        );
    }

    @Override
    public List<ProviderConnection> findAll() {
        return jdbcTemplate.query(
                """
                        SELECT provider_id,
                               athlete_id,
                               athlete_username,
                               access_token,
                               refresh_token,
                               expires_at,
                               sync_enabled,
                               sync_interval,
                               last_sync_cursor,
                               last_synced_at,
                               user_id
                        FROM provider_connections
                        ORDER BY provider_id, athlete_id
                        """,
                this::mapConnection
        );
    }

    private ProviderConnection mapConnection(ResultSet rs, int rowNum) throws SQLException {
        return new ProviderConnection(
                rs.getString("provider_id"),
                new AthleteProfile(rs.getString("athlete_id"), rs.getString("athlete_username")),
                rs.getString("access_token"),
                rs.getString("refresh_token"),
                toInstant(rs.getString("expires_at")),
                new SyncPreferences(rs.getBoolean("sync_enabled"), SyncInterval.valueOf(rs.getString("sync_interval"))),
                toCursor(rs.getString("last_sync_cursor")),
                toInstant(rs.getString("last_synced_at")),
                rs.getString("user_id")
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
