package com.h2traindata.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import com.h2traindata.bus.EventPublisher;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventPublication;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.privacy.RuleBasedSensitiveDataAnonymizer;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AccountEventPublisherTest {

    private final EventPublisher eventPublisher = Mockito.mock(EventPublisher.class);
    private final AccountEventPublisher accountEventPublisher = new AccountEventPublisher(
            eventPublisher,
            new RuleBasedSensitiveDataAnonymizer()
    );

    @Test
    void publishesUserAccountEventForRegistrations() {
        InternalUserAccount userAccount = new InternalUserAccount(
                "internal-user-1",
                "runner@example.com",
                "runner",
                "hash",
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        );

        accountEventPublisher.publishUserRegistered(userAccount, "password");

        EventPublication publication = capturedPublication();
        assertEquals("internal-user-1", publication.userId());
        assertEquals("h2train", publication.event().providerId());
        assertEquals(EventType.USER_ACCOUNT, publication.event().eventType());
        assertEquals("user_registered", publication.event().eventName());
        assertEquals("internal-user-1", publication.event().field("accountId"));
        assertEquals(RuleBasedSensitiveDataAnonymizer.ANONYMIZED_VALUE, publication.event().field("email"));
        assertEquals(RuleBasedSensitiveDataAnonymizer.ANONYMIZED_VALUE, publication.event().field("username"));
        assertEquals("password", publication.event().field("authProvider"));
        assertNotNull(publication.event().eventId());
    }

    @Test
    void publishesAccountSyncEventWhenProviderIsLinked() {
        InternalUserAccount userAccount = new InternalUserAccount(
                "internal-user-1",
                "runner@example.com",
                "runner",
                "hash",
                Set.of("strava"),
                Instant.parse("2026-04-01T10:00:00Z")
        );
        ProviderConnection connection = new ProviderConnection(
                "strava",
                new AthleteProfile("athlete-7", "strava-runner"),
                "access-token",
                "refresh-token",
                Instant.parse("2026-04-10T10:00:00Z")
        ).withUserId("internal-user-1");

        accountEventPublisher.publishProviderAccountSynced(userAccount, connection);

        EventPublication publication = capturedPublication();
        assertEquals("internal-user-1", publication.userId());
        assertEquals("h2train", publication.event().providerId());
        assertEquals(EventType.ACCOUNT_SYNC, publication.event().eventType());
        assertEquals("provider_account_synced", publication.event().eventName());
        assertEquals("strava", publication.event().field("linkedProviderId"));
        assertEquals(RuleBasedSensitiveDataAnonymizer.ANONYMIZED_VALUE, publication.event().field("providerAthleteId"));
        assertEquals(RuleBasedSensitiveDataAnonymizer.ANONYMIZED_VALUE, publication.event().field("providerAthleteUsername"));
    }

    private EventPublication capturedPublication() {
        ArgumentCaptor<EventPublication> captor = ArgumentCaptor.forClass(EventPublication.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }
}
