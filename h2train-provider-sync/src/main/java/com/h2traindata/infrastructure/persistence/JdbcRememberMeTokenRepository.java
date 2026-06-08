package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.RememberMeTokenRepository;
import com.h2traindata.domain.RememberMeToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "type", havingValue = "jdbc", matchIfMissing = true)
public class JdbcRememberMeTokenRepository implements RememberMeTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRememberMeTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RememberMeToken save(RememberMeToken rememberMeToken) {
        int updatedRows = jdbcTemplate.update(
                """
                        UPDATE remember_me_tokens
                        SET user_id = ?,
                            expires_at = ?,
                            created_at = ?,
                            last_used_at = ?
                        WHERE token_hash = ?
                        """,
                rememberMeToken.userId(),
                rememberMeToken.expiresAt().toString(),
                rememberMeToken.createdAt().toString(),
                instantToString(rememberMeToken.lastUsedAt()),
                rememberMeToken.tokenHash()
        );
        if (updatedRows == 0) {
            jdbcTemplate.update(
                    """
                            INSERT INTO remember_me_tokens (
                                token_hash,
                                user_id,
                                expires_at,
                                created_at,
                                last_used_at
                            ) VALUES (?, ?, ?, ?, ?)
                            """,
                    rememberMeToken.tokenHash(),
                    rememberMeToken.userId(),
                    rememberMeToken.expiresAt().toString(),
                    rememberMeToken.createdAt().toString(),
                    instantToString(rememberMeToken.lastUsedAt())
            );
        }
        return rememberMeToken;
    }

    @Override
    public Optional<RememberMeToken> findByTokenHash(String tokenHash) {
        return jdbcTemplate.query(
                """
                        SELECT token_hash,
                               user_id,
                               expires_at,
                               created_at,
                               last_used_at
                        FROM remember_me_tokens
                        WHERE token_hash = ?
                        """,
                (rs, rowNum) -> new RememberMeToken(
                        rs.getString("token_hash"),
                        rs.getString("user_id"),
                        Instant.parse(rs.getString("expires_at")),
                        Instant.parse(rs.getString("created_at")),
                        stringToInstant(rs.getString("last_used_at"))
                ),
                tokenHash
        ).stream().findFirst();
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        jdbcTemplate.update("DELETE FROM remember_me_tokens WHERE token_hash = ?", tokenHash);
    }

    private String instantToString(Instant value) {
        return value == null ? null : value.toString();
    }

    private Instant stringToInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
