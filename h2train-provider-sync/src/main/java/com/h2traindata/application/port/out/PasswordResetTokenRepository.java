package com.h2traindata.application.port.out;

import com.h2traindata.domain.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken passwordResetToken);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
