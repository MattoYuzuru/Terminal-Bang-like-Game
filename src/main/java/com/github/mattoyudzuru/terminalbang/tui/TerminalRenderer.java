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
    private static final String RESET = "\u001B[0m";

    public String resizeWarning(TerminalSize size) {
        return resizeWarning(size, "en");
    }

    public String resizeWarning(TerminalSize size, Account account) {
        return resizeWarning(size, I18n.lang(account));
    }

    private String resizeWarning(TerminalSize size, String language) {
        return CLEAR
                + I18n.t(language, "Terminal is too small.", "Терминал слишком маленький.") + "\r\n\r\n"
                + I18n.t(language, "Minimum size: ", "Минимальный размер: ")
                + TerminalSize.MIN_COLUMNS + "x" + TerminalSize.MIN_ROWS + "\r\n"
                + I18n.t(language, "Current size: ", "Текущий размер: ")
                + size.columns() + "x" + size.rows() + "\r\n\r\n"
                + I18n.t(language,
                "Resize the window and reconnect, or press Q to exit.",
                "Измените размер окна и переподключитесь или нажмите Q для выхода.")
                + "\r\n";
    }

    public String mainMenu(Account account) {
        String language = I18n.lang(account);
        return CLEAR
                + title(I18n.t(language, "Terminal Western Card Game", "Терминальная вестерн-игра"), language)
                + I18n.t(language, "Signed in as: ", "Вы вошли как: ") + account.nickname() + "\r\n\r\n"
                + "[1] " + I18n.t(language, "Play", "Играть") + "\r\n"
                + "[2] " + I18n.t(language, "Profile", "Профиль") + "\r\n"
                + "[3] " + I18n.t(language, "Leaderboard", "Лидерборд") + "\r\n"
                + "[4] " + I18n.t(language, "Settings", "Настройки") + "\r\n"
                + "[Q] " + I18n.t(language, "Exit", "Выход") + "\r\n";
    }

    public String playMenu(Account account, List<Room> publicRooms) {
        String language = I18n.lang(account);
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(title(I18n.t(language, "Play", "Играть"), language))
                .append("[1] ").append(I18n.t(language, "Create private room", "Создать приватную комнату")).append("\r\n")
                .append("[2] ").append(I18n.t(language, "Join public room", "Войти в публичную комнату")).append("\r\n")
                .append("[3] ").append(I18n.t(language, "Join private room by code", "Войти в приватную комнату по коду")).append("\r\n")
                .append("[B] ").append(I18n.t(language, "Back", "Назад")).append("\r\n\r\n");
        builder.append(I18n.t(language, "Public rooms: ", "Публичных комнат: ")).append(publicRooms.size()).append("\r\n");
        return builder.toString();
    }

    public String publicRooms(Account account, List<Room> rooms) {
        String language = I18n.lang(account);
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(title(I18n.t(language, "Public Rooms", "Публичные комнаты"), language));
        if (rooms.isEmpty()) {
            builder.append(I18n.t(language, "No public rooms yet.", "Публичных комнат пока нет.")).append("\r\n\r\n");
        } else {
            for (int i = 0; i < rooms.size() && i < 9; i++) {
                Room room = rooms.get(i);
                builder.append("[")
                        .append(i + 1)
                        .append("] ")
                        .append(room.code())
                        .append(I18n.t(language, " host=", " хост="))
                        .append(room.seats().getFirst().nickname())
                        .append(I18n.t(language, " players=", " игроки="))
                        .append(room.seats().size())
                        .append("/7\r\n");
            }
            builder.append("\r\n");
        }
        builder.append("[B] ").append(I18n.t(language, "Back", "Назад")).append("\r\n");
        return builder.toString();
    }

    public String profile(Account account) {
        String language = I18n.lang(account);
        return CLEAR
                + title(I18n.t(language, "Profile", "Профиль"), language)
                + I18n.t(language, "Nickname: ", "Ник: ") + account.nickname() + "\r\n"
                + I18n.t(language, "Language: ", "Язык: ") + I18n.languageName(language) + "\r\n"
                + I18n.t(language, "SSH key: ", "SSH ключ: ") + account.sshFingerprint() + "\r\n\r\n"
                + I18n.t(language, "Press any key to return.", "Нажмите любую клавишу, чтобы вернуться.") + "\r\n";
    }

    public String leaderboard(Account account, List<LeaderboardEntry> entries) {
        String language = I18n.lang(account);
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(title(I18n.t(language, "Leaderboard", "Лидерборд"), language));
        if (entries.isEmpty()) {
            builder.append(I18n.t(language, "No completed matches yet.", "Завершённых матчей пока нет.")).append("\r\n");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                builder.append(i + 1)
                        .append(". ")
                        .append(entry.nickname())
                        .append(I18n.t(language, " wins=", " побед="))
                        .append(entry.wins())
                        .append(I18n.t(language, " matches=", " матчей="))
                        .append(entry.matches())
                        .append("\r\n");
            }
        }
        builder.append("\r\n").append(I18n.t(language, "Press any key to return.", "Нажмите любую клавишу, чтобы вернуться.")).append("\r\n");
        return builder.toString();
    }

    public String lobby(Room room, Account account) {
        String language = I18n.lang(account);
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(title(I18n.t(language, "Room ", "Комната ") + room.code() + " | "
                        + I18n.t(language, "You: ", "Вы: ") + account.nickname(), language))
                .append(I18n.t(language, "Visibility: ", "Видимость: "))
                .append(roomVisibility(language, room.visibility()))
                .append(I18n.t(language, " | Status: ", " | Статус: "))
                .append(roomStatus(language, room.status()))
                .append("\r\n\r\n");
        for (int i = 0; i < room.seats().size(); i++) {
            Account seat = room.seats().get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(seat.nickname());
            if (seat.id().equals(room.hostAccountId())) {
                builder.append(I18n.t(language, " [host]", " [хост]"));
            }
            if (seat.id().equals(account.id())) {
                builder.append(I18n.t(language, " [you]", " [вы]"));
            }
            builder.append("\r\n");
        }
        builder.append("\r\n");
        if (room.status() == RoomStatus.WAITING && account.id().equals(room.hostAccountId())) {
            builder.append("[P] ").append(I18n.t(language, "Toggle public/private", "Переключить public/private"))
                    .append("  [S] ").append(I18n.t(language, "Start", "Старт"))
                    .append("  [K] ").append(I18n.t(language, "Kick", "Кик"))
                    .append("  [Q] ").append(I18n.t(language, "Close room", "Закрыть комнату")).append("\r\n");
        } else if (room.status() == RoomStatus.WAITING) {
            builder.append(I18n.t(language, "Waiting for host. Auto-refresh enabled. [Q] Leave room",
                    "Ждём хоста. Автообновление включено. [Q] Выйти из комнаты")).append("\r\n");
        } else {
            builder.append(I18n.t(language, "Game is starting. Auto-refresh enabled.",
                    "Игра начинается. Автообновление включено.")).append("\r\n");
        }
        return builder.toString();
    }

    public String settings(Account account) {
        String language = I18n.lang(account);
        return CLEAR
                + title(I18n.t(language, "Settings", "Настройки"), language)
                + "[1] " + I18n.t(language, "Nickname", "Ник") + ": " + account.nickname() + "\r\n"
                + "[2] " + I18n.t(language, "Language", "Язык") + ": " + I18n.languageName(language) + "\r\n"
                + "[B] " + I18n.t(language, "Back", "Назад") + "\r\n";
    }

    public String languageMenu(Account account) {
        String language = I18n.lang(account);
        return CLEAR
                + title(I18n.t(language, "Language", "Язык"), language)
                + "[1] English\r\n"
                + "[2] Русский\r\n"
                + "[B] " + I18n.t(language, "Back", "Назад") + "\r\n";
    }

    public String game(GameState state, Account viewerAccount, String roomCode, GameUiState uiState) {
        return game(state, viewerAccount.id(), I18n.lang(viewerAccount), roomCode, uiState);
    }

    public String game(GameState state, UUID viewerAccountId, String roomCode, GameUiState uiState) {
        return game(state, viewerAccountId, "en", roomCode, uiState);
    }

    public String cardHelp(CardInstance card, Account account) {
        String language = I18n.lang(account);
        return CLEAR
                + title(I18n.cardName(language, card), language)
                + I18n.cardDescription(language, card)
                + "\r\n\r\n" + I18n.t(language, "Press any key to return.", "Нажмите любую клавишу, чтобы вернуться.") + "\r\n";
    }

    public String message(Account account, String title, String message) {
        String language = I18n.lang(account);
        return CLEAR + title(title, language) + message + "\r\n\r\n"
                + I18n.t(language, "Press any key to continue.", "Нажмите любую клавишу, чтобы продолжить.") + "\r\n";
    }

    private String game(GameState state, UUID viewerAccountId, String language, String roomCode, GameUiState uiState) {
        PlayerState viewer = state.findPlayer(viewerAccountId).orElse(null);
        StringBuilder builder = new StringBuilder(CLEAR)
                .append(header(roomCode, state, viewer, language))
                .append("\r\n\r\n");

        appendTable(builder, state, viewerAccountId, uiState, language);
        builder.append("\r\n\r\n");

        state.pendingAction().ifPresentOrElse(
                pending -> appendPending(builder, state, viewerAccountId, pending, uiState, language),
                () -> appendTurnHint(builder, state, viewerAccountId, uiState, language)
        );

        builder.append("\r\n");
        if (viewer != null) {
            appendHand(builder, viewer, uiState, language);
        }

        if (!uiState.message().isBlank()) {
            builder.append("\r\n").append(color(uiState.message(), YELLOW)).append("\r\n");
        }

        builder.append("\r\n").append(DIM).append(I18n.t(language, "Log", "Лог")).append(RESET).append("\r\n");
        List<String> log = state.log();
        int from = Math.max(0, log.size() - 6);
        for (String line : log.subList(from, log.size())) {
            builder.append("- ").append(localizeLog(line, language)).append("\r\n");
        }
        return builder.toString();
    }

    private static String header(String roomCode, GameState state, PlayerState viewer, String language) {
        String nickname = viewer == null ? I18n.t(language, "unknown", "неизвестно") : viewer.nickname();
        return BOLD + color(I18n.t(language, "Room ", "Комната ") + roomCode, CYAN)
                + RESET + " | " + color(I18n.t(language, "Match ", "Матч ") + shortId(state.id()), MAGENTA)
                + " | " + I18n.t(language, "You: ", "Вы: ") + color(nickname, GREEN)
                + " | " + I18n.t(language, "Phase: ", "Фаза: ") + color(I18n.phase(language, state.phase()), phaseColor(state.phase()))
                + RESET;
    }

    private static void appendTable(
            StringBuilder builder,
            GameState state,
            UUID viewerAccountId,
            GameUiState uiState,
            String language
    ) {
        Optional<PlayerState> maybeViewer = state.findPlayer(viewerAccountId);
        List<PlayerState> others = state.players().stream()
                .filter(player -> !player.accountId().equals(viewerAccountId))
                .toList();
        List<PlayerState> top = others.stream().limit(3).toList();
        List<PlayerState> lower = others.stream().skip(3).toList();

        builder.append(center(joinSeats(top, state, viewerAccountId, uiState, language), 92)).append("\r\n");
        builder.append(center(color("      .------------------------------------------.", BLUE), 92)).append("\r\n");
        builder.append(center(color("   .-'                                            '-.", BLUE), 92)).append("\r\n");

        String left = lower.isEmpty() ? "" : seatLabel(lower.getFirst(), state, viewerAccountId, uiState, language);
        String right = lower.size() < 2 ? "" : seatLabel(lower.get(1), state, viewerAccountId, uiState, language);
        String middle = color(I18n.t(language, "Deck ", "Колода ") + state.drawPileSize(), GREEN)
                + "    "
                + color(I18n.t(language, "Discard ", "Сброс ")
                + state.topDiscard().map(card -> I18n.cardName(language, card)).orElse("-"), YELLOW);
        builder.append(fit(left, 31))
                .append(color(" | ", BLUE))
                .append(center(middle, 28))
                .append(color(" | ", BLUE))
                .append(fit(right, 31))
                .append("\r\n");

        String left2 = lower.size() < 3 ? "" : seatLabel(lower.get(2), state, viewerAccountId, uiState, language);
        String right2 = lower.size() < 4 ? "" : seatLabel(lower.get(3), state, viewerAccountId, uiState, language);
        builder.append(fit(left2, 31))
                .append(color(" | ", BLUE))
                .append(center(color(I18n.t(language, "active table", "игровой стол"), DIM), 28))
                .append(color(" | ", BLUE))
                .append(fit(right2, 31))
                .append("\r\n");

        builder.append(center(color("   '-.                                            .-'", BLUE), 92)).append("\r\n");
        builder.append(center(color("      '------------------------------------------'", BLUE), 92)).append("\r\n");
        maybeViewer.ifPresent(viewer -> builder.append(center(seatLabel(viewer, state, viewerAccountId, uiState, language)
                + "  " + color(I18n.t(language, "[YOU]", "[ВЫ]"), GREEN), 92)).append("\r\n"));
    }

    private static String joinSeats(
            List<PlayerState> players,
            GameState state,
            UUID viewerAccountId,
            GameUiState uiState,
            String language
    ) {
        if (players.isEmpty()) {
            return color(I18n.t(language, "Waiting for seats", "Ожидание игроков"), DIM);
        }
        return players.stream()
                .map(player -> seatLabel(player, state, viewerAccountId, uiState, language))
                .reduce((left, right) -> left + "     " + right)
                .orElse("");
    }

    private static String seatLabel(PlayerState player, GameState state, UUID viewerAccountId, GameUiState uiState, String language) {
        List<PlayerState> targets = targets(state, viewerAccountId);
        boolean current = state.currentPlayer().accountId().equals(player.accountId());
        boolean selectedTarget = uiState.focus() == GameFocus.TARGET
                && targets.size() > uiState.selectedTarget()
                && targets.get(uiState.selectedTarget()).accountId().equals(player.accountId());
        String marker = current ? color(">", YELLOW) : " ";
        String text = marker
                + player.nickname()
                + " HP " + healthBar(player)
                + " C" + player.handSize()
                + " T" + player.inPlaySize()
                + " " + visibleRole(player, viewerAccountId, language);
        if (!player.connected()) {
            text += " " + color(I18n.t(language, "offline", "оффлайн"), RED);
        }
        if (player.eliminated()) {
            text += " " + color(I18n.t(language, "out", "выбыл"), RED);
        }
        return selectedTarget ? BOLD + color("[" + text + "]", YELLOW) + RESET : text;
    }

    private static void appendHand(StringBuilder builder, PlayerState player, GameUiState uiState, String language) {
        builder.append(BOLD)
                .append(color(I18n.t(language, "Your hand", "Ваша рука"), CYAN))
                .append(RESET)
                .append(" | ").append(I18n.t(language, "Role: ", "Роль: "))
                .append(color(I18n.role(language, player.role()), MAGENTA))
                .append(" | ").append(I18n.t(language, "Character: ", "Персонаж: "))
                .append(color(I18n.characterName(language, player.character()), GREEN))
                .append(" | HP ")
                .append(healthBar(player))
                .append("\r\n");
        builder.append(I18n.t(language, "Ability: ", "Способность: "))
                .append(I18n.characterDescription(language, player.character()))
                .append("\r\n");
        if (!player.inPlay().isEmpty()) {
            builder.append(I18n.t(language, "In play: ", "На столе: "));
            for (CardInstance card : player.inPlay()) {
                builder.append(color(I18n.cardName(language, card) + "(" + card.code() + ")", cardColor(card.kind()))).append("  ");
            }
            builder.append("\r\n");
        }
        for (int i = 0; i < player.hand().size(); i++) {
            CardInstance card = player.hand().get(i);
            boolean selected = uiState.focus() == GameFocus.HAND && i == uiState.selectedCard();
            builder.append(selected ? BOLD + color(">", YELLOW) + " " : "  ");
            builder.append(cardView(i + 1, card, selected, language));
            builder.append(selected ? " " + color("<", YELLOW) + RESET + "  " : "  ");
        }
        builder.append("\r\n");
        builder.append(DIM)
                .append(I18n.t(language,
                        "Code: 9S = rank+suit for draw! checks; H/D red, S/C black.",
                        "Код: 9S = ранг+масть для draw!; H/D красные, S/C чёрные."))
                .append(RESET)
                .append("\r\n");
    }

    private static void appendPending(
            StringBuilder builder,
            GameState state,
            UUID viewerAccountId,
            PendingAction pending,
            GameUiState uiState,
            String language
    ) {
        if (pending.type() == PendingActionType.GENERAL_STORE_PICK) {
            appendGeneralStoreChoice(builder, state, viewerAccountId, pending, uiState, language);
            return;
        }
        String response = I18n.cardName(language, pending.responseKind());
        String expected = state.findPlayer(pending.expectedAccountId())
                .map(PlayerState::nickname)
                .orElse(I18n.t(language, "Unknown player", "Неизвестный игрок"));
        if (pending.expectedAccountId().equals(viewerAccountId)) {
            if (pending.type() == PendingActionType.BANG_REACTION) {
                builder.append(BOLD)
                        .append(color(I18n.t(language, "You may answer with ", "Можно ответить картой ") + response + ".", YELLOW))
                        .append(RESET)
                        .append(I18n.t(language, " Enter to answer, Backspace to take damage.",
                                " Enter - ответить, Backspace - получить урон."))
                        .append("\r\n");
            } else {
                builder.append(BOLD)
                        .append(color(I18n.t(language, "You must answer with ", "Нужно ответить картой ") + response + ".", YELLOW))
                        .append(RESET)
                        .append(I18n.t(language, " Enter to answer, Backspace to fail.",
                                " Enter - ответить, Backspace - отказаться."))
                        .append("\r\n");
            }
            return;
        }
        String action = pending.type() == PendingActionType.BANG_REACTION
                ? I18n.t(language, "may answer with ", "может ответить картой ")
                : I18n.t(language, "must answer with ", "должен ответить картой ");
        builder.append(color(expected, YELLOW))
                .append(" ")
                .append(action)
                .append(response)
                .append(I18n.t(language, ". Auto-refresh enabled.", ". Автообновление включено."))
                .append("\r\n");
    }

    private static void appendGeneralStoreChoice(
            StringBuilder builder,
            GameState state,
            UUID viewerAccountId,
            PendingAction pending,
            GameUiState uiState,
            String language
    ) {
        String expected = state.findPlayer(pending.expectedAccountId())
                .map(PlayerState::nickname)
                .orElse(I18n.t(language, "Unknown player", "Неизвестный игрок"));
        if (!pending.expectedAccountId().equals(viewerAccountId)) {
            builder.append(color(expected, YELLOW))
                    .append(I18n.t(language,
                            " is choosing a General Store card. Auto-refresh enabled.",
                            " выбирает карту из Магазина. Автообновление включено."))
                    .append("\r\n");
            appendChoiceCards(builder, pending.choiceCards(), -1, language);
            return;
        }
        int selected = Math.min(uiState.selectedChoice(), Math.max(0, pending.choiceCards().size() - 1));
        builder.append(BOLD)
                .append(color(I18n.t(language, "Choose one General Store card.", "Выберите одну карту из Магазина."), YELLOW))
                .append(RESET)
                .append(I18n.t(language,
                        " ←/→ or 1-9 select, Enter take, ?, /, or . for help.",
                        " ←/→ или 1-9 выбор, Enter взять, ?, / или . описание."))
                .append("\r\n");
        appendChoiceCards(builder, pending.choiceCards(), selected, language);
    }

    private static void appendTurnHint(StringBuilder builder, GameState state, UUID viewerAccountId, GameUiState uiState, String language) {
        if (state.winner().isPresent()) {
            builder.append(I18n.t(language, "Winner: ", "Победитель: "))
                    .append(color(I18n.winner(language, state.winner().orElseThrow()), GREEN))
                    .append(I18n.t(language, ". Press any key.", ". Нажмите любую клавишу."))
                    .append("\r\n");
            return;
        }
        if (!state.currentPlayer().accountId().equals(viewerAccountId)) {
            builder.append(I18n.t(language, "Waiting for ", "Ожидание игрока "))
                    .append(color(state.currentPlayer().nickname(), YELLOW))
                    .append(I18n.t(language, ". Auto-refresh enabled. [Q] exit session",
                            ". Автообновление включено. [Q] выйти из сессии"))
                    .append("\r\n");
            return;
        }
        if (state.phase() == GamePhase.DISCARD) {
            builder.append(I18n.t(language,
                    "Discard down to health. ←/→ or 1-9 select, Enter discard, ?, /, or . for help.",
                    "Сбросьте карты до лимита жизней. ←/→ или 1-9 выбор, Enter сбросить, ?, / или . описание."))
                    .append("\r\n");
            return;
        }
        if (uiState.focus() == GameFocus.TARGET) {
            builder.append(I18n.t(language,
                    "Select target: ←/→ move, Enter confirm, Backspace cancel.",
                    "Выберите цель: ←/→ перемещение, Enter подтвердить, Backspace отмена."))
                    .append("\r\n");
            appendTargetList(builder, state, viewerAccountId, uiState, language);
        } else {
            builder.append(I18n.t(language,
                    "←/→ or 1-9 select card, Enter play, ?, /, or . for help, E end turn, Q exit session.",
                    "←/→ или 1-9 выбор карты, Enter сыграть, ?, / или . описание, E конец хода, Q выход."))
                    .append("\r\n");
        }
    }

    private static void appendChoiceCards(StringBuilder builder, List<CardInstance> cards, int selected, String language) {
        for (int i = 0; i < cards.size(); i++) {
            CardInstance card = cards.get(i);
            boolean active = i == selected;
            builder.append(active ? BOLD + color(">", YELLOW) + " " : "  ");
            builder.append(cardView(i + 1, card, active, language));
            builder.append(active ? " " + color("<", YELLOW) + RESET + "  " : "  ");
        }
        builder.append("\r\n");
    }

    private static void appendTargetList(
            StringBuilder builder,
            GameState state,
            UUID viewerAccountId,
            GameUiState uiState,
            String language
    ) {
        List<PlayerState> targets = targets(state, viewerAccountId);
        for (int i = 0; i < targets.size(); i++) {
            PlayerState target = targets.get(i);
            boolean active = i == uiState.selectedTarget();
            builder.append(active ? BOLD + color(">", YELLOW) + " " : "  ");
            builder.append(i + 1)
                    .append(" ")
                    .append(target.nickname())
                    .append(" HP ")
                    .append(healthBar(target))
                    .append(" C")
                    .append(target.handSize())
                    .append(" T")
                    .append(target.inPlaySize());
            builder.append(active ? " " + color("<", YELLOW) + RESET + "  " : "  ");
        }
        builder.append("\r\n");
    }

    static List<PlayerState> targets(GameState state, UUID viewerAccountId) {
        return state.alivePlayers().stream()
                .filter(player -> !player.accountId().equals(viewerAccountId))
                .toList();
    }

    private static String visibleRole(PlayerState player, UUID viewerAccountId, String language) {
        if (player.accountId().equals(viewerAccountId) || player.role().visibleAtStart() || player.eliminated()) {
            return I18n.role(language, player.role());
        }
        return I18n.t(language, "HIDDEN", "СКРЫТО");
    }

    private static String cardView(int number, CardInstance card, boolean selected, String language) {
        String text = "[" + number + " " + I18n.cardName(language, card) + " " + card.code() + "]";
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
            case BANG, DUEL, GATLING, INDIANS -> RED;
            case MISSED, BARREL, JAIL, DYNAMITE -> BLUE;
            case BEER, SALOON -> GREEN;
            case STAGECOACH, WELLS_FARGO, GENERAL_STORE -> CYAN;
            case CAT_BALOU, PANIC -> YELLOW;
            case MUSTANG, SCOPE, REMINGTON, REV_CARABINE, SCHOFIELD, VOLCANIC, WINCHESTER -> MAGENTA;
        };
    }

    private static String phaseColor(GamePhase phase) {
        return switch (phase) {
            case PLAY -> GREEN;
            case DISCARD -> YELLOW;
            case FINISHED -> MAGENTA;
        };
    }

    private static String title(String text, String language) {
        return BOLD + color(text, CYAN) + RESET + "\r\n" + "=".repeat(Math.max(3, visibleLength(text))) + "\r\n\r\n";
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

    private static String roomVisibility(String language, RoomVisibility visibility) {
        if (!"ru".equals(I18n.lang(language))) {
            return visibility.name();
        }
        return switch (visibility) {
            case PRIVATE -> "ПРИВАТНАЯ";
            case PUBLIC -> "ПУБЛИЧНАЯ";
        };
    }

    private static String roomStatus(String language, RoomStatus status) {
        if (!"ru".equals(I18n.lang(language))) {
            return status.name();
        }
        return switch (status) {
            case WAITING -> "ОЖИДАНИЕ";
            case IN_GAME -> "В ИГРЕ";
            case FINISHED -> "ЗАВЕРШЕНА";
        };
    }

    private static String localizeLog(String line, String language) {
        if (!"ru".equals(I18n.lang(language))) {
            return line;
        }
        return line
                .replace(" starts as Sheriff.", " начинает как Шериф.")
                .replace(" starts a turn.", " начинает ход.")
                .replace(" draws two cards.", " берёт две карты.")
                .replace(" draws three cards.", " берёт три карты.")
                .replace(" must discard down to health limit.", " должен сбросить карты до лимита жизней.")
                .replace(" discards a card.", " сбрасывает карту.")
                .replace(" disconnected.", " отключился.")
                .replace(" reconnected.", " переподключился.")
                .replace(" is disconnected. Waiting before skip.", " отключён. Ждём перед пропуском.")
                .replace(" timed out. Turn skipped.", " не успел вернуться. Ход пропущен.")
                .replace(" cancels the Bang.", " отменяет Бэнг.")
                .replace(" answers the duel.", " отвечает в дуэли.")
                .replace(" opens General Store.", " открывает Магазин.")
                .replace(" opens the Saloon. Everyone recovers one life.", " открывает Салун. Все восстанавливают 1 жизнь.")
                .replace(" recovers one life.", " восстанавливает 1 жизнь.")
                .replace(" is eliminated as ", " выбывает с ролью ")
                .replace("Winner: LAW.", "Победитель: ЗАКОН.")
                .replace("Winner: OUTLAWS.", "Победитель: БАНДИТЫ.")
                .replace("Winner: RENEGADE.", "Победитель: РЕНЕГАТ.");
    }
}
