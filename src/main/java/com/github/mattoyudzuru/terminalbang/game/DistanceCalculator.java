package com.github.mattoyudzuru.terminalbang.game;

import java.util.List;
import java.util.UUID;

public final class DistanceCalculator {
    private DistanceCalculator() {
    }

    public static int distance(GameState state, UUID fromAccountId, UUID toAccountId) {
        List<PlayerState> alive = state.players().stream()
                .filter(player -> !player.eliminated())
                .toList();
        int from = indexOf(alive, fromAccountId);
        int to = indexOf(alive, toAccountId);
        int clockwise = Math.abs(from - to);
        int counterClockwise = alive.size() - clockwise;
        return Math.min(clockwise, counterClockwise);
    }

    private static int indexOf(List<PlayerState> players, UUID accountId) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).accountId().equals(accountId)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown or eliminated player: " + accountId);
    }
}

