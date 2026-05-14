package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.domain.InternalUserAccount;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
                "UPDATE user_accounts SET created_at = ? WHERE id = ?",
                userAccount.createdAt().toString(),
                userAccount.id()
        );
        if (updatedRows == 0) {
            jdbcTemplate.update(
                    "INSERT INTO user_accounts (id, created_at) VALUES (?, ?)",
                    userAccount.id(),
                    userAccount.createdAt().toString()
            );
        }
        return userAccount;
    }

    @Override
    public Optional<InternalUserAccount> findById(String userId) {
        return jdbcTemplate.query(
                "SELECT id, created_at FROM user_accounts WHERE id = ?",
                (rs, rowNum) -> new InternalUserAccount(
                        rs.getString("id"),
                        Instant.parse(rs.getString("created_at"))
                ),
                userId
        ).stream().findFirst();
    }
}
