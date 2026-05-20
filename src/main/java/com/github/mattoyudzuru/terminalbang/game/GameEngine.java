package com.github.mattoyudzuru.terminalbang.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

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

        List<CardInstance> deck = new ArrayList<>(StandardContent.deck());
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
        state.currentPlayer().resetTurnFlags();
        state.addLog(state.currentPlayer().nickname() + " starts as Sheriff.");

        GameEngine engine = new GameEngine(state, random, clock);
        engine.beginConnectedTurn(state.currentPlayer());
        return engine;
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
        CardKind effectiveKind = effectivePlayKind(actor, card, targetAccountId);
        boolean requiresTarget = requiresTargetForPlay(actor, card);
        if (requiresTarget && targetAccountId.isEmpty()) {
            throw new IllegalArgumentException(card.name() + " requires a target");
        }
        if (!requiresTarget && targetAccountId.isPresent()) {
            throw new IllegalArgumentException(card.name() + " does not accept a target");
        }
        if (card.definition().reactionOnly() && effectiveKind == card.kind()) {
            throw new IllegalArgumentException(card.name() + " can only be used as a reaction");
        }
        if (effectiveKind == CardKind.BANG && !actor.canPlayBangCard()) {
            throw new IllegalArgumentException("Only one Bang card can be played per turn without Volcanic or Willy the Kid.");
        }

        Optional<PlayerState> maybeTarget = targetAccountId.map(this::requireAlivePlayer);
        maybeTarget.ifPresent(target -> validateTarget(actor, effectiveKind, target));
        validateTableCard(actor, effectiveKind);
        validateImmediateCard(actor, effectiveKind);

        actor.removeCard(handIndex);
        actor.recordCardPlayed();
        afterHandChanged(actor);
        if (effectiveKind == CardKind.BANG) {
            actor.recordBangCardPlayed();
        }

        switch (effectiveKind) {
            case BANG -> {
                discard(card);
                playBang(actor, maybeTarget.orElseThrow());
            }
            case BEER -> {
                playBeer(actor);
                discard(card);
            }
            case CAT_BALOU -> {
                discard(card);
                discardRandomFromTarget(actor, maybeTarget.orElseThrow());
            }
            case DUEL -> {
                discard(card);
                startDuel(actor, maybeTarget.orElseThrow());
            }
            case GATLING -> {
                discard(card);
                startMassReaction(actor, PendingActionType.GATLING_REACTION, CardKind.MISSED);
            }
            case GENERAL_STORE -> {
                discard(card);
                playGeneralStore(actor);
            }
            case INDIANS -> {
                discard(card);
                startMassReaction(actor, PendingActionType.INDIANS_REACTION, CardKind.BANG);
            }
            case PANIC -> {
                discard(card);
                stealRandomVisibleCard(actor, maybeTarget.orElseThrow());
            }
            case SALOON -> {
                playSaloon(actor);
                discard(card);
            }
            case STAGECOACH -> {
                discard(card);
                drawCards(state, actor, 2, random);
                state.addLog(actor.nickname() + " draws two cards.");
            }
            case WELLS_FARGO -> {
                discard(card);
                drawCards(state, actor, 3, random);
                state.addLog(actor.nickname() + " draws three cards.");
            }
            case BARREL, DYNAMITE, MUSTANG, SCOPE, REMINGTON, REV_CARABINE, SCHOFIELD, VOLCANIC, WINCHESTER -> {
                playInFrontOfSelf(actor, card);
            }
            case JAIL -> playJail(actor, maybeTarget.orElseThrow(), card);
            case MISSED -> throw new IllegalArgumentException("Missed cannot be played directly");
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
        PlayerState opponent = requirePlayer(pending.opponentAccountId());
        if (pending.type() == PendingActionType.GENERAL_STORE_PICK) {
            throw new IllegalStateException("Choose a General Store card instead");
        }
        boolean answered = playResponse && consumeResponses(
                expected,
                pending.responseKind(),
                pending.requiredResponses(),
                allowsBarrel(pending.type())
        );

        switch (pending.type()) {
            case BANG_REACTION -> {
                state.clearPendingAction();
                if (answered) {
                    state.addLog(expected.nickname() + " cancels the Bang.");
                } else {
                    damage(opponent, expected, 1);
                }
                checkWinConditions();
            }
            case DUEL_RESPONSE -> {
                if (answered) {
                    state.setPendingAction(PendingAction.duelResponse(
                            opponent.accountId(),
                            expected.accountId(),
                            opponent.nickname() + " must answer the duel with Bang."
                    ));
                    state.addLog(expected.nickname() + " answers the duel.");
                } else {
                    state.clearPendingAction();
                    damage(opponent, expected, 1);
                    checkWinConditions();
                }
            }
            case GATLING_REACTION, INDIANS_REACTION -> {
                if (answered) {
                    state.addLog(expected.nickname() + " answers " + massActionName(pending.type()) + ".");
                } else {
                    damage(opponent, expected, 1);
                    if (state.phase() == GamePhase.FINISHED) {
                        return;
                    }
                }
                advanceMassReaction(pending.type(), opponent, pending.responseKind(), pending.remainingAccountIds());
                checkWinConditions();
            }
        }
    }

    public void choosePendingCard(UUID actorAccountId, int choiceIndex) {
        requireActive();
        PendingAction pending = state.pendingAction()
                .orElseThrow(() -> new IllegalStateException("There is no pending card choice"));
        if (pending.type() != PendingActionType.GENERAL_STORE_PICK) {
            throw new IllegalStateException("The game is not waiting for a card choice");
        }
        if (!pending.expectedAccountId().equals(actorAccountId)) {
            throw new IllegalArgumentException("Card choice belongs to another player");
        }
        if (choiceIndex < 0 || choiceIndex >= pending.choiceCards().size()) {
            throw new IllegalArgumentException("Card choice index out of range");
        }

        PlayerState player = requireAlivePlayer(actorAccountId);
        List<CardInstance> choices = new ArrayList<>(pending.choiceCards());
        CardInstance picked = choices.remove(choiceIndex);
        player.addCard(picked);
        state.addLog(player.nickname() + " takes " + picked.name() + " from General Store.");
        advanceGeneralStorePick(requirePlayer(pending.opponentAccountId()), pending.remainingAccountIds(), choices);
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
        afterHandChanged(actor);
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
                && state.currentPlayer().accountId().equals(accountId)) {
            state.clearCurrentTurnDeadline();
            if (!state.currentTurnDrawn()) {
                beginConnectedTurn(player);
            }
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
        if (!state.currentTurnDrawn()) {
            processDynamite(player);
            if (state.phase() == GamePhase.FINISHED) {
                return true;
            }
            if (processJail(player)) {
                state.addLog(player.nickname() + " timed out while jailed. Turn skipped.");
                advanceTurn();
                return true;
            }
            state.setCurrentTurnDrawn(true);
        }
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

    private CardKind effectivePlayKind(PlayerState actor, CardInstance card, Optional<UUID> targetAccountId) {
        if (card.kind() == CardKind.MISSED && isCharacter(actor, "calamity_janet") && targetAccountId.isPresent()) {
            return CardKind.BANG;
        }
        return card.kind();
    }

    private boolean requiresTargetForPlay(PlayerState actor, CardInstance card) {
        return card.definition().requiresTarget()
                || (card.kind() == CardKind.MISSED && isCharacter(actor, "calamity_janet"));
    }

    private void validateTarget(PlayerState actor, CardKind kind, PlayerState target) {
        if (actor.accountId().equals(target.accountId())) {
            throw new IllegalArgumentException("Cannot target yourself");
        }
        switch (kind) {
            case BANG -> {
                int distance = DistanceCalculator.distance(state, actor.accountId(), target.accountId());
                if (distance > actor.weaponRange()) {
                    throw new IllegalArgumentException("Target is out of weapon range");
                }
            }
            case PANIC -> {
                if (DistanceCalculator.distance(state, actor.accountId(), target.accountId()) > 1) {
                    throw new IllegalArgumentException("Panic requires a target at distance 1");
                }
                if (target.handSize() + target.inPlaySize() == 0) {
                    throw new IllegalArgumentException("Target has no cards to take");
                }
            }
            case CAT_BALOU -> {
                if (target.handSize() + target.inPlaySize() == 0) {
                    throw new IllegalArgumentException("Target has no cards to discard");
                }
            }
            case JAIL -> {
                if (target.role() == Role.SHERIFF) {
                    throw new IllegalArgumentException("Jail cannot be played on the Sheriff");
                }
                if (target.hasInPlay(CardKind.JAIL)) {
                    throw new IllegalArgumentException("Target already has Jail in play");
                }
            }
            default -> {
            }
        }
    }

    private void validateTableCard(PlayerState actor, CardKind kind) {
        if (PlayerState.isWeapon(kind)) {
            return;
        }
        if (kind == CardKind.JAIL) {
            return;
        }
        if (isSelfBlueCard(kind) && actor.hasInPlay(kind)) {
            throw new IllegalArgumentException("You already have " + StandardContent.card(kind).name() + " in play");
        }
    }

    private void validateImmediateCard(PlayerState actor, CardKind kind) {
        if (kind == CardKind.BEER && actor.health() >= actor.maxHealth()) {
            throw new IllegalArgumentException("Beer cannot be used at full health");
        }
        if (kind == CardKind.SALOON) {
            boolean anyDamaged = state.alivePlayers().stream()
                    .anyMatch(player -> player.health() < player.maxHealth());
            if (!anyDamaged) {
                throw new IllegalArgumentException("Saloon cannot recover anyone right now");
            }
        }
    }

    private void playBang(PlayerState actor, PlayerState target) {
        if (!target.connected()) {
            damage(actor, target, 1);
            return;
        }
        int requiredMisses = isCharacter(actor, "slab_the_killer") ? 2 : 1;
        state.setPendingAction(PendingAction.bangReaction(
                target.accountId(),
                actor.accountId(),
                requiredMisses,
                target.nickname() + " may answer with Missed."
        ));
        state.addLog(actor.nickname() + " plays Bang on " + target.nickname() + ".");
    }

    private void playBeer(PlayerState actor) {
        if (state.alivePlayers().size() <= 2) {
            state.addLog(actor.nickname() + " drinks Beer, but it has no effect with two players alive.");
            return;
        }
        actor.heal(1);
        state.addLog(actor.nickname() + " recovers one life.");
    }

    private void playSaloon(PlayerState actor) {
        for (PlayerState player : state.alivePlayers()) {
            player.heal(1);
        }
        state.addLog(actor.nickname() + " opens the Saloon. Everyone recovers one life.");
    }

    private void discardRandomFromTarget(PlayerState actor, PlayerState target) {
        target.removeRandomVisibleCard(random).ifPresentOrElse(card -> {
            discard(card);
            afterHandChanged(target);
        }, () -> {
        });
        state.addLog(actor.nickname() + " makes " + target.nickname() + " discard a card.");
    }

    private void stealRandomVisibleCard(PlayerState actor, PlayerState target) {
        target.removeRandomVisibleCard(random).ifPresent(card -> {
            actor.addCard(card);
            afterHandChanged(target);
        });
        state.addLog(actor.nickname() + " takes a card from " + target.nickname() + ".");
    }

    private void startDuel(PlayerState actor, PlayerState target) {
        if (!target.connected()) {
            damage(actor, target, 1);
            return;
        }
        state.setPendingAction(PendingAction.duelResponse(
                target.accountId(),
                actor.accountId(),
                target.nickname() + " must answer the duel with Bang."
        ));
        state.addLog(actor.nickname() + " starts a duel with " + target.nickname() + ".");
    }

    private void startMassReaction(PlayerState actor, PendingActionType type, CardKind responseKind) {
        List<UUID> targets = aliveTargetsAfter(actor).stream()
                .map(PlayerState::accountId)
                .toList();
        state.addLog(actor.nickname() + " plays " + massActionName(type) + ".");
        advanceMassReaction(type, actor, responseKind, targets);
    }

    private void advanceMassReaction(
            PendingActionType type,
            PlayerState actor,
            CardKind responseKind,
            List<UUID> targetAccountIds
    ) {
        List<UUID> remaining = new ArrayList<>(targetAccountIds);
        while (!remaining.isEmpty()) {
            Optional<PlayerState> maybeTarget = state.findPlayer(remaining.removeFirst());
            if (maybeTarget.isEmpty() || maybeTarget.orElseThrow().eliminated()) {
                continue;
            }
            PlayerState target = maybeTarget.orElseThrow();
            if (!target.connected()) {
                state.addLog(target.nickname() + " is offline and cannot answer " + massActionName(type) + ".");
                damage(actor, target, 1);
                if (state.phase() == GamePhase.FINISHED) {
                    return;
                }
                continue;
            }
            state.setPendingAction(PendingAction.massReaction(
                    type,
                    target.accountId(),
                    actor.accountId(),
                    responseKind,
                    remaining,
                    target.nickname() + " must answer " + massActionName(type) + "."
            ));
            return;
        }
        state.clearPendingAction();
    }

    private void playGeneralStore(PlayerState actor) {
        List<CardInstance> revealed = new ArrayList<>();
        for (int i = 0; i < state.alivePlayers().size(); i++) {
            drawCard(state, random).ifPresent(revealed::add);
        }
        state.addLog(actor.nickname() + " opens General Store.");
        List<UUID> pickOrder = alivePlayersFrom(actor).stream()
                .map(PlayerState::accountId)
                .toList();
        advanceGeneralStorePick(actor, pickOrder, revealed);
    }

    private void playInFrontOfSelf(PlayerState actor, CardInstance card) {
        if (PlayerState.isWeapon(card.kind())) {
            actor.removeWeaponInPlay().ifPresent(this::discard);
        }
        actor.addInPlay(card);
        state.addLog(actor.nickname() + " puts " + card.name() + " in play.");
    }

    private void playJail(PlayerState actor, PlayerState target, CardInstance card) {
        target.addInPlay(card);
        state.addLog(actor.nickname() + " puts " + target.nickname() + " in Jail.");
    }

    private boolean consumeResponses(PlayerState player, CardKind responseKind, int required, boolean allowBarrel) {
        int remaining = required;
        if (responseKind == CardKind.MISSED && allowBarrel) {
            remaining -= Math.min(remaining, barrelMisses(player, remaining));
        }
        while (remaining > 0 && discardFirstResponse(player, responseKind)) {
            player.recordCardPlayed();
            state.addLog(player.nickname() + " discards " + StandardContent.card(responseKind).name() + ".");
            remaining--;
        }
        return remaining == 0;
    }

    private int barrelMisses(PlayerState player, int needed) {
        int misses = 0;
        if (needed > misses && player.hasInPlay(CardKind.BARREL) && drawCheck(player, this::isHeart, "Barrel")) {
            misses++;
            state.addLog(player.nickname() + "'s Barrel counts as Missed.");
        }
        if (needed > misses && isCharacter(player, "jourdonnais") && drawCheck(player, this::isHeart, "Jourdonnais")) {
            misses++;
            state.addLog(player.nickname() + "'s ability counts as Missed.");
        }
        return misses;
    }

    private boolean discardFirstResponse(PlayerState player, CardKind responseKind) {
        List<CardKind> accepted = responseKindsFor(player, responseKind);
        for (CardKind kind : accepted) {
            Optional<CardInstance> maybeCard = player.removeFirst(kind);
            if (maybeCard.isPresent()) {
                discard(maybeCard.orElseThrow());
                afterHandChanged(player);
                return true;
            }
        }
        return false;
    }

    private List<CardKind> responseKindsFor(PlayerState player, CardKind responseKind) {
        if (!isCharacter(player, "calamity_janet")) {
            return List.of(responseKind);
        }
        if (responseKind == CardKind.MISSED) {
            return List.of(CardKind.MISSED, CardKind.BANG);
        }
        if (responseKind == CardKind.BANG) {
            return List.of(CardKind.BANG, CardKind.MISSED);
        }
        return List.of(responseKind);
    }

    private void damage(PlayerState source, PlayerState target, int amount) {
        if (source != null) {
            source.recordDamageDealt(amount);
        }
        target.takeDamage(amount);
        String sourceName = source == null ? "Dynamite" : source.nickname();
        state.addLog(sourceName + " deals " + amount + " damage to " + target.nickname() + ".");
        afterDamageAbilities(source, target, amount);
        tryLethalSaves(target);
        if (target.health() <= 0) {
            eliminate(source, target);
        }
        checkWinConditions();
    }

    private void afterDamageAbilities(PlayerState source, PlayerState target, int amount) {
        if (isCharacter(target, "bart_cassidy")) {
            drawCards(state, target, amount, random);
            state.addLog(target.nickname() + " draws " + amount + " card(s) from Bart Cassidy's ability.");
        }
        if (source != null && !source.accountId().equals(target.accountId()) && isCharacter(target, "el_gringo")) {
            for (int i = 0; i < amount; i++) {
                Optional<CardInstance> maybeCard = source.removeRandomCard(random);
                if (maybeCard.isEmpty()) {
                    break;
                }
                target.addCard(maybeCard.orElseThrow());
                afterHandChanged(source);
            }
            state.addLog(target.nickname() + " uses El Gringo's ability.");
        }
    }

    private void tryLethalSaves(PlayerState target) {
        if (target.health() > 0) {
            return;
        }
        if (state.alivePlayers().size() > 2) {
            while (target.health() <= 0 && discardFirst(target, CardKind.BEER)) {
                target.heal(1);
                state.addLog(target.nickname() + " spends Beer to stay alive.");
            }
        }
        if (target.health() <= 0 && isCharacter(target, "sid_ketchum")) {
            while (target.health() <= 0 && target.handSize() >= 2) {
                discard(target.removeRandomCard(random).orElseThrow());
                discard(target.removeRandomCard(random).orElseThrow());
                afterHandChanged(target);
                target.heal(1);
                state.addLog(target.nickname() + " discards two cards to stay alive.");
            }
        }
    }

    private void eliminate(PlayerState source, PlayerState target) {
        List<CardInstance> cards = target.removeAllCards();
        target.eliminate();
        Optional<PlayerState> vultureSam = state.alivePlayers().stream()
                .filter(player -> isCharacter(player, "vulture_sam"))
                .findFirst();
        if (vultureSam.isPresent()) {
            cards.forEach(vultureSam.orElseThrow()::addCard);
            state.addLog(vultureSam.orElseThrow().nickname() + " takes " + target.nickname() + "'s cards.");
        } else {
            cards.forEach(this::discard);
        }
        state.addLog(target.nickname() + " is eliminated as " + target.role() + ".");

        if (source == null || source.eliminated()) {
            return;
        }
        if (target.role() == Role.OUTLAW) {
            drawCards(state, source, 3, random);
            state.addLog(source.nickname() + " draws three cards for eliminating an Outlaw.");
        }
        if (source.role() == Role.SHERIFF && target.role() == Role.DEPUTY) {
            discardAllCards(source);
            state.addLog(source.nickname() + " discarded all cards for eliminating a Deputy.");
        }
    }

    private void discardAllCards(PlayerState player) {
        List<CardInstance> cards = player.removeAllCards();
        cards.forEach(this::discard);
        afterHandChanged(player);
    }

    private boolean discardFirst(PlayerState player, CardKind kind) {
        Optional<CardInstance> maybeCard = player.removeFirst(kind);
        maybeCard.ifPresent(card -> {
            discard(card);
            afterHandChanged(player);
        });
        return maybeCard.isPresent();
    }

    private void discard(CardInstance card) {
        state.discardPile().push(card);
    }

    private void autoDiscardToLimit(PlayerState player) {
        while (player.handSize() > Math.max(0, player.health())) {
            player.removeRandomCard(random).ifPresent(card -> {
                discard(card);
                afterHandChanged(player);
            });
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
        current.resetTurnFlags();
        if (current.connected()) {
            beginConnectedTurn(current);
        } else {
            state.setCurrentTurnDrawn(false);
            state.setCurrentTurnDeadline(Instant.now(clock).plus(DEFAULT_DISCONNECT_TIMEOUT));
            state.addLog(current.nickname() + " is disconnected. Waiting before skip.");
        }
    }

    private void beginConnectedTurn(PlayerState current) {
        state.addLog(current.nickname() + " starts a turn.");
        processDynamite(current);
        if (state.phase() == GamePhase.FINISHED || current.eliminated()) {
            return;
        }
        if (processJail(current)) {
            state.setCurrentTurnDrawn(true);
            advanceTurn();
            return;
        }
        drawPhase(current);
        state.setCurrentTurnDrawn(true);
    }

    private void advanceGeneralStorePick(PlayerState actor, List<UUID> targetAccountIds, List<CardInstance> choiceCards) {
        List<UUID> remaining = new ArrayList<>(targetAccountIds);
        List<CardInstance> choices = new ArrayList<>(choiceCards);
        while (!remaining.isEmpty() && !choices.isEmpty()) {
            Optional<PlayerState> maybePlayer = state.findPlayer(remaining.removeFirst());
            if (maybePlayer.isEmpty() || maybePlayer.orElseThrow().eliminated()) {
                continue;
            }
            PlayerState player = maybePlayer.orElseThrow();
            if (!player.connected()) {
                CardInstance picked = choices.removeFirst();
                player.addCard(picked);
                state.addLog(player.nickname() + " is offline and receives " + picked.name() + " from General Store.");
                continue;
            }
            state.setPendingAction(PendingAction.generalStorePick(
                    player.accountId(),
                    actor.accountId(),
                    remaining,
                    choices,
                    player.nickname() + " must choose a General Store card."
            ));
            return;
        }
        choices.forEach(this::discard);
        state.clearPendingAction();
    }

    private void processDynamite(PlayerState current) {
        Optional<CardInstance> maybeDynamite = current.removeInPlay(CardKind.DYNAMITE);
        if (maybeDynamite.isEmpty()) {
            return;
        }
        CardInstance dynamite = maybeDynamite.orElseThrow();
        boolean safe = drawCheck(current, card -> !isDynamiteExplosion(card), "Dynamite");
        if (safe) {
            PlayerState next = nextAliveAfter(current);
            next.addInPlay(dynamite);
            state.addLog("Dynamite passes from " + current.nickname() + " to " + next.nickname() + ".");
        } else {
            discard(dynamite);
            damage(null, current, 3);
        }
    }

    private boolean processJail(PlayerState current) {
        Optional<CardInstance> maybeJail = current.removeInPlay(CardKind.JAIL);
        if (maybeJail.isEmpty()) {
            return false;
        }
        discard(maybeJail.orElseThrow());
        boolean escaped = drawCheck(current, this::isHeart, "Jail");
        if (escaped) {
            state.addLog(current.nickname() + " escapes Jail and continues the turn.");
            return false;
        }
        state.addLog(current.nickname() + " fails Jail and skips the turn.");
        return true;
    }

    private void drawPhase(PlayerState current) {
        switch (current.character().id()) {
            case "black_jack" -> drawBlackJack(current);
            case "jesse_jones" -> drawJesseJones(current);
            case "kit_carlson" -> drawKitCarlson(current);
            case "pedro_ramirez" -> drawPedroRamirez(current);
            default -> {
                drawCards(state, current, 2, random);
                state.addLog(current.nickname() + " draws two cards.");
            }
        }
    }

    private void drawBlackJack(PlayerState current) {
        drawCard(state, random).ifPresent(current::addCard);
        Optional<CardInstance> second = drawCard(state, random);
        second.ifPresent(current::addCard);
        if (second.isPresent() && second.orElseThrow().suit().red()) {
            drawCards(state, current, 1, random);
            state.addLog(current.nickname() + " reveals a red second draw and draws one extra card.");
        } else {
            state.addLog(current.nickname() + " draws two cards.");
        }
    }

    private void drawJesseJones(PlayerState current) {
        Optional<PlayerState> target = aliveTargetsAfter(current).stream()
                .filter(player -> player.handSize() > 0)
                .findFirst();
        if (target.isPresent()) {
            PlayerState victim = target.orElseThrow();
            victim.removeRandomCard(random).ifPresent(card -> {
                current.addCard(card);
                afterHandChanged(victim);
            });
            drawCards(state, current, 1, random);
            state.addLog(current.nickname() + " takes one draw from " + victim.nickname() + " and one from the deck.");
        } else {
            drawCards(state, current, 2, random);
            state.addLog(current.nickname() + " draws two cards.");
        }
    }

    private void drawKitCarlson(PlayerState current) {
        List<CardInstance> cards = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            drawCard(state, random).ifPresent(cards::add);
        }
        if (cards.size() <= 2) {
            cards.forEach(current::addCard);
        } else {
            current.addCard(cards.get(0));
            current.addCard(cards.get(1));
            state.drawPile().push(cards.get(2));
        }
        state.addLog(current.nickname() + " looks at three cards and keeps two.");
    }

    private void drawPedroRamirez(PlayerState current) {
        if (!state.discardPile().isEmpty()) {
            current.addCard(state.discardPile().pop());
            drawCards(state, current, 1, random);
            state.addLog(current.nickname() + " draws one card from discard and one from the deck.");
        } else {
            drawCards(state, current, 2, random);
            state.addLog(current.nickname() + " draws two cards.");
        }
    }

    private boolean drawCheck(PlayerState player, Predicate<CardInstance> success, String reason) {
        int amount = isCharacter(player, "lucky_duke") ? 2 : 1;
        List<CardInstance> drawn = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            drawCard(state, random).ifPresent(drawn::add);
        }
        drawn.forEach(this::discard);
        boolean passed = drawn.stream().anyMatch(success);
        String cards = drawn.stream()
                .map(CardInstance::code)
                .reduce((left, right) -> left + "," + right)
                .orElse("-");
        state.addLog(player.nickname() + " draw! for " + reason + ": " + cards + (passed ? " succeeds." : " fails."));
        return passed;
    }

    private boolean isHeart(CardInstance card) {
        return card.suit() == CardSuit.HEARTS;
    }

    private boolean isDynamiteExplosion(CardInstance card) {
        return card.suit() == CardSuit.SPADES
                && card.rank().value() >= CardRank.TWO.value()
                && card.rank().value() <= CardRank.NINE.value();
    }

    private void afterHandChanged(PlayerState player) {
        if (player.eliminated() || !isCharacter(player, "suzy_lafayette") || player.handSize() != 0) {
            return;
        }
        int before = player.handSize();
        drawCards(state, player, 1, random);
        if (player.handSize() > before) {
            state.addLog(player.nickname() + " draws one card from Suzy Lafayette's ability.");
        }
    }

    private List<PlayerState> aliveTargetsAfter(PlayerState actor) {
        return alivePlayersFrom(actor).stream()
                .filter(player -> !player.accountId().equals(actor.accountId()))
                .toList();
    }

    private List<PlayerState> alivePlayersFrom(PlayerState start) {
        List<PlayerState> ordered = new ArrayList<>();
        List<PlayerState> players = state.players();
        int index = players.indexOf(start);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown player");
        }
        for (int offset = 0; offset < players.size(); offset++) {
            PlayerState player = players.get((index + offset) % players.size());
            if (!player.eliminated()) {
                ordered.add(player);
            }
        }
        return ordered;
    }

    private PlayerState nextAliveAfter(PlayerState player) {
        return aliveTargetsAfter(player).stream()
                .findFirst()
                .orElse(player);
    }

    private static boolean isSelfBlueCard(CardKind kind) {
        return switch (kind) {
            case BARREL, DYNAMITE, MUSTANG, SCOPE -> true;
            default -> false;
        };
    }

    private static boolean allowsBarrel(PendingActionType type) {
        return type == PendingActionType.BANG_REACTION || type == PendingActionType.GATLING_REACTION;
    }

    private static String massActionName(PendingActionType type) {
        return switch (type) {
            case GATLING_REACTION -> "Gatling";
            case INDIANS_REACTION -> "Indians";
            case BANG_REACTION -> "Bang";
            case DUEL_RESPONSE -> "Duel";
            case GENERAL_STORE_PICK -> "General Store";
        };
    }

    private static boolean isCharacter(PlayerState player, String characterId) {
        return player.character().id().equals(characterId);
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
            drawCard(state, random).ifPresent(player::addCard);
        }
    }

    private static Optional<CardInstance> drawCard(GameState state, RandomSource random) {
        if (state.drawPile().isEmpty()) {
            reshuffleDiscardIntoDraw(state, random);
        }
        if (state.drawPile().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(state.drawPile().pop());
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
