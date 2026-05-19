package com.github.mattoyudzuru.terminalbang.game;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistanceCalculatorTest {
    @Test
    void calculatesShortestDistanceAroundAlivePlayers() {
        GameEngine engine = GameEngine.start(
                UUID.randomUUID(),
                seeds(4),
                RandomSource.seeded(1),
                Clock.fixed(Instant.parse("2026-05-18T20:00:00Z"), ZoneOffset.UTC)
        );
        GameState state = engine.state();
        List<PlayerState> players = state.players();

        assertEquals(1, DistanceCalculator.distance(state, players.get(0).accountId(), players.get(1).accountId()));
        assertEquals(2, DistanceCalculator.distance(state, players.get(0).accountId(), players.get(2).accountId()));
        assertEquals(1, DistanceCalculator.distance(state, players.get(0).accountId(), players.get(3).accountId()));
    }

    @Test
    void appliesScopeAndMustangModifiers() {
        GameEngine engine = GameEngine.start(
                UUID.randomUUID(),
                seeds(4),
                RandomSource.seeded(1),
                Clock.fixed(Instant.parse("2026-05-18T20:00:00Z"), ZoneOffset.UTC)
        );
        GameState state = engine.state();
        List<PlayerState> players = state.players();
        PlayerState from = players.get(0);
        PlayerState target = players.get(2);

        assertEquals(2, DistanceCalculator.distance(state, from.accountId(), target.accountId()));

        from.addInPlay(CardInstance.create(StandardContent.card(CardKind.SCOPE)));
        assertEquals(1, DistanceCalculator.distance(state, from.accountId(), target.accountId()));

        target.addInPlay(CardInstance.create(StandardContent.card(CardKind.MUSTANG)));
        assertEquals(2, DistanceCalculator.distance(state, from.accountId(), target.accountId()));
    }

    private static List<PlayerSeed> seeds(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> new PlayerSeed(UUID.randomUUID(), "P" + index))
                .toList();
    }
}
