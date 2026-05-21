package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.UserAccountNotFoundException;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.domain.InternalUserAccount;
import org.springframework.stereotype.Service;

@Service
public class GetUserAccountUseCase {

    private final UserAccountRepository userAccountRepository;

    public GetUserAccountUseCase(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public InternalUserAccount execute(String userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserAccountNotFoundException(userId));
    }
}
