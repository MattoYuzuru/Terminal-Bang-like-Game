package com.github.mattoyudzuru.terminalbang.stats;

import com.github.mattoyudzuru.terminalbang.game.Role;

import java.util.UUID;

public record PlayerMatchStat(
        UUID accountId,
        String nickname,
        Role role,
        boolean won,
        boolean eliminated,
        int damageDealt,
        int damageTaken,
        int cardsPlayed
) {
}

