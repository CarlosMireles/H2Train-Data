package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.InvalidCredentialsException;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.application.service.PasswordHashService;
import com.h2traindata.domain.InternalUserAccount;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthenticateUserAccountUseCase {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService;
    private final AccountEventPublisher accountEventPublisher;

    public AuthenticateUserAccountUseCase(UserAccountRepository userAccountRepository,
                                          PasswordHashService passwordHashService,
                                          AccountEventPublisher accountEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHashService = passwordHashService;
        this.accountEventPublisher = accountEventPublisher;
    }

    public InternalUserAccount execute(String login, String password) {
        if (!StringUtils.hasText(login) || !StringUtils.hasText(password)) {
            throw new InvalidCredentialsException();
        }

        InternalUserAccount userAccount = findByLogin(login.trim())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHashService.matches(password, userAccount.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        accountEventPublisher.publishUserLoggedIn(userAccount, "password");
        return userAccount;
    }

    private Optional<InternalUserAccount> findByLogin(String login) {
        Optional<InternalUserAccount> byEmail = userAccountRepository.findByEmail(login);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        return userAccountRepository.findByUsername(login);
    }
}
