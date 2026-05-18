package com.github.mattoyudzuru.terminalbang.room;

import com.github.mattoyudzuru.terminalbang.game.GameEngine;
import com.github.mattoyudzuru.terminalbang.game.RandomSource;
import com.github.mattoyudzuru.terminalbang.game.WinningSide;
import com.github.mattoyudzuru.terminalbang.persistence.MatchResultRepository;
import com.github.mattoyudzuru.terminalbang.stats.LeaderboardEntry;
import com.github.mattoyudzuru.terminalbang.stats.MatchResult;
import com.github.mattoyudzuru.terminalbang.user.Account;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-18T20:00:00Z"), ZoneOffset.UTC);
    private final RecordingMatchRepository matchRepository = new RecordingMatchRepository();
    private final RoomService service = new RoomService(
            matchRepository,
            new RoomCodeGenerator(RandomSource.seeded(9)),
            RandomSource.seeded(11),
            clock
    );

    @Test
    void createsPrivateRoomAndCanExposeItAsPublic() {
        Account host = account("Host");

        Room room = service.createRoom(host);

        assertEquals(5, room.code().length());
        assertEquals(RoomVisibility.PRIVATE, room.visibility());
        assertTrue(service.publicRooms().isEmpty());

        service.togglePublic(host.id(), room.code());

        assertEquals(RoomVisibility.PUBLIC, room.visibility());
        assertEquals(List.of(room), service.publicRooms());
    }

    @Test
    void joinsAndKicksWaitingPlayers() {
        Account host = account("Host");
        Account guest = account("Guest");
        Room room = service.createRoom(host);

        service.joinRoom(guest, room.code());

        assertEquals(2, room.seats().size());
        assertTrue(service.activeRoom(guest.id()).isPresent());

        service.kick(host.id(), room.code(), guest.id());

        assertEquals(1, room.seats().size());
        assertFalse(service.activeRoom(guest.id()).isPresent());
    }

    @Test
    void startsGameWhenRoomHasFourPlayers() {
        Room room = roomWithPlayers(4);
        Account host = room.seats().getFirst();

        GameEngine engine = service.startGame(host.id(), room.code());

        assertEquals(RoomStatus.IN_GAME, room.status());
        assertEquals(4, engine.state().players().size());
        assertTrue(service.activeGame(host.id()).isPresent());
    }

    @Test
    void abandonedGameIsPersistedWhenAllPlayersDisconnect() {
        Room room = roomWithPlayers(4);
        service.startGame(room.hostAccountId(), room.code());

        for (Account account : room.seats()) {
            service.disconnect(account.id());
        }

        assertEquals(RoomStatus.FINISHED, room.status());
        assertEquals(1, matchRepository.saved.size());
        assertEquals(WinningSide.ABANDONED, matchRepository.saved.getFirst().winner().orElseThrow());
    }

    private Room roomWithPlayers(int count) {
        Account host = account("P1");
        Room room = service.createRoom(host);
        for (int i = 2; i <= count; i++) {
            service.joinRoom(account("P" + i), room.code());
        }
        return room;
    }

    private static Account account(String nickname) {
        Instant now = Instant.parse("2026-05-18T20:00:00Z");
        return new Account(UUID.randomUUID(), "fp-" + nickname, nickname, now, now);
    }

    private static final class RecordingMatchRepository implements MatchResultRepository {
        private final List<MatchResult> saved = new ArrayList<>();

        @Override
        public void save(MatchResult result) {
            saved.add(result);
        }

        @Override
        public List<LeaderboardEntry> leaderboard(int limit) {
            return List.of();
        }
    }
}

