package com.h2traindata.application.port.out;

import com.h2traindata.domain.InternalUserAccount;
import java.util.Optional;

public interface UserAccountRepository {

    InternalUserAccount save(InternalUserAccount userAccount);

    Optional<InternalUserAccount> findById(String userId);

    Optional<InternalUserAccount> findByEmail(String email);

    Optional<InternalUserAccount> findByUsername(String username);
}
