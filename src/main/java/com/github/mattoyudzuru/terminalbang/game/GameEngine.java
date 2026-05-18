package com.github.mattoyudzuru.terminalbang.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GameEngine {
    public static final int MIN_PLAYERS = 4;
    public static final int MAX_PLAYERS = 7;
    public static final Duration DEFAULT_DISCONNECT_TIMEOUT = Duration.ofSeconds(60);

    private final GameState state;
    private final RandomSource random;
    private final Clock clock;

    private GameEngine(GameState state, RandomSource random, Clock clock) {
        this.state = state;
        this.random = random;
        this.clock = clock;
    }

    public static GameEngine start(UUID gameId, List<PlayerSeed> seats, RandomSource random, Clock clock) {
        if (seats.size() < MIN_PLAYERS || seats.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Game requires 4-7 players");
        }

        List<Role> roles = new ArrayList<>(RoleDistribution.forPlayerCount(seats.size()));
        random.shuffle(roles);

        List<CharacterDefinition> characters = new ArrayList<>(StandardContent.characters());
        random.shuffle(characters);

        List<PlayerState> players = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            PlayerSeed seed = seats.get(i);
            Role role = roles.get(i);
            CharacterDefinition character = characters.get(i % characters.size());
            int maxHealth = role == Role.SHERIFF ? character.maxHealth() + 1 : character.maxHealth();
            players.add(new PlayerState(seed.accountId(), seed.nickname(), role, character, maxHealth));
        }

        List<CardInstance> deck = StandardContent.deck();
        random.shuffle(deck);
        ArrayDeque<CardInstance> drawPile = new ArrayDeque<>(deck);
        ArrayDeque<CardInstance> discardPile = new ArrayDeque<>();
        GameState state = new GameState(gameId, Instant.now(clock), players, drawPile, discardPile);

        for (PlayerState player : players) {
            drawCards(state, player, player.maxHealth(), random);
        }

        int sheriffIndex = 0;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).role() == Role.SHERIFF) {
                sheriffIndex = i;
                break;
            }
        }
        state.setCurrentPlayerIndex(sheriffIndex);
        state.setPhase(GamePhase.PLAY);
        state.setCurrentTurnStartedAt(Instant.now(clock));
        state.setCurrentTurnDrawn(true);
        state.addLog(state.currentPlayer().nickname() + " starts as Sheriff.");

        return new GameEngine(state, random, clock);
    }

    public GameState state() {
        return state;
    }

    public void playCard(UUID actorAccountId, int handIndex, Optional<UUID> targetAccountId) {
        requireActive();
        requireNoPending();
        PlayerState actor = requireCurrentActor(actorAccountId);
        if (state.phase() != GamePhase.PLAY) {
            throw new IllegalStateException("Cards can only be played during play phase");
        }

        CardInstance card = actor.cardAt(handIndex);
        CardDefinition definition = card.definition();
        if (definition.reactionOnly()) {
            throw new IllegalArgumentException(definition.name() + " can only be used as a reaction");
        }
        if (definition.requiresTarget() && targetAccountId.isEmpty()) {
            throw new IllegalArgumentException(definition.name() + " requires a target");
        }
        if (!definition.requiresTarget() && targetAccountId.isPresent()) {
            throw new IllegalArgumentException(definition.name() + " does not accept a target");
        }

        Optional<PlayerState> maybeTarget = targetAccountId.map(this::requireAlivePlayer);
        maybeTarget.ifPresent(target -> validateTarget(actor, card.kind(), target));

        actor.removeCard(handIndex);
        actor.recordCardPlayed();
        discard(card);

        switch (card.kind()) {
            case SHOT -> playShot(actor, maybeTarget.orElseThrow());
            case SALOON -> {
                actor.heal(1);
                state.addLog(actor.nickname() + " restores one health.");
            }
            case TRAIL_RIDE -> {
                drawCards(state, actor, 2, random);
                state.addLog(actor.nickname() + " draws two cards.");
            }
            case DISARM -> discardRandomFromTarget(actor, maybeTarget.orElseThrow());
            case RUSTLE -> stealRandomFromTarget(actor, maybeTarget.orElseThrow());
            case STANDOFF -> startStandoff(actor, maybeTarget.orElseThrow());
            case DODGE -> throw new IllegalArgumentException("Dodge cannot be played directly");
        }

        checkWinConditions();
    }

    public void resolvePending(UUID actorAccountId, boolean playResponse) {
        requireActive();
        PendingAction pending = state.pendingAction()
                .orElseThrow(() -> new IllegalStateException("There is no pending action"));
        if (!pending.expectedAccountId().equals(actorAccountId)) {
            throw new IllegalArgumentException("Pending action belongs to another player");
        }

        PlayerState expected = requireAlivePlayer(pending.expectedAccountId());
        PlayerState opponent = requireAlivePlayer(pending.opponentAccountId());
        boolean answered = playResponse && discardFirst(expected, pending.responseKind());

        if (pending.type() == PendingActionType.SHOT_REACTION) {
            state.clearPendingAction();
            if (answered) {
                expected.recordCardPlayed();
                state.addLog(expected.nickname() + " dodges the shot.");
            } else {
                damage(opponent, expected, 1);
            }
            checkWinConditions();
            return;
        }

        if (answered) {
            expected.recordCardPlayed();
            state.setPendingAction(PendingAction.standoffResponse(
                    opponent.accountId(),
                    expected.accountId(),
                    opponent.nickname() + " must answer the standoff with Shot."
            ));
            state.addLog(expected.nickname() + " answers the standoff.");
        } else {
            state.clearPendingAction();
            damage(opponent, expected, 1);
            checkWinConditions();
        }
    }

    public void endTurn(UUID actorAccountId) {
        requireActive();
        requireNoPending();
        PlayerState actor = requireCurrentActor(actorAccountId);
        if (actor.handSize() > actor.health()) {
            state.setPhase(GamePhase.DISCARD);
            state.addLog(actor.nickname() + " must discard down to health limit.");
            return;
        }
        advanceTurn();
    }

    public void discardForLimit(UUID actorAccountId, int handIndex) {
        requireActive();
        requireNoPending();
        PlayerState actor = requireCurrentActor(actorAccountId);
        if (state.phase() != GamePhase.DISCARD) {
            throw new IllegalStateException("The game is not waiting for discard");
        }
        discard(actor.removeCard(handIndex));
        state.addLog(actor.nickname() + " discards a card.");
        if (actor.handSize() <= actor.health()) {
            advanceTurn();
        }
    }

    public void disconnect(UUID accountId, Instant disconnectedAt) {
        PlayerState player = requirePlayer(accountId);
        player.disconnect(disconnectedAt);
        state.addLog(player.nickname() + " disconnected.");
        if (state.phase() == GamePhase.FINISHED) {
            return;
        }
        if (state.alivePlayers().stream().noneMatch(PlayerState::connected)) {
            state.finish(WinningSide.ABANDONED, Instant.now(clock));
            state.addLog("Game abandoned: all active players disconnected.");
            return;
        }
        if (state.currentPlayer().accountId().equals(accountId)) {
            state.setCurrentTurnDeadline(disconnectedAt.plus(DEFAULT_DISCONNECT_TIMEOUT));
        }
    }

    public void reconnect(UUID accountId) {
        PlayerState player = requirePlayer(accountId);
        player.reconnect();
        state.addLog(player.nickname() + " reconnected.");
        if (state.phase() != GamePhase.FINISHED
                && state.currentPlayer().accountId().equals(accountId)
                && !state.currentTurnDrawn()) {
            drawCards(state, player, 2, random);
            state.setCurrentTurnDrawn(true);
            state.clearCurrentTurnDeadline();
        }
    }

    public boolean skipTimedOutTurn(Instant now) {
        if (state.phase() == GamePhase.FINISHED || state.currentTurnDeadline().isEmpty()) {
            return false;
        }
        if (now.isBefore(state.currentTurnDeadline().orElseThrow())) {
            return false;
        }
        PlayerState player = state.currentPlayer();
        autoDiscardToLimit(player);
        state.clearPendingAction();
        state.addLog(player.nickname() + " timed out. Turn skipped.");
        advanceTurn();
        return true;
    }

    private PlayerState requireCurrentActor(UUID actorAccountId) {
        PlayerState current = state.currentPlayer();
        if (!current.accountId().equals(actorAccountId)) {
            throw new IllegalArgumentException("It is not this player's turn");
        }
        if (!current.connected()) {
            throw new IllegalStateException("Disconnected player cannot act");
        }
        if (current.eliminated()) {
            throw new IllegalStateException("Eliminated player cannot act");
        }
        return current;
    }

    private PlayerState requirePlayer(UUID accountId) {
        return state.findPlayer(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + accountId));
    }

    private PlayerState requireAlivePlayer(UUID accountId) {
        PlayerState player = requirePlayer(accountId);
        if (player.eliminated()) {
            throw new IllegalArgumentException("Player is eliminated: " + player.nickname());
        }
        return player;
    }

    private void validateTarget(PlayerState actor, CardKind kind, PlayerState target) {
        if (actor.accountId().equals(target.accountId())) {
            throw new IllegalArgumentException("Cannot target yourself");
        }
        if ((kind == CardKind.SHOT || kind == CardKind.RUSTLE)
                && DistanceCalculator.distance(state, actor.accountId(), target.accountId()) > 1) {
            throw new IllegalArgumentException("Target is out of range");
        }
    }

    private void playShot(PlayerState actor, PlayerState target) {
        if (!target.connected()) {
            damage(actor, target, 1);
            return;
        }
        state.setPendingAction(PendingAction.shotReaction(
                target.accountId(),
                actor.accountId(),
                target.nickname() + " may answer with Dodge."
        ));
        state.addLog(actor.nickname() + " shoots at " + target.nickname() + ".");
    }

    private void startStandoff(PlayerState actor, PlayerState target) {
        if (!target.connected()) {
            damage(actor, target, 1);
            return;
        }
        state.setPendingAction(PendingAction.standoffResponse(
                target.accountId(),
                actor.accountId(),
                target.nickname() + " must answer the standoff with Shot."
        ));
        state.addLog(actor.nickname() + " starts a standoff with " + target.nickname() + ".");
    }

    private void discardRandomFromTarget(PlayerState actor, PlayerState target) {
        target.removeRandomCard(random).ifPresentOrElse(this::discard, () -> { });
        state.addLog(actor.nickname() + " disarms " + target.nickname() + ".");
    }

    private void stealRandomFromTarget(PlayerState actor, PlayerState target) {
        target.removeRandomCard(random).ifPresent(actor::addCard);
        state.addLog(actor.nickname() + " takes a card from " + target.nickname() + ".");
    }

    private void damage(PlayerState source, PlayerState target, int amount) {
        source.recordDamageDealt(amount);
        target.takeDamage(amount);
        state.addLog(source.nickname() + " deals " + amount + " damage to " + target.nickname() + ".");
        if (target.health() <= 0) {
            eliminate(target);
        }
    }

    private void eliminate(PlayerState target) {
        target.eliminate();
        while (target.handSize() > 0) {
            discard(target.removeCard(0));
        }
        state.addLog(target.nickname() + " is eliminated as " + target.role() + ".");
    }

    private boolean discardFirst(PlayerState player, CardKind kind) {
        Optional<CardInstance> maybeCard = player.removeFirst(kind);
        maybeCard.ifPresent(this::discard);
        return maybeCard.isPresent();
    }

    private void discard(CardInstance card) {
        state.discardPile().push(card);
    }

    private void autoDiscardToLimit(PlayerState player) {
        while (player.handSize() > Math.max(0, player.health())) {
            player.removeRandomCard(random).ifPresent(this::discard);
        }
    }

    private void advanceTurn() {
        checkWinConditions();
        if (state.phase() == GamePhase.FINISHED) {
            return;
        }
        state.clearPendingAction();
        state.clearCurrentTurnDeadline();
        int next = state.currentPlayerIndex();
        do {
            next = (next + 1) % state.players().size();
        } while (state.players().get(next).eliminated());

        state.setCurrentPlayerIndex(next);
        state.setPhase(GamePhase.PLAY);
        state.setCurrentTurnStartedAt(Instant.now(clock));
        PlayerState current = state.currentPlayer();
        if (current.connected()) {
            drawCards(state, current, 2, random);
            state.setCurrentTurnDrawn(true);
            state.addLog(current.nickname() + " starts a turn and draws two cards.");
        } else {
            state.setCurrentTurnDrawn(false);
            state.setCurrentTurnDeadline(Instant.now(clock).plus(DEFAULT_DISCONNECT_TIMEOUT));
            state.addLog(current.nickname() + " is disconnected. Waiting before skip.");
        }
    }

    private void checkWinConditions() {
        if (state.phase() == GamePhase.FINISHED) {
            return;
        }
        Optional<PlayerState> sheriff = state.players().stream()
                .filter(player -> player.role() == Role.SHERIFF)
                .findFirst();
        if (sheriff.isEmpty() || sheriff.orElseThrow().eliminated()) {
            long alive = state.alivePlayers().size();
            boolean renegadeAlive = state.alivePlayers().stream()
                    .anyMatch(player -> player.role() == Role.RENEGADE);
            WinningSide winner = alive == 1 && renegadeAlive ? WinningSide.RENEGADE : WinningSide.OUTLAWS;
            state.finish(winner, Instant.now(clock));
            state.addLog("Winner: " + winner + ".");
            return;
        }

        boolean anySheriffEnemyAlive = state.alivePlayers().stream()
                .anyMatch(player -> player.role() == Role.OUTLAW || player.role() == Role.RENEGADE);
        if (!anySheriffEnemyAlive) {
            state.finish(WinningSide.LAW, Instant.now(clock));
            state.addLog("Winner: " + WinningSide.LAW + ".");
        }
    }

    private void requireActive() {
        if (state.phase() == GamePhase.FINISHED) {
            throw new IllegalStateException("Game is finished");
        }
    }

    private void requireNoPending() {
        if (state.pendingAction().isPresent()) {
            throw new IllegalStateException("Resolve pending action first");
        }
    }

    private static void drawCards(GameState state, PlayerState player, int amount, RandomSource random) {
        for (int i = 0; i < amount; i++) {
            if (state.drawPile().isEmpty()) {
                reshuffleDiscardIntoDraw(state, random);
            }
            if (state.drawPile().isEmpty()) {
                return;
            }
            player.addCard(state.drawPile().pop());
        }
    }

    private static void reshuffleDiscardIntoDraw(GameState state, RandomSource random) {
        if (state.discardPile().isEmpty()) {
            return;
        }
        List<CardInstance> cards = new ArrayList<>(state.discardPile());
        state.discardPile().clear();
        random.shuffle(cards);
        state.drawPile().addAll(cards);
    }
}

