package com.github.mattoyudzuru.terminalbang.stats;

import com.github.mattoyudzuru.terminalbang.game.WinningSide;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record MatchResult(
        UUID id,
        String roomCode,
        String status,
        Optional<WinningSide> winner,
        Instant startedAt,
        Instant finishedAt,
        List<PlayerMatchStat> players
) {
    public MatchResult {
        players = List.copyOf(players);
    }
}

