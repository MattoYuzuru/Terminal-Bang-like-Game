package com.github.mattoyudzuru.terminalbang.game;

public record CardDefinition(
        CardKind kind,
        String name,
        String type,
        String description,
        boolean requiresTarget,
        boolean reactionOnly
) {
}

