package com.github.mattoyudzuru.terminalbang.room;

import com.github.mattoyudzuru.terminalbang.game.GameEngine;
import com.github.mattoyudzuru.terminalbang.game.GameState;
import com.github.mattoyudzuru.terminalbang.game.PlayerSeed;
import com.github.mattoyudzuru.terminalbang.game.RandomSource;
import com.github.mattoyudzuru.terminalbang.game.Role;
import com.github.mattoyudzuru.terminalbang.game.WinningSide;
import com.github.mattoyudzuru.terminalbang.persistence.MatchResultRepository;
import com.github.mattoyudzuru.terminalbang.stats.MatchResult;
import com.github.mattoyudzuru.terminalbang.stats.PlayerMatchStat;
import com.github.mattoyudzuru.terminalbang.user.Account;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RoomService {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeRoomByAccount = new ConcurrentHashMap<>();
    private final Map<UUID, GameEngine> games = new ConcurrentHashMap<>();
    private final MatchResultRepository matchResultRepository;
    private final RoomCodeGenerator codeGenerator;
    private final RandomSource random;
    private final Clock clock;

    public RoomService(
            MatchResultRepository matchResultRepository,
            RoomCodeGenerator codeGenerator,
            RandomSource random,
            Clock clock
    ) {
        this.matchResultRepository = matchResultRepository;
        this.codeGenerator = codeGenerator;
        this.random = random;
        this.clock = clock;
    }

    public Room createRoom(Account host) {
        String code = uniqueCode();
        Room room = new Room(code, host, Instant.now(clock));
        rooms.put(code, room);
        activeRoomByAccount.put(host.id(), code);
        return room;
    }

    public Room joinRoom(Account account, String code) {
        Room room = requireRoom(code);
        room.addSeat(account);
        activeRoomByAccount.put(account.id(), room.code());
        return room;
    }

    public List<Room> publicRooms() {
        return rooms.values().stream()
                .filter(room -> room.visibility() == RoomVisibility.PUBLIC)
                .filter(room -> room.status() == RoomStatus.WAITING)
                .sorted(Comparator.comparing(Room::createdAt))
                .toList();
    }

    public Room togglePublic(UUID hostAccountId, String code) {
        Room room = requireRoom(code);
        room.togglePublic(hostAccountId);
        return room;
    }

    public Room kick(UUID hostAccountId, String code, UUID targetAccountId) {
        Room room = requireRoom(code);
        room.requireHost(hostAccountId);
        room.removeSeat(targetAccountId);
        activeRoomByAccount.remove(targetAccountId);
        return room;
    }

    public GameEngine startGame(UUID hostAccountId, String code) {
        Room room = requireRoom(code);
        UUID gameId = UUID.randomUUID();
        room.start(hostAccountId, gameId);
        List<PlayerSeed> seeds = room.seats().stream()
                .map(account -> new PlayerSeed(account.id(), account.nickname()))
                .toList();
        GameEngine engine = GameEngine.start(gameId, seeds, random, clock);
        games.put(gameId, engine);
        return engine;
    }

    public Optional<Room> activeRoom(UUID accountId) {
        return Optional.ofNullable(activeRoomByAccount.get(accountId)).map(this::requireRoom);
    }

    public Optional<GameEngine> activeGame(UUID accountId) {
        return activeRoom(accountId)
                .flatMap(Room::gameId)
                .map(games::get);
    }

    public Optional<Room> room(String code) {
        return Optional.ofNullable(rooms.get(normalize(code)));
    }

    public void disconnect(UUID accountId) {
        activeGame(accountId).ifPresent(engine -> {
            engine.disconnect(accountId, Instant.now(clock));
            persistIfFinished(engine);
        });
    }

    public void reconnect(UUID accountId) {
        activeGame(accountId).ifPresent(engine -> engine.reconnect(accountId));
    }

    public void tick() {
        for (GameEngine engine : games.values()) {
            if (engine.skipTimedOutTurn(Instant.now(clock))) {
                persistIfFinished(engine);
            }
        }
    }

    public void persistIfFinished(GameEngine engine) {
        GameState state = engine.state();
        if (state.winner().isEmpty()) {
            return;
        }
        Optional<Room> room = rooms.values().stream()
                .filter(candidate -> candidate.gameId().filter(state.id()::equals).isPresent())
                .findFirst();
        if (room.isEmpty() || room.orElseThrow().status() == RoomStatus.FINISHED) {
            return;
        }
        Room finishedRoom = room.orElseThrow();
        matchResultRepository.save(toResult(finishedRoom.code(), state));
        finishedRoom.finish();
    }

    private MatchResult toResult(String roomCode, GameState state) {
        List<PlayerMatchStat> players = new ArrayList<>();
        for (var player : state.players()) {
            players.add(new PlayerMatchStat(
                    player.accountId(),
                    player.nickname(),
                    player.role(),
                    won(player.role(), state.winner()),
                    player.eliminated(),
                    player.damageDealt(),
                    player.damageTaken(),
                    player.cardsPlayed()
            ));
        }
        return new MatchResult(
                state.id(),
                roomCode,
                state.winner().filter(winner -> winner == WinningSide.ABANDONED).isPresent() ? "ABANDONED" : "FINISHED",
                state.winner(),
                state.startedAt(),
                state.finishedAt().orElseGet(() -> Instant.now(clock)),
                players
        );
    }

    private static boolean won(Role role, Optional<WinningSide> winner) {
        if (winner.isEmpty()) {
            return false;
        }
        return switch (winner.orElseThrow()) {
            case LAW -> role.winsWithLaw();
            case OUTLAWS -> role == Role.OUTLAW;
            case RENEGADE -> role == Role.RENEGADE;
            case ABANDONED -> false;
        };
    }

    private String uniqueCode() {
        for (int attempts = 0; attempts < 100; attempts++) {
            String code = codeGenerator.nextCode();
            if (!rooms.containsKey(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not allocate room code");
    }

    private Room requireRoom(String code) {
        Room room = rooms.get(normalize(code));
        if (room == null) {
            throw new IllegalArgumentException("Unknown room code: " + code);
        }
        return room;
    }

    private static String normalize(String code) {
        return code.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public Map<UUID, GameEngine> gamesSnapshot() {
        return new HashMap<>(games);
    }
}

