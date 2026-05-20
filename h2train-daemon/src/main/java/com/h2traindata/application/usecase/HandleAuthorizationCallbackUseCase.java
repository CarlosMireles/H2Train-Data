package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.AuthenticationRequiredException;
import com.h2traindata.application.exception.UserAccountNotFoundException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.ProviderConnection;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HandleAuthorizationCallbackUseCase {

    private final ProviderRegistry providerRegistry;
    private final ConnectionRepository connectionRepository;
    private final UserAccountRepository userAccountRepository;

    public HandleAuthorizationCallbackUseCase(ProviderRegistry providerRegistry,
                                              ConnectionRepository connectionRepository,
                                              UserAccountRepository userAccountRepository) {
        this.providerRegistry = providerRegistry;
        this.connectionRepository = connectionRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public ProviderConnection execute(String providerId, String code, String userId) {
        ProviderConnection connection = providerRegistry.connector(providerId).connect(code);
        InternalUserAccount userAccount = resolveUserAccount(userId);
        ProviderConnection linkedConnection = connection.withUserId(userAccount.id());
        connectionRepository.save(linkedConnection);
        userAccountRepository.save(userAccount.withProvider(providerId));
        return linkedConnection;
    }

    private InternalUserAccount resolveUserAccount(String requestedUserId) {
        if (!StringUtils.hasText(requestedUserId)) {
            throw new AuthenticationRequiredException();
        }
        return userAccountRepository.findById(requestedUserId)
                .orElseThrow(() -> new UserAccountNotFoundException(requestedUserId));
    }
}
