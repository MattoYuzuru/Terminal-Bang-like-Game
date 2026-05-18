package com.github.mattoyudzuru.terminalbang.tui;

import com.github.mattoyudzuru.terminalbang.game.CardInstance;
import com.github.mattoyudzuru.terminalbang.game.GamePhase;
import com.github.mattoyudzuru.terminalbang.game.GameState;
import com.github.mattoyudzuru.terminalbang.game.PlayerState;
import com.github.mattoyudzuru.terminalbang.room.Room;
import com.github.mattoyudzuru.terminalbang.room.RoomStatus;
import com.github.mattoyudzuru.terminalbang.room.RoomVisibility;
import com.github.mattoyudzuru.terminalbang.ssh.TerminalSize;
import com.github.mattoyudzuru.terminalbang.stats.LeaderboardEntry;
import com.github.mattoyudzuru.terminalbang.user.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TerminalRenderer {
    private static final String CLEAR = "\u001B[2J\u001B[H";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String RESET = "\u001B[0m";

    public String resizeWarning(TerminalSize size) {
        return CLEAR
                + "Terminal is too small.\r\n\r\n"
                + "Minimum size: " + TerminalSize.MIN_COLUMNS + "x" + TerminalSize.MIN_ROWS + "\r\n"
                + "Current size: " + size.columns() + "x" + size.rows() + "\r\n\r\n"
                + "Resize the window and reconnect, or press Q to exit.\r\n";
    }

    public String mainMenu(Account account) {
        return CLEAR
                + title("Terminal Western Card Game")
                + "Signed in as: " + account.nickname() + "\r\n\r\n"
                + "[1] Play\r\n"
                + "[2] Profile\r\n"
                + "[3] Leaderboard\r\n"
                + "[4] Settings\r\n"
                + "[Q] Exit\r\n";
    }

    public String playMenu(List<Room> publicRooms) {
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(title("Play"))
                .append("[1] Create private room\r\n")
                .append("[2] Join public room\r\n")
                .append("[3] Join private room by code\r\n")
                .append("[B] Back\r\n\r\n");
        builder.append("Public rooms: ").append(publicRooms.size()).append("\r\n");
        return builder.toString();
    }

    public String publicRooms(List<Room> rooms) {
        StringBuilder builder = new StringBuilder(CLEAR).append(title("Public Rooms"));
        if (rooms.isEmpty()) {
            builder.append("No public rooms yet.\r\n\r\n");
        } else {
            for (int i = 0; i < rooms.size() && i < 9; i++) {
                Room room = rooms.get(i);
                builder.append("[")
                        .append(i + 1)
                        .append("] ")
                        .append(room.code())
                        .append(" host=")
                        .append(room.seats().getFirst().nickname())
                        .append(" players=")
                        .append(room.seats().size())
                        .append("/7\r\n");
            }
            builder.append("\r\n");
        }
        builder.append("[B] Back\r\n");
        return builder.toString();
    }

    public String profile(Account account) {
        return CLEAR
                + title("Profile")
                + "Nickname: " + account.nickname() + "\r\n"
                + "SSH key: " + account.sshFingerprint() + "\r\n\r\n"
                + "Press any key to return.\r\n";
    }

    public String leaderboard(List<LeaderboardEntry> entries) {
        StringBuilder builder = new StringBuilder(CLEAR).append(title("Leaderboard"));
        if (entries.isEmpty()) {
            builder.append("No completed matches yet.\r\n");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                builder.append(i + 1)
                        .append(". ")
                        .append(entry.nickname())
                        .append(" wins=")
                        .append(entry.wins())
                        .append(" matches=")
                        .append(entry.matches())
                        .append("\r\n");
            }
        }
        builder.append("\r\nPress any key to return.\r\n");
        return builder.toString();
    }

    public String lobby(Room room, Account account) {
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(title("Room " + room.code()))
                .append("Visibility: ")
                .append(room.visibility())
                .append(" | Status: ")
                .append(room.status())
                .append("\r\n\r\n");
        for (int i = 0; i < room.seats().size(); i++) {
            Account seat = room.seats().get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(seat.nickname());
            if (seat.id().equals(room.hostAccountId())) {
                builder.append(" [host]");
            }
            if (seat.id().equals(account.id())) {
                builder.append(" [you]");
            }
            builder.append("\r\n");
        }
        builder.append("\r\n");
        if (room.status() == RoomStatus.WAITING && account.id().equals(room.hostAccountId())) {
            builder.append("[P] Toggle public/private  [S] Start  [K] Kick  [Q] Close room\r\n");
        } else if (room.status() == RoomStatus.WAITING) {
            builder.append("[R] Refresh  [Q] Leave room\r\n");
        } else {
            builder.append("Game is starting. Press any key.\r\n");
        }
        return builder.toString();
    }

    public String game(GameState state, UUID viewerAccountId, GameUiState uiState) {
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(title("Match " + shortId(state.id())))
                .append("Deck: ")
                .append(state.drawPileSize())
                .append(" | Discard: ")
                .append(state.topDiscard().map(CardInstance::name).orElse("-"))
                .append(" | Phase: ")
                .append(state.phase())
                .append("\r\n\r\n");

        appendPlayers(builder, state, viewerAccountId, uiState);
        builder.append("\r\n");

        Optional<PlayerState> viewer = state.findPlayer(viewerAccountId);
        viewer.ifPresent(player -> appendHand(builder, player, uiState));

        builder.append("\r\n");
        state.pendingAction().ifPresentOrElse(
                pending -> builder.append(BOLD).append(pending.prompt()).append(RESET).append("\r\n"),
                () -> appendTurnHint(builder, state, viewerAccountId, uiState)
        );

        if (!uiState.message().isBlank()) {
            builder.append("\r\n").append(uiState.message()).append("\r\n");
        }

        builder.append("\r\n").append(DIM).append("Log").append(RESET).append("\r\n");
        List<String> log = state.log();
        int from = Math.max(0, log.size() - 6);
        for (String line : log.subList(from, log.size())) {
            builder.append("- ").append(line).append("\r\n");
        }
        return builder.toString();
    }

    public String cardHelp(CardInstance card) {
        return CLEAR
                + title(card.name())
                + card.definition().description()
                + "\r\n\r\nPress any key to return.\r\n";
    }

    public String message(String title, String message) {
        return CLEAR + title(title) + message + "\r\n\r\nPress any key to continue.\r\n";
    }

    private static void appendPlayers(StringBuilder builder, GameState state, UUID viewerAccountId, GameUiState uiState) {
        List<PlayerState> targets = targets(state, viewerAccountId);
        for (int i = 0; i < state.players().size(); i++) {
            PlayerState player = state.players().get(i);
            boolean current = state.currentPlayer().accountId().equals(player.accountId());
            boolean selectedTarget = uiState.focus() == GameFocus.TARGET
                    && targets.size() > uiState.selectedTarget()
                    && targets.get(uiState.selectedTarget()).accountId().equals(player.accountId());
            builder.append(current ? "=> " : "   ");
            builder.append(selectedTarget ? BOLD + "[" : " ");
            builder.append(player.nickname())
                    .append(" HP=")
                    .append(player.health())
                    .append("/")
                    .append(player.maxHealth())
                    .append(" cards=")
                    .append(player.handSize())
                    .append(" role=")
                    .append(visibleRole(player, viewerAccountId))
                    .append(player.connected() ? "" : " disconnected")
                    .append(player.eliminated() ? " eliminated" : "");
            builder.append(selectedTarget ? "]" + RESET : "");
            builder.append("\r\n");
        }
    }

    private static void appendHand(StringBuilder builder, PlayerState player, GameUiState uiState) {
        builder.append("Your role: ").append(player.role())
                .append(" | Character: ").append(player.character().name())
                .append(" | HP: ").append(player.health()).append("/")
                .append(player.maxHealth()).append("\r\n");
        builder.append("Hand:\r\n");
        for (int i = 0; i < player.hand().size(); i++) {
            CardInstance card = player.hand().get(i);
            boolean selected = uiState.focus() == GameFocus.HAND && i == uiState.selectedCard();
            builder.append(selected ? BOLD + "[" : " ");
            builder.append(i + 1).append(" ").append(card.name());
            builder.append(selected ? "]" + RESET : "");
            builder.append("  ");
        }
        builder.append("\r\n");
    }

    private static void appendTurnHint(StringBuilder builder, GameState state, UUID viewerAccountId, GameUiState uiState) {
        if (state.winner().isPresent()) {
            builder.append("Winner: ").append(state.winner().orElseThrow()).append(". Press any key.\r\n");
            return;
        }
        if (!state.currentPlayer().accountId().equals(viewerAccountId)) {
            builder.append("Waiting for ").append(state.currentPlayer().nickname()).append(". [Q] exit session\r\n");
            return;
        }
        if (state.phase() == GamePhase.DISCARD) {
            builder.append("Discard down to health. ←/→ or 1-9 select, Enter discard, ? help.\r\n");
            return;
        }
        if (uiState.focus() == GameFocus.TARGET) {
            builder.append("Select target: ←/→ move, Enter confirm, Backspace cancel.\r\n");
        } else {
            builder.append("←/→ or 1-9 select card, Enter play, ? help, E end turn, Q exit session.\r\n");
        }
    }

    static List<PlayerState> targets(GameState state, UUID viewerAccountId) {
        return state.alivePlayers().stream()
                .filter(player -> !player.accountId().equals(viewerAccountId))
                .toList();
    }

    private static String visibleRole(PlayerState player, UUID viewerAccountId) {
        if (player.accountId().equals(viewerAccountId) || player.role().visibleAtStart() || player.eliminated()) {
            return player.role().name();
        }
        return "HIDDEN";
    }

    private static String title(String text) {
        return BOLD + text + RESET + "\r\n" + "=".repeat(Math.max(3, text.length())) + "\r\n\r\n";
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}

