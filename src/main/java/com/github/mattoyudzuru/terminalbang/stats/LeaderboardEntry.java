package com.github.mattoyudzuru.terminalbang.stats;

import java.util.UUID;

public record LeaderboardEntry(UUID accountId, String nickname, int matches, int wins) {
}

