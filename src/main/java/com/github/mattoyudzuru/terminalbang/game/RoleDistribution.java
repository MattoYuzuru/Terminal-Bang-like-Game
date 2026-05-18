package com.github.mattoyudzuru.terminalbang.game;

import java.util.List;

public final class RoleDistribution {
    private RoleDistribution() {
    }

    public static List<Role> forPlayerCount(int playerCount) {
        return switch (playerCount) {
            case 4 -> List.of(Role.SHERIFF, Role.RENEGADE, Role.OUTLAW, Role.OUTLAW);
            case 5 -> List.of(Role.SHERIFF, Role.DEPUTY, Role.RENEGADE, Role.OUTLAW, Role.OUTLAW);
            case 6 -> List.of(Role.SHERIFF, Role.DEPUTY, Role.RENEGADE, Role.OUTLAW, Role.OUTLAW, Role.OUTLAW);
            case 7 -> List.of(
                    Role.SHERIFF,
                    Role.DEPUTY,
                    Role.DEPUTY,
                    Role.RENEGADE,
                    Role.OUTLAW,
                    Role.OUTLAW,
                    Role.OUTLAW
            );
            default -> throw new IllegalArgumentException("Unsupported player count: " + playerCount);
        };
    }
}

