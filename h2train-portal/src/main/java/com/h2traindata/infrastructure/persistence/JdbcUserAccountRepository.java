package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.domain.InternalUserAccount;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "type", havingValue = "jdbc", matchIfMissing = true)
public class JdbcUserAccountRepository implements UserAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public InternalUserAccount save(InternalUserAccount userAccount) {
        int updatedRows = jdbcTemplate.update(
                """
                        UPDATE user_accounts
                        SET email = ?,
                            username = ?,
                            password_hash = ?,
                            provider_ids = ?,
                            created_at = ?
                        WHERE id = ?
                        """,
                userAccount.email(),
                userAccount.username(),
                userAccount.passwordHash(),
                fromProviderIds(userAccount.providerIds()),
                userAccount.createdAt().toString(),
                userAccount.id()
        );
        if (updatedRows == 0) {
            jdbcTemplate.update(
                    """
                            INSERT INTO user_accounts (
                                id,
                                email,
                                username,
                                password_hash,
                                provider_ids,
                                created_at
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    userAccount.id(),
                    userAccount.email(),
                    userAccount.username(),
                    userAccount.passwordHash(),
                    fromProviderIds(userAccount.providerIds()),
                    userAccount.createdAt().toString()
            );
        }
        return userAccount;
    }

    @Override
    public Optional<InternalUserAccount> findById(String userId) {
        return jdbcTemplate.query(
                """
                        SELECT id,
                               email,
                               username,
                               password_hash,
                               provider_ids,
                               created_at
                        FROM user_accounts
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new InternalUserAccount(
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        toProviderIds(rs.getString("provider_ids")),
                        Instant.parse(rs.getString("created_at"))
                ),
                userId
        ).stream().findFirst();
    }

    @Override
    public Optional<InternalUserAccount> findByEmail(String email) {
        return findByUniqueField("email", email);
    }

    @Override
    public Optional<InternalUserAccount> findByUsername(String username) {
        return findByUniqueField("username", username);
    }

    private Optional<InternalUserAccount> findByUniqueField(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                """
                        SELECT id,
                               email,
                               username,
                               password_hash,
                               provider_ids,
                               created_at
                        FROM user_accounts
                        WHERE LOWER(%s) = LOWER(?)
                        """.formatted(fieldName),
                (rs, rowNum) -> new InternalUserAccount(
                        rs.getString("id"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        toProviderIds(rs.getString("provider_ids")),
                        Instant.parse(rs.getString("created_at"))
                ),
                value.trim()
        ).stream().findFirst();
    }

    private String fromProviderIds(Set<String> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) {
            return "";
        }
        return String.join(",", providerIds);
    }

    private Set<String> toProviderIds(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
