package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.PasswordResetTokenRepository;
import com.h2traindata.domain.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "type", havingValue = "jdbc", matchIfMissing = true)
public class JdbcPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPasswordResetTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        int updatedRows = jdbcTemplate.update(
                """
                        UPDATE password_reset_tokens
                        SET user_id = ?,
                            email = ?,
                            expires_at = ?,
                            created_at = ?,
                            used_at = ?
                        WHERE token_hash = ?
                        """,
                passwordResetToken.userId(),
                passwordResetToken.email(),
                passwordResetToken.expiresAt().toString(),
                passwordResetToken.createdAt().toString(),
                instantToString(passwordResetToken.usedAt()),
                passwordResetToken.tokenHash()
        );
        if (updatedRows == 0) {
            jdbcTemplate.update(
                    """
                            INSERT INTO password_reset_tokens (
                                token_hash,
                                user_id,
                                email,
                                expires_at,
                                created_at,
                                used_at
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    passwordResetToken.tokenHash(),
                    passwordResetToken.userId(),
                    passwordResetToken.email(),
                    passwordResetToken.expiresAt().toString(),
                    passwordResetToken.createdAt().toString(),
                    instantToString(passwordResetToken.usedAt())
            );
        }
        return passwordResetToken;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jdbcTemplate.query(
                """
                        SELECT token_hash,
                               user_id,
                               email,
                               expires_at,
                               created_at,
                               used_at
                        FROM password_reset_tokens
                        WHERE token_hash = ?
                        """,
                (rs, rowNum) -> new PasswordResetToken(
                        rs.getString("token_hash"),
                        rs.getString("user_id"),
                        rs.getString("email"),
                        Instant.parse(rs.getString("expires_at")),
                        Instant.parse(rs.getString("created_at")),
                        stringToInstant(rs.getString("used_at"))
                ),
                tokenHash
        ).stream().findFirst();
    }

    private String instantToString(Instant value) {
        return value == null ? null : value.toString();
    }

    private Instant stringToInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
