package com.github.mattoyudzuru.terminalbang.tui;

import com.github.mattoyudzuru.terminalbang.game.CardInstance;
import com.github.mattoyudzuru.terminalbang.game.GameEngine;
import com.github.mattoyudzuru.terminalbang.game.GamePhase;
import com.github.mattoyudzuru.terminalbang.game.GameState;
import com.github.mattoyudzuru.terminalbang.game.PlayerState;
import com.github.mattoyudzuru.terminalbang.persistence.AccountRepository;
import com.github.mattoyudzuru.terminalbang.persistence.MatchResultRepository;
import com.github.mattoyudzuru.terminalbang.room.Room;
import com.github.mattoyudzuru.terminalbang.room.RoomService;
import com.github.mattoyudzuru.terminalbang.room.RoomStatus;
import com.github.mattoyudzuru.terminalbang.ssh.LoginIdentity;
import com.github.mattoyudzuru.terminalbang.ssh.TerminalSize;
import com.github.mattoyudzuru.terminalbang.user.Account;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class TerminalSession {
    private static final Duration AUTO_REFRESH_INTERVAL = Duration.ofMillis(750);

    private final AccountRepository accountRepository;
    private final MatchResultRepository matchResultRepository;
    private final RoomService roomService;
    private final TerminalRenderer renderer;

    public TerminalSession(
            AccountRepository accountRepository,
            MatchResultRepository matchResultRepository,
            RoomService roomService,
            TerminalRenderer renderer
    ) {
        this.accountRepository = accountRepository;
        this.matchResultRepository = matchResultRepository;
        this.roomService = roomService;
        this.renderer = renderer;
    }

    public void run(LoginIdentity identity, TerminalSize size, InputStream inputStream, OutputStream outputStream) throws IOException {
        SessionIo io = new SessionIo(inputStream, outputStream);
        Account account = accountRepository.findOrCreateByFingerprint(
                identity.sshFingerprint(),
                defaultNickname(identity.username())
        );
        roomService.reconnect(account.id());
        try {
            if (size.tooSmall()) {
                io.write(renderer.resizeWarning(size));
                TerminalKey key = io.readKey();
                if (key.isCharacter('q') || key.type() == TerminalKeyType.CTRL_C) {
                    return;
                }
            }

            Optional<GameEngine> activeGame = roomService.activeGame(account.id());
            if (activeGame.isPresent()) {
                runGame(io, account, activeGame.orElseThrow());
            }
            runMainMenu(io, account);
        } finally {
            roomService.disconnect(account.id());
        }
    }

    private void runMainMenu(SessionIo io, Account account) throws IOException {
        Account current = account;
        while (true) {
            io.write(renderer.mainMenu(current));
            TerminalKey key = io.readKey();
            if (key.type() == TerminalKeyType.CTRL_C || key.isCharacter('q')) {
                return;
            }
            if (key.type() != TerminalKeyType.DIGIT) {
                continue;
            }
            switch (key.digitIndex()) {
                case 0 -> runPlayMenu(io, current);
                case 1 -> {
                    io.write(renderer.profile(current));
                    io.readKey();
                }
                case 2 -> {
                    io.write(renderer.leaderboard(matchResultRepository.leaderboard(10)));
                    io.readKey();
                }
                case 3 -> current = updateNickname(io, current);
                default -> {
                }
            }
        }
    }

    private void runPlayMenu(SessionIo io, Account account) throws IOException {
        while (true) {
            io.write(renderer.playMenu(roomService.publicRooms()));
            TerminalKey key = io.readKey();
            if (key.isCharacter('b') || key.isCharacter('q') || key.type() == TerminalKeyType.CTRL_C) {
                return;
            }
            if (key.type() != TerminalKeyType.DIGIT) {
                continue;
            }
            switch (key.digitIndex()) {
                case 0 -> runLobby(io, account, roomService.createRoom(account));
                case 1 -> joinPublicRoom(io, account);
                case 2 -> joinPrivateRoom(io, account);
                default -> {
                }
            }
        }
    }

    private Account updateNickname(SessionIo io, Account account) throws IOException {
        io.write("\u001B[2J\u001B[H");
        String nickname = io.readLine("Nickname: ").trim();
        if (nickname.isBlank()) {
            return account;
        }
        return accountRepository.updateNickname(account.id(), nickname);
    }

    private void joinPublicRoom(SessionIo io, Account account) throws IOException {
        List<Room> publicRooms = roomService.publicRooms();
        io.write(renderer.publicRooms(publicRooms));
        TerminalKey key = io.readKey();
        if (key.type() != TerminalKeyType.DIGIT || key.digitIndex() >= publicRooms.size()) {
            return;
        }
        runLobby(io, account, roomService.joinRoom(account, publicRooms.get(key.digitIndex()).code()));
    }

    private void joinPrivateRoom(SessionIo io, Account account) throws IOException {
        io.write("\u001B[2J\u001B[H");
        String code = io.readLine("Room code: ").trim();
        if (code.isBlank()) {
            return;
        }
        try {
            runLobby(io, account, roomService.joinRoom(account, code));
        } catch (RuntimeException exception) {
            showMessage(io, "Join failed", exception.getMessage());
        }
    }

    private void runLobby(SessionIo io, Account account, Room room) throws IOException {
        Room currentRoom = room;
        while (true) {
            currentRoom = roomService.room(currentRoom.code()).orElse(currentRoom);
            io.write(renderer.lobby(currentRoom, account));
            if (currentRoom.status() == RoomStatus.IN_GAME) {
                Optional<GameEngine> activeGame = roomService.activeGame(account.id());
                if (activeGame.isPresent()) {
                    runGame(io, account, activeGame.orElseThrow());
                    return;
                }
            }
            Optional<TerminalKey> maybeKey = io.readKey(AUTO_REFRESH_INTERVAL);
            if (maybeKey.isEmpty()) {
                continue;
            }
            TerminalKey key = maybeKey.orElseThrow();
            if (key.type() == TerminalKeyType.CTRL_C || key.isCharacter('q')) {
                roomService.leaveWaitingRoom(account.id());
                return;
            }
            try {
                if (key.isCharacter('p')) {
                    currentRoom = roomService.togglePublic(account.id(), currentRoom.code());
                } else if (key.isCharacter('s')) {
                    runGame(io, account, roomService.startGame(account.id(), currentRoom.code()));
                    return;
                } else if (key.isCharacter('k')) {
                    kickPlayer(io, account, currentRoom);
                }
            } catch (RuntimeException exception) {
                showMessage(io, "Room action failed", exception.getMessage());
            }
        }
    }

    private void kickPlayer(SessionIo io, Account account, Room room) throws IOException {
        if (!room.hostAccountId().equals(account.id())) {
            return;
        }
        io.write("\u001B[2J\u001B[H");
        String value = io.readLine("Kick seat number: ").trim();
        if (value.isBlank()) {
            return;
        }
        int index = Integer.parseInt(value) - 1;
        if (index < 0 || index >= room.seats().size()) {
            return;
        }
        roomService.kick(account.id(), room.code(), room.seats().get(index).id());
    }

    private void runGame(SessionIo io, Account account, GameEngine engine) throws IOException {
        GameUiState uiState = new GameUiState();
        while (true) {
            roomService.tick();
            GameState state = engine.state();
            clampSelection(state, account, uiState);
            io.write(renderer.game(state, account.id(), currentRoomCode(account), uiState));
            Optional<TerminalKey> maybeKey = io.readKey(AUTO_REFRESH_INTERVAL);
            if (maybeKey.isEmpty()) {
                roomService.persistIfFinished(engine);
                continue;
            }
            TerminalKey key = maybeKey.orElseThrow();
            if (key.type() == TerminalKeyType.CTRL_C || key.isCharacter('q')) {
                return;
            }
            if (state.winner().isPresent()) {
                return;
            }
            try {
                handleGameKey(state, engine, account, uiState, key, io);
                roomService.persistIfFinished(engine);
            } catch (RuntimeException exception) {
                uiState.setMessage(exception.getMessage());
            }
        }
    }

    private void handleGameKey(
            GameState state,
            GameEngine engine,
            Account account,
            GameUiState uiState,
            TerminalKey key,
            SessionIo io
    ) throws IOException {
        if (state.pendingAction().isPresent()) {
            var pending = state.pendingAction().orElseThrow();
            if (!pending.expectedAccountId().equals(account.id())) {
                return;
            }
            if (key.type() == TerminalKeyType.ENTER) {
                engine.resolvePending(account.id(), true);
            } else if (key.type() == TerminalKeyType.BACKSPACE) {
                engine.resolvePending(account.id(), false);
            }
            return;
        }

        Optional<PlayerState> viewer = state.findPlayer(account.id());
        if (viewer.isEmpty() || !state.currentPlayer().accountId().equals(account.id())) {
            return;
        }

        PlayerState player = viewer.orElseThrow();
        if (key.type() == TerminalKeyType.LEFT) {
            moveSelection(state, account, uiState, -1);
            return;
        }
        if (key.type() == TerminalKeyType.RIGHT) {
            moveSelection(state, account, uiState, 1);
            return;
        }
        if (key.type() == TerminalKeyType.DIGIT && key.digitIndex() < player.handSize()) {
            uiState.setSelectedCard(key.digitIndex());
            uiState.setFocus(GameFocus.HAND);
            return;
        }
        if (key.isCharacter('?') || key.isCharacter('/') || key.isCharacter('.')) {
            if (player.handSize() > 0) {
                CardInstance card = player.hand().get(uiState.selectedCard());
                uiState.setMessage(card.name() + ": " + card.definition().description());
            } else {
                uiState.setMessage("No card selected.");
            }
            return;
        }
        if (key.isCharacter('e')) {
            engine.endTurn(account.id());
            uiState.setFocus(GameFocus.HAND);
            return;
        }
        if (state.phase() == GamePhase.DISCARD && key.type() == TerminalKeyType.ENTER) {
            engine.discardForLimit(account.id(), uiState.selectedCard());
            return;
        }
        if (key.type() == TerminalKeyType.BACKSPACE && uiState.focus() == GameFocus.TARGET) {
            uiState.setFocus(GameFocus.HAND);
            return;
        }
        if (key.type() == TerminalKeyType.ENTER && state.phase() == GamePhase.PLAY && player.handSize() > 0) {
            playSelectedCard(state, engine, account, uiState, player);
        }
    }

    private void playSelectedCard(GameState state, GameEngine engine, Account account, GameUiState uiState, PlayerState player) {
        CardInstance card = player.hand().get(uiState.selectedCard());
        if (card.definition().requiresTarget() && uiState.focus() == GameFocus.HAND) {
            uiState.setFocus(GameFocus.TARGET);
            return;
        }
        Optional<java.util.UUID> target = Optional.empty();
        if (card.definition().requiresTarget()) {
            List<PlayerState> targets = TerminalRenderer.targets(state, account.id());
            if (targets.isEmpty()) {
                throw new IllegalStateException("No valid targets");
            }
            target = Optional.of(targets.get(uiState.selectedTarget()).accountId());
        }
        engine.playCard(account.id(), uiState.selectedCard(), target);
        uiState.setFocus(GameFocus.HAND);
        uiState.setSelectedCard(Math.max(0, uiState.selectedCard() - 1));
    }

    private static void moveSelection(GameState state, Account account, GameUiState uiState, int delta) {
        if (uiState.focus() == GameFocus.TARGET) {
            List<PlayerState> targets = TerminalRenderer.targets(state, account.id());
            if (!targets.isEmpty()) {
                uiState.setSelectedTarget(wrap(uiState.selectedTarget() + delta, targets.size()));
            }
            return;
        }
        state.findPlayer(account.id()).ifPresent(player -> {
            if (player.handSize() > 0) {
                uiState.setSelectedCard(wrap(uiState.selectedCard() + delta, player.handSize()));
            }
        });
    }

    private static void clampSelection(GameState state, Account account, GameUiState uiState) {
        state.findPlayer(account.id()).ifPresent(player -> {
            if (player.handSize() == 0) {
                uiState.setSelectedCard(0);
            } else if (uiState.selectedCard() >= player.handSize()) {
                uiState.setSelectedCard(player.handSize() - 1);
            }
        });
        List<PlayerState> targets = TerminalRenderer.targets(state, account.id());
        if (targets.isEmpty() || uiState.selectedTarget() >= targets.size()) {
            uiState.setSelectedTarget(0);
        }
    }

    private static int wrap(int value, int size) {
        return Math.floorMod(value, size);
    }

    private void showMessage(SessionIo io, String title, String message) throws IOException {
        io.write(renderer.message(title, message == null ? "" : message));
        io.readKey();
    }

    private String currentRoomCode(Account account) {
        return roomService.activeRoom(account.id())
                .map(Room::code)
                .orElse("UNKNOWN");
    }

    private static String defaultNickname(String username) {
        if (username == null || username.isBlank()) {
            return "player";
        }
        return username;
    }
}
