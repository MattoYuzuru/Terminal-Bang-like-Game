package com.github.mattoyudzuru.terminalbang.game;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleDistributionTest {
    @Test
    void supportsClassicFourToSevenPlayerCounts() {
        assertCounts(4, 1, 0, 2, 1);
        assertCounts(5, 1, 1, 2, 1);
        assertCounts(6, 1, 1, 3, 1);
        assertCounts(7, 1, 2, 3, 1);
    }

    private static void assertCounts(
            int players,
            int sheriffs,
            int deputies,
            int outlaws,
            int renegades
    ) {
        var roles = RoleDistribution.forPlayerCount(players);
        assertEquals(sheriffs, Collections.frequency(roles, Role.SHERIFF));
        assertEquals(deputies, Collections.frequency(roles, Role.DEPUTY));
        assertEquals(outlaws, Collections.frequency(roles, Role.OUTLAW));
        assertEquals(renegades, Collections.frequency(roles, Role.RENEGADE));
    }
}

