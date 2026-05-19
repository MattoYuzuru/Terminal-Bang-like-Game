package com.github.mattoyudzuru.terminalbang.tui;

import com.github.mattoyudzuru.terminalbang.game.CardInstance;
import com.github.mattoyudzuru.terminalbang.game.CardKind;
import com.github.mattoyudzuru.terminalbang.game.GamePhase;
import com.github.mattoyudzuru.terminalbang.game.GameState;
import com.github.mattoyudzuru.terminalbang.game.PendingAction;
import com.github.mattoyudzuru.terminalbang.game.PendingActionType;
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
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
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
                .append(title("Room " + room.code() + " | You: " + account.nickname()))
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
            builder.append("Waiting for host. Auto-refresh enabled. [Q] Leave room\r\n");
        } else {
            builder.append("Game is starting. Auto-refresh enabled.\r\n");
        }
        return builder.toString();
    }

    public String game(GameState state, UUID viewerAccountId, String roomCode, GameUiState uiState) {
        PlayerState viewer = state.findPlayer(viewerAccountId).orElse(null);
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(header(roomCode, state, viewer))
                .append("\r\n\r\n");

        appendTable(builder, state, viewerAccountId, uiState);
        builder.append("\r\n\r\n");

        state.pendingAction().ifPresentOrElse(
                pending -> appendPending(builder, state, viewerAccountId, pending),
                () -> appendTurnHint(builder, state, viewerAccountId, uiState)
        );

        builder.append("\r\n");
        if (viewer != null) {
            appendHand(builder, viewer, uiState);
        }

        if (!uiState.message().isBlank()) {
            builder.append("\r\n").append(color(uiState.message(), YELLOW)).append("\r\n");
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

    private static String header(String roomCode, GameState state, PlayerState viewer) {
        String nickname = viewer == null ? "unknown" : viewer.nickname();
        return BOLD + color("Room " + roomCode, CYAN)
                + RESET + " | " + color("Match " + shortId(state.id()), MAGENTA)
                + " | You: " + color(nickname, GREEN)
                + " | Phase: " + color(state.phase().name(), phaseColor(state.phase()))
                + RESET;
    }

    private static void appendTable(StringBuilder builder, GameState state, UUID viewerAccountId, GameUiState uiState) {
        Optional<PlayerState> maybeViewer = state.findPlayer(viewerAccountId);
        List<PlayerState> others = state.players().stream()
                .filter(player -> !player.accountId().equals(viewerAccountId))
                .toList();
        List<PlayerState> top = others.stream().limit(3).toList();
        List<PlayerState> lower = others.stream().skip(3).toList();

        builder.append(center(joinSeats(top, state, viewerAccountId, uiState), 92)).append("\r\n");
        builder.append(center(color("      .------------------------------------------.", BLUE), 92)).append("\r\n");
        builder.append(center(color("   .-'                                            '-.", BLUE), 92)).append("\r\n");

        String left = lower.isEmpty() ? "" : seatLabel(lower.getFirst(), state, viewerAccountId, uiState);
        String right = lower.size() < 2 ? "" : seatLabel(lower.get(1), state, viewerAccountId, uiState);
        String middle = color("Deck " + state.drawPileSize(), GREEN)
                + "    "
                + color("Discard " + state.topDiscard().map(CardInstance::name).orElse("-"), YELLOW);
        builder.append(fit(left, 31))
                .append(color(" | ", BLUE))
                .append(center(middle, 28))
                .append(color(" | ", BLUE))
                .append(fit(right, 31))
                .append("\r\n");

        String left2 = lower.size() < 3 ? "" : seatLabel(lower.get(2), state, viewerAccountId, uiState);
        String right2 = lower.size() < 4 ? "" : seatLabel(lower.get(3), state, viewerAccountId, uiState);
        builder.append(fit(left2, 31))
                .append(color(" | ", BLUE))
                .append(center(color("active table", DIM), 28))
                .append(color(" | ", BLUE))
                .append(fit(right2, 31))
                .append("\r\n");

        builder.append(center(color("   '-.                                            .-'", BLUE), 92)).append("\r\n");
        builder.append(center(color("      '------------------------------------------'", BLUE), 92)).append("\r\n");
        maybeViewer.ifPresent(viewer -> builder.append(center(seatLabel(viewer, state, viewerAccountId, uiState) + "  " + color("[YOU]", GREEN), 92)).append("\r\n"));
    }

    private static String joinSeats(List<PlayerState> players, GameState state, UUID viewerAccountId, GameUiState uiState) {
        if (players.isEmpty()) {
            return color("Waiting for seats", DIM);
        }
        return players.stream()
                .map(player -> seatLabel(player, state, viewerAccountId, uiState))
                .reduce((left, right) -> left + "     " + right)
                .orElse("");
    }

    private static String seatLabel(PlayerState player, GameState state, UUID viewerAccountId, GameUiState uiState) {
        List<PlayerState> targets = targets(state, viewerAccountId);
        boolean current = state.currentPlayer().accountId().equals(player.accountId());
        boolean selectedTarget = uiState.focus() == GameFocus.TARGET
                && targets.size() > uiState.selectedTarget()
                && targets.get(uiState.selectedTarget()).accountId().equals(player.accountId());
        String marker = current ? color(">", YELLOW) : " ";
        String nickname = player.accountId().equals(viewerAccountId) ? player.nickname() : player.nickname();
        String text = marker
                + nickname
                + " HP " + healthBar(player)
                + " C" + player.handSize()
                + " " + visibleRole(player, viewerAccountId);
        if (!player.connected()) {
            text += " " + color("offline", RED);
        }
        if (player.eliminated()) {
            text += " " + color("out", RED);
        }
        return selectedTarget ? BOLD + color("[" + text + "]", YELLOW) + RESET : text;
    }

    private static void appendHand(StringBuilder builder, PlayerState player, GameUiState uiState) {
        builder.append(BOLD)
                .append(color("Your hand", CYAN))
                .append(RESET)
                .append(" | Role: ")
                .append(color(player.role().name(), MAGENTA))
                .append(" | Character: ")
                .append(color(player.character().name(), GREEN))
                .append(" | HP ")
                .append(healthBar(player))
                .append("\r\n");
        for (int i = 0; i < player.hand().size(); i++) {
            CardInstance card = player.hand().get(i);
            boolean selected = uiState.focus() == GameFocus.HAND && i == uiState.selectedCard();
            builder.append(selected ? BOLD + color("^", YELLOW) + " " : "  ");
            builder.append(cardView(i + 1, card, selected));
            builder.append("  ");
        }
        builder.append("\r\n");
    }

    private static void appendPending(StringBuilder builder, GameState state, UUID viewerAccountId, PendingAction pending) {
        String response = pending.responseKind().name();
        String expected = state.findPlayer(pending.expectedAccountId())
                .map(PlayerState::nickname)
                .orElse("Unknown player");
        if (pending.expectedAccountId().equals(viewerAccountId)) {
            if (pending.type() == PendingActionType.SHOT_REACTION) {
                builder.append(BOLD)
                        .append(color("You may answer with " + response + ".", YELLOW))
                        .append(RESET)
                        .append(" Enter to answer, Backspace to take damage.\r\n");
            } else {
                builder.append(BOLD)
                        .append(color("You must answer with " + response + ".", YELLOW))
                        .append(RESET)
                        .append(" Enter to answer, Backspace to fail.\r\n");
            }
            return;
        }
        String action = pending.type() == PendingActionType.SHOT_REACTION ? "may answer with " : "must answer with ";
        builder.append(color(expected, YELLOW))
                .append(" ")
                .append(action)
                .append(response)
                .append(". Auto-refresh enabled.\r\n");
    }

    private static void appendTurnHint(StringBuilder builder, GameState state, UUID viewerAccountId, GameUiState uiState) {
        if (state.winner().isPresent()) {
            builder.append("Winner: ").append(color(state.winner().orElseThrow().name(), GREEN)).append(". Press any key.\r\n");
            return;
        }
        if (!state.currentPlayer().accountId().equals(viewerAccountId)) {
            builder.append("Waiting for ").append(color(state.currentPlayer().nickname(), YELLOW)).append(". Auto-refresh enabled. [Q] exit session\r\n");
            return;
        }
        if (state.phase() == GamePhase.DISCARD) {
            builder.append("Discard down to health. ←/→ or 1-9 select, Enter discard, ?, /, or . for help.\r\n");
            return;
        }
        if (uiState.focus() == GameFocus.TARGET) {
            builder.append("Select target: ←/→ move, Enter confirm, Backspace cancel.\r\n");
        } else {
            builder.append("←/→ or 1-9 select card, Enter play, ?, /, or . for help, E end turn, Q exit session.\r\n");
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

    private static String cardView(int number, CardInstance card, boolean selected) {
        String text = "[" + number + " " + card.name() + "]";
        String color = cardColor(card.kind());
        return selected ? color(text, color) : color(text, color);
    }

    private static String healthBar(PlayerState player) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < player.maxHealth(); i++) {
            builder.append(i < player.health() ? "*" : ".");
        }
        return color(builder.toString(), player.health() <= 1 ? RED : GREEN);
    }

    private static String cardColor(CardKind kind) {
        return switch (kind) {
            case SHOT, STANDOFF -> RED;
            case DODGE -> BLUE;
            case SALOON -> GREEN;
            case TRAIL_RIDE -> CYAN;
            case DISARM, RUSTLE -> YELLOW;
        };
    }

    private static String phaseColor(GamePhase phase) {
        return switch (phase) {
            case PLAY -> GREEN;
            case DISCARD -> YELLOW;
            case FINISHED -> MAGENTA;
        };
    }

    private static String title(String text) {
        return BOLD + color(text, CYAN) + RESET + "\r\n" + "=".repeat(Math.max(3, text.length())) + "\r\n\r\n";
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static String center(String text, int width) {
        int visible = visibleLength(text);
        if (visible >= width) {
            return text;
        }
        return " ".repeat((width - visible) / 2) + text;
    }

    private static String fit(String text, int width) {
        int visible = visibleLength(text);
        if (visible >= width) {
            return text;
        }
        return text + " ".repeat(width - visible);
    }

    private static int visibleLength(String text) {
        return text.replaceAll("\u001B\\[[;\\d]*m", "").length();
    }

    private static String color(String text, String color) {
        return color + text + RESET;
    }
}
