package com.github.mattoyudzuru.terminalbang.game;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-18T20:00:00Z"), ZoneOffset.UTC);

    @Test
    void startsGameWithSheriffCurrentAndInitialHands() {
        GameEngine engine = newGame(4);
        GameState state = engine.state();

        assertEquals(GamePhase.PLAY, state.phase());
        assertEquals(Role.SHERIFF, state.currentPlayer().role());
        assertEquals(4, state.players().size());
        assertEquals(5, state.currentPlayer().maxHealth());
        assertEquals(5, state.currentPlayer().handSize());
        assertTrue(state.currentTurnDrawn());
    }

    @Test
    void createsPendingShotReactionAndAllowsDodge() {
        GameEngine engine = newGame(4);
        PlayerState shooter = engine.state().currentPlayer();
        PlayerState target = adjacentTarget(engine.state());
        int healthBefore = target.health();
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.SHOT)));
        target.addCard(CardInstance.create(StandardContent.card(CardKind.DODGE)));

        engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()));

        assertTrue(engine.state().pendingAction().isPresent());
        assertEquals(PendingActionType.SHOT_REACTION, engine.state().pendingAction().orElseThrow().type());

        engine.resolvePending(target.accountId(), true);

        assertFalse(engine.state().pendingAction().isPresent());
        assertEquals(healthBefore, target.health());
    }

    @Test
    void failedShotReactionDealsDamage() {
        GameEngine engine = newGame(4);
        PlayerState shooter = engine.state().currentPlayer();
        PlayerState target = adjacentTarget(engine.state());
        int healthBefore = target.health();
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.SHOT)));

        engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()));
        engine.resolvePending(target.accountId(), false);

        assertEquals(healthBefore - 1, target.health());
        assertEquals(1, shooter.damageDealt());
        assertEquals(1, target.damageTaken());
    }

    @Test
    void endTurnRequiresDiscardDownToCurrentHealth() {
        GameEngine engine = newGame(4);
        PlayerState current = engine.state().currentPlayer();
        while (current.handSize() <= current.health()) {
            current.addCard(CardInstance.create(StandardContent.card(CardKind.TRAIL_RIDE)));
        }

        engine.endTurn(current.accountId());

        assertEquals(GamePhase.DISCARD, engine.state().phase());

        while (engine.state().phase() == GamePhase.DISCARD) {
            engine.discardForLimit(current.accountId(), 0);
        }

        assertEquals(GamePhase.PLAY, engine.state().phase());
        assertNotEquals(current.accountId(), engine.state().currentPlayer().accountId());
    }

    @Test
    void disconnectedCurrentPlayerGetsDeadlineAndCanBeSkipped() {
        GameEngine engine = newGame(4);
        PlayerState current = engine.state().currentPlayer();
        Instant disconnectedAt = Instant.parse("2026-05-18T20:00:05Z");

        engine.disconnect(current.accountId(), disconnectedAt);

        assertTrue(engine.state().currentTurnDeadline().isPresent());
        assertFalse(engine.skipTimedOutTurn(disconnectedAt.plusSeconds(59)));
        assertTrue(engine.skipTimedOutTurn(disconnectedAt.plusSeconds(60)));
        assertNotEquals(current.accountId(), engine.state().currentPlayer().accountId());
    }

    private GameEngine newGame(int players) {
        return GameEngine.start(UUID.randomUUID(), seeds(players), RandomSource.seeded(3), clock);
    }

    private static PlayerState adjacentTarget(GameState state) {
        List<PlayerState> players = state.players();
        int current = state.currentPlayerIndex();
        return players.get((current + 1) % players.size());
    }

    private static List<PlayerSeed> seeds(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> new PlayerSeed(UUID.randomUUID(), "P" + index))
                .toList();
    }
}

