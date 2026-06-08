package com.h2traindata.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.PasswordResetToken;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.RememberMeToken;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.domain.SyncInterval;
import com.h2traindata.domain.SyncPreferences;
import com.h2traindata.domain.SyncState;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

class JdbcPersistenceRepositoryTest {

    private final EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("schema.sql")
            .build();
    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(database);
    private final JdbcUserAccountRepository userAccountRepository = new JdbcUserAccountRepository(jdbcTemplate);
    private final JdbcConnectionRepository connectionRepository = new JdbcConnectionRepository(jdbcTemplate);
    private final JdbcSyncStateRepository syncStateRepository = new JdbcSyncStateRepository(jdbcTemplate);
    private final JdbcPasswordResetTokenRepository passwordResetTokenRepository =
            new JdbcPasswordResetTokenRepository(jdbcTemplate);
    private final JdbcRememberMeTokenRepository rememberMeTokenRepository =
            new JdbcRememberMeTokenRepository(jdbcTemplate);

    @AfterEach
    void shutdownDatabase() {
        database.shutdown();
    }

    @Test
    void persistsUserConnectionPreferencesAndSyncState() {
        userAccountRepository.save(new InternalUserAccount("internal-user-1", Instant.parse("2026-04-01T10:00:00Z")));

        ProviderConnection connection = new ProviderConnection(
                "strava",
                new AthleteProfile("1143069702", "runner"),
                "access-token",
                "refresh-token",
                Instant.parse("2026-04-02T10:00:00Z"),
                new SyncPreferences(false, SyncInterval.EVERY_5_HOURS),
                new SyncCursor("cursor-1"),
                Instant.parse("2026-04-03T10:00:00Z"),
                "internal-user-1"
        );
        connectionRepository.save(connection);

        SyncState syncState = new SyncState(
                "strava",
                "1143069702",
                EventType.ACTIVITY,
                new SyncCursor("cursor-2"),
                Instant.parse("2026-04-04T10:00:00Z")
        );
        syncStateRepository.save(syncState);

        ProviderConnection storedConnection = connectionRepository
                .findByProviderAndAthlete("strava", "1143069702")
                .orElseThrow();
        SyncState storedSyncState = syncStateRepository
                .findByProviderAndAthleteAndEventType("strava", "1143069702", EventType.ACTIVITY)
                .orElseThrow();

        assertEquals("internal-user-1", storedConnection.userId());
        assertEquals(false, storedConnection.syncPreferences().enabled());
        assertEquals(SyncInterval.EVERY_5_HOURS, storedConnection.syncPreferences().interval());
        assertEquals("cursor-1", storedConnection.lastSyncCursor().value());
        assertEquals("cursor-2", storedSyncState.lastCursor().value());
        assertTrue(connectionRepository.findAll().contains(storedConnection));
        assertTrue(userAccountRepository.findById("internal-user-1").isPresent());
    }

    @Test
    void persistsPasswordResetTokens() {
        PasswordResetToken token = new PasswordResetToken(
                "token-hash",
                "internal-user-1",
                "runner@example.com",
                Instant.parse("2026-06-08T10:30:00Z"),
                Instant.parse("2026-06-08T10:00:00Z"),
                null
        );

        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.save(token.markUsed(Instant.parse("2026-06-08T10:05:00Z")));

        PasswordResetToken stored = passwordResetTokenRepository.findByTokenHash("token-hash").orElseThrow();
        assertEquals("internal-user-1", stored.userId());
        assertEquals("runner@example.com", stored.email());
        assertEquals(Instant.parse("2026-06-08T10:05:00Z"), stored.usedAt());
    }

    @Test
    void persistsRememberMeTokens() {
        RememberMeToken token = new RememberMeToken(
                "remember-hash",
                "internal-user-1",
                Instant.parse("2026-07-08T10:00:00Z"),
                Instant.parse("2026-06-08T10:00:00Z"),
                Instant.parse("2026-06-08T10:00:00Z")
        );

        rememberMeTokenRepository.save(token);
        rememberMeTokenRepository.save(token.markUsed(Instant.parse("2026-06-08T11:00:00Z")));

        RememberMeToken stored = rememberMeTokenRepository.findByTokenHash("remember-hash").orElseThrow();
        assertEquals("internal-user-1", stored.userId());
        assertEquals(Instant.parse("2026-07-08T10:00:00Z"), stored.expiresAt());
        assertEquals(Instant.parse("2026-06-08T11:00:00Z"), stored.lastUsedAt());

        rememberMeTokenRepository.deleteByTokenHash("remember-hash");
        assertTrue(rememberMeTokenRepository.findByTokenHash("remember-hash").isEmpty());
    }
}
