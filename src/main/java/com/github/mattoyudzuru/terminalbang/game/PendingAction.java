package com.github.mattoyudzuru.terminalbang.game;

import java.util.List;
import java.util.UUID;

public record PendingAction(
        PendingActionType type,
        UUID expectedAccountId,
        UUID opponentAccountId,
        CardKind responseKind,
        int requiredResponses,
        List<UUID> remainingAccountIds,
        List<CardInstance> choiceCards,
        String prompt
) {
    public PendingAction {
        remainingAccountIds = List.copyOf(remainingAccountIds);
        choiceCards = List.copyOf(choiceCards);
    }

    public static PendingAction bangReaction(
            UUID expectedAccountId,
            UUID attackerAccountId,
            int requiredResponses,
            String prompt
    ) {
        return new PendingAction(
                PendingActionType.BANG_REACTION,
                expectedAccountId,
                attackerAccountId,
                CardKind.MISSED,
                requiredResponses,
                List.of(),
                List.of(),
                prompt
        );
    }

    public static PendingAction duelResponse(UUID expectedAccountId, UUID opponentAccountId, String prompt) {
        return new PendingAction(
                PendingActionType.DUEL_RESPONSE,
                expectedAccountId,
                opponentAccountId,
                CardKind.BANG,
                1,
                List.of(),
                List.of(),
                prompt
        );
    }

    public static PendingAction massReaction(
            PendingActionType type,
            UUID expectedAccountId,
            UUID attackerAccountId,
            CardKind responseKind,
            List<UUID> remainingAccountIds,
            String prompt
    ) {
        return new PendingAction(type, expectedAccountId, attackerAccountId, responseKind, 1, remainingAccountIds, List.of(), prompt);
    }

    public static PendingAction generalStorePick(
            UUID expectedAccountId,
            UUID actorAccountId,
            List<UUID> remainingAccountIds,
            List<CardInstance> choiceCards,
            String prompt
    ) {
        return new PendingAction(
                PendingActionType.GENERAL_STORE_PICK,
                expectedAccountId,
                actorAccountId,
                CardKind.GENERAL_STORE,
                0,
                remainingAccountIds,
                choiceCards,
                prompt
        );
    }
}
