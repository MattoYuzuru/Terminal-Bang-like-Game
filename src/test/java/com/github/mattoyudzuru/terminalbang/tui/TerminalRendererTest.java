package com.github.mattoyudzuru.terminalbang.tui;

import com.github.mattoyudzuru.terminalbang.game.CardKind;
import com.github.mattoyudzuru.terminalbang.game.GameEngine;
import com.github.mattoyudzuru.terminalbang.game.GameState;
import com.github.mattoyudzuru.terminalbang.game.PlayerState;
import com.github.mattoyudzuru.terminalbang.game.RandomSource;
import com.github.mattoyudzuru.terminalbang.game.PlayerSeed;
import com.github.mattoyudzuru.terminalbang.room.Room;
import com.github.mattoyudzuru.terminalbang.room.RoomService;
import com.github.mattoyudzuru.terminalbang.room.RoomCodeGenerator;
import com.github.mattoyudzuru.terminalbang.persistence.MatchResultRepository;
import com.github.mattoyudzuru.terminalbang.stats.LeaderboardEntry;
import com.github.mattoyudzuru.terminalbang.stats.MatchResult;
import com.github.mattoyudzuru.terminalbang.ssh.TerminalSize;
import com.github.mattoyudzuru.terminalbang.user.Account;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TerminalRendererTest {
    @Test
    void rendersResizeWarningWithActualAndMinimumSize() {
        TerminalRenderer renderer = new TerminalRenderer();

        String screen = renderer.resizeWarning(new TerminalSize(70, 20));

        assertTrue(screen.contains("Minimum size: 90x30"));
        assertTrue(screen.contains("Current size: 70x20"));
    }

    @Test
    void rendersRoomCodeInGameScreen() {
        List<PlayerSeed> seats = java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(index -> new PlayerSeed(UUID.randomUUID(), "P" + index))
                .toList();
        GameEngine engine = GameEngine.start(
                UUID.randomUUID(),
                seats,
                RandomSource.seeded(1),
                Clock.fixed(Instant.parse("2026-05-19T10:00:00Z"), ZoneOffset.UTC)
        );

        String screen = new TerminalRenderer().game(
                engine.state(),
                engine.state().currentPlayer().accountId(),
                "ABCDE",
                new GameUiState()
        );

        assertTrue(screen.contains("Room ABCDE"));
    }

    @Test
    void lobbyShowsAutoRefreshForGuests() {
        Instant now = Instant.parse("2026-05-19T10:00:00Z");
        Account host = new Account(UUID.randomUUID(), "host-fp", "Host", now, now);
        Account guest = new Account(UUID.randomUUID(), "guest-fp", "Guest", now, now);
        RoomService roomService = new RoomService(
                new NoopMatchResultRepository(),
                new RoomCodeGenerator(RandomSource.seeded(3)),
                RandomSource.seeded(4),
                Clock.fixed(now, ZoneOffset.UTC)
        );
        Room room = roomService.createRoom(host);
        roomService.joinRoom(guest, room.code());

        String screen = new TerminalRenderer().lobby(room, guest);

        assertTrue(screen.contains("Auto-refresh enabled"));
        assertFalse(screen.contains("[R]"));
    }

    @Test
    void pendingActionIsPersonalizedForExpectedPlayerAndObservers() {
        GameEngine engine = gameWithPendingAction();
        GameState state = engine.state();
        UUID expected = state.pendingAction().orElseThrow().expectedAccountId();
        UUID observer = state.players().stream()
                .map(PlayerState::accountId)
                .filter(accountId -> !accountId.equals(expected))
                .findFirst()
                .orElseThrow();
        String expectedNickname = state.findPlayer(expected).orElseThrow().nickname();
        TerminalRenderer renderer = new TerminalRenderer();

        String expectedScreen = renderer.game(state, expected, "ABCDE", new GameUiState());
        String observerScreen = renderer.game(state, observer, "ABCDE", new GameUiState());

        assertTrue(expectedScreen.contains("You may answer with") || expectedScreen.contains("You must answer with"));
        assertTrue(observerScreen.contains(expectedNickname));
        assertTrue(observerScreen.contains("answer with"));
    }

    private static final class NoopMatchResultRepository implements MatchResultRepository {
        @Override
        public void save(MatchResult result) {
        }

        @Override
        public List<LeaderboardEntry> leaderboard(int limit) {
            return List.of();
        }
    }

    private static GameEngine gameWithPendingAction() {
        for (long seed = 1; seed < 200; seed++) {
            GameEngine engine = GameEngine.start(
                    UUID.randomUUID(),
                    java.util.stream.IntStream.rangeClosed(1, 4)
                            .mapToObj(index -> new PlayerSeed(UUID.randomUUID(), "P" + index))
                            .toList(),
                    RandomSource.seeded(seed),
                    Clock.fixed(Instant.parse("2026-05-19T10:00:00Z"), ZoneOffset.UTC)
            );
            PlayerState actor = engine.state().currentPlayer();
            Optional<Integer> maybeCardIndex = java.util.stream.IntStream.range(0, actor.handSize())
                    .filter(index -> actor.hand().get(index).kind() == CardKind.SHOT
                            || actor.hand().get(index).kind() == CardKind.STANDOFF)
                    .boxed()
                    .findFirst();
            if (maybeCardIndex.isEmpty()) {
                continue;
            }
            PlayerState target = TerminalRenderer.targets(engine.state(), actor.accountId()).getFirst();
            engine.playCard(actor.accountId(), maybeCardIndex.orElseThrow(), Optional.of(target.accountId()));
            if (engine.state().pendingAction().isPresent()) {
                return engine;
            }
        }
        throw new IllegalStateException("Could not create a pending-action game");
    }
}
