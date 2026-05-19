package com.github.mattoyudzuru.terminalbang.tui;

import com.github.mattoyudzuru.terminalbang.game.GameEngine;
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

    private static final class NoopMatchResultRepository implements MatchResultRepository {
        @Override
        public void save(MatchResult result) {
        }

        @Override
        public List<LeaderboardEntry> leaderboard(int limit) {
            return List.of();
        }
    }
}
