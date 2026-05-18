package com.github.mattoyudzuru.terminalbang.persistence;

import com.github.mattoyudzuru.terminalbang.user.Account;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class JdbcAccountRepository implements AccountRepository {
    private final DataSource dataSource;

    public JdbcAccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Account findOrCreateByFingerprint(String sshFingerprint, String defaultNickname) {
        return findByFingerprint(sshFingerprint).orElseGet(() -> insert(sshFingerprint, defaultNickname));
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        String sql = """
                SELECT id, ssh_fingerprint, nickname, created_at, updated_at
                FROM accounts
                WHERE id = ?
                """;
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to find account by id", exception);
        }
    }

    @Override
    public Account updateNickname(UUID accountId, String nickname) {
        String sql = """
                UPDATE accounts
                SET nickname = ?, updated_at = now()
                WHERE id = ?
                RETURNING id, ssh_fingerprint, nickname, created_at, updated_at
                """;
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, nickname);
            statement.setObject(2, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return map(resultSet);
                }
                throw new PersistenceException("Account does not exist: " + accountId);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to update account nickname", exception);
        }
    }

    private Optional<Account> findByFingerprint(String sshFingerprint) {
        String sql = """
                SELECT id, ssh_fingerprint, nickname, created_at, updated_at
                FROM accounts
                WHERE ssh_fingerprint = ?
                """;
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, sshFingerprint);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to find account by fingerprint", exception);
        }
    }

    private Account insert(String sshFingerprint, String nickname) {
        String sql = """
                INSERT INTO accounts (id, ssh_fingerprint, nickname)
                VALUES (?, ?, ?)
                RETURNING id, ssh_fingerprint, nickname, created_at, updated_at
                """;
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, sshFingerprint);
            statement.setString(3, nickname);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return map(resultSet);
            }
        } catch (SQLException exception) {
            Optional<Account> existing = findByFingerprint(sshFingerprint);
            if (existing.isPresent()) {
                return existing.orElseThrow();
            }
            throw new PersistenceException("Failed to create account", exception);
        }
    }

    private static Account map(ResultSet resultSet) throws SQLException {
        return new Account(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("ssh_fingerprint"),
                resultSet.getString("nickname"),
                toInstant(resultSet, "created_at"),
                toInstant(resultSet, "updated_at")
        );
    }

    private static Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp.toInstant();
    }
}

