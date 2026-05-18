package com.github.mattoyudzuru.terminalbang.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StandardContent {
    private static final Map<CardKind, CardDefinition> DEFINITIONS = Map.of(
            CardKind.SHOT,
            new CardDefinition(CardKind.SHOT, "Shot", "ATTACK", "Attack a player in range.", true, false),
            CardKind.DODGE,
            new CardDefinition(CardKind.DODGE, "Dodge", "REACTION", "Avoid one Shot.", false, true),
            CardKind.SALOON,
            new CardDefinition(CardKind.SALOON, "Saloon", "UTILITY", "Restore one health.", false, false),
            CardKind.TRAIL_RIDE,
            new CardDefinition(CardKind.TRAIL_RIDE, "Trail Ride", "UTILITY", "Draw two cards.", false, false),
            CardKind.DISARM,
            new CardDefinition(CardKind.DISARM, "Disarm", "UTILITY", "Target discards one random card.", true, false),
            CardKind.RUSTLE,
            new CardDefinition(CardKind.RUSTLE, "Rustle", "UTILITY", "Take one random card from a target in range.", true, false),
            CardKind.STANDOFF,
            new CardDefinition(CardKind.STANDOFF, "Standoff", "ATTACK", "Start a duel answered with Shot.", true, false)
    );

    private StandardContent() {
    }

    public static CardDefinition card(CardKind kind) {
        return DEFINITIONS.get(kind);
    }

    public static List<CardInstance> deck() {
        List<CardInstance> cards = new ArrayList<>();
        add(cards, CardKind.SHOT, 28);
        add(cards, CardKind.DODGE, 14);
        add(cards, CardKind.SALOON, 8);
        add(cards, CardKind.TRAIL_RIDE, 6);
        add(cards, CardKind.DISARM, 6);
        add(cards, CardKind.RUSTLE, 4);
        add(cards, CardKind.STANDOFF, 4);
        return cards;
    }

    public static List<CharacterDefinition> characters() {
        return List.of(
                new CharacterDefinition("marshal", "Marshal", 4, "Balanced starter character."),
                new CharacterDefinition("scout", "Scout", 4, "Balanced starter character."),
                new CharacterDefinition("gambler", "Gambler", 4, "Balanced starter character."),
                new CharacterDefinition("rancher", "Rancher", 4, "Balanced starter character."),
                new CharacterDefinition("doctor", "Doctor", 4, "Balanced starter character."),
                new CharacterDefinition("tracker", "Tracker", 4, "Balanced starter character."),
                new CharacterDefinition("blacksmith", "Blacksmith", 4, "Balanced starter character.")
        );
    }

    private static void add(List<CardInstance> cards, CardKind kind, int amount) {
        CardDefinition definition = card(kind);
        for (int i = 0; i < amount; i++) {
            cards.add(CardInstance.create(definition));
        }
    }
}

