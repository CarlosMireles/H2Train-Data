package com.h2traindata.application.port.out;

import com.h2traindata.domain.RememberMeToken;
import java.util.Optional;

public interface RememberMeTokenRepository {

    RememberMeToken save(RememberMeToken rememberMeToken);

    Optional<RememberMeToken> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);
}
