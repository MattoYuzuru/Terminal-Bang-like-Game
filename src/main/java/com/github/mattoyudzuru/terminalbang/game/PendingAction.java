package com.github.mattoyudzuru.terminalbang.game;

import java.util.UUID;

public record PendingAction(
        PendingActionType type,
        UUID expectedAccountId,
        UUID opponentAccountId,
        CardKind responseKind,
        String prompt
) {
    public static PendingAction shotReaction(UUID expectedAccountId, UUID attackerAccountId, String prompt) {
        return new PendingAction(
                PendingActionType.SHOT_REACTION,
                expectedAccountId,
                attackerAccountId,
                CardKind.DODGE,
                prompt
        );
    }

    public static PendingAction standoffResponse(UUID expectedAccountId, UUID opponentAccountId, String prompt) {
        return new PendingAction(
                PendingActionType.STANDOFF_RESPONSE,
                expectedAccountId,
                opponentAccountId,
                CardKind.SHOT,
                prompt
        );
    }
}

