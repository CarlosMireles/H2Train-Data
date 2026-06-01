package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.AuthenticationRequiredException;
import com.h2traindata.application.exception.ProviderConnectionAlreadyLinkedException;
import com.h2traindata.application.exception.UserAccountNotFoundException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.ProviderCatalog;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.ProviderConnection;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HandleAuthorizationCallbackUseCase {

    private final ProviderCatalog providerCatalog;
    private final ConnectionRepository connectionRepository;
    private final UserAccountRepository userAccountRepository;
    private final AccountEventPublisher accountEventPublisher;

    public HandleAuthorizationCallbackUseCase(ProviderCatalog providerCatalog,
                                              ConnectionRepository connectionRepository,
                                              UserAccountRepository userAccountRepository,
                                              AccountEventPublisher accountEventPublisher) {
        this.providerCatalog = providerCatalog;
        this.connectionRepository = connectionRepository;
        this.userAccountRepository = userAccountRepository;
        this.accountEventPublisher = accountEventPublisher;
    }

    @Transactional
    public ProviderConnection execute(String providerId, String code, String userId) {
        ProviderConnection connection = providerCatalog.connector(providerId).connect(code);
        InternalUserAccount userAccount = resolveUserAccount(userId);
        Optional<ProviderConnection> existingConnection = connectionRepository.findByProviderAndAthlete(
                connection.providerId(),
                connection.athlete().id()
        );
        ensureCanLink(existingConnection, userAccount.id(), connection.providerId(), connection.athlete().id());

        ProviderConnection linkedConnection = linkToUser(connection, existingConnection, userAccount.id());
        connectionRepository.save(linkedConnection);
        InternalUserAccount updatedUserAccount = userAccount.withProvider(providerId);
        userAccountRepository.save(updatedUserAccount);
        accountEventPublisher.publishProviderAccountSynced(updatedUserAccount, linkedConnection);
        return linkedConnection;
    }

    private void ensureCanLink(Optional<ProviderConnection> existingConnection,
                               String userId,
                               String providerId,
                               String athleteId) {
        existingConnection
                .map(ProviderConnection::userId)
                .filter(StringUtils::hasText)
                .filter(existingUserId -> !existingUserId.equals(userId))
                .ifPresent(existingUserId -> {
                    throw new ProviderConnectionAlreadyLinkedException(providerId, athleteId);
                });
    }

    private ProviderConnection linkToUser(ProviderConnection connection,
                                          Optional<ProviderConnection> existingConnection,
                                          String userId) {
        return existingConnection
                .map(existing -> new ProviderConnection(
                        connection.providerId(),
                        connection.athlete(),
                        connection.accessToken(),
                        connection.refreshToken(),
                        connection.expiresAt(),
                        existing.syncPreferences(),
                        existing.lastSyncCursor(),
                        existing.lastSyncedAt(),
                        userId
                ))
                .orElseGet(() -> connection.withUserId(userId));
    }

    private InternalUserAccount resolveUserAccount(String requestedUserId) {
        if (!StringUtils.hasText(requestedUserId)) {
            throw new AuthenticationRequiredException();
        }
        return userAccountRepository.findById(requestedUserId)
                .orElseThrow(() -> new UserAccountNotFoundException(requestedUserId));
    }
}
