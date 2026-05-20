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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertTrue(state.currentPlayer().handSize() >= state.currentPlayer().maxHealth() + 2);
        assertTrue(state.currentTurnDrawn());
    }

    @Test
    void standardDeckHasOfficialBaseCounts() {
        List<CardInstance> deck = StandardContent.deck();

        assertEquals(80, deck.size());
        assertEquals(25, count(deck, CardKind.BANG));
        assertEquals(12, count(deck, CardKind.MISSED));
        assertEquals(6, count(deck, CardKind.BEER));
        assertEquals(3, count(deck, CardKind.JAIL));
        assertEquals(1, count(deck, CardKind.GATLING));
        assertEquals(2, count(deck, CardKind.INDIANS));
        assertEquals(1, count(deck, CardKind.WELLS_FARGO));
    }

    @Test
    void createsPendingBangReactionAndAllowsMissed() {
        GameEngine engine = newGame(4);
        PlayerState shooter = engine.state().currentPlayer();
        PlayerState target = adjacentTarget(engine.state());
        int healthBefore = target.health();
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));
        target.addCard(CardInstance.create(StandardContent.card(CardKind.MISSED)));

        engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()));

        assertTrue(engine.state().pendingAction().isPresent());
        assertEquals(PendingActionType.BANG_REACTION, engine.state().pendingAction().orElseThrow().type());

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
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));

        engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()));
        engine.resolvePending(target.accountId(), false);

        assertEquals(healthBefore - 1, target.health());
        assertEquals(1, shooter.damageDealt());
        assertEquals(1, target.damageTaken());
    }

    @Test
    void barrelCanCancelBangOnHeartDraw() {
        GameEngine engine = newGame(4);
        PlayerState shooter = engine.state().currentPlayer();
        PlayerState target = adjacentTarget(engine.state());
        int healthBefore = target.health();
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));
        target.addInPlay(CardInstance.create(StandardContent.card(CardKind.BARREL)));
        engine.state().drawPile().push(CardInstance.create(
                StandardContent.card(CardKind.BEER),
                CardSuit.HEARTS,
                CardRank.FIVE
        ));

        engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()));
        engine.resolvePending(target.accountId(), true);

        assertFalse(engine.state().pendingAction().isPresent());
        assertEquals(healthBefore, target.health());
    }

    @Test
    void duelConsumesBangOnlyWhenPlayerAnswers() {
        GameEngine engine = newGame(4);
        PlayerState actor = engine.state().currentPlayer();
        PlayerState target = adjacentTarget(engine.state());
        actor.addCard(CardInstance.create(StandardContent.card(CardKind.DUEL)));
        actor.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));
        target.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));
        int actorBangBefore = countHand(actor, CardKind.BANG);
        int targetBangBefore = countHand(target, CardKind.BANG);

        engine.playCard(actor.accountId(), actor.handSize() - 2, Optional.of(target.accountId()));

        assertEquals(actorBangBefore, countHand(actor, CardKind.BANG));
        assertEquals(PendingActionType.DUEL_RESPONSE, engine.state().pendingAction().orElseThrow().type());

        engine.resolvePending(target.accountId(), true);

        assertEquals(targetBangBefore - 1, countHand(target, CardKind.BANG));
        assertEquals(actor.accountId(), engine.state().pendingAction().orElseThrow().expectedAccountId());

        engine.resolvePending(actor.accountId(), true);

        assertEquals(actorBangBefore - 1, countHand(actor, CardKind.BANG));
    }

    @Test
    void bangCardsAreLimitedUnlessWeaponOrCharacterAllowsMore() {
        GameEngine engine = newGameWithCurrentNot("willy_the_kid");
        PlayerState shooter = engine.state().currentPlayer();
        PlayerState target = adjacentTarget(engine.state());
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));
        target.addCard(CardInstance.create(StandardContent.card(CardKind.MISSED)));

        engine.playCard(shooter.accountId(), shooter.handSize() - 2, Optional.of(target.accountId()));
        engine.resolvePending(target.accountId(), true);

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()))
        );

        shooter.addInPlay(CardInstance.create(StandardContent.card(CardKind.VOLCANIC)));
        engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()));
        assertTrue(engine.state().pendingAction().isPresent());
    }

    @Test
    void weaponRangeAllowsFarBangTargets() {
        GameEngine engine = newGameWithDistanceTwoTarget();
        PlayerState shooter = engine.state().currentPlayer();
        PlayerState target = farTarget(engine.state());
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()))
        );

        shooter.addInPlay(CardInstance.create(StandardContent.card(CardKind.SCHOFIELD)));
        engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()));

        assertEquals(PendingActionType.BANG_REACTION, engine.state().pendingAction().orElseThrow().type());
    }

    @Test
    void beerCanSaveFromLethalDamageBeforeElimination() {
        GameEngine engine = newGame(4);
        PlayerState shooter = engine.state().currentPlayer();
        PlayerState target = adjacentTarget(engine.state());
        target.takeDamage(target.health() - 1);
        shooter.addCard(CardInstance.create(StandardContent.card(CardKind.BANG)));
        target.addCard(CardInstance.create(StandardContent.card(CardKind.BEER)));

        engine.playCard(shooter.accountId(), shooter.handSize() - 1, Optional.of(target.accountId()));
        engine.resolvePending(target.accountId(), false);

        assertFalse(target.eliminated());
        assertEquals(1, target.health());
    }

    @Test
    void beerAtFullHealthIsRejectedWithoutSpendingCard() {
        GameEngine engine = newGame(4);
        PlayerState actor = engine.state().currentPlayer();
        actor.addCard(CardInstance.create(StandardContent.card(CardKind.BEER)));
        int handBefore = actor.handSize();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.playCard(actor.accountId(), actor.handSize() - 1, Optional.empty())
        );
        assertEquals(handBefore, actor.handSize());
    }

    @Test
    void jailCannotBePlayedOnSheriff() {
        GameEngine engine = newGame(4);
        PlayerState sheriff = engine.state().currentPlayer();
        engine.endTurn(sheriff.accountId());
        while (engine.state().phase() == GamePhase.DISCARD) {
            engine.discardForLimit(sheriff.accountId(), 0);
        }
        PlayerState actor = engine.state().currentPlayer();
        actor.addCard(CardInstance.create(StandardContent.card(CardKind.JAIL)));

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.playCard(actor.accountId(), actor.handSize() - 1, Optional.of(sheriff.accountId()))
        );
    }

    @Test
    void gatlingCreatesMassMissedReaction() {
        GameEngine engine = newGame(4);
        PlayerState actor = engine.state().currentPlayer();
        actor.addCard(CardInstance.create(StandardContent.card(CardKind.GATLING)));

        engine.playCard(actor.accountId(), actor.handSize() - 1, Optional.empty());

        assertEquals(PendingActionType.GATLING_REACTION, engine.state().pendingAction().orElseThrow().type());
        assertEquals(CardKind.MISSED, engine.state().pendingAction().orElseThrow().responseKind());
    }

    @Test
    void generalStoreWaitsForPlayersToChooseCards() {
        GameEngine engine = newGame(4);
        PlayerState actor = engine.state().currentPlayer();
        actor.addCard(CardInstance.create(StandardContent.card(CardKind.GENERAL_STORE)));

        engine.playCard(actor.accountId(), actor.handSize() - 1, Optional.empty());

        PendingAction firstPick = engine.state().pendingAction().orElseThrow();
        assertEquals(PendingActionType.GENERAL_STORE_PICK, firstPick.type());
        assertEquals(actor.accountId(), firstPick.expectedAccountId());
        assertEquals(engine.state().alivePlayers().size(), firstPick.choiceCards().size());

        engine.choosePendingCard(actor.accountId(), 0);

        PendingAction secondPick = engine.state().pendingAction().orElseThrow();
        assertEquals(PendingActionType.GENERAL_STORE_PICK, secondPick.type());
        assertNotEquals(actor.accountId(), secondPick.expectedAccountId());
        assertEquals(firstPick.choiceCards().size() - 1, secondPick.choiceCards().size());
    }

    @Test
    void endTurnRequiresDiscardDownToCurrentHealth() {
        GameEngine engine = newGame(4);
        PlayerState current = engine.state().currentPlayer();
        while (current.handSize() <= current.health()) {
            current.addCard(CardInstance.create(StandardContent.card(CardKind.STAGECOACH)));
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

    private GameEngine newGameWithCurrentNot(String characterId) {
        for (long seed = 1; seed < 200; seed++) {
            GameEngine engine = GameEngine.start(UUID.randomUUID(), seeds(4), RandomSource.seeded(seed), clock);
            if (!engine.state().currentPlayer().character().id().equals(characterId)) {
                return engine;
            }
        }
        throw new IllegalStateException("Could not find a matching game");
    }

    private GameEngine newGameWithDistanceTwoTarget() {
        for (long seed = 1; seed < 200; seed++) {
            GameEngine engine = GameEngine.start(UUID.randomUUID(), seeds(4), RandomSource.seeded(seed), clock);
            PlayerState shooter = engine.state().currentPlayer();
            PlayerState target = farTarget(engine.state());
            if (DistanceCalculator.distance(engine.state(), shooter.accountId(), target.accountId()) == 2) {
                return engine;
            }
        }
        throw new IllegalStateException("Could not find a distance-two game");
    }

    private static long count(List<CardInstance> deck, CardKind kind) {
        return deck.stream().filter(card -> card.kind() == kind).count();
    }

    private static int countHand(PlayerState player, CardKind kind) {
        return (int) player.hand().stream()
                .filter(card -> card.kind() == kind)
                .count();
    }

    private static PlayerState adjacentTarget(GameState state) {
        List<PlayerState> players = state.players();
        int current = state.currentPlayerIndex();
        return players.get((current + 1) % players.size());
    }

    private static PlayerState farTarget(GameState state) {
        List<PlayerState> players = state.players();
        int current = state.currentPlayerIndex();
        return players.get((current + 2) % players.size());
    }

    private static List<PlayerSeed> seeds(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> new PlayerSeed(UUID.randomUUID(), "P" + index))
                .toList();
    }
}
