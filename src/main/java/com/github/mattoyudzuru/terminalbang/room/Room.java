package com.github.mattoyudzuru.terminalbang.room;

import com.github.mattoyudzuru.terminalbang.user.Account;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Room {
    private final String code;
    private final UUID hostAccountId;
    private final List<Account> seats = new ArrayList<>();
    private final Instant createdAt;

    private RoomVisibility visibility = RoomVisibility.PRIVATE;
    private RoomStatus status = RoomStatus.WAITING;
    private Optional<UUID> gameId = Optional.empty();

    Room(String code, Account host, Instant createdAt) {
        this.code = code;
        this.hostAccountId = host.id();
        this.createdAt = createdAt;
        this.seats.add(host);
    }

    public String code() {
        return code;
    }

    public UUID hostAccountId() {
        return hostAccountId;
    }

    public List<Account> seats() {
        return List.copyOf(seats);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public RoomVisibility visibility() {
        return visibility;
    }

    public RoomStatus status() {
        return status;
    }

    public Optional<UUID> gameId() {
        return gameId;
    }

    public boolean contains(UUID accountId) {
        return seats.stream().anyMatch(account -> account.id().equals(accountId));
    }

    void addSeat(Account account) {
        if (contains(account.id())) {
            return;
        }
        if (status != RoomStatus.WAITING) {
            throw new IllegalStateException("Cannot join a room that already started");
        }
        if (seats.size() >= 7) {
            throw new IllegalStateException("Room is full");
        }
        seats.add(account);
    }

    void removeSeat(UUID accountId) {
        if (hostAccountId.equals(accountId)) {
            throw new IllegalArgumentException("Host cannot be removed from the room");
        }
        seats.removeIf(account -> account.id().equals(accountId));
    }

    void togglePublic(UUID actorAccountId) {
        requireHost(actorAccountId);
        visibility = visibility == RoomVisibility.PUBLIC ? RoomVisibility.PRIVATE : RoomVisibility.PUBLIC;
    }

    void start(UUID actorAccountId, UUID gameId) {
        requireHost(actorAccountId);
        if (seats.size() < 4) {
            throw new IllegalStateException("At least 4 players are required");
        }
        status = RoomStatus.IN_GAME;
        this.gameId = Optional.of(gameId);
    }

    void finish() {
        status = RoomStatus.FINISHED;
    }

    void requireHost(UUID accountId) {
        if (!hostAccountId.equals(accountId)) {
            throw new IllegalArgumentException("Only host can perform this action");
        }
    }
}
