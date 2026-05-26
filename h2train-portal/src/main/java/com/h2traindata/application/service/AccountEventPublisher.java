package com.h2traindata.application.service;

import com.h2traindata.bus.EventPublisher;
import com.h2traindata.domain.EventPublication;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.privacy.SensitiveDataAnonymizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AccountEventPublisher.class);
    private static final String PLATFORM_PROVIDER_ID = "h2train";
    private static final String SOURCE_SYSTEM = "h2train";

    private final EventPublisher eventPublisher;
    private final SensitiveDataAnonymizer sensitiveDataAnonymizer;

    public AccountEventPublisher(EventPublisher eventPublisher,
                                 SensitiveDataAnonymizer sensitiveDataAnonymizer) {
        this.eventPublisher = eventPublisher;
        this.sensitiveDataAnonymizer = sensitiveDataAnonymizer;
    }

    public void publishUserRegistered(InternalUserAccount userAccount, String authProvider) {
        publishUserAccountEvent(userAccount, "user_registered", authProvider);
    }

    public void publishUserLoggedIn(InternalUserAccount userAccount, String authProvider) {
        publishUserAccountEvent(userAccount, "user_logged_in", authProvider);
    }

    public void publishProviderAccountSynced(InternalUserAccount userAccount, ProviderConnection connection) {
        Map<String, Object> attributes = baseAccountAttributes(userAccount);
        attributes.put("linkedProviderId", connection.providerId());
        attributes.put("providerAthleteId", connection.athlete().id());
        attributes.put("providerAthleteUsername", connection.athlete().username());
        attributes.put("syncEnabled", connection.syncPreferences().enabled());
        attributes.put("syncInterval", connection.syncPreferences().interval().name());

        publish(userAccount.id(), new ProviderEvent(
                PLATFORM_PROVIDER_ID,
                null,
                EventType.ACCOUNT_SYNC,
                "provider_account_synced",
                UUID.randomUUID().toString(),
                Instant.now(),
                attributes
        ));
    }

    private void publishUserAccountEvent(InternalUserAccount userAccount, String eventName, String authProvider) {
        Map<String, Object> attributes = baseAccountAttributes(userAccount);
        attributes.put("authProvider", authProvider);

        publish(userAccount.id(), new ProviderEvent(
                PLATFORM_PROVIDER_ID,
                null,
                EventType.USER_ACCOUNT,
                eventName,
                UUID.randomUUID().toString(),
                Instant.now(),
                attributes
        ));
    }

    private Map<String, Object> baseAccountAttributes(InternalUserAccount userAccount) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("accountId", userAccount.id());
        attributes.put("email", userAccount.email());
        attributes.put("username", userAccount.username());
        attributes.put("providerIds", userAccount.providerIds());
        return attributes;
    }

    private void publish(String userId, ProviderEvent event) {
        try {
            eventPublisher.publish(sensitiveDataAnonymizer.anonymizePublication(new EventPublication(userId, event)));
        } catch (RuntimeException exception) {
            log.warn(
                    "Account event publication failed userId={} eventType={} eventName={}",
                    userId,
                    event.eventType(),
                    event.eventName(),
                    exception
            );
        }
    }
}
