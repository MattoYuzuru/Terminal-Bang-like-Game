package com.github.mattoyudzuru.terminalbang.persistence;

import com.github.mattoyudzuru.terminalbang.stats.LeaderboardEntry;
import com.github.mattoyudzuru.terminalbang.stats.MatchResult;
import com.github.mattoyudzuru.terminalbang.stats.PlayerMatchStat;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcMatchResultRepository implements MatchResultRepository {
    private final DataSource dataSource;

    public JdbcMatchResultRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(MatchResult result) {
        String matchSql = """
                INSERT INTO match_results (id, room_code, status, winner, started_at, finished_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;
        String playerSql = """
                INSERT INTO player_match_stats (
                    match_id, account_id, nickname, role, won, eliminated, damage_dealt, damage_taken, cards_played
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (match_id, account_id) DO NOTHING
                """;
        try (var connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (var matchStatement = connection.prepareStatement(matchSql);
                 var playerStatement = connection.prepareStatement(playerSql)) {
                matchStatement.setObject(1, result.id());
                matchStatement.setString(2, result.roomCode());
                matchStatement.setString(3, result.status());
                matchStatement.setString(4, result.winner().map(Enum::name).orElse(null));
                matchStatement.setObject(5, result.startedAt());
                matchStatement.setObject(6, result.finishedAt());
                matchStatement.executeUpdate();

                for (PlayerMatchStat player : result.players()) {
                    playerStatement.setObject(1, result.id());
                    playerStatement.setObject(2, player.accountId());
                    playerStatement.setString(3, player.nickname());
                    playerStatement.setString(4, player.role().name());
                    playerStatement.setBoolean(5, player.won());
                    playerStatement.setBoolean(6, player.eliminated());
                    playerStatement.setInt(7, player.damageDealt());
                    playerStatement.setInt(8, player.damageTaken());
                    playerStatement.setInt(9, player.cardsPlayed());
                    playerStatement.addBatch();
                }
                playerStatement.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to save match result", exception);
        }
    }

    @Override
    public List<LeaderboardEntry> leaderboard(int limit) {
        String sql = """
                SELECT account_id, max(nickname) AS nickname, count(*) AS matches, sum(CASE WHEN won THEN 1 ELSE 0 END) AS wins
                FROM player_match_stats
                GROUP BY account_id
                ORDER BY wins DESC, matches DESC, nickname ASC
                LIMIT ?
                """;
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (var resultSet = statement.executeQuery()) {
                List<LeaderboardEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    entries.add(new LeaderboardEntry(
                            resultSet.getObject("account_id", java.util.UUID.class),
                            resultSet.getString("nickname"),
                            resultSet.getInt("matches"),
                            resultSet.getInt("wins")
                    ));
                }
                return entries;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to load leaderboard", exception);
        }
    }
}

