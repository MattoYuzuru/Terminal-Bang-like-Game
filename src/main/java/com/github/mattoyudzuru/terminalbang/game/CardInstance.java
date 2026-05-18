package com.github.mattoyudzuru.terminalbang.game;

import java.util.UUID;

public record CardInstance(UUID id, CardDefinition definition) {
    public static CardInstance create(CardDefinition definition) {
        return new CardInstance(UUID.randomUUID(), definition);
    }

    public CardKind kind() {
        return definition.kind();
    }

    public String name() {
        return definition.name();
    }
}

