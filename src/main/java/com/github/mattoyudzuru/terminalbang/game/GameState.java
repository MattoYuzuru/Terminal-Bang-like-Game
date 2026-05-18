package com.github.mattoyudzuru.terminalbang.game;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GameState {
    private final UUID id;
    private final Instant startedAt;
    private final List<PlayerState> players;
    private final ArrayDeque<CardInstance> drawPile;
    private final ArrayDeque<CardInstance> discardPile;
    private final List<String> log = new ArrayList<>();

    private int currentPlayerIndex;
    private GamePhase phase = GamePhase.PLAY;
    private Optional<PendingAction> pendingAction = Optional.empty();
    private Optional<WinningSide> winner = Optional.empty();
    private Optional<Instant> finishedAt = Optional.empty();
    private Optional<Instant> currentTurnDeadline = Optional.empty();
    private Instant currentTurnStartedAt;
    private boolean currentTurnDrawn;

    GameState(
            UUID id,
            Instant startedAt,
            List<PlayerState> players,
            ArrayDeque<CardInstance> drawPile,
            ArrayDeque<CardInstance> discardPile
    ) {
        this.id = id;
        this.startedAt = startedAt;
        this.players = players;
        this.drawPile = drawPile;
        this.discardPile = discardPile;
        this.currentTurnStartedAt = startedAt;
    }

    public UUID id() {
        return id;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public List<PlayerState> players() {
        return List.copyOf(players);
    }

    public List<PlayerState> alivePlayers() {
        return players.stream()
                .filter(player -> !player.eliminated())
                .toList();
    }

    ArrayDeque<CardInstance> drawPile() {
        return drawPile;
    }

    ArrayDeque<CardInstance> discardPile() {
        return discardPile;
    }

    public int drawPileSize() {
        return drawPile.size();
    }

    public Optional<CardInstance> topDiscard() {
        return Optional.ofNullable(discardPile.peek());
    }

    public PlayerState currentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int currentPlayerIndex() {
        return currentPlayerIndex;
    }

    void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public GamePhase phase() {
        return phase;
    }

    void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public Optional<PendingAction> pendingAction() {
        return pendingAction;
    }

    void setPendingAction(PendingAction pendingAction) {
        this.pendingAction = Optional.of(pendingAction);
    }

    void clearPendingAction() {
        this.pendingAction = Optional.empty();
    }

    public Optional<WinningSide> winner() {
        return winner;
    }

    public Optional<Instant> finishedAt() {
        return finishedAt;
    }

    void finish(WinningSide winner, Instant finishedAt) {
        this.phase = GamePhase.FINISHED;
        this.winner = Optional.of(winner);
        this.finishedAt = Optional.of(finishedAt);
        this.currentTurnDeadline = Optional.empty();
    }

    public Optional<Instant> currentTurnDeadline() {
        return currentTurnDeadline;
    }

    void setCurrentTurnDeadline(Instant deadline) {
        this.currentTurnDeadline = Optional.of(deadline);
    }

    void clearCurrentTurnDeadline() {
        this.currentTurnDeadline = Optional.empty();
    }

    public Instant currentTurnStartedAt() {
        return currentTurnStartedAt;
    }

    void setCurrentTurnStartedAt(Instant currentTurnStartedAt) {
        this.currentTurnStartedAt = currentTurnStartedAt;
    }

    public boolean currentTurnDrawn() {
        return currentTurnDrawn;
    }

    void setCurrentTurnDrawn(boolean currentTurnDrawn) {
        this.currentTurnDrawn = currentTurnDrawn;
    }

    public Optional<PlayerState> findPlayer(UUID accountId) {
        return players.stream()
                .filter(player -> player.accountId().equals(accountId))
                .findFirst();
    }

    public List<String> log() {
        return List.copyOf(log);
    }

    void addLog(String line) {
        log.add(line);
        if (log.size() > 80) {
            log.remove(0);
        }
    }
}

