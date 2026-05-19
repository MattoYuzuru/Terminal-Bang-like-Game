package com.github.mattoyudzuru.terminalbang.game;

import java.util.UUID;

public record CardInstance(UUID id, CardDefinition definition, CardSuit suit, CardRank rank) {
    public static CardInstance create(CardDefinition definition) {
        return new CardInstance(UUID.randomUUID(), definition, CardSuit.SPADES, CardRank.ACE);
    }

    public static CardInstance create(CardDefinition definition, CardSuit suit, CardRank rank) {
        return new CardInstance(UUID.randomUUID(), definition, suit, rank);
    }

    public CardKind kind() {
        return definition.kind();
    }

    public String name() {
        return definition.name();
    }

    public String code() {
        return rank.label() + suit.symbol();
    }
}
